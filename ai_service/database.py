import os
from urllib.parse import quote_plus

from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker

AI_DB_HOST = os.getenv("AI_DB_HOST", "127.0.0.1")
AI_DB_PORT = os.getenv("AI_DB_PORT", "3306")
AI_DB_NAME = os.getenv("AI_DB_NAME", "ry-vue")
AI_DB_USER = os.getenv("AI_DB_USER", "root")
AI_DB_PASSWORD = os.getenv("AI_DB_PASSWORD", "")

auth = quote_plus(AI_DB_USER)
if AI_DB_PASSWORD:
    auth = f"{auth}:{quote_plus(AI_DB_PASSWORD)}"

SQLALCHEMY_DATABASE_URL = (
    f"mysql+pymysql://{auth}@{AI_DB_HOST}:{AI_DB_PORT}/{AI_DB_NAME}"
    "?charset=utf8mb4"
)

# 创建数据库引擎（MySQL 无需 check_same_thread 参数）
engine = create_engine(SQLALCHEMY_DATABASE_URL)

# 创建会话工厂
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

# 声明基类（ORM 模型继承用）
Base = declarative_base()
