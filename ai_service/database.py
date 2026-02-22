# database.py
from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker

# MySQL 连接配置（替换为你的实际信息）
# 格式：mysql+pymysql://用户名:密码@主机地址:端口号/数据库名
SQLALCHEMY_DATABASE_URL = "mysql+pymysql://root:@localhost:3306/ry-vue"
# 说明：
# - root：MySQL 用户名（通常默认是 root）
# - 123456：你的 MySQL 密码（请替换为实际密码）
# - localhost：数据库主机地址（本地数据库用 localhost，远程数据库填 IP）
# - 3306：MySQL 默认端口（一般无需修改）
# - rag_database：数据库名（需提前在 MySQL 中创建）

# 创建数据库引擎（MySQL 无需 check_same_thread 参数）
engine = create_engine(SQLALCHEMY_DATABASE_URL)

# 创建会话工厂
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

# 声明基类（ORM 模型继承用）
Base = declarative_base()