package com.ruoyi.student.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.student.service.EduAiProxyService;

@RestController
@PreAuthorize("@ss.hasRole('teacher') or @ss.hasRole('student') or @ss.hasRole('admin') or @ss.hasRole('manager')")
public class EduAiLegacyController
{
    @Autowired
    private EduAiProxyService eduAiProxyService;

    /**
     * Backward-compatible aliases for historical frontend bundles that still call
     * legacy RAG and prediction endpoints directly on Spring Boot.
     */
    @PostMapping("/upload-excel")
    public Map<String, Object> uploadRagFilesLegacy(
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "file", required = false) MultipartFile file)
    {
        if ((files == null || files.length == 0) && file != null)
        {
            files = new MultipartFile[] { file };
        }
        return eduAiProxyService.uploadRagFiles(files);
    }

    @GetMapping("/query")
    public Map<String, Object> queryRagLegacy(@RequestParam("question") String question)
    {
        return eduAiProxyService.queryRag(question);
    }

    @GetMapping({"/rag-api/datasets", "/datasets"})
    public Map<String, Object> getDatasetsLegacy()
    {
        return eduAiProxyService.getDatasets();
    }

    @GetMapping({"/rag-api/datasets/{datasetId}", "/datasets/{datasetId}"})
    public Map<String, Object> getDatasetDetailLegacy(@PathVariable Long datasetId)
    {
        return eduAiProxyService.getDatasetDetail(datasetId);
    }

    @DeleteMapping({"/rag-api/datasets/{datasetId}", "/datasets/{datasetId}"})
    public Map<String, Object> deleteDatasetLegacy(@PathVariable Long datasetId)
    {
        return eduAiProxyService.deleteDataset(datasetId);
    }

    @PostMapping("/train-prediction-model")
    public Map<String, Object> trainPredictionModelLegacy(@RequestParam("file") MultipartFile file)
    {
        return eduAiProxyService.trainPredictionModel(file);
    }

    @GetMapping("/model-info")
    public Map<String, Object> getPredictionModelInfoLegacy()
    {
        return eduAiProxyService.getModelInfo();
    }

    @PostMapping("/predict-score")
    public Map<String, Object> predictScoreLegacy(@RequestBody Map<String, Object> body)
    {
        return eduAiProxyService.predictScore(body);
    }
}
