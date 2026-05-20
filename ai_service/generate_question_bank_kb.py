from __future__ import annotations

from collections import Counter
from dataclasses import dataclass
from pathlib import Path
import re

import pandas as pd
import pymysql


BASE_DIR = Path(__file__).resolve().parent
OUTPUT_MASTER = BASE_DIR / "education_question_bank_kb_master.xlsx"
OUTPUT_COVERAGE = BASE_DIR / "education_question_bank_kb_coverage.xlsx"
OUTPUT_PARTITIONS_DIR = BASE_DIR / "education_question_bank_kb_partitions"
TEMPLATE_PATH = BASE_DIR / "education_knowledge_base.xlsx"

DB_CONFIG = {
    "host": "localhost",
    "user": "root",
    "password": "123456",
    "database": "ry-vue",
    "charset": "utf8mb4",
}


QUESTION_TYPE_LABELS = {
    "choice": "选择题",
    "judge": "判断题",
    "blank": "填空题",
    "program": "程序题",
    "short_answer": "简答题",
    "essay": "论述题",
}

STOPWORDS = {
    "下列",
    "关于",
    "对于",
    "其中",
    "属于",
    "不是",
    "一个",
    "什么",
    "如何",
    "以下",
    "可以",
    "需要",
    "进行",
    "采用",
    "具有",
    "若有",
    "已知",
    "根据",
    "说法",
    "正确",
    "错误",
    "题目",
    "模块",
    "课程",
    "未分类",
    "计算机",
    "数据",
    "程序",
    "系统",
}


@dataclass
class ModuleInfo:
    course_name: str
    chapter_name: str
    question_count: int
    type_counter: Counter
    knowledge_points: list[str]
    sample_rows: list[dict]


def normalize_text(value: object, default: str = "未分类") -> str:
    text = str(value or "").strip()
    return text if text else default


def trim_text(value: object, limit: int = 120) -> str:
    text = re.sub(r"\s+", " ", str(value or "").strip())
    if len(text) <= limit:
      return text
    return text[: limit - 1] + "…"


def sanitize_file_part(value: str) -> str:
    return re.sub(r'[\\/:*?"<>|]+', "_", value).strip() or "unnamed"


def clean_knowledge_point(value: object) -> str:
    text = str(value or "").strip()
    if not text:
        return ""
    if text in {"[]", "nan", "None"}:
        return ""
    if "[" in text and "]" in text:
        return ""
    if re.fullmatch(r"[\d,\s.-]+", text):
        return ""
    return text


def tokenize_keywords(*parts: str) -> list[str]:
    tokens: list[str] = []
    for part in parts:
        text = str(part or "").strip()
        if not text:
            continue
        tokens.append(text)
        tokens.extend(re.findall(r"[A-Za-z0-9#+_.-]+", text))
    deduped: list[str] = []
    seen = set()
    for token in tokens:
        key = token.lower()
        if key in seen or len(token) < 2:
            continue
        seen.add(key)
        deduped.append(token)
    return deduped[:18]


def question_type_label(question_type: str) -> str:
    return QUESTION_TYPE_LABELS.get(question_type, question_type or "未分类题型")


def answer_strategy(type_counter: Counter) -> str:
    top_types = [question_type_label(item[0]) for item in type_counter.most_common(2)]
    if not top_types:
        return "建议先定位题干关键词，再按概念定义、条件限制、排除干扰项的顺序作答。"
    if "选择题" in top_types:
        return "本模块以选择题为主，建议优先圈出题干关键词，先判断考查概念，再用排除法处理干扰项。"
    if "判断题" in top_types:
        return "本模块含较多判断题，建议重点核对概念边界、适用条件和例外情况，避免绝对化判断。"
    if "程序题" in top_types:
        return "本模块含程序题，建议按“题意拆分-步骤设计-边界验证-结果复核”的顺序作答。"
    return f"本模块以{'、'.join(top_types)}为主，建议围绕核心概念、典型条件和易混点组织答案。"


def build_module_summary(module: ModuleInfo) -> str:
    type_text = "、".join(
        f"{question_type_label(question_type)}{count}题"
        for question_type, count in module.type_counter.most_common(3)
    )
    kp_text = "、".join(module.knowledge_points[:6]) if module.knowledge_points else "当前题库中该模块的知识点字段较少，可优先围绕模块名和典型题干进行检索"
    sample_stems = [trim_text(item["question_stem"], 54) for item in module.sample_rows[:3]]
    stem_text = "；".join(sample_stems) if sample_stems else "暂无代表性题干摘录"
    return (
        f"课程：{module.course_name}；模块：{module.chapter_name}。"
        f"当前题库中本模块共收录 {module.question_count} 道题，主要题型为：{type_text or '未分类'}。"
        f"优先检索标签：{kp_text}。"
        f"学生在提问本模块时，可优先从定义辨析、条件判断、易错项排除和典型例题迁移四个角度组织回答。"
        f"代表性题干示例：{stem_text}。"
    )


def build_strategy_detail(module: ModuleInfo) -> str:
    points = module.knowledge_points[:5]
    point_text = "、".join(points) if points else f"{module.chapter_name}基础概念、常见题型、核心步骤"
    return (
        f"适用于课程“{module.course_name}”下的“{module.chapter_name}”模块。"
        f"{answer_strategy(module.type_counter)}"
        f"若学生追问“为什么选这个答案”，回答时建议依次给出：1. 本题考查的核心知识点；2. 该选项成立的直接依据；"
        f"3. 其他选项为何不成立或不完整；4. 若是易错题，再补一条记忆口诀或判别标准。"
        f"本模块建议重点覆盖：{point_text}。"
    )


def build_examples_detail(module: ModuleInfo) -> str:
    chunks: list[str] = []
    for index, row in enumerate(module.sample_rows[:3], start=1):
        stem = trim_text(row["question_stem"], 88)
        answer = trim_text(row["standard_answer"], 36)
        analysis = trim_text(row["analysis"], 90)
        if analysis and analysis != "暂无现成解析":
            detail = f"示例{index}：题干“{stem}”；参考答案：{answer}；题库解析摘录：{analysis}。"
        else:
            detail = f"示例{index}：题干“{stem}”；参考答案：{answer}；回答时可先解释该答案对应的定义、原理或适用条件，再说明干扰项为何错误。"
        chunks.append(detail)
    return " ".join(chunks) if chunks else f"{module.course_name}-{module.chapter_name} 当前暂无可摘录样题。"


def build_common_mistakes_detail(module: ModuleInfo) -> str:
    stems = [trim_text(item["question_stem"], 26) for item in module.sample_rows[:5]]
    stem_text = "、".join(stems[:4]) if stems else "典型概念辨析题"
    return (
        f"本模块常见误区包括：只记结论不看题干限定条件、把相近概念混为一谈、遇到英文缩写或专业术语时直接猜测、"
        f"对计算/推理步骤缺少中间校验。若学生提问内容与“{stem_text}”相近，回答时应明确指出判题依据，"
        f"并补充一条“看到什么关键词就优先想到什么知识点”的提示。"
    )


def fetch_question_bank() -> pd.DataFrame:
    conn = pymysql.connect(**DB_CONFIG)
    try:
        sql = """
        select id,
               ifnull(nullif(course_name, ''), '未分类') as course_name,
               ifnull(nullif(chapter_name, ''), '未分类') as chapter_name,
               ifnull(nullif(question_type, ''), '未分类') as question_type,
               ifnull(nullif(knowledge_point, ''), '') as knowledge_point,
               ifnull(nullif(question_stem, ''), '暂无题干') as question_stem,
               ifnull(nullif(standard_answer, ''), '暂无答案') as standard_answer,
               ifnull(nullif(analysis, ''), '暂无现成解析') as analysis
        from edu_question_bank
        where status = '0'
        order by course_name asc, chapter_name asc, id asc
        """
        return pd.read_sql(sql, conn)
    finally:
        conn.close()


def build_modules(df: pd.DataFrame) -> list[ModuleInfo]:
    modules: list[ModuleInfo] = []
    grouped = df.groupby(["course_name", "chapter_name"], sort=True)
    for (course_name, chapter_name), group in grouped:
        kp_counter = Counter(
            clean_knowledge_point(item)
            for item in group["knowledge_point"].tolist()
            if clean_knowledge_point(item)
        )
        sample_rows = (
            group[["question_stem", "standard_answer", "analysis", "knowledge_point", "question_type"]]
            .head(5)
            .to_dict("records")
        )
        modules.append(
            ModuleInfo(
                course_name=course_name,
                chapter_name=chapter_name,
                question_count=len(group),
                type_counter=Counter(group["question_type"].tolist()),
                knowledge_points=[item for item, _ in kp_counter.most_common(8)],
                sample_rows=sample_rows,
            )
        )
    return modules


def build_template_rows() -> list[dict[str, str]]:
    if not TEMPLATE_PATH.exists():
        return []
    df = pd.read_excel(TEMPLATE_PATH)
    return df.fillna("").to_dict("records")


def generate_rows(modules: list[ModuleInfo], template_rows: list[dict[str, str]]) -> list[dict[str, str]]:
    rows: list[dict[str, str]] = []
    for item in template_rows:
        rows.append(
            {
                "知识ID": str(item.get("知识ID", "")).strip(),
                "规则适用条件": str(item.get("规则适用条件", "")).strip(),
                "知识类别": str(item.get("知识类别", "")).strip(),
                "知识详细内容": str(item.get("知识详细内容", "")).strip(),
                "检索关键词": str(item.get("检索关键词", "")).strip(),
            }
        )

    next_id = len(rows) + 1

    def push_row(condition: str, category: str, detail: str, keywords: list[str]) -> None:
        nonlocal next_id
        rows.append(
            {
                "知识ID": f"QBKB{next_id:04d}",
                "规则适用条件": condition,
                "知识类别": category,
                "知识详细内容": detail,
                "检索关键词": "、".join(keywords),
            }
        )
        next_id += 1

    global_rules = [
        (
            "学生端智能刷题推荐 / RAG 问答 / 学业诊断",
            "回答风格规范",
            "当学生提问具体题目时，优先按“题目考查点-正确答案-解析-易错项辨析-复习建议”的结构回答；当学生提问课程模块时，优先按“模块画像-常见考法-答题策略-下一步练习建议”的结构回答。",
            ["智能刷题", "RAG问答", "学业诊断", "正确答案", "解析", "复习建议"],
        ),
        (
            "学生端智能刷题推荐 / 推荐练习清单",
            "个性化推荐说明",
            "若问题来自推荐练习清单，回答时要结合课程、模块、题型和学生易错点来解释“为什么推荐这道题”，避免脱离当前模块泛泛作答。",
            ["推荐练习清单", "为什么推荐", "课程", "模块", "题型", "易错点"],
        ),
    ]
    for condition, category, detail, keywords in global_rules:
        push_row(condition, category, detail, keywords)

    for module in modules:
        base_condition = f"课程={module.course_name}；模块={module.chapter_name}"
        base_keywords = tokenize_keywords(
            module.course_name,
            module.chapter_name,
            "智能刷题推荐",
            "RAG问答",
            *module.knowledge_points[:5],
            *(item["question_stem"] for item in module.sample_rows[:2]),
        )
        push_row(
            condition=base_condition,
            category="模块知识画像",
            detail=build_module_summary(module),
            keywords=base_keywords,
        )
        push_row(
            condition=f"{base_condition}；用于题目解析/追问",
            category="答题策略与解析框架",
            detail=build_strategy_detail(module),
            keywords=tokenize_keywords(module.course_name, module.chapter_name, "题目解析", "答题步骤", *module.knowledge_points[:3]),
        )
        push_row(
            condition=f"{base_condition}；用于示例题解释",
            category="典型题示例",
            detail=build_examples_detail(module),
            keywords=tokenize_keywords(module.course_name, module.chapter_name, "例题", *(item["question_stem"] for item in module.sample_rows[:3])),
        )
        push_row(
            condition=f"{base_condition}；用于错题复盘/学业诊断",
            category="常见易错点",
            detail=build_common_mistakes_detail(module),
            keywords=tokenize_keywords(module.course_name, module.chapter_name, "错题", "易错点", *module.knowledge_points[:3]),
        )

    return rows


def build_coverage_df(modules: list[ModuleInfo]) -> pd.DataFrame:
    coverage_rows = []
    for module in modules:
        coverage_rows.append(
            {
                "课程": module.course_name,
                "模块": module.chapter_name,
                "题目数量": module.question_count,
                "主要题型": "、".join(
                    f"{question_type_label(question_type)}({count})"
                    for question_type, count in module.type_counter.most_common(3)
                ),
                "知识点标签": "、".join(module.knowledge_points[:8]),
                "样题示例": "；".join(trim_text(item["question_stem"], 36) for item in module.sample_rows[:2]),
            }
        )
    return pd.DataFrame(coverage_rows)


def write_course_partitions(kb_df: pd.DataFrame) -> None:
    OUTPUT_PARTITIONS_DIR.mkdir(parents=True, exist_ok=True)
    for old_file in OUTPUT_PARTITIONS_DIR.glob("*.xlsx"):
        old_file.unlink()

    course_condition = kb_df["规则适用条件"].astype(str)
    course_names = sorted(
        {
            match.group(1)
            for value in course_condition
            for match in [re.search(r"课程=([^；]+)", value)]
            if match and match.group(1)
        }
    )

    for course_name in course_names:
        course_rows = kb_df[
            kb_df["规则适用条件"].astype(str).str.contains(f"课程={re.escape(course_name)}", regex=True, na=False)
        ]
        if course_rows.empty:
            continue
        target = OUTPUT_PARTITIONS_DIR / f"{sanitize_file_part(course_name)}_kb.xlsx"
        course_rows.to_excel(target, index=False)


def main() -> None:
    df = fetch_question_bank()
    modules = build_modules(df)
    template_rows = build_template_rows()
    kb_rows = generate_rows(modules, template_rows)

    kb_df = pd.DataFrame(kb_rows, columns=["知识ID", "规则适用条件", "知识类别", "知识详细内容", "检索关键词"])
    coverage_df = build_coverage_df(modules)

    kb_df.to_excel(OUTPUT_MASTER, index=False)
    coverage_df.to_excel(OUTPUT_COVERAGE, index=False)
    write_course_partitions(kb_df)

    print(f"题库知识库已生成：{OUTPUT_MASTER}")
    print(f"覆盖清单已生成：{OUTPUT_COVERAGE}")
    print(f"课程分区目录：{OUTPUT_PARTITIONS_DIR}")
    print(f"原模板行数：{len(template_rows)}")
    print(f"模块数量：{len(modules)}")
    print(f"知识库总行数：{len(kb_df)}")


if __name__ == "__main__":
    main()
