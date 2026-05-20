package com.ruoyi.student.controller;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.student.domain.EduExam;
import com.ruoyi.student.domain.EduExamScore;
import com.ruoyi.student.domain.EduHomeworkSubmission;

@RestController
@RequestMapping("/education/pad")
public class EduPadStudentController extends EduPadSupport
{
    @PreAuthorize("@ss.hasRole('student')")
    @GetMapping("/homework/student")
    public AjaxResult listStudentHomework()
    {
        String className = getCurrentClassName();
        if (StringUtils.isEmpty(className)) return emptyListSuccess();
        return success(eduPadMapper.selectHomeworkByClassName(className));
    }

    @PreAuthorize("@ss.hasRole('student')")
    @PostMapping("/homework/{homeworkId}/submit")
    public AjaxResult submitHomework(@PathVariable Long homeworkId, @RequestBody EduHomeworkSubmission submission)
    {
        Long studentId = SecurityUtils.getUserId();
        if (submission == null)
        {
            log.warn("学生提交作业参数为空: homeworkId={}, studentId={}", homeworkId, studentId);
            return error("提交参数不能为空");
        }
        String answerContent = submission.getAnswerContent();
        log.info("学生提交作业请求: homeworkId={}, studentId={}, answerLength={}, hasImageMarker={}", homeworkId, studentId, answerContent == null ? 0 : answerContent.length(), answerContent != null && answerContent.contains("![作答图片]("));
        Long exists = eduPadMapper.selectHomeworkSubmissionExists(homeworkId, studentId);
        if (exists != null)
        {
            log.warn("学生重复提交作业: homeworkId={}, studentId={}, submissionId={}", homeworkId, studentId, exists);
            return error("该作业已提交，无需重复提交");
        }
        submission.setHomeworkId(homeworkId);
        submission.setStudentId(studentId);
        submission.setStudentName(SecurityUtils.getLoginUser().getUser().getNickName());
        submission.setSubmitTime(new Date());
        int rows = eduPadMapper.insertHomeworkSubmission(submission);
        log.info("学生提交作业完成: homeworkId={}, studentId={}, rows={}", homeworkId, studentId, rows);
        return toAjax(rows);
    }

    @PreAuthorize("@ss.hasRole('student')")
    @GetMapping("/homework/submissions/student")
    public AjaxResult listHomeworkSubmissionsByStudent()
    {
        List<Map<String, Object>> list = eduPadMapper.selectHomeworkSubmissionByStudentId(SecurityUtils.getUserId());
        if (list == null) list = Collections.emptyList();
        int hasImageCount = 0;
        for (Map<String, Object> item : list)
        {
            String feedback = String.valueOf(item == null ? "" : item.getOrDefault("feedback", ""));
            if (StringUtils.isNotEmpty(extractReviewImageUrlFromFeedback(feedback))) hasImageCount++;
        }
        log.info("查询学生作业提交: studentId={}, total={}, withReviewImage={}", SecurityUtils.getUserId(), list.size(), hasImageCount);
        return success(list);
    }

    @PreAuthorize("@ss.hasRole('student')")
    @GetMapping("/exam/student")
    public AjaxResult listStudentExam()
    {
        String className = getCurrentClassName();
        if (StringUtils.isEmpty(className)) return emptyListSuccess();
        return success(eduPadMapper.selectExamByClassName(className));
    }

    @PreAuthorize("@ss.hasRole('student')")
    @PostMapping("/exam/{examId}/submit")
    public AjaxResult submitExam(@PathVariable Long examId, @RequestBody Map<String, Object> body)
    {
        EduExam exam = eduPadMapper.selectExamById(examId);
        if (exam == null) return error("考试不存在");
        String className = getCurrentClassName();
        if (StringUtils.isEmpty(className) || !className.equals(exam.getClassName())) return error("只能提交自己班级的考试");
        String answerContent = body == null ? "" : String.valueOf(body.getOrDefault("answerContent", "")).trim();
        if (StringUtils.isEmpty(answerContent)) return error("请填写考试作答内容");
        EduExamScore examScore = new EduExamScore();
        examScore.setExamId(examId);
        examScore.setStudentId(currentUserId());
        examScore.setStudentName(currentUserNickName());
        examScore.setScore(null);
        examScore.setRemark(answerContent);
        return toAjax(eduPadMapper.upsertExamScore(examScore));
    }

    @PreAuthorize("@ss.hasRole('student')")
    @GetMapping("/exam/score/student")
    public AjaxResult listStudentExamScore()
    {
        return success(eduPadMapper.selectExamScoreByStudentId(SecurityUtils.getUserId()));
    }

    @PreAuthorize("@ss.hasRole('student')")
    @GetMapping("/student/scores")
    public AjaxResult listStudentSelfScores()
    {
        AjaxResult result = AjaxResult.success();
        result.put("examScores", eduPadMapper.selectExamScoreByStudentId(currentUserId()));
        List<Map<String, Object>> selfPerformance = eduPadMapper.selectStudentPerformanceByStudentId(currentUserId());
        result.put("performanceScores", selfPerformance == null ? Collections.emptyList() : selfPerformance);
        return result;
    }

    @PreAuthorize("@ss.hasRole('student')")
    @GetMapping("/question/catalogs")
    public AjaxResult listQuestionCatalogs(@RequestParam(required = false) String courseName)
    {
        String studentNo = getMappedLegacyStudentNo();
        if (StringUtils.isEmpty(studentNo))
        {
            return legacyBoundEmptyResult("当前账号暂未绑定真实历史作答目录数据。");
        }
        List<Map<String, Object>> data = cleanCatalogs(eduLegacyAnalyticsMapper.selectStudentHistoryCatalogs(
                studentNo,
                StringUtils.trimToEmpty(courseName)));
        AjaxResult result = AjaxResult.success();
        result.put("bound", true);
        result.put("data", data);
        return result;
    }

    @PreAuthorize("@ss.hasRole('student')")
    @GetMapping("/student/history/catalogs")
    public AjaxResult listStudentHistoryCatalogs(@RequestParam(required = false) String courseName)
    {
        String studentNo = getMappedLegacyStudentNo();
        if (StringUtils.isEmpty(studentNo))
        {
            return legacyBoundEmptyResult("当前账号尚未绑定历史做题数据，暂时无法生成课程和模块筛选项。");
        }
        AjaxResult result = AjaxResult.success();
        result.put("bound", true);
        result.put("data", cleanCatalogs(eduLegacyAnalyticsMapper.selectStudentHistoryCatalogs(studentNo, StringUtils.trimToEmpty(courseName))));
        return result;
    }

    @PreAuthorize("@ss.hasRole('student')")
    @GetMapping("/student/diagnosis/overview")
    public AjaxResult getStudentDiagnosisOverview()
    {
        String studentNo = getMappedLegacyStudentNo();
        if (StringUtils.isEmpty(studentNo)) return legacyBoundMapEmptyResult("当前账号尚未绑定历史做题数据，暂时无法生成真实诊断。");
        AjaxResult result = AjaxResult.success();
        result.put("bound", true);
        result.put("data", eduLegacyAnalyticsMapper.selectStudentDiagnosisOverview(studentNo));
        return result;
    }

    @PreAuthorize("@ss.hasRole('student')")
    @GetMapping("/student/diagnosis/chapters")
    public AjaxResult getStudentDiagnosisChapters()
    {
        String studentNo = getMappedLegacyStudentNo();
        if (StringUtils.isEmpty(studentNo)) return legacyBoundEmptyResult("当前账号尚未绑定历史做题数据，暂时无法查看章节诊断。");
        AjaxResult result = AjaxResult.success();
        result.put("bound", true);
        result.put("data", eduLegacyAnalyticsMapper.selectStudentChapterDiagnosis(studentNo));
        return result;
    }

    @PreAuthorize("@ss.hasRole('student')")
    @GetMapping("/student/diagnosis/wrong-questions")
    public AjaxResult getStudentWrongQuestions(@RequestParam(defaultValue = "10") Integer limit)
    {
        String studentNo = getMappedLegacyStudentNo();
        if (StringUtils.isEmpty(studentNo)) return legacyBoundEmptyResult("当前账号尚未绑定历史做题数据，暂时无法查看高频错题。");
        AjaxResult result = AjaxResult.success();
        result.put("bound", true);
        result.put("data", eduLegacyAnalyticsMapper.selectStudentWrongQuestions(studentNo, Math.max(1, Math.min(limit, 20))));
        return result;
    }

    @PreAuthorize("@ss.hasRole('student')")
    @GetMapping("/student/diagnosis/knowledge-points")
    public AjaxResult getStudentWeakKnowledgePoints(@RequestParam(defaultValue = "10") Integer limit)
    {
        String studentNo = getMappedLegacyStudentNo();
        if (StringUtils.isEmpty(studentNo)) return legacyBoundEmptyResult("当前账号尚未绑定历史做题数据，暂时无法查看薄弱知识点。");
        AjaxResult result = AjaxResult.success();
        result.put("bound", true);
        result.put("data", eduLegacyAnalyticsMapper.selectStudentWeakKnowledgePoints(studentNo, Math.max(1, Math.min(limit, 20))));
        return result;
    }

    @PreAuthorize("@ss.hasRole('student')")
    @GetMapping("/student/recommendations/practice")
    public AjaxResult getStudentPracticeRecommendations(@RequestParam(required = false) String courseName,
                                                        @RequestParam(required = false) String chapterCode,
                                                        @RequestParam(required = false) String chapterName,
                                                        @RequestParam(defaultValue = "8") Integer limit)
    {
        String studentNo = getMappedLegacyStudentNo();
        if (StringUtils.isEmpty(studentNo)) return legacyBoundEmptyResult("当前账号尚未绑定历史做题数据，暂时无法生成真实刷题推荐。");

        String targetCourseName = StringUtils.trimToEmpty(courseName);
        String targetChapterCode = StringUtils.trimToEmpty(chapterCode);
        String targetChapterName = StringUtils.trimToEmpty(chapterName);
        if (StringUtils.isEmpty(targetChapterCode))
        {
            List<Map<String, Object>> chapters = eduLegacyAnalyticsMapper.selectStudentChapterDiagnosis(studentNo);
            if (chapters != null && !chapters.isEmpty())
            {
                Map<String, Object> weakest = chapters.get(0);
                if (StringUtils.isEmpty(targetCourseName)) targetCourseName = String.valueOf(weakest.getOrDefault("courseName", ""));
                targetChapterCode = String.valueOf(weakest.getOrDefault("chapterCode", ""));
                if (StringUtils.isEmpty(targetChapterName)) targetChapterName = String.valueOf(weakest.getOrDefault("chapterName", ""));
            }
        }

        AjaxResult result = AjaxResult.success();
        result.put("bound", true);
        result.put("courseName", targetCourseName);
        result.put("chapterCode", targetChapterCode);
        int safeLimit = Math.max(1, Math.min(limit, 20));
        List<Map<String, Object>> data = eduLegacyAnalyticsMapper.selectPracticeRecommendationCandidates(
                studentNo,
                targetCourseName,
                targetChapterCode,
                targetChapterName,
                safeLimit);
        if ((data == null || data.isEmpty()) && StringUtils.isNotEmpty(targetCourseName))
        {
            data = eduLegacyAnalyticsMapper.selectPracticeRecommendationCandidates(
                    studentNo,
                    "",
                    targetChapterCode,
                    targetChapterName,
                    safeLimit);
        }
        if (data == null || data.isEmpty())
        {
            data = eduLegacyAnalyticsMapper.selectPracticeRecommendationCandidates(
                    studentNo,
                    "",
                    "",
                    "",
                    safeLimit);
        }
        result.put("chapterName", targetChapterName);
        result.put("data", data == null ? Collections.emptyList() : data);
        return result;
    }

    @PreAuthorize("@ss.hasRole('student')")
    @GetMapping("/student/program/overview")
    public AjaxResult getStudentProgramOverview()
    {
        String studentNo = getMappedLegacyStudentNo();
        if (StringUtils.isEmpty(studentNo)) return legacyBoundMapEmptyResult("当前账号尚未绑定历史做题数据，暂时无法查看编程题表现。");
        AjaxResult result = AjaxResult.success();
        result.put("bound", true);
        result.put("data", eduLegacyAnalyticsMapper.selectStudentProgramOverview(studentNo));
        return result;
    }

    @PreAuthorize("@ss.hasRole('student')")
    @GetMapping("/student/program/assignments")
    public AjaxResult getStudentProgramAssignments(@RequestParam(defaultValue = "5") Integer limit)
    {
        String studentNo = getMappedLegacyStudentNo();
        if (StringUtils.isEmpty(studentNo)) return legacyBoundEmptyResult("当前账号尚未绑定历史做题数据，暂时无法查看编程题作业表现。");
        AjaxResult result = AjaxResult.success();
        result.put("bound", true);
        result.put("data", eduLegacyAnalyticsMapper.selectStudentProgramWeakAssignments(studentNo, Math.max(1, Math.min(limit, 10))));
        return result;
    }

    @PreAuthorize("@ss.hasRole('student')")
    @GetMapping("/student/history/answers")
    public AjaxResult listStudentAnswerHistory(@RequestParam(required = false) String courseName,
                                               @RequestParam(required = false) String chapterCode,
                                               @RequestParam(required = false) String questionType,
                                               @RequestParam(defaultValue = "40") Integer limit)
    {
        String studentNo = getMappedLegacyStudentNo();
        if (StringUtils.isEmpty(studentNo)) return legacyBoundEmptyResult("当前账号尚未绑定历史做题数据，暂时无法查看历史做题情况。");
        AjaxResult result = AjaxResult.success();
        result.put("bound", true);
        result.put("data", eduLegacyAnalyticsMapper.selectStudentAnswerHistory(
                studentNo,
                StringUtils.trimToEmpty(courseName),
                StringUtils.trimToEmpty(chapterCode),
                StringUtils.trimToEmpty(questionType),
                Math.max(10, Math.min(limit, 100))));
        return result;
    }

    @Autowired
    private com.ruoyi.student.service.EduAiStudentProfileService eduAiStudentProfileService;

    @PreAuthorize("@ss.hasRole('student')")
    @GetMapping("/student/rag/profile")
    public AjaxResult getStudentRagProfile(@RequestParam(required = false) String courseName,
                                           @RequestParam(required = false) String chapterCode,
                                           @RequestParam(required = false) String chapterName)
    {
        return success(eduAiStudentProfileService.buildCurrentStudentRagProfile(courseName, chapterCode, chapterName));
    }
}
