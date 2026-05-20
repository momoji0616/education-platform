import pymysql


MAIN_DB = "ry-vue"
LEGACY_DB = "education_legacy_piclass"


def get_conn(database, dict_cursor=False):
    conn = pymysql.connect(
        host="localhost",
        user="root",
        password="123456",
        database=database,
        charset="utf8mb4",
        autocommit=False,
        cursorclass=pymysql.cursors.DictCursor if dict_cursor else pymysql.cursors.Cursor,
    )
    with conn.cursor() as cur:
        cur.execute("SET SESSION sql_mode=''")
    conn.commit()
    return conn


def load_current_users(main_conn):
    with main_conn.cursor() as cur:
        cur.execute(
            """
            SELECT u.user_id, u.user_name, u.nick_name,
                   p.role_key, p.class_name, p.grade_no, p.class_no
            FROM sys_user u
            LEFT JOIN edu_user_class_profile p ON u.user_id = p.user_id
            WHERE u.del_flag = '0'
            """
        )
        return cur.fetchall()


def load_legacy_students(legacy_conn):
    with legacy_conn.cursor() as cur:
        cur.execute(
            """
            SELECT student_no, student_name, class_code
            FROM legacy_staging_student_profile
            """
        )
        return cur.fetchall()


def load_legacy_classes(legacy_conn):
    with legacy_conn.cursor() as cur:
        cur.execute(
            """
            SELECT class_code, class_name
            FROM legacy_staging_class
            """
        )
        return cur.fetchall()


def seed_class_candidates(main_conn, legacy_classes, current_users):
    current_classes = {}
    for row in current_users:
        class_name = row.get("class_name")
        if not class_name:
            continue
        current_classes[class_name] = {
            "current_class_name": class_name,
            "current_grade_no": row.get("grade_no"),
            "current_class_no": row.get("class_no"),
        }

    exact_name_map = {item["class_name"]: item for item in legacy_classes if item.get("class_name")}

    inserted = 0
    with main_conn.cursor() as cur:
        cur.execute("TRUNCATE TABLE edu_legacy_class_map")
        for item in current_classes.values():
            legacy = exact_name_map.get(item["current_class_name"])
            legacy_code = None
            legacy_name = None
            status = "0"
            remark = "未自动匹配到旧班级"
            if legacy:
                legacy_code = legacy["class_code"]
                legacy_name = legacy["class_name"]
                status = "1"
                remark = "按班级名称自动匹配"
            cur.execute(
                """
                INSERT INTO edu_legacy_class_map
                (current_class_name, current_grade_no, current_class_no, legacy_class_code,
                 legacy_class_name, source_system, map_status, remark)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
                """,
                (
                    item["current_class_name"],
                    item["current_grade_no"],
                    item["current_class_no"],
                    legacy_code,
                    legacy_name,
                    "piclass2",
                    status,
                    remark,
                ),
            )
            inserted += 1
    main_conn.commit()
    return inserted


def seed_user_candidates(main_conn, legacy_students, current_users):
    by_student_no = {item["student_no"]: item for item in legacy_students if item.get("student_no")}
    by_student_name = {}
    for item in legacy_students:
        name = item.get("student_name")
        if name and name not in by_student_name:
            by_student_name[name] = item

    inserted = 0
    with main_conn.cursor() as cur:
        cur.execute("TRUNCATE TABLE edu_legacy_user_map")
        for row in current_users:
            legacy = by_student_no.get(row["user_name"])
            remark = "未自动匹配到旧学生数据"
            status = "0"
            if legacy is None:
                legacy = by_student_name.get(row["nick_name"])
                if legacy is not None:
                    remark = "按昵称与旧学生姓名自动匹配"
                    status = "1"
            else:
                remark = "按用户名与旧学号自动匹配"
                status = "1"

            cur.execute(
                """
                INSERT INTO edu_legacy_user_map
                (current_user_id, current_user_name, current_role_key, current_class_name,
                 legacy_user_name, legacy_student_no, legacy_student_name, legacy_class_code,
                 source_system, map_status, remark)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                """,
                (
                    row["user_id"],
                    row["user_name"],
                    row.get("role_key"),
                    row.get("class_name"),
                    legacy["student_no"] if legacy else None,
                    legacy["student_no"] if legacy else None,
                    legacy["student_name"] if legacy else None,
                    legacy["class_code"] if legacy else None,
                    "piclass2",
                    status,
                    remark,
                ),
            )
            inserted += 1
    main_conn.commit()
    return inserted


def main():
    main_conn = get_conn(MAIN_DB, dict_cursor=True)
    legacy_conn = get_conn(LEGACY_DB, dict_cursor=True)
    try:
        current_users = load_current_users(main_conn)
        legacy_students = load_legacy_students(legacy_conn)
        legacy_classes = load_legacy_classes(legacy_conn)
        class_rows = seed_class_candidates(main_conn, legacy_classes, current_users)
        user_rows = seed_user_candidates(main_conn, legacy_students, current_users)

        with main_conn.cursor() as cur:
            cur.execute("SELECT map_status, COUNT(*) AS c FROM edu_legacy_class_map GROUP BY map_status")
            class_stats = cur.fetchall()
            cur.execute("SELECT map_status, COUNT(*) AS c FROM edu_legacy_user_map GROUP BY map_status")
            user_stats = cur.fetchall()
        print("CLASS_ROWS", class_rows)
        print("USER_ROWS", user_rows)
        print("CLASS_STATUS", class_stats)
        print("USER_STATUS", user_stats)
    finally:
        main_conn.close()
        legacy_conn.close()


if __name__ == "__main__":
    main()
