import io
import json
import logging
import os
import sys
from pathlib import Path
from typing import Any

import joblib
import numpy as np
import pandas as pd
import tensorflow as tf
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder, StandardScaler

from llm_client import call_deepseek_chat, is_deepseek_available

os.environ.setdefault("TF_USE_LEGACY_KERAS", "False")
os.environ.setdefault("TF_CPP_MIN_LOG_LEVEL", "2")
sys.setrecursionlimit(5000)

LOGGER = logging.getLogger(__name__)
BASE_DIR = Path(__file__).resolve().parent
MODEL_DIR = BASE_DIR / "models"
DEEPSEEK_TEXT_MODEL = "deepseek-v4-flash"


class PredictionService:
    MODEL_PATH = str(MODEL_DIR / "student_performance_model.keras")
    SCALER_PATH = str(MODEL_DIR / "scaler.save")
    ENCODERS_PATH = str(MODEL_DIR / "encoders.save")
    FEATURE_NAMES_PATH = str(MODEL_DIR / "feature_names.save")
    TARGET_COLUMN = "Exam_Score"

    @staticmethod
    def _ensure_directory_exists(path: str) -> None:
        os.makedirs(os.path.dirname(path), exist_ok=True)

    @staticmethod
    def _error(message: str) -> dict[str, Any]:
        return {"status": "error", "message": message}

    @staticmethod
    def _ensure_api_key() -> bool:
        return is_deepseek_available()

    @staticmethod
    def _call_qwen(messages: list[dict[str, str]]) -> str:
        return call_deepseek_chat(messages, timeout=20)

    @staticmethod
    def _preprocess_data(df: pd.DataFrame):
        data = df.copy()
        if PredictionService.TARGET_COLUMN not in data.columns:
            raise ValueError(f"数据集中缺少目标列 {PredictionService.TARGET_COLUMN}")

        data = data.dropna()
        x_frame = data.drop(columns=[PredictionService.TARGET_COLUMN])
        y = data[PredictionService.TARGET_COLUMN].values

        categorical_columns = x_frame.select_dtypes(include=["object", "category"]).columns
        encoders: dict[str, LabelEncoder] = {}
        for column in categorical_columns:
            encoder = LabelEncoder()
            x_frame[column] = encoder.fit_transform(x_frame[column])
            encoders[column] = encoder

        scaler = StandardScaler()
        x_scaled = scaler.fit_transform(x_frame)
        return x_scaled, y, x_frame.columns, scaler, encoders

    @staticmethod
    def _build_model(input_dim: int) -> tf.keras.Model:
        model = tf.keras.Sequential(
            [
                tf.keras.layers.Input(shape=(input_dim,)),
                tf.keras.layers.Dense(64, activation="relu"),
                tf.keras.layers.Dense(32, activation="relu"),
                tf.keras.layers.Dense(16, activation="relu"),
                tf.keras.layers.Dense(1),
            ]
        )
        model.compile(
            optimizer=tf.keras.optimizers.Adam(learning_rate=0.001),
            loss="mse",
            metrics=["mae"],
        )
        return model

    @staticmethod
    def _load_train_dataframe(file_or_path: Any) -> pd.DataFrame:
        if isinstance(file_or_path, str):
            dataframe = pd.read_csv(file_or_path)
            LOGGER.info("从路径读取训练数据，行数=%s", len(dataframe))
            return dataframe

        if hasattr(file_or_path, "seek"):
            file_or_path.seek(0)
        content = file_or_path.read()
        if isinstance(content, bytes):
            content = content.decode("utf-8")
        dataframe = pd.read_csv(io.StringIO(content))
        LOGGER.info("从上传文件读取训练数据，行数=%s", len(dataframe))
        return dataframe

    @staticmethod
    def train_model_from_csv(file_or_path):
        try:
            PredictionService._ensure_directory_exists(PredictionService.MODEL_PATH)
            dataframe = PredictionService._load_train_dataframe(file_or_path)
            x_scaled, y, feature_names, scaler, encoders = PredictionService._preprocess_data(dataframe)
            LOGGER.info("训练数据预处理完成，特征维度=%s", x_scaled.shape)

            x_train, x_test, y_train, y_test = train_test_split(
                x_scaled,
                y,
                test_size=0.2,
                random_state=42,
            )

            model = PredictionService._build_model(x_scaled.shape[1])
            model.fit(
                x_train,
                y_train,
                epochs=100,
                batch_size=32,
                validation_split=0.2,
                verbose=1,
                callbacks=[
                    tf.keras.callbacks.EarlyStopping(
                        monitor="val_loss",
                        patience=10,
                        restore_best_weights=True,
                    ),
                    tf.keras.callbacks.ReduceLROnPlateau(
                        monitor="val_loss",
                        factor=0.5,
                        patience=5,
                    ),
                ],
            )

            test_loss, test_mae = model.evaluate(x_test, y_test, verbose=1)
            LOGGER.info("模型评估完成，loss=%.4f, mae=%.4f", test_loss, test_mae)

            model.save(PredictionService.MODEL_PATH)
            joblib.dump(scaler, PredictionService.SCALER_PATH)
            joblib.dump(encoders, PredictionService.ENCODERS_PATH)
            joblib.dump(list(feature_names), PredictionService.FEATURE_NAMES_PATH)

            return {
                "status": "success",
                "message": "模型训练完成",
                "test_mae": float(test_mae),
                "test_loss": float(test_loss),
                "feature_count": int(len(feature_names)),
                "feature_names": list(feature_names),
            }
        except Exception as exc:
            LOGGER.exception("训练模型失败")
            return PredictionService._error(f"训练模型时出错: {str(exc)}")

    @staticmethod
    def predict_student_score(input_data):
        try:
            if not os.path.exists(PredictionService.MODEL_PATH):
                return PredictionService._error("模型不存在，请先训练模型")

            model = tf.keras.models.load_model(PredictionService.MODEL_PATH)
            scaler = joblib.load(PredictionService.SCALER_PATH)
            encoders = joblib.load(PredictionService.ENCODERS_PATH)
            feature_names = joblib.load(PredictionService.FEATURE_NAMES_PATH)

            input_frame = pd.DataFrame([input_data])
            for column in feature_names:
                if column not in input_frame.columns:
                    input_frame[column] = np.nan

            for column, encoder in encoders.items():
                if column not in input_frame.columns:
                    input_frame[column] = encoder.classes_[0]
                input_frame[column] = input_frame[column].fillna(encoder.classes_[0])
                input_frame[column] = input_frame[column].map(
                    lambda value: value if value in encoder.classes_ else encoder.classes_[0]
                )
                input_frame[column] = encoder.transform(input_frame[column])

            for column in feature_names:
                if column not in encoders:
                    input_frame[column] = pd.to_numeric(input_frame[column], errors="coerce").fillna(0.0)

            input_frame = input_frame[feature_names]
            input_scaled = scaler.transform(input_frame)
            prediction = model.predict(input_scaled, verbose=0)[0][0]
            prediction_float = float(prediction)
            return {
                "status": "success",
                "predicted_score": round(prediction_float, 2),
                "message": "预测成功",
            }
        except Exception as exc:
            LOGGER.exception("预测失败")
            return PredictionService._error(f"预测时出错: {str(exc)}")

    @staticmethod
    def get_model_info():
        try:
            if not os.path.exists(PredictionService.MODEL_PATH):
                return PredictionService._error("模型不存在")

            model = tf.keras.models.load_model(PredictionService.MODEL_PATH)
            encoders = joblib.load(PredictionService.ENCODERS_PATH)
            layers = [
                {
                    "name": layer.name,
                    "units": layer.units if hasattr(layer, "units") else None,
                    "activation": layer.activation.__name__ if hasattr(layer, "activation") else None,
                }
                for layer in model.layers
            ]
            return {
                "status": "success",
                "model_info": {
                    "layers": layers,
                    "categorical_features": list(encoders.keys()),
                    "input_dim": model.input_shape[1],
                },
            }
        except Exception as exc:
            LOGGER.exception("获取模型信息失败")
            return PredictionService._error(f"获取模型信息时出错: {str(exc)}")

    @staticmethod
    def predict_student_score_with_ai(input_data, business_context: str = ""):
        base_result = PredictionService.predict_student_score(input_data)
        if base_result.get("status") != "success":
            return base_result

        base_score = float(base_result.get("predicted_score") or 0.0)
        context_text = str(business_context or "").strip()
        fallback_analysis = (
            f"基础模型预测分数为 {round(base_score, 2)} 分。"
            " 当前已尝试把学生最近做题表现、学生诊断和学习规划一起纳入解释。"
            " 如果近期正确率偏低、薄弱章节集中、错题重复率高，则这个分数仍有继续下滑风险；"
            " 如果最近练习节奏稳定、重点章节正确率回升、学习规划执行更聚焦，则说明这个分数还有继续上探空间。"
        )

        if not context_text:
            return {
                "status": "success",
                "predicted_score": round(base_score, 2),
                "base_predicted_score": round(base_score, 2),
                "ai_adjusted_score": round(base_score, 2),
                "confidence": "中",
                "message": "模型+AI预测完成，当前暂无可结合的学生画像数据。",
                "ai_analysis": fallback_analysis,
                "ai_focus": [],
                "adjustment_reason": "当前只拿到了模型输入变量，尚未补充学生画像上下文。",
            }

        if not PredictionService._ensure_api_key():
            return {
                "status": "success",
                "predicted_score": round(base_score, 2),
                "base_predicted_score": round(base_score, 2),
                "ai_adjusted_score": round(base_score, 2),
                "confidence": "中",
                "message": "模型+AI预测完成（当前由规则回退生成解读）。",
                "ai_analysis": fallback_analysis,
                "ai_focus": [],
                "adjustment_reason": "当前未配置在线大模型，先沿用基础模型分数。",
            }

        prompt = [
            {
                "role": "system",
                "content": (
                    "你是学生成绩预测分析助手。"
                    "你需要把机器学习模型预测分数与学生真实学习画像结合起来，形成最终的模型+AI预测成绩。"
                    "不能脱离基础模型乱改分数，最终分数相对基础模型的改动必须控制在 ±8 分以内。"
                    "请只输出 JSON，格式固定为："
                    "{\"final_score\":0,\"confidence\":\"高/中/低\",\"summary\":\"\",\"analysis\":\"\",\"focus\":[\"\",\"\",\"\"],\"adjustment_reason\":\"\"}"
                    "其中 final_score 为最终分数，summary 为一句话结论，analysis 为详细解读，focus 为 3 条提分重点。"
                ),
            },
            {
                "role": "user",
                "content": (
                    f"基础模型预测分数：{round(base_score, 2)}\n"
                    f"本次输入变量：{input_data}\n"
                    f"学生学习画像：\n{context_text}"
                ),
            },
        ]

        try:
            answer = PredictionService._call_qwen(prompt)
            if not answer:
                raise ValueError("empty ai analysis")
            start = answer.find("{")
            end = answer.rfind("}")
            payload = answer[start:end + 1] if start >= 0 and end > start else answer
            parsed = json.loads(payload)
            final_score = float(parsed.get("final_score", base_score) or base_score)
            final_score = max(base_score - 8, min(base_score + 8, final_score))
            focus_items = parsed.get("focus") if isinstance(parsed.get("focus"), list) else []
            return {
                "status": "success",
                "predicted_score": round(final_score, 2),
                "base_predicted_score": round(base_score, 2),
                "ai_adjusted_score": round(final_score, 2),
                "confidence": str(parsed.get("confidence", "中")),
                "message": str(parsed.get("summary") or "模型+AI预测完成"),
                "ai_analysis": str(parsed.get("analysis") or fallback_analysis),
                "ai_focus": [str(item) for item in focus_items[:3]],
                "adjustment_reason": str(parsed.get("adjustment_reason") or ""),
            }
        except Exception:
            LOGGER.exception("AI增强成绩预测失败，回退到基础模型结果")
            return {
                "status": "success",
                "predicted_score": round(base_score, 2),
                "base_predicted_score": round(base_score, 2),
                "ai_adjusted_score": round(base_score, 2),
                "confidence": "中",
                "message": "模型+AI预测完成（当前由规则回退生成解读）。",
                "ai_analysis": fallback_analysis,
                "ai_focus": [],
                "adjustment_reason": "AI 解读生成失败，本次先沿用基础模型分数。",
            }
