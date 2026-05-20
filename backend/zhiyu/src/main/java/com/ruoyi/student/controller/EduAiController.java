package com.ruoyi.student.controller;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.student.service.EduAiProxyService;
import com.ruoyi.student.service.EduAiInteractionService;
import com.ruoyi.student.service.EduAiScopeService;
import com.ruoyi.student.service.EduAiStudentProfileService;
import com.ruoyi.student.service.EduAiTeacherProfileService;

@RestController
@RequestMapping("/education/ai")
@PreAuthorize("@ss.hasRole('teacher') or @ss.hasRole('student') or @ss.hasRole('admin') or @ss.hasRole('manager')")
public class EduAiController
{
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EduAiController.class);

    @Autowired
    private EduAiProxyService eduAiProxyService;

    @Autowired
    private EduAiScopeService eduAiScopeService;

    @Autowired
    private EduAiInteractionService eduAiInteractionService;

    @Autowired
    private EduAiStudentProfileService eduAiStudentProfileService;

    @Autowired
    private EduAiTeacherProfileService eduAiTeacherProfileService;

    @PostMapping({"/interactions/log", "/interactionLog"})
    public com.ruoyi.common.core.domain.AjaxResult logInteraction(@RequestBody Map<String, Object> body)
    {
        return com.ruoyi.common.core.domain.AjaxResult.success(eduAiInteractionService.logInteraction(body));
    }

    @PostMapping("/rag/upload-excel")
    public Map<String, Object> uploadRagFiles(
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "file", required = false) MultipartFile file)
    {
        if ((files == null || files.length == 0) && file != null)
        {
            files = new MultipartFile[] { file };
        }
        Map<String, Object> result = eduAiProxyService.uploadRagFiles(files);
        eduAiScopeService.bindUploadedDatasets(result);
        return result;
    }

    @PostMapping("/rag/import-current-scene")
    public Map<String, Object> importCurrentScene(
            @RequestParam(value = "sourceScene", required = false) String sourceScene,
            @RequestParam(value = "studentNo", required = false) String studentNo,
            @RequestParam(value = "studentName", required = false) String studentName,
            @RequestParam(value = "courseName", required = false) String courseName,
            @RequestParam(value = "chapterCode", required = false) String chapterCode,
            @RequestParam(value = "chapterName", required = false) String chapterName)
    {
        try
        {
            boolean teacherView = isTeacherSideUser();
            log.info("导入当前场景开始: userId={}, teacherView={}, sourceScene={}, studentNo={}, studentName={}, courseName={}, chapterCode={}, chapterName={}",
                    safeUserId(),
                    teacherView,
                    StringUtils.trimToEmpty(sourceScene),
                    StringUtils.trimToEmpty(studentNo),
                    StringUtils.trimToEmpty(studentName),
                    StringUtils.trimToEmpty(courseName),
                    StringUtils.trimToEmpty(chapterCode),
                    StringUtils.trimToEmpty(chapterName));
            Map<String, Object> sceneDataset = teacherView
                    ? eduAiTeacherProfileService.buildCurrentTeacherSceneDataset(
                            sourceScene,
                            studentNo,
                            studentName,
                            courseName,
                            chapterCode,
                            chapterName)
                    : eduAiStudentProfileService.buildCurrentStudentSceneDataset(
                            sourceScene,
                            courseName,
                            chapterCode,
                            chapterName);
            Map<String, Object> result = eduAiProxyService.uploadRagTextFile(
                    String.valueOf(sceneDataset.get("fileName")),
                    String.valueOf(sceneDataset.get("content")));
            eduAiScopeService.bindUploadedDatasets(result);
            result.put("sceneProfile", sceneDataset.get("profile"));
            result.put("sceneFileName", sceneDataset.get("fileName"));
            log.info("导入当前场景完成: userId={}, sceneFileName={}", safeUserId(), String.valueOf(sceneDataset.get("fileName")));
            return result;
        }
        catch (Exception ex)
        {
            log.error("导入当前场景失败: userId={}, sourceScene={}, studentNo={}, courseName={}, chapterCode={}, chapterName={}",
                    safeUserId(),
                    StringUtils.trimToEmpty(sourceScene),
                    StringUtils.trimToEmpty(studentNo),
                    StringUtils.trimToEmpty(courseName),
                    StringUtils.trimToEmpty(chapterCode),
                    StringUtils.trimToEmpty(chapterName),
                    ex);
            throw ex;
        }
    }

    @GetMapping("/rag/query")
    public Map<String, Object> queryRag(@RequestParam("question") String question,
                                        @RequestParam(value = "sourceScene", required = false) String sourceScene,
                                        @RequestParam(value = "studentNo", required = false) String studentNo,
                                        @RequestParam(value = "studentName", required = false) String studentName,
                                        @RequestParam(value = "courseName", required = false) String courseName,
                                        @RequestParam(value = "chapterCode", required = false) String chapterCode,
                                        @RequestParam(value = "chapterName", required = false) String chapterName,
                                        @RequestParam(value = "knowledgePoint", required = false) String knowledgePoint,
                                        @RequestParam(value = "questionId", required = false) Long questionId)
    {
        java.util.List<Long> datasetIds = eduAiScopeService.listCurrentUserDatasetIds();
        if (datasetIds == null || datasetIds.isEmpty())
        {
            datasetIds = java.util.Collections.singletonList(-1L);
        }
        boolean teacherView = isTeacherSideUser();
        String businessContext = teacherView
                ? eduAiTeacherProfileService.buildCurrentTeacherBusinessContext(studentNo, studentName, courseName, chapterCode, chapterName)
                : eduAiStudentProfileService.buildCurrentStudentBusinessContext(courseName, chapterCode, chapterName);
        Map<String, Object> result = eduAiProxyService.queryRag(
                eduAiScopeService.buildScopedQuestion(question, sourceScene, courseName, chapterName, knowledgePoint),
                datasetIds,
                businessContext);
        Object answer = result.get("answer");
        if (answer == null)
        {
            answer = result.get("data");
        }
        if (answer != null)
        {
            java.util.Map<String, Object> interaction = new java.util.LinkedHashMap<>();
            interaction.put("askedQuestion", question);
            interaction.put("answerSnapshot", String.valueOf(answer));
            interaction.put("sourceScene", StringUtils.trimToEmpty(sourceScene));
            interaction.put("studentNo", StringUtils.trimToEmpty(studentNo));
            interaction.put("courseName", StringUtils.trimToEmpty(courseName));
            interaction.put("chapterCode", StringUtils.trimToEmpty(chapterCode));
            interaction.put("chapterName", StringUtils.trimToEmpty(chapterName));
            interaction.put("knowledgePoint", StringUtils.trimToEmpty(knowledgePoint));
            interaction.put("questionId", questionId);
            eduAiInteractionService.logInteraction(interaction);
        }
        return result;
    }

    @GetMapping("/rag/datasets")
    public Map<String, Object> getDatasets()
    {
        Map<String, Object> result = eduAiProxyService.getDatasets();
        Object rows = result.get("data");
        if (rows instanceof java.util.List)
        {
            result.put("data", eduAiScopeService.filterDatasets((java.util.List<Map<String, Object>>) rows));
        }
        return result;
    }

    @GetMapping("/rag/datasets/{datasetId}")
    public Map<String, Object> getDatasetDetail(@PathVariable Long datasetId)
    {
        if (!eduAiScopeService.canAccessDataset(datasetId))
        {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "无权查看该知识库");
        }
        return eduAiProxyService.getDatasetDetail(datasetId);
    }

    @DeleteMapping("/rag/datasets/{datasetId}")
    public Map<String, Object> deleteDataset(@PathVariable Long datasetId)
    {
        if (!eduAiScopeService.canAccessDataset(datasetId))
        {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "无权删除该知识库");
        }
        return eduAiProxyService.deleteDataset(datasetId);
    }

    @PostMapping("/prediction/train")
    public Map<String, Object> trainPredictionModel(@RequestParam("file") MultipartFile file)
    {
        return eduAiProxyService.trainPredictionModel(file);
    }

    @GetMapping("/prediction/model-info")
    public Map<String, Object> getPredictionModelInfo()
    {
        return eduAiProxyService.getModelInfo();
    }

    @PostMapping("/prediction/predict")
    public Map<String, Object> predictScore(@RequestBody Map<String, Object> body)
    {
        Map<String, Object> result = eduAiProxyService.predictScore(body);
        if ("success".equals(String.valueOf(result.get("status"))))
        {
            eduAiStudentProfileService.saveCurrentUserPredictionSnapshot(body, result);
        }
        return result;
    }

    @PreAuthorize("@ss.hasRole('student')")
    @PostMapping("/prediction/ai-enhanced")
    public Map<String, Object> predictScoreWithAi(@RequestBody Map<String, Object> body)
    {
        String businessContext = eduAiStudentProfileService.buildCurrentStudentBusinessContext("", "", "");
        Map<String, Object> result = eduAiProxyService.predictScoreWithAi(body, businessContext);
        if ("success".equals(String.valueOf(result.get("status"))))
        {
            eduAiStudentProfileService.saveCurrentUserPredictionSnapshot(body, result);
        }
        return result;
    }

    @PostMapping("/grading/reference")
    public Map<String, Object> uploadAiReference(@RequestParam("file") MultipartFile file)
    {
        return eduAiProxyService.uploadAiReference(file);
    }

    @PostMapping("/grading/single")
    public Map<String, Object> aiGradeSingle(
            @RequestParam("studentFile") MultipartFile studentFile,
            @RequestParam(value = "referenceId", required = false) String referenceId,
            @RequestParam(value = "rubric", required = false) String rubric,
            @RequestParam(value = "maxScore", required = false) Integer maxScore,
            @RequestParam(value = "questionCount", required = false) Integer questionCount)
    {
        return eduAiProxyService.aiGradeSingle(studentFile, referenceId, rubric, maxScore, questionCount);
    }

    @PostMapping("/grading/batch")
    public Map<String, Object> aiGradeBatch(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "referenceId", required = false) String referenceId,
            @RequestParam(value = "rubric", required = false) String rubric,
            @RequestParam(value = "maxScore", required = false) Integer maxScore,
            @RequestParam(value = "questionCount", required = false) Integer questionCount)
    {
        return eduAiProxyService.aiGradeBatch(files, referenceId, rubric, maxScore, questionCount);
    }

    @GetMapping("/files/**")
    public ResponseEntity<byte[]> fetchAiFile(HttpServletRequest request)
    {
        return eduAiProxyService.fetchAiFile(request);
    }

    private boolean isTeacherSideUser()
    {
        return hasRole("teacher") || hasRole("admin") || hasRole("manager");
    }

    private boolean hasRole(String roleKey)
    {
        try
        {
            return SecurityUtils.hasRole(roleKey);
        }
        catch (Exception ignore)
        {
            return false;
        }
    }

    private Long safeUserId()
    {
        try
        {
            return SecurityUtils.getUserId();
        }
        catch (Exception ignore)
        {
            return null;
        }
    }
}
