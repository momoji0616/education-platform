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


def build_catalog_models(legacy_conn):
    with legacy_conn.cursor() as cur:
        cur.execute(
            """
            SELECT source_catalog_id, source_chapter_code, catalog_name, question_count
            FROM legacy_staging_question_catalog
            ORDER BY source_catalog_id, source_chapter_code
            """
        )
        rows = cur.fetchall()

    grouped = {}
    for row in rows:
        grouped.setdefault(row["source_catalog_id"], []).append(row)

    models = []
    for source_catalog_id, items in grouped.items():
        course_name = None
        for item in items:
            if str(item["source_chapter_code"]) in {"0", "*"}:
                course_name = item["catalog_name"]
                break
        if not course_name and items:
            course_name = items[0]["catalog_name"]

        for item in items:
            chapter_code = str(item["source_chapter_code"])
            chapter_name = item["catalog_name"]
            catalog_code = f"PICLASS-C-{source_catalog_id}-{chapter_code}"
            models.append(
                {
                    "catalog_code": catalog_code,
                    "catalog_name": item["catalog_name"],
                    "course_name": course_name,
                    "chapter_code": chapter_code,
                    "chapter_name": chapter_name,
                    "question_type": "choice",
                    "question_count": int(item["question_count"] or 0),
                    "source_system": "piclass2",
                    "source_catalog_id": int(source_catalog_id),
                    "status": "0",
                }
            )
    return models


def sync_catalogs(main_conn, catalog_models):
    with main_conn.cursor() as cur:
        cur.execute("TRUNCATE TABLE edu_student_answer_record")
        cur.execute("TRUNCATE TABLE edu_question_bank")
        cur.execute("TRUNCATE TABLE edu_question_catalog")
        for item in catalog_models:
            cur.execute(
                """
                INSERT INTO edu_question_catalog
                (catalog_code, catalog_name, course_name, chapter_code, chapter_name,
                 question_type, question_count, source_system, source_catalog_id, status)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                """,
                (
                    item["catalog_code"],
                    item["catalog_name"],
                    item["course_name"],
                    item["chapter_code"],
                    item["chapter_name"],
                    item["question_type"],
                    item["question_count"],
                    item["source_system"],
                    item["source_catalog_id"],
                    item["status"],
                ),
            )
    main_conn.commit()


def build_catalog_id_map(main_conn):
    with main_conn.cursor() as cur:
        cur.execute("SELECT id, source_catalog_id, chapter_code FROM edu_question_catalog")
        rows = cur.fetchall()
    return {(int(row["source_catalog_id"]), str(row["chapter_code"])): int(row["id"]) for row in rows}


def sync_questions(legacy_conn, main_conn):
    catalog_id_map = build_catalog_id_map(main_conn)
    with legacy_conn.cursor() as cur:
        cur.execute(
            """
            SELECT source_question_id, source_catalog_id, source_question_no, question_type,
                   course_name, chapter_code, chapter_name, question_stem, options_json,
                   standard_answer, analysis, knowledge_point
            FROM legacy_staging_question_bank
            ORDER BY source_question_id
            """
        )
        rows = cur.fetchall()

    inserted = 0
    with main_conn.cursor() as cur:
        for row in rows:
            catalog_id = catalog_id_map.get((int(row["source_catalog_id"]), str(row["chapter_code"])))
            question_code = f"PICLASS-Q-{int(row['source_question_id'])}"
            cur.execute(
                """
                INSERT INTO edu_question_bank
                (catalog_id, question_code, question_type, course_name, chapter_code, chapter_name,
                 question_stem, options_json, standard_answer, analysis, knowledge_point,
                 difficulty_level, source_system, source_question_id, status)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                """,
                (
                    catalog_id,
                    question_code,
                    row["question_type"],
                    row["course_name"],
                    row["chapter_code"],
                    row["chapter_name"],
                    row["question_stem"],
                    row["options_json"],
                    row["standard_answer"],
                    row["analysis"],
                    row["knowledge_point"],
                    "medium",
                    "piclass2",
                    int(row["source_question_id"]),
                    "0",
                ),
            )
            inserted += 1
    main_conn.commit()
    return inserted


def build_question_id_map(main_conn):
    with main_conn.cursor() as cur:
        cur.execute("SELECT id, source_question_id FROM edu_question_bank")
        rows = cur.fetchall()
    return {int(row["source_question_id"]): int(row["id"]) for row in rows}


def sync_answers(legacy_conn, main_conn):
    question_id_map = build_question_id_map(main_conn)
    with legacy_conn.cursor() as cur:
        cur.execute(
            """
            SELECT source_record_id, student_no, source_question_id, answer_type,
                   answer_content, standardized_score, is_correct, submit_time
            FROM legacy_staging_student_answer
            ORDER BY source_record_id
            """
        )
        rows = cur.fetchall()

    inserted = 0
    with main_conn.cursor() as cur:
        for row in rows:
            question_id = question_id_map.get(int(row["source_question_id"])) if row["source_question_id"] is not None else None
            if question_id is None:
                continue
            cur.execute(
                """
                INSERT INTO edu_student_answer_record
                (student_no, question_id, question_type, assignment_id, answer_content,
                 score, is_correct, submit_time, source_system, source_record_id)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                """,
                (
                    row["student_no"],
                    question_id,
                    row["answer_type"],
                    None,
                    row["answer_content"],
                    row["standardized_score"] or 0,
                    "1" if int(row["is_correct"] or 0) == 1 else "0",
                    row["submit_time"],
                    "piclass2",
                    int(row["source_record_id"]),
                ),
            )
            inserted += 1
    main_conn.commit()
    return inserted


def verify(main_conn):
    with main_conn.cursor() as cur:
        cur.execute("SELECT COUNT(*) AS c FROM edu_question_catalog")
        catalog_count = cur.fetchone()["c"]
        cur.execute("SELECT COUNT(*) AS c FROM edu_question_bank")
        question_count = cur.fetchone()["c"]
        cur.execute("SELECT COUNT(*) AS c FROM edu_student_answer_record")
        answer_count = cur.fetchone()["c"]
    return {
        "edu_question_catalog": catalog_count,
        "edu_question_bank": question_count,
        "edu_student_answer_record": answer_count,
    }


def main():
    legacy_conn = get_connection(LEGACY_DB, dict_cursor=True)
    main_conn = get_connection(MAIN_DB, dict_cursor=True)
    try:
        catalog_models = build_catalog_models(legacy_conn)
        sync_catalogs(main_conn, catalog_models)
        question_count = sync_questions(legacy_conn, main_conn)
        answer_count = sync_answers(legacy_conn, main_conn)
        counts = verify(main_conn)
        print("CATALOG_MODELS", len(catalog_models))
        print("QUESTION_PROMOTED", question_count)
        print("ANSWER_PROMOTED", answer_count)
        print("MAIN_COUNTS", counts)
    finally:
        legacy_conn.close()
        main_conn.close()


if __name__ == "__main__":
    main()
