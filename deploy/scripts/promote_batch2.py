import pymysql


LEGACY_DB = "education_legacy_piclass"
MAIN_DB = "ry-vue"


def get_connection(database, dict_cursor=False):
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
        cur.execute("SET SESSION foreign_key_checks=0")
    conn.commit()
    return conn


def sync_assignments(legacy_conn, main_conn):
    with legacy_conn.cursor() as cur:
        cur.execute(
            """
            SELECT source_id, teacher_no, class_code, title, description, program_count, choice_count,
                   office_count, file_count, deadline, status
            FROM legacy_staging_assignment
            ORDER BY source_id
            """
        )
        rows = cur.fetchall()

    with main_conn.cursor() as cur:
        cur.execute("TRUNCATE TABLE edu_assignment_score")
        cur.execute("TRUNCATE TABLE edu_assignment_question")
        cur.execute("TRUNCATE TABLE edu_program_answer_record")
        cur.execute("TRUNCATE TABLE edu_assignment")
        for row in rows:
            cur.execute(
                """
                INSERT INTO edu_assignment
                (assignment_code, teacher_no, class_code, title, description, program_count, choice_count,
                 office_count, file_count, deadline, status, source_system, source_assignment_id)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                """,
                (
                    f"PICLASS-A-{int(row['source_id'])}",
                    row["teacher_no"],
                    row["class_code"],
                    row["title"],
                    row["description"],
                    int(row["program_count"] or 0),
                    int(row["choice_count"] or 0),
                    int(row["office_count"] or 0),
                    int(row["file_count"] or 0),
                    row["deadline"],
                    row["status"],
                    "piclass2",
                    int(row["source_id"]),
                ),
            )
    main_conn.commit()
    return len(rows)


def sync_program_questions(legacy_conn, main_conn):
    with legacy_conn.cursor() as cur:
        cur.execute(
            """
            SELECT source_question_id, question_type, course_name, chapter_code, chapter_name, question_stem,
                   standard_answer, analysis, knowledge_point, program_language, sample_input, sample_output,
                   reference_code
            FROM legacy_staging_question_bank
            WHERE question_type = 'program'
            ORDER BY source_question_id
            """
        )
        rows = cur.fetchall()

    with main_conn.cursor() as cur:
        cur.execute("DELETE FROM edu_question_bank WHERE question_type = 'program' AND source_system = 'piclass2'")
        for row in rows:
            cur.execute(
                """
                INSERT INTO edu_question_bank
                (catalog_id, question_code, question_type, course_name, chapter_code, chapter_name,
                 question_stem, options_json, standard_answer, analysis, knowledge_point,
                 difficulty_level, source_system, source_question_id, status)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                """,
                (
                    None,
                    f"PICLASS-P-{int(row['source_question_id'])}",
                    "program",
                    row["course_name"],
                    row["chapter_code"],
                    row["chapter_name"],
                    row["question_stem"],
                    None,
                    row["reference_code"] or row["standard_answer"],
                    row["analysis"],
                    row["knowledge_point"] or row["program_language"],
                    "medium",
                    "piclass2",
                    int(row["source_question_id"]),
                    "0",
                ),
            )
    main_conn.commit()
    return len(rows)


def build_assignment_map(main_conn):
    with main_conn.cursor() as cur:
        cur.execute("SELECT id, source_assignment_id FROM edu_assignment")
        rows = cur.fetchall()
    return {int(row["source_assignment_id"]): int(row["id"]) for row in rows}


def build_program_question_map(main_conn):
    with main_conn.cursor() as cur:
        cur.execute(
            """
            SELECT id, source_question_id
            FROM edu_question_bank
            WHERE question_type = 'program'
            """
        )
        rows = cur.fetchall()
    return {int(row["source_question_id"]): int(row["id"]) for row in rows}


def sync_assignment_questions(legacy_conn, main_conn):
    assignment_map = build_assignment_map(main_conn)
    question_map = build_program_question_map(main_conn)
    with legacy_conn.cursor() as cur:
        cur.execute(
            """
            SELECT source_id, assignment_source_id, question_source_id_1, question_source_id_2,
                   question_type_code, remark, peer_review_flag
            FROM legacy_staging_assignment_question
            ORDER BY source_id
            """
        )
        rows = cur.fetchall()

    inserted = 0
    with main_conn.cursor() as cur:
        for row in rows:
            assignment_id = assignment_map.get(int(row["assignment_source_id"])) if row["assignment_source_id"] is not None else None
            if assignment_id is None:
                continue
            qid = None
            if row["question_source_id_1"] is not None:
                qid = question_map.get(int(row["question_source_id_1"]))
            cur.execute(
                """
                INSERT INTO edu_assignment_question
                (assignment_id, question_id, source_question_id_1, source_question_id_2, question_type_code,
                 remark, peer_review_flag, source_system, source_record_id)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
                """,
                (
                    assignment_id,
                    qid,
                    row["question_source_id_1"],
                    row["question_source_id_2"],
                    row["question_type_code"],
                    row["remark"],
                    int(row["peer_review_flag"] or 0),
                    "piclass2",
                    int(row["source_id"]),
                ),
            )
            inserted += 1
    main_conn.commit()
    return inserted


def sync_assignment_scores(legacy_conn, main_conn):
    assignment_map = build_assignment_map(main_conn)
    with legacy_conn.cursor() as cur:
        cur.execute(
            """
            SELECT source_id, student_no, assignment_source_id, program_score, choice_score,
                   office_score, file_score, total_score, score_time
            FROM legacy_staging_assignment_score
            ORDER BY source_id
            """
        )
        rows = cur.fetchall()

    inserted = 0
    with main_conn.cursor() as cur:
        for row in rows:
            assignment_id = assignment_map.get(int(row["assignment_source_id"])) if row["assignment_source_id"] is not None else None
            cur.execute(
                """
                INSERT INTO edu_assignment_score
                (student_no, assignment_id, program_score, choice_score, office_score, file_score,
                 total_score, score_time, source_system, source_record_id)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                """,
                (
                    row["student_no"],
                    assignment_id,
                    row["program_score"] or 0,
                    row["choice_score"] or 0,
                    row["office_score"] or 0,
                    row["file_score"] or 0,
                    row["total_score"] or 0,
                    row["score_time"],
                    "piclass2",
                    int(row["source_id"]),
                ),
            )
            inserted += 1
    main_conn.commit()
    return inserted


def sync_program_answers(legacy_conn, main_conn):
    question_map = build_program_question_map(main_conn)
    with legacy_conn.cursor() as cur:
        cur.execute(
            """
            SELECT source_record_id, student_no, source_question_id, assignment_source_id,
                   answer_content, raw_code, standardized_score, is_correct, teacher_feedback, submit_time
            FROM legacy_staging_student_answer
            WHERE answer_type = 'program'
            ORDER BY source_record_id
            """
        )
        rows = cur.fetchall()

    inserted = 0
    with main_conn.cursor() as cur:
        for row in rows:
            question_id = question_map.get(int(row["source_question_id"])) if row["source_question_id"] is not None else None
            cur.execute(
                """
                INSERT INTO edu_program_answer_record
                (student_no, question_id, assignment_source_id, answer_content, raw_code, score,
                 is_correct, teacher_feedback, submit_time, source_system, source_record_id)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                """,
                (
                    row["student_no"],
                    question_id,
                    row["assignment_source_id"],
                    row["answer_content"],
                    row["raw_code"],
                    row["standardized_score"] or 0,
                    "1" if int(row["is_correct"] or 0) == 1 else "0",
                    row["teacher_feedback"],
                    row["submit_time"],
                    "piclass2",
                    int(row["source_record_id"]),
                ),
            )
            inserted += 1
    main_conn.commit()
    return inserted


def verify(main_conn):
    tables = [
        "edu_assignment",
        "edu_assignment_question",
        "edu_assignment_score",
        "edu_program_answer_record",
    ]
    result = {}
    with main_conn.cursor() as cur:
        for table in tables:
            cur.execute(f"SELECT COUNT(*) AS c FROM {table}")
            result[table] = cur.fetchone()["c"]
    return result


def main():
    legacy_conn = get_connection(LEGACY_DB, dict_cursor=True)
    main_conn = get_connection(MAIN_DB, dict_cursor=True)
    try:
        assignment_count = sync_assignments(legacy_conn, main_conn)
        program_question_count = sync_program_questions(legacy_conn, main_conn)
        assignment_question_count = sync_assignment_questions(legacy_conn, main_conn)
        assignment_score_count = sync_assignment_scores(legacy_conn, main_conn)
        program_answer_count = sync_program_answers(legacy_conn, main_conn)
        counts = verify(main_conn)
        print("ASSIGNMENT_PROMOTED", assignment_count)
        print("PROGRAM_QUESTION_PROMOTED", program_question_count)
        print("ASSIGNMENT_QUESTION_PROMOTED", assignment_question_count)
        print("ASSIGNMENT_SCORE_PROMOTED", assignment_score_count)
        print("PROGRAM_ANSWER_PROMOTED", program_answer_count)
        print("MAIN_COUNTS", counts)
    finally:
        legacy_conn.close()
        main_conn.close()


if __name__ == "__main__":
    main()
