-- Manual mapping examples for legacy question data
-- Target database: ry-vue
-- Update the values below before execution.

-- 1. Map a current-system class to a legacy class
insert into edu_legacy_class_map (
    current_class_name,
    legacy_class_code,
    legacy_class_name,
    source_type,
    map_status,
    remark,
    create_time,
    update_time
) values (
    'G1-C1',
    '1002',
    '22智能',
    'manual',
    '1',
    'manually confirmed class binding',
    now(),
    now()
)
on duplicate key update
    legacy_class_code = values(legacy_class_code),
    legacy_class_name = values(legacy_class_name),
    source_type = 'manual',
    map_status = '1',
    remark = values(remark),
    update_time = now();

-- 2. Map a current-system student account to a legacy student number
insert into edu_legacy_user_map (
    current_user_id,
    current_user_name,
    legacy_student_no,
    legacy_student_name,
    legacy_class_code,
    source_type,
    map_status,
    remark,
    create_time,
    update_time
) values (
    104,
    's_g1c1',
    '20221113479',
    '阮鑫涛',
    '1002',
    'manual',
    '1',
    'manually confirmed student binding',
    now(),
    now()
)
on duplicate key update
    legacy_student_no = values(legacy_student_no),
    legacy_student_name = values(legacy_student_name),
    legacy_class_code = values(legacy_class_code),
    source_type = 'manual',
    map_status = '1',
    remark = values(remark),
    update_time = now();
