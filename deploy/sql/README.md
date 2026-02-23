# 数据库初始化说明（ry-vue）

本目录用于给其他开发者快速初始化数据库。

## 文件说明
- `schema.sql`：数据库结构（表、索引、触发器、存储过程/函数）
- `seed.sql`：初始化数据（组织、角色、菜单、字典、教育业务样例数据）

## 使用步骤
1. 创建数据库
```bash
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS \`ry-vue\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
```

2. 导入结构
```bash
mysql -uroot -p ry-vue < deploy/sql/schema.sql
```

3. 导入初始化数据
```bash
mysql -uroot -p ry-vue < deploy/sql/seed.sql
```

## 启动前检查
- 后端配置文件：`backend/ruoyi-admin/src/main/resources/application-druid.yml`
- 需要把数据库连接改成你本机/服务器的 MySQL 地址和账号密码。

## 说明
- `seed.sql` 已包含系统菜单/角色/部门和教育模块样例数据，导入后可直接联调。
- 不包含运行日志类历史数据（如 `sys_logininfor`、`sys_oper_log`、`sys_job_log`）。
- 如果你在本地新增了菜单或业务测试数据，重新执行导出命令覆盖 `seed.sql` 即可。
