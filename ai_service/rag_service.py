import os
import pandas as pd
import chromadb
import dashscope  # 通义千问SDK
from datetime import datetime
from sqlalchemy.orm import Session
from database import SessionLocal, engine, Base
from models import RagDataset
# 初始化本地向量数据库（数据永久保存）
chroma_client = chromadb.PersistentClient(path="./chroma_db")
# 使用ChromaDB内置的嵌入函数，无需下载外部模型
collection = chroma_client.get_or_create_collection(name="excel_data")

# 创建数据库表（如果不存在）
Base.metadata.create_all(bind=engine)

# 获取数据库会话的依赖函数
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# 配置通义千问API密钥（需替换为你的密钥）
dashscope.api_key = "sk-34169b02217a4c9fa10d5c6a176d4063"  # 关键：这里需要填写你的密钥

class RAGService:
    @staticmethod
    def process_excel(file_path):
        """解析Excel并存入向量数据库和MySQL数据库"""
        # 解析Excel文件
        df = pd.read_excel(file_path)
        
        # 生成唯一的文件标识符
        file_name = os.path.basename(file_path)
        timestamp = int(datetime.now().timestamp())
        file_identifier = f"{file_name}_{timestamp}"
        
        # 存入向量数据库
        texts, ids = [], []
        for idx, row in df.iterrows():
            text = ", ".join([f"{col}：{row[col]}" for col in df.columns])
            texts.append(text)
            ids.append(f"{file_identifier}_id_{idx}")  # 唯一ID：文件名_时间戳_行索引
        # 使用ChromaDB内置的嵌入函数
        collection.add(documents=texts, ids=ids)
        
        # 存入MySQL数据库
        try:
            db = SessionLocal()
            file_name = os.path.basename(file_path)
            file_size = os.path.getsize(file_path) / 1024  # KB
            
            # 创建数据集记录
            dataset = RagDataset(
                file_name=file_name,
                file_path=file_path,
                upload_time=datetime.now(),
                file_size=file_size,
                is_deleted="0"  # 0表示未删除，1表示已删除
            )
            db.add(dataset)
            db.commit()
            db.refresh(dataset)
            
            return f"成功导入 {len(texts)} 条数据，数据集ID：{dataset.id}"
        except Exception as e:
            return f"导入数据成功，但数据库记录失败：{str(e)}"
        finally:
            db.close()

    @staticmethod
    def query_answer(question):
        """步骤1：从本地向量数据库检索相关上下文"""
        results = collection.query(
            query_texts=[question],
            n_results=100  # 取最相关的3条数据
        )
        context = "\n".join(results["documents"][0])  # 拼接上下文（关键：补充这一步）
        
        """步骤2：调用通义千问API生成回答"""
        response = dashscope.Generation.call(
            model="qwen-turbo",  # 通义千问的轻量模型（免费额度足够测试）
            messages=[
                {"role": "system", "content": "请根据提供的已知信息回答问题，不要编造内容。"},
                {"role": "user", "content": f"已知信息：{context}\n问题：{question}"}
            ]
        )
        # 提取回答（处理API返回格式）
        if response.status_code == 200:
            answer = response.output["text"].strip()
        else:
            answer = f"API调用失败：{response.message}"
        return answer
    
    @staticmethod
    def get_datasets():
        """获取所有未删除的数据集"""
        try:
            db = SessionLocal()
            datasets = db.query(RagDataset).filter(RagDataset.is_deleted == "0").all()
            return [{
                "id": dataset.id,
                "file_name": dataset.file_name,
                "file_path": dataset.file_path,
                "upload_time": dataset.upload_time.strftime("%Y-%m-%d %H:%M:%S"),
                "file_size": dataset.file_size
            } for dataset in datasets]
        except Exception as e:
            return {"error": str(e)}
        finally:
            db.close()
    
    @staticmethod
    def delete_dataset(dataset_id):
        """删除数据集（软删除）"""
        try:
            db = SessionLocal()
            dataset = db.query(RagDataset).filter(RagDataset.id == dataset_id).first()
            if not dataset:
                return {"error": "数据集不存在"}
            
            # 软删除
            dataset.is_deleted = "1"
            db.commit()
            return {"message": "数据集删除成功"}
        except Exception as e:
            db.rollback()
            return {"error": str(e)}
        finally:
            db.close()
    
    @staticmethod
    def get_dataset_detail(dataset_id):
        """获取数据集详情"""
        try:
            db = SessionLocal()
            dataset = db.query(RagDataset).filter(
                RagDataset.id == dataset_id,
                RagDataset.is_deleted == "0"
            ).first()
            
            if not dataset:
                return {"error": "数据集不存在或已删除"}
            
            # 读取Excel文件内容
            df = pd.read_excel(dataset.file_path)
            data = df.to_dict('records')
            
            return {
                "id": dataset.id,
                "file_name": dataset.file_name,
                "file_path": dataset.file_path,
                "upload_time": dataset.upload_time.strftime("%Y-%m-%d %H:%M:%S"),
                "file_size": dataset.file_size,
                "data": data
            }
        except Exception as e:
            return {"error": str(e)}
        finally:
            db.close()