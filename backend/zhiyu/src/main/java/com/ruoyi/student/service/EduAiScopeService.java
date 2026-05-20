package com.ruoyi.student.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;

@Service
public class EduAiScopeService
{
    private static final Pattern DATASET_ID_PATTERN = Pattern.compile("(\\d+)");
    private static final String ROLE_TEACHER = "teacher";
    private static final String ROLE_STUDENT = "student";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureDatasetScopeTable()
    {
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS edu_ai_dataset_scope ("
                        + "dataset_id BIGINT NOT NULL PRIMARY KEY,"
                        + "file_name VARCHAR(255) NULL,"
                        + "owner_user_id BIGINT NOT NULL,"
                        + "owner_user_name VARCHAR(64) NOT NULL,"
                        + "owner_role_key VARCHAR(32) NOT NULL,"
                        + "owner_display_name VARCHAR(64) NULL,"
                        + "scope_type VARCHAR(16) NOT NULL DEFAULT 'private',"
                        + "status CHAR(1) NOT NULL DEFAULT '0',"
                        + "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                        + "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    public void bindUploadedDatasets(Map<String, Object> uploadResult)
    {
        if (uploadResult == null)
        {
            return;
        }
        Object rows = uploadResult.get("results");
        if (!(rows instanceof Iterable))
        {
            return;
        }

        SysUser user = SecurityUtils.getLoginUser() == null ? null : SecurityUtils.getLoginUser().getUser();
        Long currentUserId = user == null ? null : user.getUserId();
        if (user == null || currentUserId == null)
        {
            return;
        }

        String ownerRole = resolveCurrentRoleKey();
        String displayName = StringUtils.defaultIfEmpty(user.getNickName(), user.getUserName());
        for (Object item : (Iterable<?>) rows)
        {
            if (!(item instanceof Map))
            {
                continue;
            }
            Map<?, ?> row = (Map<?, ?>) item;
            if (!"success".equals(String.valueOf(row.get("status"))))
            {
                continue;
            }

            String message = mapString(row, "message");
            Long datasetId = extractDatasetId(message);
            if (datasetId == null)
            {
                continue;
            }

            String fileName = mapString(row, "filename");
            jdbcTemplate.update(
                    "INSERT INTO edu_ai_dataset_scope(dataset_id, file_name, owner_user_id, owner_user_name, owner_role_key, owner_display_name, scope_type, status) "
                            + "VALUES (?, ?, ?, ?, ?, ?, 'private', '0') "
                            + "ON DUPLICATE KEY UPDATE file_name = VALUES(file_name), owner_user_id = VALUES(owner_user_id), "
                            + "owner_user_name = VALUES(owner_user_name), owner_role_key = VALUES(owner_role_key), "
                            + "owner_display_name = VALUES(owner_display_name), scope_type = VALUES(scope_type), status = '0', update_time = NOW()",
                    datasetId,
                    fileName,
                    currentUserId,
                    user.getUserName(),
                    ownerRole,
                    displayName);
        }
    }

    public List<Long> listCurrentUserDatasetIds()
    {
        Long currentUserId = SecurityUtils.getUserId();
        if (currentUserId == null)
        {
            return Collections.emptyList();
        }
        return jdbcTemplate.query(
                "SELECT dataset_id FROM edu_ai_dataset_scope WHERE owner_user_id = ? AND status = '0' ORDER BY update_time DESC",
                (rs, rowNum) -> rs.getLong(1),
                currentUserId);
    }

    public boolean canAccessDataset(Long datasetId)
    {
        if (datasetId == null)
        {
            return false;
        }
        Long currentUserId = SecurityUtils.getUserId();
        if (currentUserId == null)
        {
            return false;
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM edu_ai_dataset_scope WHERE dataset_id = ? AND owner_user_id = ? AND status = '0'",
                Integer.class,
                datasetId,
                currentUserId);
        return count != null && count > 0;
    }

    public List<Map<String, Object>> filterDatasets(List<Map<String, Object>> datasets)
    {
        if (datasets == null || datasets.isEmpty())
        {
            return Collections.emptyList();
        }
        List<Long> visibleIds = listCurrentUserDatasetIds();
        if (visibleIds.isEmpty())
        {
            return Collections.emptyList();
        }
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> item : datasets)
        {
            if (item == null)
            {
                continue;
            }
            Long datasetId = toLong(item.get("id"));
            if (datasetId != null && visibleIds.contains(datasetId))
            {
                filtered.add(item);
            }
        }
        return filtered;
    }

    public String buildScopedQuestion(String question, String sourceScene, String courseName, String chapterName, String knowledgePoint)
    {
        List<String> scopeParts = new ArrayList<>();
        if (StringUtils.isNotEmpty(sourceScene))
        {
            scopeParts.add("来源场景：" + StringUtils.trim(sourceScene));
        }
        if (StringUtils.isNotEmpty(courseName))
        {
            scopeParts.add("当前课程：" + StringUtils.trim(courseName));
        }
        if (StringUtils.isNotEmpty(chapterName))
        {
            scopeParts.add("当前模块：" + StringUtils.trim(chapterName));
        }
        if (StringUtils.isNotEmpty(knowledgePoint))
        {
            scopeParts.add("当前知识点：" + StringUtils.trim(knowledgePoint));
        }
        String scoped = StringUtils.trimToEmpty(question);
        if (scopeParts.isEmpty())
        {
            return scoped;
        }
        return String.join("；", scopeParts) + "；学生提问：" + scoped;
    }

    private Long extractDatasetId(String message)
    {
        if (StringUtils.isEmpty(message))
        {
            return null;
        }
        Matcher matcher = DATASET_ID_PATTERN.matcher(message);
        Long lastId = null;
        if (matcher.find())
        {
            do
            {
                try
                {
                    lastId = Long.parseLong(matcher.group(1));
                }
                catch (NumberFormatException ignore)
                {
                }
            }
            while (matcher.find());
        }
        return lastId;
    }

    private String resolveCurrentRoleKey()
    {
        Long currentUserId = SecurityUtils.getUserId();
        if (currentUserId != null)
        {
            Integer teacherCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM sys_user_role ur JOIN sys_role r ON ur.role_id = r.role_id WHERE ur.user_id = ? AND r.role_key = ?",
                    Integer.class,
                    currentUserId,
                    ROLE_TEACHER);
            if (teacherCount != null && teacherCount > 0)
            {
                return ROLE_TEACHER;
            }
        }
        if (SecurityContextHolder.getContext() == null || SecurityContextHolder.getContext().getAuthentication() == null)
        {
            return ROLE_STUDENT;
        }
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(item -> item.getAuthority())
                .anyMatch(item -> item != null && item.contains(ROLE_TEACHER)) ? ROLE_TEACHER : ROLE_STUDENT;
    }

    private Long toLong(Object value)
    {
        if (value instanceof Number)
        {
            return ((Number) value).longValue();
        }
        if (value == null)
        {
            return null;
        }
        try
        {
            return Long.parseLong(String.valueOf(value));
        }
        catch (NumberFormatException ex)
        {
            return null;
        }
    }

    private String mapString(Map<?, ?> row, String key)
    {
        if (row == null || key == null)
        {
            return "";
        }
        Object value = row.get(key);
        return value == null ? "" : String.valueOf(value);
    }
}
