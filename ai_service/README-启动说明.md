# RAG 服务启动说明

## 问题解决

### 问题 1：找不到 main 模块
**错误信息：** `ERROR: Error loading ASGI app. Could not import module "main".`

**原因：** 在项目根目录运行 uvicorn，但 `main.py` 在 `ai_service` 目录下。

**解决方法：**
```powershell
# 方法 1：切换到 ai_service 目录运行（推荐）
cd E:\tensorflow_education\tensorflow_education\ai_service
uvicorn main:app --reload --port 8000

# 方法 2：从根目录运行，指定模块路径
cd E:\tensorflow_education\tensorflow_education
uvicorn ai_service.main:app --reload --port 8000

# 方法 3：使用启动脚本
.\ai_service\start_rag.bat
# 或
.\ai_service\start_rag.ps1
```

### 问题 2：sqlite3 版本过低
**错误信息：** `RuntimeError: Your system has an unsupported version of sqlite3. Chroma requires sqlite3 >= 3.35.0.`

**原因：** Python 3.8 自带的 sqlite3 版本较旧，不满足 ChromaDB 的要求。

**解决方法：**

#### 方法 1：升级 Python 版本（推荐）
安装 Python 3.9+ 或 Python 3.10+，这些版本包含更新的 sqlite3。

#### 方法 2：使用 pysqlite3-binary（临时方案）
```powershell
pip install pysqlite3-binary
```

然后在 `rag_service.py` 开头添加：
```python
import pysqlite3
import sys
sys.modules['sqlite3'] = pysqlite3
```

#### 方法 3：手动更新 sqlite3
1. 下载最新的 sqlite3 DLL：https://www.sqlite.org/download.html
2. 替换 Python 安装目录下的 sqlite3 DLL

## 启动步骤

### 1. 安装依赖
```powershell
cd E:\tensorflow_education\tensorflow_education\ai_service
pip install fastapi uvicorn chromadb sentence-transformers pandas scikit-learn tensorflow
```

### 2. 解决 sqlite3 问题（如果遇到）
```powershell
pip install pysqlite3-binary
```

### 3. 启动服务

**使用启动脚本（推荐）：**
```powershell
cd E:\tensorflow_education\tensorflow_education\ai_service
.\start_rag.bat
```

**手动启动：**
```powershell
cd E:\tensorflow_education\tensorflow_education\ai_service
uvicorn main:app --reload --port 8000 --host 0.0.0.0
```

## 验证服务

启动成功后，访问：
- API 文档：http://127.0.0.1:8000/docs
- 健康检查：http://127.0.0.1:8000/docs（查看所有接口）

## 常见问题

### Q: 为什么要在 ai_service 目录下运行？
A: 因为 `main.py` 中使用了相对导入（`from rag_service import RAGService`），需要在 `ai_service` 目录下运行才能正确导入模块。

### Q: 可以修改代码在根目录运行吗？
A: 可以，但需要修改导入方式为绝对导入，例如：
```python
from ai_service.rag_service import RAGService
from ai_service.prediction_service import PredictionService
```
然后从根目录运行：`uvicorn ai_service.main:app --reload --port 8000`
