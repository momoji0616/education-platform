from __future__ import annotations

from datetime import datetime
from pathlib import Path
from typing import Iterable

import pandas as pd


BASE_DIR = Path(__file__).resolve().parent
KB_PATH = BASE_DIR / "student_course_structured_kb.xlsx"
OUTPUT_PATH = BASE_DIR / "student_course_structured_kb_enhanced.xlsx"


DETAIL_SHEET = "知识库明细"
OVERVIEW_SHEET = "课程模块总览"
RULE_SHEET = "命中规则总览"


def rule_items(*items: str) -> list[str]:
    return [item.strip() for item in items if item and item.strip()]


SPECIAL_RULES: dict[tuple[str, str], list[dict[str, object]]] = {
    ("华为ICT-人工智能", "人工智能基础"): [
        {"knowledge_point": "人工智能三要素", "aliases": rule_items("人工智能", "感知", "决策", "执行", "智能体")},
        {"knowledge_point": "机器学习基本概念", "aliases": rule_items("机器学习", "训练集", "测试集", "特征", "标签")},
        {"knowledge_point": "监督学习与无监督学习", "aliases": rule_items("监督学习", "无监督学习", "分类", "聚类")},
        {"knowledge_point": "张量基本概念", "aliases": rule_items("张量", "tensor", "维度", "形状", "rank")},
    ],
    ("华为ICT-人工智能", "深度学习基础"): [
        {"knowledge_point": "张量与维度表示", "aliases": rule_items("张量", "tensor", "维度", "shape", "秩")},
        {"knowledge_point": "计算图与前向传播", "aliases": rule_items("计算图", "前向传播", "forward", "节点", "边")},
        {"knowledge_point": "损失函数", "aliases": rule_items("损失函数", "loss", "交叉熵", "均方误差")},
        {"knowledge_point": "反向传播", "aliases": rule_items("反向传播", "backpropagation", "梯度", "链式法则")},
        {"knowledge_point": "激活函数", "aliases": rule_items("激活函数", "relu", "sigmoid", "tanh", "softmax")},
    ],
    ("华为ICT-人工智能", "卷积与循环网络"): [
        {"knowledge_point": "卷积核与特征图", "aliases": rule_items("卷积核", "卷积", "特征图", "feature map")},
        {"knowledge_point": "池化层", "aliases": rule_items("池化", "最大池化", "平均池化")},
        {"knowledge_point": "LSTM与GRU", "aliases": rule_items("LSTM", "GRU", "门控", "长短期记忆")},
    ],
    ("华为ICT-人工智能", "优化"): [
        {"knowledge_point": "梯度下降法", "aliases": rule_items("梯度下降", "gradient descent", "更新参数")},
        {"knowledge_point": "学习率", "aliases": rule_items("学习率", "learning rate", "步长")},
        {"knowledge_point": "过拟合与正则化", "aliases": rule_items("过拟合", "正则化", "dropout", "L1", "L2")},
    ],
    ("数据结构", "树"): [
        {"knowledge_point": "树与二叉树定义", "aliases": rule_items("树", "二叉树", "孩子结点", "双亲结点")},
        {"knowledge_point": "二叉树性质", "aliases": rule_items("层次", "叶子结点", "满二叉树", "完全二叉树")},
        {"knowledge_point": "二叉树遍历", "aliases": rule_items("前序", "中序", "后序", "层序")},
        {"knowledge_point": "二叉排序树", "aliases": rule_items("二叉排序树", "BST", "查找树")},
        {"knowledge_point": "哈夫曼树", "aliases": rule_items("哈夫曼树", "WPL", "带权路径长度")},
    ],
    ("数据结构", "图"): [
        {"knowledge_point": "图的存储结构", "aliases": rule_items("邻接矩阵", "邻接表", "顶点", "边")},
        {"knowledge_point": "图的遍历", "aliases": rule_items("DFS", "BFS", "深度优先", "广度优先")},
        {"knowledge_point": "最小生成树与最短路径", "aliases": rule_items("最小生成树", "Prim", "Kruskal", "Dijkstra")},
    ],
    ("数据结构", "排序"): [
        {"knowledge_point": "插入排序与冒泡排序", "aliases": rule_items("插入排序", "冒泡排序", "稳定")},
        {"knowledge_point": "快速排序与堆排序", "aliases": rule_items("快速排序", "堆排序", "分治", "不稳定")},
        {"knowledge_point": "归并排序", "aliases": rule_items("归并排序", "分而治之", "稳定排序")},
    ],
    ("数据库系统", "SQL基础"): [
        {"knowledge_point": "SELECT查询", "aliases": rule_items("SELECT", "FROM", "WHERE")},
        {"knowledge_point": "连接查询", "aliases": rule_items("JOIN", "INNER JOIN", "LEFT JOIN")},
        {"knowledge_point": "分组统计", "aliases": rule_items("GROUP BY", "HAVING", "COUNT", "SUM", "AVG")},
        {"knowledge_point": "排序与子查询", "aliases": rule_items("ORDER BY", "子查询", "EXISTS", "IN")},
    ],
    ("数据库系统", "关系数据库"): [
        {"knowledge_point": "关系代数基本运算", "aliases": rule_items("选择", "投影", "连接", "并", "差")},
        {"knowledge_point": "主键外键完整性", "aliases": rule_items("主键", "外键", "实体完整性", "参照完整性")},
        {"knowledge_point": "函数依赖与范式", "aliases": rule_items("函数依赖", "1NF", "2NF", "3NF", "BCNF")},
    ],
    ("数据库系统", "事务与安全"): [
        {"knowledge_point": "事务特性ACID", "aliases": rule_items("事务", "ACID", "原子性", "一致性", "隔离性", "持久性")},
        {"knowledge_point": "并发控制与封锁", "aliases": rule_items("并发控制", "封锁", "死锁", "隔离级别")},
    ],
    ("Python程序设计", "面向对象"): [
        {"knowledge_point": "类与对象", "aliases": rule_items("类", "对象", "实例", "instance")},
        {"knowledge_point": "构造方法", "aliases": rule_items("__init__", "构造方法", "初始化")},
        {"knowledge_point": "封装继承多态", "aliases": rule_items("封装", "继承", "多态", "super")},
        {"knowledge_point": "实例属性与类属性", "aliases": rule_items("实例属性", "类属性", "self", "cls")},
        {"knowledge_point": "魔术方法", "aliases": rule_items("__str__", "__repr__", "__len__", "特殊方法")},
    ],
    ("Python程序设计", "函数"): [
        {"knowledge_point": "函数定义与调用", "aliases": rule_items("def", "return", "参数", "实参", "形参")},
        {"knowledge_point": "匿名函数与作用域", "aliases": rule_items("lambda", "作用域", "global", "nonlocal")},
    ],
    ("Python程序设计", "数据类型"): [
        {"knowledge_point": "列表元组字典集合", "aliases": rule_items("list", "tuple", "dict", "set")},
        {"knowledge_point": "可变对象与不可变对象", "aliases": rule_items("可变对象", "不可变对象", "引用", "拷贝")},
    ],
    ("C语言程序设计", "指针"): [
        {"knowledge_point": "指针与地址", "aliases": rule_items("指针", "地址", "&", "*")},
        {"knowledge_point": "指针与数组关系", "aliases": rule_items("指针", "数组", "指针运算")},
    ],
    ("C语言程序设计", "数组"): [
        {"knowledge_point": "一维数组与二维数组", "aliases": rule_items("数组", "一维数组", "二维数组", "下标")},
        {"knowledge_point": "字符串数组", "aliases": rule_items("字符串", "字符数组", "gets", "puts")},
    ],
    ("Linux操作系统", "文件系统与权限"): [
        {"knowledge_point": "文件权限表示", "aliases": rule_items("rwx", "chmod", "chown", "umask")},
        {"knowledge_point": "链接文件", "aliases": rule_items("硬链接", "软链接", "ln -s")},
    ],
    ("Linux操作系统", "Shell编程"): [
        {"knowledge_point": "变量与参数", "aliases": rule_items("Shell", "$1", "$?", "变量", "export")},
        {"knowledge_point": "条件与循环", "aliases": rule_items("if", "case", "for", "while", "test")},
    ],
    ("计算机基础与Office", "Excel"): [
        {"knowledge_point": "常用函数", "aliases": rule_items("SUM", "AVERAGE", "IF", "VLOOKUP", "COUNTIF")},
        {"knowledge_point": "排序筛选与数据透视", "aliases": rule_items("排序", "筛选", "数据透视表", "图表")},
    ],
}

MODULE_PROFILES: dict[tuple[str, str], dict[str, str]] = {
    ("华为ICT-人工智能", "人工智能基础"): {
        "reason": "通常错在没有分清概念定义、任务类型和应用场景，把机器学习、深度学习、人工智能三个层级混为一谈。",
        "pitfall": "容易把监督学习和无监督学习混淆，或把“张量/特征/标签”这类术语按生活化含义去理解。",
        "review": "先画出概念关系图，再按“定义-输入-输出-典型场景”复习每个核心术语。",
        "example": "适合出“概念判断、术语匹配、场景归类”类选择题。",
    },
    ("华为ICT-人工智能", "深度学习基础"): {
        "reason": "通常错在没把张量、计算图、损失函数和反向传播串成同一条训练链路来理解。",
        "pitfall": "容易只记名词，不会判断维度变化、损失函数作用以及梯度更新方向。",
        "review": "按“输入张量-前向传播-损失计算-反向传播-参数更新”顺序复习。",
        "example": "适合出“张量维度、激活函数、损失函数、反向传播”类题目。",
    },
    ("华为ICT-人工智能", "卷积与循环网络"): {
        "reason": "通常错在没有把卷积网络处理空间特征、循环网络处理序列特征区分开。",
        "pitfall": "卷积核、特征图、池化、步长、RNN/LSTM/GRU 的作用范围容易串题。",
        "review": "分别整理 CNN 与 RNN 的输入类型、关键结构、典型场景和易混概念。",
        "example": "适合出“结构作用、场景匹配、参数含义”类题目。",
    },
    ("华为ICT-人工智能", "优化"): {
        "reason": "通常错在不会从训练目标角度理解梯度下降、学习率和正则化。",
        "pitfall": "学习率过大过小、过拟合欠拟合、BN 与 Dropout 的作用经常混淆。",
        "review": "围绕“如何更快更稳地让损失下降”来复习优化相关概念。",
        "example": "适合出“优化策略、现象判断、调参方向”类题目。",
    },
    ("数据结构", "树"): {
        "reason": "通常错在没有把树的定义、性质和遍历结果放到同一张结构图里理解。",
        "pitfall": "叶子结点、度、层次、完全二叉树、满二叉树、遍历序列特别容易混淆。",
        "review": "先画图再判断，所有遍历题都按“访问根结点的先后顺序”复盘。",
        "example": "适合出“二叉树性质、遍历序列、哈夫曼树计算”类题目。",
    },
    ("数据结构", "图"): {
        "reason": "容易把图的存储结构、遍历算法和路径算法当成孤立知识点去记。",
        "pitfall": "邻接矩阵/邻接表、DFS/BFS、最小生成树/最短路径是最常见串题点。",
        "review": "按“存储-遍历-路径-应用”四层结构复习图。",
        "example": "适合出“遍历顺序、算法适用条件、路径长度判断”类题目。",
    },
    ("数据结构", "排序"): {
        "reason": "常见问题是只记过程，不会比较时间复杂度、稳定性和适用场景。",
        "pitfall": "快速排序、堆排序、归并排序的比较题特别容易出错。",
        "review": "建立排序算法对比表：思想、最好/最坏复杂度、稳定性、空间开销。",
        "example": "适合出“算法比较、复杂度判断、执行过程”类题目。",
    },
    ("数据库系统", "SQL基础"): {
        "reason": "通常错在没有按照 SQL 执行逻辑理解查询语句，而是只凭语感选答案。",
        "pitfall": "WHERE 与 HAVING、连接条件、分组统计、子查询返回结果最容易出错。",
        "review": "按“FROM/JOIN -> WHERE -> GROUP BY -> HAVING -> SELECT -> ORDER BY”顺序复习。",
        "example": "适合出“查询结果判断、语句补全、聚合与分组”类题目。",
    },
    ("数据库系统", "关系数据库"): {
        "reason": "容易把主键、外键、函数依赖、范式当成背诵题，导致做题不会迁移。",
        "pitfall": "候选键与主键、部分依赖与传递依赖、完整性约束高频混淆。",
        "review": "先从表结构判断依赖，再逐步判断范式和约束是否满足。",
        "example": "适合出“键的判断、范式分析、关系代数”类题目。",
    },
    ("Python程序设计", "面向对象"): {
        "reason": "通常错在只记住类和对象的定义，没有把属性、方法、继承关系真正跑通。",
        "pitfall": "self/cls、实例属性/类属性、重写/重载、魔术方法最容易混淆。",
        "review": "先画类图，再用最小示例代码验证属性访问和方法调用过程。",
        "example": "适合出“代码结果判断、属性归属、继承调用”类题目。",
    },
    ("Python程序设计", "函数"): {
        "reason": "容易把形参实参、返回值、局部变量作用域混在一起。",
        "pitfall": "默认参数、可变参数、lambda、闭包是常见易错点。",
        "review": "用几段短代码专门练习参数传递和作用域变化。",
        "example": "适合出“调用结果、作用域判断、参数匹配”类题目。",
    },
    ("C语言程序设计", "指针"): {
        "reason": "通常错在地址、值、指针变量三者关系没有彻底分清。",
        "pitfall": "取地址、解引用、指针运算、数组名退化成指针是高频陷阱。",
        "review": "每做一道题都先写出变量类型、存储内容和地址关系。",
        "example": "适合出“表达式求值、内存关系、函数参数传递”类题目。",
    },
    ("Linux操作系统", "文件系统与权限"): {
        "reason": "通常错在不会把权限位和具体用户身份对应起来判断。",
        "pitfall": "rwx 含义、chmod 数字权限、硬链接软链接经常混淆。",
        "review": "先分清属主、属组、其他用户，再逐位判断权限。",
        "example": "适合出“命令结果、权限判断、文件关系”类题目。",
    },
    ("Linux操作系统", "Shell编程"): {
        "reason": "容易把 Shell 语法当自然语言理解，没有按脚本执行顺序推导。",
        "pitfall": "变量展开、条件测试、循环语法和特殊参数经常出错。",
        "review": "把脚本逐行展开，重点记住变量、判断和循环模板。",
        "example": "适合出“脚本输出、语法纠错、命令组合”类题目。",
    },
    ("计算机基础与Office", "Excel"): {
        "reason": "容易只记函数名字，不会根据题目场景选择函数和参数。",
        "pitfall": "IF、VLOOKUP、COUNTIF、绝对引用、数据透视表是常见失分点。",
        "review": "把函数用途、参数顺序、典型场景配对复习。",
        "example": "适合出“函数结果、公式填空、数据处理操作”类题目。",
    },
}


DEFAULT_PROFILE = {
    "reason": "通常错在没有把题目放回当前课程模块的核心概念和常见考法里去理解，只记了零散结论。",
    "pitfall": "容易把相近概念、关键词和解题步骤混淆，导致看起来懂但选项判断不稳。",
    "review": "建议按“概念定义-典型考法-判断依据-同类变式”四步复习。",
    "example": "适合补做本模块的基础判断题、概念辨析题和一到两道变式练习题。",
}


PRIORITY_HIGH = "高"
PRIORITY_MEDIUM = "中"


def normalize_text(value: object) -> str:
    if pd.isna(value):
        return ""
    return str(value).strip()


def split_terms(text: str) -> list[str]:
    cleaned = (
        text.replace("（", " ")
        .replace("）", " ")
        .replace("(", " ")
        .replace(")", " ")
        .replace("、", " ")
        .replace("；", " ")
        .replace("，", " ")
        .replace("/", " ")
        .replace("-", " ")
        .replace("_", " ")
        .replace("&", " ")
    )
    return [item for item in cleaned.split() if item]


def dedupe_keep_order(items: Iterable[str]) -> list[str]:
    result: list[str] = []
    seen: set[str] = set()
    for item in items:
        token = item.strip()
        if not token:
            continue
        if token in seen:
            continue
        seen.add(token)
        result.append(token)
    return result


def get_profile(course_category: str, standard_module: str) -> dict[str, str]:
    return MODULE_PROFILES.get((course_category, standard_module), DEFAULT_PROFILE)


def build_aliases(course_category: str, system_course: str, system_module: str, standard_module: str, knowledge_point: str) -> list[str]:
    aliases = dedupe_keep_order(
        [
            knowledge_point,
            standard_module,
            system_module,
            *split_terms(knowledge_point),
            *split_terms(standard_module),
            *split_terms(system_module),
            course_category,
            system_course,
        ]
    )
    return aliases[:16]


def build_rule_text(course_category: str, system_course: str, system_module: str, standard_module: str, knowledge_point: str, aliases: list[str]) -> str:
    return "；".join(
        [
            f"课程优先命中：{system_course or course_category}",
            f"模块优先命中：{system_module or standard_module} / {standard_module}",
            f"知识点优先命中：{knowledge_point}",
            f"题干关键词：{'、'.join(aliases[:8])}",
        ]
    )


def build_reason_bundle(course_category: str, standard_module: str, knowledge_point: str) -> dict[str, str]:
    profile = get_profile(course_category, standard_module)
    return {
        "错误原因": f"{profile['reason']} 当前题如果落在“{knowledge_point}”上，往往说明学生没有抓住该知识点的判断依据。",
        "易错点": f"{profile['pitfall']} 做到“{knowledge_point}”时要重点防止概念张冠李戴、关键词误判和步骤漏看。",
        "复习建议": f"{profile['review']} 建议围绕“{knowledge_point}”再补 2 到 3 道同类题，确认自己能说清为什么选、为什么不选。",
        "例题提示": f"{profile['example']} 如果再遇到“{knowledge_point}”相关题，先抓题干关键词，再回到模块主线去判断。",
        "讲题模板": (
            f"这题考的是“{knowledge_point}”。先点出它属于“{standard_module}”模块，再说明正确选项为什么对，"
            "最后补一句本题最容易混淆的干扰点和下次遇到时的判断口诀。"
        ),
    }


def enrich_base_rows(detail_df: pd.DataFrame) -> pd.DataFrame:
    df = detail_df.copy()
    extra_columns = [
        "命中规则",
        "题干关键词",
        "模块锚点",
        "知识点别名",
        "优先级",
        "错误原因",
        "易错点",
        "复习建议",
        "例题提示",
        "讲题模板",
    ]
    for column in extra_columns:
        if column not in df.columns:
            df[column] = ""

    for idx, row in df.iterrows():
        course_category = normalize_text(row["课程类别"])
        system_course = normalize_text(row["系统课程名称"])
        system_module = normalize_text(row["系统模块名称"])
        standard_module = normalize_text(row["规范模块名称"])
        knowledge_point = normalize_text(row["知识点"])
        aliases = build_aliases(course_category, system_course, system_module, standard_module, knowledge_point)
        bundle = build_reason_bundle(course_category, standard_module, knowledge_point)

        df.at[idx, "命中规则"] = build_rule_text(course_category, system_course, system_module, standard_module, knowledge_point, aliases)
        df.at[idx, "题干关键词"] = "；".join(aliases[:12])
        df.at[idx, "模块锚点"] = f"{system_course}::{system_module}::{standard_module}"
        df.at[idx, "知识点别名"] = "；".join(aliases)
        df.at[idx, "优先级"] = PRIORITY_MEDIUM
        for key, value in bundle.items():
            df.at[idx, key] = value
    return df


def append_specialized_rows(detail_df: pd.DataFrame) -> pd.DataFrame:
    extra_rows: list[dict[str, object]] = []
    grouped = detail_df.groupby(["课程类别", "系统课程名称", "系统模块名称", "规范模块名称"], dropna=False)
    for (course_category, system_course, system_module, standard_module), group in grouped:
        rules = SPECIAL_RULES.get((str(course_category), str(standard_module)), [])
        if not rules:
            continue
        source_row = group.iloc[0].to_dict()
        base_index = int(group["知识点序号"].max()) if "知识点序号" in group.columns else len(group)
        for offset, item in enumerate(rules, start=1):
            knowledge_point = str(item["knowledge_point"])
            aliases = dedupe_keep_order([knowledge_point, *item.get("aliases", [])])
            row = dict(source_row)
            row["知识点序号"] = base_index + offset
            row["知识点"] = knowledge_point
            row["知识点解析"] = (
                f"该知识点属于{course_category}中的“{standard_module}”模块。"
                f"当题干出现“{'、'.join(aliases[:6])}”等关键词时，应优先从该知识点解释定义、考法、判断依据和易错点。"
            )
            row["个性化回答提示"] = (
                f"当学生在“{standard_module}”模块提问且题干涉及“{'、'.join(aliases[:6])}”时，"
                "先明确它在本模块中的位置，再结合题目解释为什么选、为什么不选。"
            )
            row["检索关键词"] = "；".join(dedupe_keep_order([str(system_course), str(system_module), str(standard_module), *aliases]))
            row["参考来源"] = normalize_text(source_row.get("参考来源", ""))
            row["来源说明"] = normalize_text(source_row.get("来源说明", ""))
            row["命中规则"] = build_rule_text(str(course_category), str(system_course), str(system_module), str(standard_module), knowledge_point, aliases)
            row["题干关键词"] = "；".join(aliases[:12])
            row["模块锚点"] = f"{system_course}::{system_module}::{standard_module}"
            row["知识点别名"] = "；".join(aliases)
            row["优先级"] = PRIORITY_HIGH
            row.update(build_reason_bundle(str(course_category), str(standard_module), knowledge_point))
            extra_rows.append(row)

    if not extra_rows:
        return detail_df
    return pd.concat([detail_df, pd.DataFrame(extra_rows)], ignore_index=True)


def rebuild_overview(detail_df: pd.DataFrame, overview_df: pd.DataFrame) -> pd.DataFrame:
    counts = (
        detail_df.groupby(["课程类别", "系统课程名称", "系统模块名称", "规范模块名称"], dropna=False)
        .size()
        .reset_index(name="知识点数量")
    )
    merged = overview_df.merge(
        counts,
        on=["课程类别", "系统课程名称", "系统模块名称", "规范模块名称"],
        how="left",
    )
    merged["知识点数量"] = merged["知识点数量"].fillna(0).astype(int)
    return merged


def build_rule_overview(detail_df: pd.DataFrame) -> pd.DataFrame:
    summary = (
        detail_df.groupby(["课程类别", "规范模块名称", "优先级"], dropna=False)
        .agg(
            知识点数量=("知识点", "count"),
            关键词示例=("题干关键词", lambda s: " | ".join([normalize_text(item) for item in s.head(2)])),
        )
        .reset_index()
        .sort_values(["课程类别", "规范模块名称", "优先级"])
        .reset_index(drop=True)
    )
    return summary


def main() -> None:
    detail_df = pd.read_excel(KB_PATH, sheet_name=0)
    overview_df = pd.read_excel(KB_PATH, sheet_name=1)

    detail_df = enrich_base_rows(detail_df)
    detail_df = append_specialized_rows(detail_df)
    detail_df = detail_df.sort_values(
        ["课程类别", "系统课程名称", "系统模块名称", "规范模块名称", "知识点序号", "优先级"],
        ascending=[True, True, True, True, True, False],
    ).reset_index(drop=True)
    overview_df = rebuild_overview(detail_df, overview_df)
    rule_overview_df = build_rule_overview(detail_df)

    output_path = OUTPUT_PATH
    try:
        writer = pd.ExcelWriter(output_path, engine="openpyxl")
    except PermissionError:
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        output_path = BASE_DIR / f"student_course_structured_kb_enhanced_{timestamp}.xlsx"
        writer = pd.ExcelWriter(output_path, engine="openpyxl")

    with writer:
        detail_df.to_excel(writer, sheet_name=DETAIL_SHEET, index=False)
        overview_df.to_excel(writer, sheet_name=OVERVIEW_SHEET, index=False)
        rule_overview_df.to_excel(writer, sheet_name=RULE_SHEET, index=False)

    print(f"enhanced: {output_path}")
    print(f"detail_rows={len(detail_df)} overview_rows={len(overview_df)} rule_rows={len(rule_overview_df)}")


if __name__ == "__main__":
    main()
