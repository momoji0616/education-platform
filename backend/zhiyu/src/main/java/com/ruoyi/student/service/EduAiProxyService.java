package com.ruoyi.student.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import com.ruoyi.common.utils.StringUtils;

@Service
public class EduAiProxyService
{
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${education.ai.base-url:http://127.0.0.1:8000}")
    private String aiBaseUrl;

    public Map<String, Object> uploadRagFiles(MultipartFile[] files)
    {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        if (files != null)
        {
            for (MultipartFile file : files)
            {
                body.add("files", toResource(file));
            }
        }
        return exchangeForMap("/upload-excel", HttpMethod.POST, multipartEntity(body));
    }

    public Map<String, Object> uploadRagTextFile(String fileName, String content)
    {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        byte[] bytes = (content == null ? "" : content).getBytes(StandardCharsets.UTF_8);
        body.add("files", new ByteArrayResource(bytes)
        {
            @Override
            public String getFilename()
            {
                return StringUtils.isEmpty(fileName) ? "scene_profile.txt" : fileName;
            }
        });
        return exchangeForMap("/upload-excel", HttpMethod.POST, multipartEntity(body));
    }

    public Map<String, Object> queryRag(String question)
    {
        return queryRag(question, Collections.emptyList());
    }

    public Map<String, Object> queryRag(String question, java.util.List<Long> datasetIds)
    {
        return queryRag(question, datasetIds, "");
    }

    public Map<String, Object> queryRag(String question, java.util.List<Long> datasetIds, String businessContext)
    {
        String normalizedQuestion = normalizeQuestion(question);
        String encodedQuestion = UriUtils.encodeQueryParam(normalizedQuestion, StandardCharsets.UTF_8);
        StringBuilder urlBuilder = new StringBuilder(buildUrl("/query"))
                .append("?question=")
                .append(encodedQuestion);
        if (businessContext != null && !businessContext.trim().isEmpty())
        {
            urlBuilder.append("&businessContext=")
                    .append(UriUtils.encodeQueryParam(businessContext.trim(), StandardCharsets.UTF_8));
        }
        if (datasetIds != null && !datasetIds.isEmpty())
        {
            String joinedIds = datasetIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
            urlBuilder.append("&datasetIds=").append(UriUtils.encodeQueryParam(joinedIds, StandardCharsets.UTF_8));
        }
        URI uri = URI.create(urlBuilder.toString());
        Map<String, Object> result = exchangeForMap(uri, HttpMethod.GET, new HttpEntity<>(defaultHeaders()));
        Object echoedQuestion = result.get("question");
        if (echoedQuestion != null)
        {
            result.put("question", normalizeQuestion(String.valueOf(echoedQuestion)));
        }
        return result;
    }

    public Map<String, Object> getDatasets()
    {
        Object result = exchangeForObject("/datasets", HttpMethod.GET, new HttpEntity<>(defaultHeaders()));
        if (result instanceof Map)
        {
            return (Map<String, Object>) result;
        }
        Map<String, Object> wrapped = new LinkedHashMap<>();
        wrapped.put("data", result == null ? Collections.emptyList() : result);
        return wrapped;
    }

    public Map<String, Object> getDatasetDetail(Long datasetId)
    {
        return exchangeForMap("/datasets/" + datasetId, HttpMethod.GET, new HttpEntity<>(defaultHeaders()));
    }

    public Map<String, Object> deleteDataset(Long datasetId)
    {
        return exchangeForMap("/datasets/" + datasetId, HttpMethod.DELETE, new HttpEntity<>(defaultHeaders()));
    }

    public Map<String, Object> trainPredictionModel(MultipartFile file)
    {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", toResource(file));
        return exchangeForMap("/train-prediction-model", HttpMethod.POST, multipartEntity(body));
    }

    public Map<String, Object> getModelInfo()
    {
        return exchangeForMap("/model-info", HttpMethod.GET, new HttpEntity<>(defaultHeaders()));
    }

    public Map<String, Object> predictScore(Map<String, Object> body)
    {
        HttpHeaders headers = defaultHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return exchangeForMap("/predict-score", HttpMethod.POST, new HttpEntity<>(body, headers));
    }

    public Map<String, Object> predictScoreWithAi(Map<String, Object> body, String businessContext)
    {
        HttpHeaders headers = defaultHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("input_data", body);
        payload.put("business_context", businessContext);
        return exchangeForMap("/predict-score-ai", HttpMethod.POST, new HttpEntity<>(payload, headers));
    }

    public Map<String, Object> uploadAiReference(MultipartFile file)
    {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", toResource(file));
        Map<String, Object> result = exchangeForMap("/ai-grade/reference", HttpMethod.POST, multipartEntity(body));
        rewriteFileUrl(result, "referenceUrl");
        return result;
    }

    public Map<String, Object> aiGradeSingle(MultipartFile studentFile, String referenceId, String rubric, Integer maxScore, Integer questionCount)
    {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("studentFile", toResource(studentFile));
        body.add("referenceId", referenceId == null ? "" : referenceId);
        body.add("rubric", rubric == null ? "" : rubric);
        body.add("maxScore", String.valueOf(maxScore == null ? 100 : maxScore));
        body.add("questionCount", String.valueOf(questionCount == null ? 12 : questionCount));
        Map<String, Object> result = exchangeForMap("/ai-grade/single", HttpMethod.POST, multipartEntity(body));
        rewriteFileUrl(result, "annotatedImageUrl");
        return result;
    }

    public Map<String, Object> aiGradeBatch(MultipartFile[] files, String referenceId, String rubric, Integer maxScore, Integer questionCount)
    {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        if (files != null)
        {
            for (MultipartFile file : files)
            {
                body.add("files", toResource(file));
            }
        }
        body.add("referenceId", referenceId == null ? "" : referenceId);
        body.add("rubric", rubric == null ? "" : rubric);
        body.add("maxScore", String.valueOf(maxScore == null ? 100 : maxScore));
        body.add("questionCount", String.valueOf(questionCount == null ? 12 : questionCount));
        Map<String, Object> result = exchangeForMap("/ai-grade/batch", HttpMethod.POST, multipartEntity(body));
        Object rows = result.get("results");
        if (rows instanceof Iterable)
        {
            for (Object item : (Iterable<?>) rows)
            {
                if (item instanceof Map)
                {
                    rewriteFileUrl((Map<String, Object>) item, "annotatedImageUrl");
                }
            }
        }
        return result;
    }

    public ResponseEntity<byte[]> fetchAiFile(HttpServletRequest request)
    {
        String path = extractFilePath(request);
        try
        {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    buildUrl("/files/" + path),
                    HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders()),
                    byte[].class);
            HttpHeaders headers = new HttpHeaders();
            MediaType contentType = response.getHeaders().getContentType();
            headers.setContentType(contentType == null ? MediaType.APPLICATION_OCTET_STREAM : contentType);
            if (response.getHeaders().getContentLength() >= 0)
            {
                headers.setContentLength(response.getHeaders().getContentLength());
            }
            ContentDisposition disposition = response.getHeaders().getContentDisposition();
            if (disposition != null && disposition.getFilename() != null)
            {
                headers.setContentDisposition(disposition);
            }
            return new ResponseEntity<>(response.getBody(), headers, response.getStatusCode());
        }
        catch (HttpStatusCodeException ex)
        {
            throw toResponseStatusException(ex);
        }
    }

    private String extractFilePath(HttpServletRequest request)
    {
        String uri = request.getRequestURI();
        String prefix = request.getContextPath() + "/education/ai/files/";
        int index = uri.indexOf(prefix);
        if (index < 0)
        {
            return "";
        }
        return uri.substring(index + prefix.length());
    }

    private HttpEntity<MultiValueMap<String, Object>> multipartEntity(MultiValueMap<String, Object> body)
    {
        HttpHeaders headers = defaultHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return new HttpEntity<>(body, headers);
    }

    private Map<String, Object> exchangeForMap(String path, HttpMethod method, HttpEntity<?> entity)
    {
        try
        {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    buildUrl(path),
                    method,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            return response.getBody() == null ? Collections.emptyMap() : response.getBody();
        }
        catch (HttpStatusCodeException ex)
        {
            throw toResponseStatusException(ex);
        }
    }

    private Map<String, Object> exchangeForMap(URI uri, HttpMethod method, HttpEntity<?> entity)
    {
        try
        {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    uri,
                    method,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            return response.getBody() == null ? Collections.emptyMap() : response.getBody();
        }
        catch (HttpStatusCodeException ex)
        {
            throw toResponseStatusException(ex);
        }
    }

    private Object exchangeForObject(String path, HttpMethod method, HttpEntity<?> entity)
    {
        try
        {
            ResponseEntity<Object> response = restTemplate.exchange(
                    buildUrl(path),
                    method,
                    entity,
                    Object.class);
            return response.getBody();
        }
        catch (HttpStatusCodeException ex)
        {
            throw toResponseStatusException(ex);
        }
    }

    private ResponseStatusException toResponseStatusException(HttpStatusCodeException ex)
    {
        String message = ex.getResponseBodyAsString(StandardCharsets.UTF_8);
        if (message != null && !message.isEmpty())
        {
            try
            {
                Map<String, Object> body = objectMapper.readValue(message, new TypeReference<Map<String, Object>>() {});
                Object detail = body.get("detail");
                if (detail != null && !String.valueOf(detail).trim().isEmpty())
                {
                    message = String.valueOf(detail).trim();
                }
            }
            catch (Exception ignore)
            {
            }
        }
        if (message == null || message.trim().isEmpty())
        {
            message = "AI 服务调用失败";
        }
        return new ResponseStatusException(HttpStatus.valueOf(ex.getRawStatusCode()), message, ex);
    }

    private HttpHeaders defaultHeaders()
    {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    private ByteArrayResource toResource(MultipartFile file)
    {
        try
        {
            return new ByteArrayResource(file.getBytes())
            {
                @Override
                public String getFilename()
                {
                    return file.getOriginalFilename();
                }
            };
        }
        catch (IOException e)
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "读取上传文件失败", e);
        }
    }

    private String buildUrl(String path)
    {
        return aiBaseUrl.replaceAll("/+$", "") + path;
    }

    private String normalizeQuestion(String question)
    {
        String value = question == null ? "" : question.trim();
        if (value.contains("%"))
        {
            try
            {
                return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
            }
            catch (Exception ignore)
            {
            }
        }
        return value;
    }

    private void rewriteFileUrl(Map<String, Object> body, String key)
    {
        Object value = body.get(key);
        if (value == null)
        {
            return;
        }
        String url = String.valueOf(value);
        if (url.startsWith("/files/"))
        {
            body.put(key, "/education/ai" + url);
        }
    }
}
