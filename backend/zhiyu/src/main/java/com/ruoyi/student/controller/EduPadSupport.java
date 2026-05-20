package com.ruoyi.student.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.SysLoginService;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.student.domain.EduForumPost;
import com.ruoyi.student.domain.EduHomework;
import com.ruoyi.student.domain.EduHomeworkSubmission;
import com.ruoyi.student.domain.EduRegisterBody;
import com.ruoyi.student.mapper.EduLegacyAnalyticsMapper;
import com.ruoyi.student.mapper.EduPadMapper;
import com.ruoyi.student.service.EduAiInteractionService;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.service.ISysUserService;

public abstract class EduPadSupport extends BaseController
{
    protected static final Logger log = LoggerFactory.getLogger(EduPadSupport.class);
    protected static final int MAJOR_DATA_SCIENCE_GRADE_NO = 1;
    protected static final int MAJOR_NETWORK_ENGINEERING_GRADE_NO = 2;
    protected static final int DEFAULT_MAJOR_CLASS_NO = 1;
    protected static final int HOMEWORK_MIN_SCORE = 0;
    protected static final int HOMEWORK_MAX_SCORE = 100;
    protected static final int DEFAULT_EXAM_MAX_SCORE = 100;
    protected static final int DEFAULT_EXAMPLE_SCORE = 80;
    protected static final int MIN_AI_FALLBACK_SCORE = 60;
    protected static final String STATUS_PUBLISHED = "PUBLISHED";
    protected static final String STATUS_TODO = "TODO";
    protected static final String CHAT_DM_TITLE = "CHAT_DM";
    protected static final String CHAT_GROUP_TITLE = "CHAT_GROUP";
    protected static final String DEFAULT_CLASS_GROUP_ID = "CLASS_ALL";
    protected static final String ROLE_KEY_TEACHER = "teacher";
    protected static final String ROLE_KEY_STUDENT = "student";
    protected static final String ROLE_KEY_DEFAULT_STUDENT = "role_default";
    protected static final String MAJOR_DATA_SCIENCE_KEY = "data-science";
    protected static final String MAJOR_NETWORK_ENGINEERING_KEY = "network-engineering";
    protected static final String MAJOR_DATA_SCIENCE_NAME = "数据科学与大数据";
    protected static final String MAJOR_NETWORK_ENGINEERING_NAME = "网络工程";
    protected static final String CLASS_NAME_KEY = "class_name";
    protected static final String GRADE_NO_KEY = "grade_no";
    protected static final String CLASS_NO_KEY = "class_no";
    protected static final String ROLE_KEY_KEY = "role_key";
    protected static final String POST_ID_KEY = "post_id";
    protected static final String ROLE_MANAGER = "MANAGER";
    protected static final String ROLE_TEACHER = "TEACHER";
    protected static final String ROLE_STUDENT = "STUDENT";
    protected static final String ROLE_ALL = "ALL";
    protected static final Pattern REVIEW_IMAGE_PATTERN = Pattern.compile("\\[REVIEW_IMAGE\\]\\(([^)\\s]+)\\)", Pattern.CASE_INSENSITIVE);

    @Autowired
    protected EduPadMapper eduPadMapper;

    @Autowired
    protected EduLegacyAnalyticsMapper eduLegacyAnalyticsMapper;

    @Autowired
    protected SysRoleMapper roleMapper;

    @Autowired
    protected ISysUserService userService;

    @Autowired
    protected AuthenticationManager authenticationManager;

    @Autowired
    protected TokenService tokenService;

    @Autowired
    protected SysLoginService loginService;

    @Autowired
    protected EduAiInteractionService eduAiInteractionService;

    protected static final class MajorProfile
    {
        private final String majorKey;
        private final String majorName;
        private final Integer gradeNo;
        private final Integer classNo;

        private MajorProfile(String majorKey, String majorName, Integer gradeNo, Integer classNo)
        {
            this.majorKey = majorKey;
            this.majorName = majorName;
            this.gradeNo = gradeNo;
            this.classNo = classNo;
        }

        public String getMajorKey()
        {
            return majorKey;
        }

        public String getMajorName()
        {
            return majorName;
        }

        public Integer getGradeNo()
        {
            return gradeNo;
        }

        public Integer getClassNo()
        {
            return classNo;
        }
    }

    protected String registerPadUser(EduRegisterBody registerBody)
    {
        String username = registerBody.getUsername();
        String password = registerBody.getPassword();
        String roleKey = normalizeRegisterRoleKey(registerBody.getRoleKey());
        MajorProfile majorProfile = resolveMajorProfile(registerBody.getMajorKey(), registerBody.getGradeNo(), registerBody.getClassNo());
        SysUser sysUser = new SysUser();
        sysUser.setUserName(username);

        if (StringUtils.isEmpty(username)) return "用户名不能为空";
        if (StringUtils.isEmpty(password)) return "密码不能为空";
        if (StringUtils.isEmpty(roleKey)) return "注册角色无效，仅支持 teacher 或 student";
        if (majorProfile == null) return "请选择专业，目前仅支持数据科学与大数据、网络工程";
        if (username.length() < UserConstants.USERNAME_MIN_LENGTH || username.length() > UserConstants.USERNAME_MAX_LENGTH)
            return "账号长度必须在 2 到 20 个字符之间";
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
            return "密码长度必须在 5 到 20 个字符之间";
        if (!userService.checkUserNameUnique(sysUser)) return "保存用户 '" + username + "' 失败，注册账号已存在";

        SysRole role = roleMapper.checkRoleKeyUnique(roleKey);
        if (role == null) return "注册失败，系统未配置角色：" + roleKey;
        if (!"0".equals(role.getStatus())) return "注册失败，角色已停用：" + roleKey;

        sysUser.setNickName(username);
        sysUser.setPwdUpdateDate(DateUtils.getNowDate());
        sysUser.setPassword(SecurityUtils.encryptPassword(password));
        sysUser.setRoleIds(new Long[] { role.getRoleId() });
        if (!userService.registerUser(sysUser)) return "注册失败，请联系系统管理员";

        SysUser created = userService.selectUserByUserName(username);
        if (created == null || created.getUserId() == null) return "注册失败，无法获取用户信息";
        int rows = eduPadMapper.upsertUserClassProfile(
                created.getUserId(),
                roleKey,
                majorProfile.getGradeNo(),
                majorProfile.getClassNo(),
                majorProfile.getMajorName(),
                ROLE_KEY_TEACHER.equals(roleKey) ? 1 : 0);
        return rows <= 0 ? "注册失败，专业档案保存失败" : "";
    }

    protected boolean validateMajorOnLogin(Long userId, MajorProfile majorProfile)
    {
        if (majorProfile == null) return false;
        Map<String, Object> profile = eduPadMapper.selectUserClassProfileByUserId(userId);
        if (profile == null || profile.isEmpty())
        {
            String roleKey = resolveUserRoleKey(userId);
            if (StringUtils.isEmpty(roleKey)) return false;
            int rows = eduPadMapper.upsertUserClassProfile(
                    userId,
                    roleKey,
                    majorProfile.getGradeNo(),
                    majorProfile.getClassNo(),
                    majorProfile.getMajorName(),
                    ROLE_KEY_TEACHER.equals(roleKey) ? 1 : 0);
            return rows > 0;
        }
        String storedClassName = String.valueOf(profile.getOrDefault(CLASS_NAME_KEY, ""));
        if (majorProfile.getMajorName().equals(storedClassName))
        {
            return true;
        }
        Object pGrade = profile.get(GRADE_NO_KEY);
        Object pClass = profile.get(CLASS_NO_KEY);
        return pGrade instanceof Number
                && pClass instanceof Number
                && ((Number) pGrade).intValue() == majorProfile.getGradeNo().intValue()
                && ((Number) pClass).intValue() == majorProfile.getClassNo().intValue();
    }

    protected String resolveUserRoleKey(Long userId)
    {
        for (SysRole role : roleMapper.selectRolePermissionByUserId(userId))
        {
            if (ROLE_KEY_TEACHER.equalsIgnoreCase(role.getRoleKey())) return ROLE_KEY_TEACHER;
            if (ROLE_KEY_STUDENT.equalsIgnoreCase(role.getRoleKey()) || ROLE_KEY_DEFAULT_STUDENT.equalsIgnoreCase(role.getRoleKey())) return ROLE_KEY_STUDENT;
        }
        return "";
    }

    protected String normalizeRegisterRoleKey(String roleKey)
    {
        String normalized = StringUtils.trimToEmpty(roleKey).toLowerCase();
        if (ROLE_KEY_TEACHER.equals(normalized)) return ROLE_KEY_TEACHER;
        if (ROLE_KEY_STUDENT.equals(normalized)) return ROLE_KEY_STUDENT;
        return "";
    }

    protected MajorProfile resolveMajorProfile(String majorKey, Integer gradeNo, Integer classNo)
    {
        String rawMajorName = StringUtils.trimToEmpty(majorKey);
        String normalizedMajorKey = normalizeMajorKey(majorKey);
        if (MAJOR_DATA_SCIENCE_KEY.equals(normalizedMajorKey))
        {
            return new MajorProfile(MAJOR_DATA_SCIENCE_KEY, MAJOR_DATA_SCIENCE_NAME, MAJOR_DATA_SCIENCE_GRADE_NO, DEFAULT_MAJOR_CLASS_NO);
        }
        if (MAJOR_NETWORK_ENGINEERING_KEY.equals(normalizedMajorKey))
        {
            return new MajorProfile(MAJOR_NETWORK_ENGINEERING_KEY, MAJOR_NETWORK_ENGINEERING_NAME, MAJOR_NETWORK_ENGINEERING_GRADE_NO, DEFAULT_MAJOR_CLASS_NO);
        }
        if (StringUtils.isNotEmpty(rawMajorName))
        {
            Integer resolvedGradeNo = gradeNo == null ? 0 : gradeNo;
            Integer resolvedClassNo = classNo == null ? DEFAULT_MAJOR_CLASS_NO : classNo;
            return new MajorProfile(buildCustomMajorKey(rawMajorName), rawMajorName, resolvedGradeNo, resolvedClassNo);
        }
        if (gradeNo == null || classNo == null)
        {
            return null;
        }
        if (classNo == 2)
        {
            return new MajorProfile(MAJOR_NETWORK_ENGINEERING_KEY, MAJOR_NETWORK_ENGINEERING_NAME, MAJOR_NETWORK_ENGINEERING_GRADE_NO, DEFAULT_MAJOR_CLASS_NO);
        }
        if (classNo == 1)
        {
            return new MajorProfile(MAJOR_DATA_SCIENCE_KEY, MAJOR_DATA_SCIENCE_NAME, MAJOR_DATA_SCIENCE_GRADE_NO, DEFAULT_MAJOR_CLASS_NO);
        }
        return null;
    }

    protected String normalizeMajorKey(String majorKey)
    {
        String normalized = StringUtils.trimToEmpty(majorKey).toLowerCase(Locale.ROOT);
        if (MAJOR_DATA_SCIENCE_KEY.equals(normalized)) return MAJOR_DATA_SCIENCE_KEY;
        if (MAJOR_NETWORK_ENGINEERING_KEY.equals(normalized)) return MAJOR_NETWORK_ENGINEERING_KEY;
        return "";
    }

    protected String buildCustomMajorKey(String majorName)
    {
        String normalized = StringUtils.trimToEmpty(majorName)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
        return StringUtils.isEmpty(normalized) ? "custom-major" : normalized;
    }

    protected boolean hasManagerRole(LoginUser loginUser)
    {
        if (loginUser == null || loginUser.getUser() == null)
        {
            return false;
        }
        if (loginUser.getUser().isAdmin())
        {
            return true;
        }
        if (loginUser.getUser().getRoles() == null)
        {
            return false;
        }
        for (SysRole role : loginUser.getUser().getRoles())
        {
            String roleKey = role == null ? "" : StringUtils.trimToEmpty(role.getRoleKey()).toLowerCase(Locale.ROOT);
            if (roleKey.contains("admin") || roleKey.contains("manager"))
            {
                return true;
            }
        }
        return false;
    }

    protected String extractReviewImageUrlFromFeedback(String feedback)
    {
        String value = StringUtils.trimToEmpty(feedback);
        if (StringUtils.isEmpty(value)) return "";
        Matcher matcher = REVIEW_IMAGE_PATTERN.matcher(value);
        return matcher.find() ? StringUtils.trimToEmpty(matcher.group(1)) : "";
    }

    protected void fillTeacherInfo(EduHomework homework) { homework.setTeacherId(currentUserId()); homework.setTeacherName(currentUserNickName()); }

    protected void notifyHomeworkReviewed(Map<String, Object> current, EduHomeworkSubmission submission)
    {
        if (current == null || submission == null) return;
        Long studentId = toLong(current.get("student_id"));
        if (studentId == null || studentId <= 0) return;
        String className = String.valueOf(current.getOrDefault("class_name", ""));
        String homeworkTitle = String.valueOf(current.getOrDefault("homework_title", "作业"));
        String feedback = StringUtils.trimToEmpty(submission.getFeedback()).replaceAll("\\n?\\[REVIEW_IMAGE\\]\\(([^)\\s]+)\\)\\s*", "").trim();
        String brief = StringUtils.isEmpty(feedback) ? "" : "，评语：" + StringUtils.substring(feedback, 0, 40);
        String msgContent = "你的《" + homeworkTitle + "》已批改，分数：" + submission.getScore() + brief;
        try
        {
            EduForumPost msg = new EduForumPost();
            msg.setTitle(CHAT_DM_TITLE);
            msg.setContent(msgContent);
            msg.setAuthorId(currentUserId());
            msg.setAuthorName(currentUserNickName());
            msg.setAuthorRole(resolveCurrentForumRole());
            msg.setTargetRole(String.valueOf(studentId));
            msg.setClassName(className);
            eduPadMapper.insertForumPost(msg);
        }
        catch (Exception ex)
        {
            log.warn("作业批改消息发送失败: submissionId={}, studentId={}, reason={}", submission.getSubmissionId(), studentId, ex.getMessage());
        }
    }

    protected String resolveCurrentForumRole()
    {
        List<SysRole> roles = roleMapper.selectRolePermissionByUserId(SecurityUtils.getUserId());
        if (roles != null)
        {
            for (SysRole role : roles)
            {
                String roleKey = role.getRoleKey();
                if ("admin".equalsIgnoreCase(roleKey) || "manager".equalsIgnoreCase(roleKey)) return ROLE_MANAGER;
            }
            for (SysRole role : roles)
            {
                if (ROLE_KEY_TEACHER.equalsIgnoreCase(role.getRoleKey())) return ROLE_TEACHER;
            }
        }
        return ROLE_STUDENT;
    }

    protected String normalizeTargetRole(String targetRole, String currentRole)
    {
        String normalized = targetRole == null ? ROLE_ALL : targetRole.trim().toUpperCase();
        if (normalized.isEmpty()) normalized = ROLE_ALL;
        if (!(ROLE_ALL.equals(normalized) || ROLE_MANAGER.equals(normalized) || ROLE_TEACHER.equals(normalized) || ROLE_STUDENT.equals(normalized))) return null;
        if (ROLE_MANAGER.equals(currentRole)) return normalized;
        if (ROLE_TEACHER.equals(currentRole)) return (ROLE_STUDENT.equals(normalized) || ROLE_ALL.equals(normalized)) ? normalized : null;
        if (ROLE_STUDENT.equals(currentRole)) return (ROLE_TEACHER.equals(normalized) || ROLE_ALL.equals(normalized)) ? normalized : null;
        return ROLE_ALL;
    }

    protected boolean isTeacherRole()
    {
        List<SysRole> roles = roleMapper.selectRolePermissionByUserId(SecurityUtils.getUserId());
        return roles != null && roles.stream().anyMatch(role -> ROLE_KEY_TEACHER.equalsIgnoreCase(role.getRoleKey()));
    }

    protected String getCurrentClassName()
    {
        Map<String, Object> profile = eduPadMapper.selectUserClassProfileByUserId(SecurityUtils.getUserId());
        if (profile == null) return null;
        Object className = profile.get(CLASS_NAME_KEY);
        return className == null ? null : String.valueOf(className);
    }

    protected String getMappedLegacyStudentNo()
    {
        return eduLegacyAnalyticsMapper.selectMappedStudentNoByCurrentUserId(currentUserId());
    }

    protected String getMappedLegacyClassCode()
    {
        String className = getCurrentClassName();
        if (StringUtils.isEmpty(className)) return "";
        return StringUtils.trimToEmpty(eduLegacyAnalyticsMapper.selectMappedClassCodeByCurrentClassName(className));
    }

    protected String getCurrentMajorKey()
    {
        Map<String, Object> profile = eduPadMapper.selectUserClassProfileByUserId(SecurityUtils.getUserId());
        if (profile == null || profile.isEmpty())
        {
            return "";
        }
        Object gradeNo = profile.get(GRADE_NO_KEY);
        if (gradeNo instanceof Number)
        {
            int currentGradeNo = ((Number) gradeNo).intValue();
            if (currentGradeNo == MAJOR_DATA_SCIENCE_GRADE_NO)
            {
                return MAJOR_DATA_SCIENCE_KEY;
            }
            if (currentGradeNo == MAJOR_NETWORK_ENGINEERING_GRADE_NO)
            {
                return MAJOR_NETWORK_ENGINEERING_KEY;
            }
        }
        String className = String.valueOf(profile.getOrDefault(CLASS_NAME_KEY, ""));
        if (className.contains("网络"))
        {
            return MAJOR_NETWORK_ENGINEERING_KEY;
        }
        if (StringUtils.isNotEmpty(className))
        {
            return buildCustomMajorKey(className);
        }
        return "";
    }

    protected List<String> getCurrentLegacyClassCodesForMajor()
    {
        String majorKey = getCurrentMajorKey();
        if (MAJOR_DATA_SCIENCE_KEY.equals(majorKey))
        {
            return Arrays.asList("1002", "1005", "1008", "1009", "1010", "1011", "1012", "1013", "1014", "1015", "1016");
        }
        return Collections.emptyList();
    }

    protected List<String> getCurrentLegacyCourseWhitelist()
    {
        String majorKey = getCurrentMajorKey();
        if (MAJOR_DATA_SCIENCE_KEY.equals(majorKey))
        {
            return Arrays.asList(
                    "数据结构-二工大",
                    "C语言（新）",
                    "程序设计",
                    "数据库-二工大",
                    "华为ICT-AI",
                    "华为ICT-云计算&大数据",
                    "linux操作系统",
                    "大基(新)",
                    "PYTHON选择题库");
        }
        return Collections.emptyList();
    }

    protected AjaxResult legacyBoundEmptyResult(String message)
    {
        AjaxResult ajax = AjaxResult.success();
        ajax.put("bound", false);
        ajax.put("message", message);
        ajax.put("data", Collections.emptyList());
        return ajax;
    }

    protected AjaxResult legacyBoundMapEmptyResult(String message)
    {
        AjaxResult ajax = AjaxResult.success();
        ajax.put("bound", false);
        ajax.put("message", message);
        ajax.put("data", Collections.emptyMap());
        return ajax;
    }

    protected boolean canChatWith(Long peerUserId)
    {
        Long current = SecurityUtils.getUserId();
        if (peerUserId == null || current.equals(peerUserId)) return false;
        Map<String, Object> me = eduPadMapper.selectUserClassProfileByUserId(current);
        Map<String, Object> peer = eduPadMapper.selectUserClassProfileByUserId(peerUserId);
        if (me == null || peer == null) return false;
        String currentClassName = String.valueOf(me.get(CLASS_NAME_KEY));
        String peerClassName = String.valueOf(peer.get(CLASS_NAME_KEY));
        if (StringUtils.isEmpty(currentClassName) || !currentClassName.equals(peerClassName)) return false;
        String currentRole = String.valueOf(me.get(ROLE_KEY_KEY));
        String peerRole = String.valueOf(peer.get(ROLE_KEY_KEY));
        return (ROLE_KEY_TEACHER.equalsIgnoreCase(currentRole) && ROLE_KEY_STUDENT.equalsIgnoreCase(peerRole))
                || (ROLE_KEY_STUDENT.equalsIgnoreCase(currentRole) && ROLE_KEY_TEACHER.equalsIgnoreCase(peerRole));
    }

    protected Long currentUserId() { return SecurityUtils.getUserId(); }
    protected String currentUserNickName() { return SecurityUtils.getLoginUser().getUser().getNickName(); }
    protected AjaxResult emptyListSuccess() { return success(Collections.emptyList()); }

    protected int toInt(Object value, int defaultValue)
    {
        if (value == null) return defaultValue;
        try { return Integer.parseInt(String.valueOf(value)); } catch (Exception ex) { return defaultValue; }
    }

    protected Long toLong(Object value)
    {
        if (value == null) return null;
        try { return Long.parseLong(String.valueOf(value)); } catch (Exception ex) { return null; }
    }

    protected double computeTextSimilarity(String a, String b)
    {
        if (StringUtils.isEmpty(a) || StringUtils.isEmpty(b)) return 0.0;
        Set<String> gramsA = buildBigrams(a);
        Set<String> gramsB = buildBigrams(b);
        if (gramsA.isEmpty() || gramsB.isEmpty()) return 0.0;
        int inter = 0;
        for (String item : gramsA) if (gramsB.contains(item)) inter++;
        return (2.0 * inter) / (gramsA.size() + gramsB.size());
    }

    protected Set<String> buildBigrams(String s)
    {
        Set<String> set = new HashSet<>();
        String raw = s.replaceAll("\\s+", "");
        if (raw.length() < 2)
        {
            if (!raw.isEmpty()) set.add(raw);
            return set;
        }
        for (int i = 0; i < raw.length() - 1; i++) set.add(raw.substring(i, i + 2));
        return set;
    }

    protected List<Map<String, Object>> cleanCatalogs(List<Map<String, Object>> source)
    {
        if (source == null || source.isEmpty())
        {
            return Collections.emptyList();
        }
        Map<String, Map<String, Object>> cleaned = new LinkedHashMap<>();
        for (Map<String, Object> item : source)
        {
            Map<String, Object> normalized = normalizeCatalog(item);
            if (normalized == null)
            {
                continue;
            }
            String courseName = String.valueOf(normalized.getOrDefault("courseName", ""));
            String chapterCode = String.valueOf(normalized.getOrDefault("chapterCode", ""));
            String chapterName = String.valueOf(normalized.getOrDefault("chapterName", ""));
            cleaned.putIfAbsent(courseName + "||" + chapterCode + "||" + chapterName, normalized);
        }
        return new ArrayList<>(cleaned.values());
    }

    protected Map<String, Object> normalizeCatalog(Map<String, Object> item)
    {
        if (item == null || item.isEmpty())
        {
            return null;
        }
        String courseName = normalizeCatalogText(item.getOrDefault("courseName", item.get("course_name")));
        String chapterCode = normalizeCatalogText(item.getOrDefault("chapterCode", item.get("chapter_code")));
        String chapterName = normalizeCatalogText(item.getOrDefault("chapterName", item.get("chapter_name")));
        if (StringUtils.isEmpty(chapterCode))
        {
            chapterCode = chapterName;
        }
        if (StringUtils.isEmpty(chapterName))
        {
            chapterName = chapterCode;
        }
        if (!isValidCatalogValue(courseName) || !isValidCatalogValue(chapterName))
        {
            return null;
        }
        Map<String, Object> normalized = new LinkedHashMap<>(item);
        normalized.put("courseName", courseName);
        normalized.put("chapterCode", chapterCode);
        normalized.put("chapterName", chapterName);
        return normalized;
    }

    protected String normalizeCatalogText(Object value)
    {
        String text = StringUtils.trimToEmpty(value == null ? "" : String.valueOf(value));
        if ("null".equalsIgnoreCase(text) || "undefined".equalsIgnoreCase(text))
        {
            return "";
        }
        return text;
    }

    protected boolean isValidCatalogValue(String value)
    {
        String normalized = normalizeCatalogText(value);
        if (StringUtils.isEmpty(normalized))
        {
            return false;
        }
        String lower = normalized.toLowerCase();
        if (lower.matches("^\\d+$"))
        {
            return false;
        }
        return !Arrays.asList(
                "未分类",
                "未命名课程",
                "未命名模块",
                "综合模块",
                "unnamed course",
                "unnamed module",
                "no code",
                "null",
                "undefined",
                "?")
                .contains(lower);
    }
}
