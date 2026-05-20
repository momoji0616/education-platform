from __future__ import annotations

from pathlib import Path
import re

import pandas as pd
import pymysql


BASE_DIR = Path(__file__).resolve().parent
OUTPUT_PATH = BASE_DIR / "student_course_structured_kb.xlsx"

DB_CONFIG = {
    "host": "localhost",
    "user": "root",
    "password": "123456",
    "database": "ry-vue",
    "charset": "utf8mb4",
}


COURSE_SOURCES = {
    "数据结构": (
        "https://jwba.ucas.ac.cn/sc/course/courseplan/195087",
        "高校课程大纲常见结构，覆盖绪论、线性表、栈队列、串、树、图、查找、排序",
    ),
    "C语言程序设计": (
        "https://www.htu.edu.cn/teaching/2010/0727/c3256a45275/page.htm",
        "课程大纲常见结构，覆盖基础语法、三种控制结构、数组、函数、指针、结构体、文件",
    ),
    "Python程序设计": (
        "https://tcfc.ouc.edu.cn/_t450/2023/1007/c7264a444079/page.htm",
        "课程大纲常见结构，覆盖基础语法、数据类型、控制流、函数、文件、面向对象、GUI、数据库、网络",
    ),
    "数据库系统": (
        "https://jwc.sues.edu.cn/_t18/52/fc/c879a217852/page.htm",
        "数据库课程常见教学结构，覆盖系统原理、关系模型、SQL、设计、事务并发、恢复、安全、应用开发",
    ),
    "Linux操作系统": (
        "https://cet.hunau.edu.cn/info/1146/4363.htm",
        "Linux课程常见教学结构，覆盖命令、文件系统、用户权限、Shell、进程、网络、服务与运维",
    ),
    "华为ICT-人工智能": (
        "https://e.huawei.com/cn/ict-academy/curriculum",
        "结合华为ICT学院课程方向与题库模块，归纳人工智能基础、机器学习、深度学习与工程实践",
    ),
    "华为ICT-云计算与大数据": (
        "https://e.huawei.com/cn/ict-academy/curriculum",
        "结合华为ICT学院课程方向与题库模块，归纳云计算基础、虚拟化、存储网络、大数据组件与运维",
    ),
    "计算机基础与Office": (
        "https://ncre.neea.edu.cn/res/Home/2412/9ee6dd725a956e088c694df720528cbd.pdf",
        "全国计算机等级考试一级计算机基础及MS Office应用考试大纲（2025年版）",
    ),
    "大学物理": (
        "https://higher.smartedu.cn/course/6786ebcd225d72705e5dc145",
        "大学物理常见教学结构，覆盖力学、振动波动、热学、电磁学、光学与近代物理",
    ),
    "中国近现代史纲要": (
        "https://mks.ccut.edu.cn/info/1033/2151.htm",
        "课程大纲常见结构，覆盖近代中国救亡图存、新民主主义革命、新中国建设与改革开放",
    ),
    "软件项目管理": (
        "https://ccs.snnu.edu.cn/__local/F/8E/29/C72F468045BE844C0E171BB7354_47B409D0_3B55BD.pdf",
        "课程大纲常见结构，覆盖立项、范围、进度、成本、质量、风险、沟通与收尾",
    ),
    "VB程序设计": (
        "https://jwc.nyist.edu.cn/info/1041/2879.htm",
        "VB程序设计常见教学结构，覆盖输入输出、选择、循环、数组、过程、文件、控件与事件",
    ),
    "大学英语与翻译": (
        "https://felc.gdufs.edu.cn/info/1930/7479.htm",
        "大学英语课程常见结构，覆盖词汇语法、篇章理解、写作、翻译、阅读策略与语境应用",
    ),
}


COURSE_RULES = [
    ("数据结构", ["数据结构"]),
    ("C语言程序设计", ["C语言", "C程序设计", "赵敏C", "程序设计"]),
    ("Python程序设计", ["PYTHON", "Python"]),
    ("数据库系统", ["数据库"]),
    ("Linux操作系统", ["linux", "Linux"]),
    ("华为ICT-人工智能", ["华为ICT-AI"]),
    ("华为ICT-云计算与大数据", ["华为ICT-云计算&大数据"]),
    ("计算机基础与Office", ["access二级", "大基", "全国二级公共", "问卷大基"]),
    ("大学物理", ["大学物理"]),
    ("中国近现代史纲要", ["近代史"]),
    ("软件项目管理", ["软件项目管理"]),
    ("VB程序设计", ["VB programming"]),
    ("大学英语与翻译", ["综合英语", "高级英语阅读", "专业阅读", "大英6级词汇"]),
]


MODULE_LIBRARY = {
    "数据结构": {
        "数据结构绪论": ("绪论", ["数据结构基本概念", "逻辑结构与存储结构", "算法特性与复杂度", "抽象数据类型", "时间复杂度与空间复杂度"]),
        "线性表": ("线性表", ["顺序表与链表定义", "顺序存储与链式存储", "插入删除操作", "单链表循环链表双向链表", "线性表应用"]),
        "栈和队列": ("栈和队列", ["栈与队列定义", "顺序栈与链栈", "循环队列", "栈与递归", "队列应用"]),
        "串和广义表": ("串和广义表", ["字符串存储结构", "模式匹配基础", "朴素匹配与KMP思想", "广义表定义", "串的基本操作"]),
        "树": ("树", ["树与二叉树定义", "二叉树性质", "二叉树遍历", "线索二叉树", "二叉排序树与平衡思想", "哈夫曼树"]),
        "图": ("图", ["图的定义与存储", "深度优先搜索", "广度优先搜索", "最小生成树", "最短路径", "拓扑排序与关键路径"]),
        "查找": ("查找", ["顺序查找", "折半查找", "分块查找", "哈希表原理", "查找性能分析"]),
        "排序": ("排序", ["插入排序", "交换排序", "选择排序", "归并排序", "快速排序", "堆排序", "排序稳定性"]),
    },
    "C语言程序设计": {
        "C语言概述": ("C语言概述", ["C语言特点", "程序结构", "标识符与关键字", "编译预处理基础", "输入输出函数"]),
        "数据类型与表达式": ("变量，常量2", ["常量与变量", "基本数据类型", "运算符优先级", "类型转换", "表达式求值"]),
        "顺序结构": ("顺序结构", ["顺序结构流程", "格式化输入输出", "表达式与语句", "数学库函数", "简单程序设计"]),
        "选择结构": ("选择结构", ["if语句", "switch语句", "条件表达式", "多分支选择", "选择结构易错点"]),
        "循环结构": ("循环结构", ["while循环", "do while循环", "for循环", "break与continue", "循环嵌套"]),
        "数组": ("数组", ["一维数组", "二维数组", "字符数组", "数组与字符串处理", "数组常见算法"]),
        "函数": ("函数", ["函数定义与调用", "参数传递", "函数递归", "变量作用域", "编译预处理与宏"]),
        "指针": ("指针", ["指针定义与运算", "指针与数组", "指针与函数", "字符串指针", "动态内存基础"]),
        "结构体与链表": ("结构体，链表", ["结构体定义", "结构体数组与指针", "链表创建与遍历", "链表插入删除", "共用体与枚举"]),
        "文件": ("文件", ["文件指针", "文件打开关闭", "文本文件与二进制文件", "文件读写函数", "文件型编程"]),
    },
    "Python程序设计": {
        "Python基础": ("Python基础", ["程序输入输出", "标识符与注释", "保留字", "缩进规则", "常用标准库入门"]),
        "基本语法": ("基本语法", ["变量与赋值", "表达式", "分支与缩进", "异常基础", "模块导入"]),
        "数据类型": ("数据类型", ["数字类型", "字符串", "列表元组集合字典", "切片与推导式", "类型转换"]),
        "控制结构": ("控制结构", ["if条件语句", "for循环", "while循环", "break continue pass", "迭代与范围对象"]),
        "函数": ("函数", ["函数定义", "参数类型", "返回值", "匿名函数", "递归与作用域"]),
        "文件": ("文件", ["文件打开关闭", "读写模式", "with语句", "路径处理", "文本与二进制文件"]),
        "面向对象": ("面向对象的程序", ["类与对象", "构造方法", "封装继承多态", "类属性与实例属性", "特殊方法"]),
        "图形界面与绘图": ("图形化界面设计", ["Tkinter基础", "控件布局", "事件绑定", "Canvas绘图", "简单GUI程序"]),
        "数据库操作": ("数据库操作", ["数据库连接", "SQL基本操作", "游标与事务", "数据增删改查", "Python与数据库集成"]),
        "网络与多媒体": ("网络通讯与音频处理", ["Socket基础", "HTTP请求", "网络编程流程", "多媒体文件处理", "第三方库调用"]),
    },
    "数据库系统": {
        "数据库概论": ("数据库系统基本原理", ["数据库系统组成", "数据模型", "三级模式两级映像", "数据库管理系统功能", "数据独立性"]),
        "关系数据库": ("数据库技术的发展", ["关系模型基础", "关系代数基础", "关系完整性", "数据库发展阶段", "数据库应用领域"]),
        "SQL基础": ("sqlserver 2008数据库基础", ["数据库与表操作", "SELECT查询", "连接查询", "分组统计", "视图与索引基础"]),
        "Access基础": ("access 2010数据库基础", ["Access对象体系", "表设计", "查询设计", "窗体报表基础", "数据类型与主键"]),
        "数据库设计": ("access 2010 数据库设计实例", ["需求分析", "E-R模型", "关系模式转换", "规范化", "数据库设计实例"]),
        "网络数据库开发": ("网络数据库开发基础", ["B/S数据库应用", "数据库连接技术", "参数化查询", "表单与数据交互", "数据库安全基础"]),
        "事务与安全": ("未分类", ["事务特性ACID", "并发控制", "封锁与调度", "数据库恢复", "用户权限与安全管理"]),
    },
    "Linux操作系统": {
        "Linux基础": ("未分类", ["Linux系统结构", "常用命令", "目录与文件管理", "帮助命令与软件安装", "vi/vim基础"]),
        "文件系统与权限": ("文件系统", ["绝对路径与相对路径", "文件权限表示", "chmod chown", "链接文件", "磁盘与挂载基础"]),
        "用户与进程管理": ("进程管理", ["用户与用户组", "进程查看", "前台后台任务", "信号与进程控制", "计划任务"]),
        "Shell编程": ("shell", ["Shell变量", "条件判断", "循环语句", "位置参数", "Shell脚本调试"]),
        "网络与服务": ("网络", ["网络配置", "远程连接", "常见网络命令", "服务管理", "日志排查"]),
    },
    "华为ICT-人工智能": {
        "人工智能基础": ("人工智能基础", ["人工智能发展与应用", "机器学习基本流程", "监督学习与非监督学习", "训练集验证集测试集", "评价指标基础"]),
        "数据预处理与监督学习": ("监督&预处理", ["数据清洗", "特征工程", "归一化与标准化", "分类与回归任务", "常见监督学习算法"]),
        "算法模型": ("算法模型", ["线性模型", "决策树", "聚类基础", "模型训练与评估", "过拟合与欠拟合"]),
        "深度学习基础": ("深度学习基础", ["神经网络基本结构", "激活函数", "损失函数", "反向传播思想", "深度学习应用"]),
        "前馈网络": ("前馈网络", ["感知机", "多层前馈网络", "全连接层", "前向传播", "BP网络"]),
        "卷积与循环网络": ("卷积&循环网络", ["卷积神经网络", "池化层", "循环神经网络", "序列建模", "典型应用场景"]),
        "编程框架": ("编程框架", ["AI开发框架", "数据加载流程", "模型搭建", "训练与推理", "实验管理"]),
        "优化": ("优化", ["梯度下降", "学习率", "正则化", "Batch Normalization思想", "模型调参"]),
    },
    "华为ICT-云计算与大数据": {
        "云计算基础": ("基础", ["云计算服务模式", "IaaS PaaS SaaS", "资源池化", "弹性伸缩", "云平台架构"]),
        "虚拟化": ("虚拟化", ["虚拟化原理", "计算虚拟化", "存储虚拟化", "网络虚拟化", "虚拟机管理"]),
        "存储": ("存储", ["块存储文件存储对象存储", "RAID基础", "分布式存储", "数据备份", "容灾基础"]),
        "网络": ("网络", ["数据中心网络", "虚拟网络", "网络隔离", "负载均衡", "云网络安全"]),
        "Hadoop": ("Hadoop", ["HDFS", "MapReduce", "YARN", "Hadoop生态", "大数据处理流程"]),
        "HBase": ("HBase", ["列族模型", "Region基础", "读写流程", "HBase与HDFS关系", "适用场景"]),
        "运维": ("运维", ["监控告警", "日志管理", "容量规划", "故障排查", "自动化运维"]),
        "调度": ("调度", ["任务调度原理", "工作流编排", "资源调度", "作业依赖", "调度优化"]),
        "数据科学": ("数据科学-二级考", ["数据采集", "数据清洗", "统计分析基础", "可视化基础", "数据驱动决策"]),
    },
    "计算机基础与Office": {
        "计算机基础": ("基础", ["计算机系统组成", "信息表示", "操作系统基础", "网络基础", "信息安全基础"]),
        "Word": ("word", ["文档编辑", "样式与页面设置", "表格处理", "图文混排", "邮件合并"]),
        "Excel": ("excel", ["工作表操作", "公式与函数", "数据排序筛选", "图表制作", "数据透视表"]),
        "PowerPoint": ("ppt", ["演示文稿设计", "主题版式", "动画切换", "多媒体插入", "放映与输出"]),
        "Access数据库": ("数据库", ["数据库基础", "表查询窗体报表", "主键与关系", "宏与VBA基础", "综合应用"]),
        "操作系统": ("操作系统", ["Windows基础操作", "文件管理", "系统设置", "设备管理", "常见维护"]),
        "多媒体": ("多媒体", ["媒体类型", "音视频基础", "图像压缩", "多媒体编辑", "数字媒体应用"]),
        "网络基础": ("网络", ["网络概念", "Internet服务", "网页与浏览器", "网络安全常识", "常见网络故障"]),
        "Access二级专项": ("查询", ["查询设计", "SQL视图", "参数查询", "操作查询", "查询应用"]),
        "Access窗体报表": ("窗体", ["窗体设计", "控件使用", "报表设计", "数据绑定", "事件响应"]),
        "AccessVBA与宏": ("VBA", ["VBA语法基础", "过程与函数", "宏对象", "事件过程", "自动化处理"]),
    },
    "大学物理": {
        "力学基础": ("大学物理上", ["质点运动学", "牛顿定律", "功和能", "动量与角动量", "刚体转动"]),
        "振动与波": ("振动与波动", ["简谐振动", "阻尼与受迫振动", "机械波传播", "波的叠加", "驻波与多普勒效应"]),
        "热学": ("热学", ["温度与内能", "热力学第一定律", "热力学第二定律", "理想气体模型", "熵的概念"]),
        "电磁学": ("电磁学", ["静电场", "高斯定理", "电势与电容", "磁场与安培定律", "电磁感应"]),
        "光学与近代物理": ("大学物理下", ["干涉衍射偏振", "几何光学基础", "相对论初步", "量子论基础", "原子物理基础"]),
    },
    "中国近现代史纲要": {
        "近代中国的开端": ("???", ["鸦片战争", "半殖民地半封建社会", "列强侵略", "民族危机", "早期救亡图存"]),
        "旧民主主义革命": ("辛亥革命", ["太平天国运动", "洋务运动", "戊戌变法", "义和团运动", "辛亥革命"]),
        "新民主主义革命": ("新民主主义革命", ["五四运动", "中国共产党成立", "国共合作", "抗日战争", "解放战争"]),
        "新中国建设与探索": ("社会主义建设", ["新中国成立", "社会主义改造", "社会主义建设道路探索", "曲折发展", "历史经验"]),
        "改革开放与新时代": ("改革开放", ["改革开放起步", "社会主义市场经济", "现代化建设", "中国特色社会主义", "新时代发展"]),
    },
    "软件项目管理": {
        "项目管理概论": ("未分类", ["项目与项目管理", "项目生命周期", "干系人识别", "组织结构", "项目经理职责"]),
        "范围与进度管理": ("进度管理", ["需求与范围定义", "WBS", "网络图", "关键路径法", "进度控制"]),
        "成本与质量管理": ("成本管理", ["成本估算", "成本预算", "挣值分析", "质量计划", "质量保证与质量控制"]),
        "风险与沟通管理": ("风险管理", ["风险识别", "风险评估", "风险应对", "沟通计划", "冲突协调"]),
        "配置与收尾": ("配置管理", ["配置管理", "变更控制", "采购与合同", "项目验收", "项目总结"]),
    },
    "VB程序设计": {
        "Visual Basic基础": ("Visual Basic, Controls, and Events", ["VB开发环境", "窗体与控件", "事件驱动机制", "常用控件属性", "简单交互程序"]),
        "变量输入输出": ("Variables, Input, and Output", ["变量声明", "数据类型", "输入输出函数", "表达式计算", "格式化显示"]),
        "选择结构": ("Decisions", ["If语句", "Select Case", "逻辑表达式", "多分支决策", "条件判断"]),
        "循环结构": ("Repetition", ["For循环", "Do循环", "循环嵌套", "计数与累加", "循环控制"]),
        "数组": ("Arrays", ["一维数组", "二维数组", "动态数组", "数组遍历", "数组应用"]),
        "过程": ("General Procedures", ["Sub过程", "Function函数", "参数传递", "作用域", "模块化程序设计"]),
        "文件": ("Text Files", ["文本文件读写", "顺序文件", "文件打开关闭", "文件处理流程", "数据持久化"]),
        "高级控件": ("Additional Controls and Objects", ["菜单与对话框", "定时器", "图片与列表控件", "对象操作", "综合界面设计"]),
    },
    "大学英语与翻译": {
        "词汇与语法": ("未分类", ["高频词汇辨析", "固定搭配", "时态语态", "从句与非谓语", "语境选词"]),
        "阅读理解": ("高级英语阅读", ["主旨大意", "细节定位", "推理判断", "篇章结构分析", "长难句理解"]),
        "综合英语": ("综合英语", ["课文主题理解", "词汇应用", "语法与写作", "篇章衔接", "课堂任务表达"]),
        "中译英": ("中译英", ["句法转换", "词语搭配", "时态与语态处理", "增译减译", "语篇连贯"]),
        "英译中": ("英译中", ["长句拆分", "定语从句处理", "被动语态转换", "术语理解", "语义通顺表达"]),
        "六级词汇": ("大英6级词汇", ["高频词汇", "熟词僻义", "词根词缀", "近义词辨析", "搭配记忆"]),
    },
}


def normalize_course(course_name: str) -> str:
    for category, keys in COURSE_RULES:
        if any(key.lower() in course_name.lower() for key in keys):
            return category
    return "计算机基础与Office" if course_name == "未分类" else "大学英语与翻译"


def canonical_to_aliases(category: str) -> dict[str, list[str]]:
    library = MODULE_LIBRARY[category]
    aliases: dict[str, list[str]] = {}
    for canonical, (source_module, _) in library.items():
        aliases[canonical] = [canonical.lower()]
        if source_module not in {"未分类", "???", ""}:
            aliases[canonical].append(source_module.lower())
    if category == "大学英语与翻译":
        aliases["综合英语"].extend(["综合英语3", "综合英语4", "quiz"])
        aliases["中译英"].append("中译英")
        aliases["英译中"].append("英译中")
    return aliases


def infer_canonical_modules(category: str, course_name: str, module_name: str, sample_text: str) -> list[str]:
    module_lower = module_name.lower()
    sample_lower = sample_text.lower()
    for canonical, values in canonical_to_aliases(category).items():
        if any(alias in module_lower for alias in values):
            return [canonical]
    if module_name in {"未分类", "???", ""}:
        if category == "Linux操作系统":
            return ["Linux基础", "文件系统与权限", "用户与进程管理", "Shell编程", "网络与服务"]
        if category == "软件项目管理":
            return ["项目管理概论", "范围与进度管理", "成本与质量管理", "风险与沟通管理", "配置与收尾"]
        if category == "中国近现代史纲要":
            return ["近代中国的开端", "旧民主主义革命", "新民主主义革命", "新中国建设与探索", "改革开放与新时代"]
        if category == "大学物理":
            if "上" in course_name:
                return ["力学基础", "振动与波"]
            return ["热学", "电磁学", "光学与近代物理"]
        if category == "数据库系统" and "数据库-二工大" in course_name:
            return ["数据库概论", "关系数据库", "SQL基础", "事务与安全"]
        if category == "大学英语与翻译":
            if "大英6级词汇" in course_name:
                return ["六级词汇"]
            if "高级英语阅读" in course_name:
                return ["阅读理解"]
            if "综合英语" in course_name:
                return ["综合英语", "词汇与语法"]
            if "中译英" in course_name:
                return ["中译英"]
            if "英译中" in course_name:
                return ["英译中"]
            return ["词汇与语法"]
    if category == "Linux操作系统":
        if any(key in sample_lower for key in ["chmod", "chown", "权限", "目录", "文件"]):
            return ["文件系统与权限"]
        if any(key in sample_lower for key in ["shell", "bash", "脚本"]):
            return ["Shell编程"]
        if any(key in sample_lower for key in ["进程", "ps", "kill", "用户", "cron"]):
            return ["用户与进程管理"]
        if any(key in sample_lower for key in ["ip", "网络", "ssh", "端口", "服务"]):
            return ["网络与服务"]
        return ["Linux基础"]
    if category == "数据库系统":
        if "access" in sample_lower:
            return ["Access基础"]
        if any(key in sample_lower for key in ["select", "sql", "where", "group by", "join"]):
            return ["SQL基础"]
        if any(key in sample_lower for key in ["事务", "并发", "恢复", "安全"]):
            return ["事务与安全"]
        return ["数据库概论"]
    if category == "软件项目管理":
        if any(key in sample_lower for key in ["风险", "沟通", "干系人"]):
            return ["风险与沟通管理"]
        if any(key in sample_lower for key in ["成本", "质量", "挣值"]):
            return ["成本与质量管理"]
        if any(key in sample_lower for key in ["wbs", "进度", "关键路径", "范围"]):
            return ["范围与进度管理"]
        if any(key in sample_lower for key in ["配置", "变更", "验收", "收尾"]):
            return ["配置与收尾"]
        return ["项目管理概论"]
    if category == "大学物理":
        if any(key in sample_lower for key in ["电场", "磁场", "电势", "电流", "电磁"]):
            return ["电磁学"]
        if any(key in sample_lower for key in ["光", "干涉", "衍射", "量子", "原子"]):
            return ["光学与近代物理"]
        if any(key in sample_lower for key in ["热", "气体", "熵", "热力学"]):
            return ["热学"]
        if any(key in sample_lower for key in ["振动", "波", "频率"]):
            return ["振动与波"]
        return ["力学基础"]
    if category == "中国近现代史纲要":
        if any(key in sample_lower for key in ["鸦片", "洋务", "戊戌", "辛亥"]):
            return ["旧民主主义革命"]
        if any(key in sample_lower for key in ["五四", "抗日", "解放", "中共"]):
            return ["新民主主义革命"]
        if any(key in sample_lower for key in ["改革开放", "现代化", "新时代"]):
            return ["改革开放与新时代"]
        if any(key in sample_lower for key in ["建国", "改造", "社会主义建设"]):
            return ["新中国建设与探索"]
        return ["近代中国的开端"]
    if category == "大学英语与翻译":
        if "中译英" in course_name or "中译英" in module_name or "中译英" in sample_text:
            return ["中译英"]
        if "英译中" in course_name or "英译中" in module_name or "英译中" in sample_text:
            return ["英译中"]
        if "高级英语阅读" in course_name or "高级英语阅读" in module_name or "阅读" in module_name:
            return ["阅读理解"]
        if "综合英语" in course_name or "综合英语" in module_name or "quiz" in module_lower:
            return ["综合英语", "词汇与语法"]
        if "6级词汇" in course_name or "6级词汇" in module_name or "词汇" in sample_text:
            return ["六级词汇"]
        return ["词汇与语法"]
    return [next(iter(MODULE_LIBRARY[category]))]


def explain_point(category: str, module: str, point: str) -> str:
    return f"该知识点属于{category}中的“{module}”模块，RAG命中后应优先围绕“概念定义、典型考法、易错点、解题步骤”展开，帮助学生把题目与本模块核心知识建立直接关联。"


def build_hint(category: str, module: str, point: str) -> str:
    return f"当学生在“{module}”模块内提问时，优先解释“{point}”与当前题目的关系，再补充该模块常见陷阱和同类题判断方法。"


def fetch_question_bank() -> pd.DataFrame:
    conn = pymysql.connect(**DB_CONFIG)
    try:
        sql = """
        select ifnull(nullif(course_name,''),'未分类') as course_name,
               ifnull(nullif(chapter_name,''),'未分类') as module_name,
               ifnull(question_stem,'') as question_stem,
               count(*) as question_count
        from edu_question_bank
        where status='0'
        group by 1,2,3
        """
        return pd.read_sql(sql, conn)
    finally:
        conn.close()


def main() -> None:
    df = fetch_question_bank()
    grouped = (
        df.groupby(["course_name", "module_name"], as_index=False)
        .agg(question_count=("question_count", "sum"), sample_text=("question_stem", lambda x: " ".join(x.astype(str).head(8))))
    )
    detail_rows: list[dict[str, object]] = []
    overview_rows: list[dict[str, object]] = []
    for row in grouped.to_dict("records"):
        course_name = str(row["course_name"]).strip()
        module_name = str(row["module_name"]).strip()
        category = normalize_course(course_name)
        canonical_modules = infer_canonical_modules(category, course_name, module_name, str(row["sample_text"]))
        source_url, source_note = COURSE_SOURCES[category]
        for canonical_module in canonical_modules:
            _, points = MODULE_LIBRARY[category][canonical_module]
            keywords = [course_name, module_name, canonical_module, *points[:3]]
            overview_rows.append(
                {
                    "课程类别": category,
                    "系统课程名称": course_name,
                    "系统模块名称": module_name,
                    "规范模块名称": canonical_module,
                    "题目数量": int(row["question_count"]),
                    "模块来源依据": source_note,
                }
            )
            for index, point in enumerate(points, start=1):
                detail_rows.append(
                    {
                        "课程类别": category,
                        "系统课程名称": course_name,
                        "系统模块名称": module_name,
                        "规范模块名称": canonical_module,
                        "知识点序号": index,
                        "知识点": point,
                        "知识点解析": explain_point(category, canonical_module, point),
                        "个性化回答提示": build_hint(category, canonical_module, point),
                        "检索关键词": "；".join(dict.fromkeys([item for item in keywords if item and item != "未分类"])),
                        "参考来源": source_url,
                        "来源说明": source_note,
                    }
                )
    detail_df = pd.DataFrame(detail_rows).sort_values(["课程类别", "系统课程名称", "系统模块名称", "知识点序号"])
    overview_df = pd.DataFrame(overview_rows).drop_duplicates().sort_values(["课程类别", "系统课程名称", "系统模块名称"])
    with pd.ExcelWriter(OUTPUT_PATH, engine="openpyxl") as writer:
        detail_df.to_excel(writer, sheet_name="知识库明细", index=False)
        overview_df.to_excel(writer, sheet_name="课程模块总览", index=False)
    print(f"generated: {OUTPUT_PATH}")
    print(f"detail_rows={len(detail_df)} overview_rows={len(overview_df)}")


if __name__ == "__main__":
    main()
