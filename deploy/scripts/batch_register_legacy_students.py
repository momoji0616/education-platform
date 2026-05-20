import pymysql


MAIN_DB = "ry-vue"
LEGACY_DB = "education_legacy_piclass"
DEFAULT_PASSWORD_HASH = "$2a$10$DfgzBvCRzWGKwrPfNX59celzgtcHBxCp7v2iLWgMgWM84JC8JmmGy"
MAJOR_NAME = "数据科学与大数据技术"
CREATE_BY = "codex-batch"


def get_conn(database, dict_cursor=False):
    return pymysql.connect(
        host="localhost",
        user="root",
        password="123456",
        database=database,
        charset="utf8mb4",
        autocommit=False,
        cursorclass=pymysql.cursors.DictCursor if dict_cursor else pymysql.cursors.Cursor,
    )


def load_candidate_students(main_conn, legacy_conn):
    with main_conn.cursor() as cur:
        cur.execute(
            """
            select distinct student_no
            from edu_student_answer_record
            where student_no is not null
              and student_no <> ''
            order by student_no asc
            """
        )
        answer_students = [row[0] for row in cur.fetchall()]

    if not answer_students:
        return []

    placeholders = ",".join(["%s"] * len(answer_students))
    with legacy_conn.cursor() as cur:
        cur.execute(
            f"""
            select student_no, student_name, major_name, class_code
            from legacy_staging_student_profile
            where student_no in ({placeholders})
            order by student_no asc
            """,
            answer_students,
        )
        profile_rows = cur.fetchall()

    profile_map = {row["student_no"]: row for row in profile_rows}
    result = []
    for student_no in answer_students:
        profile = profile_map.get(student_no, {})
        result.append(
            {
                "student_no": student_no,
                "student_name": (profile.get("student_name") or student_no).strip()[:30],
                "legacy_class_code": profile.get("class_code"),
                "major_name": (profile.get("major_name") or MAJOR_NAME).strip()[:128],
            }
        )
    return result


def load_existing_users(main_conn):
    with main_conn.cursor() as cur:
        cur.execute(
            """
            select user_id, user_name, nick_name
            from sys_user
            where del_flag = '0'
            """
        )
        return {row[1]: {"user_id": row[0], "nick_name": row[2]} for row in cur.fetchall()}


def load_student_role_id(main_conn):
    with main_conn.cursor() as cur:
        cur.execute("select role_id from sys_role where role_key = 'student' limit 1")
        row = cur.fetchone()
        if not row:
            raise RuntimeError("未找到 student 角色")
        return row[0]


def ensure_user(main_conn, student, existing_users, student_role_id):
    username = student["student_no"]
    student_name = student["student_name"] or username
    existing = existing_users.get(username)

    with main_conn.cursor() as cur:
        if existing:
            user_id = existing["user_id"]
            cur.execute(
                """
                update sys_user
                set nick_name = %s,
                    status = '0',
                    del_flag = '0',
                    update_by = %s,
                    update_time = now()
                where user_id = %s
                """,
                (student_name, CREATE_BY, user_id),
            )
        else:
            cur.execute(
                """
                insert into sys_user
                (dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar,
                 password, status, del_flag, create_by, create_time, remark)
                values
                (103, %s, %s, '00', '', '', '0', '', %s, '0', '0', %s, now(), %s)
                """,
                (username, student_name, DEFAULT_PASSWORD_HASH, CREATE_BY, "批量导入的历史答题学生账号"),
            )
            user_id = cur.lastrowid
            existing_users[username] = {"user_id": user_id, "nick_name": student_name}

        cur.execute(
            """
            select count(1)
            from sys_user_role
            where user_id = %s and role_id = %s
            """,
            (user_id, student_role_id),
        )
        if cur.fetchone()[0] == 0:
            cur.execute(
                "insert into sys_user_role(user_id, role_id) values (%s, %s)",
                (user_id, student_role_id),
            )

        cur.execute(
            """
            insert into edu_user_class_profile(user_id, role_key, grade_no, class_no, class_name, head_teacher, create_time, update_time)
            values (%s, 'student', 1, 1, %s, 0, now(), now())
            on duplicate key update
                role_key = values(role_key),
                grade_no = values(grade_no),
                class_no = values(class_no),
                class_name = values(class_name),
                head_teacher = values(head_teacher),
                update_time = now()
            """,
            (user_id, MAJOR_NAME),
        )

        cur.execute(
            """
            insert into edu_legacy_user_map
            (current_user_id, current_user_name, current_role_key, current_class_name,
             legacy_user_name, legacy_student_no, legacy_student_name, legacy_class_code,
             source_system, map_status, remark)
            values (%s, %s, 'student', %s, %s, %s, %s, %s, 'piclass2', '1', '按学号批量导入并自动绑定')
            on duplicate key update
                current_user_name = values(current_user_name),
                current_role_key = values(current_role_key),
                current_class_name = values(current_class_name),
                legacy_user_name = values(legacy_user_name),
                legacy_student_no = values(legacy_student_no),
                legacy_student_name = values(legacy_student_name),
                legacy_class_code = values(legacy_class_code),
                source_system = values(source_system),
                map_status = values(map_status),
                remark = values(remark),
                update_time = now()
            """,
            (
                user_id,
                username,
                MAJOR_NAME,
                username,
                student["student_no"],
                student_name,
                student["legacy_class_code"],
            ),
        )

    return user_id


def main():
    main_conn = get_conn(MAIN_DB)
    legacy_conn = get_conn(LEGACY_DB, dict_cursor=True)
    try:
        candidates = load_candidate_students(main_conn, legacy_conn)
        existing_users = load_existing_users(main_conn)
        student_role_id = load_student_role_id(main_conn)

        created = 0
        updated = 0
        with_credentials = []

        for student in candidates:
            existed = student["student_no"] in existing_users
            ensure_user(main_conn, student, existing_users, student_role_id)
            if existed:
                updated += 1
            else:
                created += 1
            with_credentials.append((student["student_no"], student["student_name"], "123456"))

        main_conn.commit()
        print("TOTAL_CANDIDATES", len(candidates))
        print("CREATED", created)
        print("UPDATED", updated)
        print("SAMPLE")
        for row in with_credentials[:15]:
            print(row)
    finally:
        main_conn.close()
        legacy_conn.close()


if __name__ == "__main__":
    main()
