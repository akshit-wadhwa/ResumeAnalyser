package com.resumeanalyser.frontend.service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.resumeanalyser.frontend.model.AdminSummary;
import com.resumeanalyser.frontend.model.AnalysisResult;
import com.resumeanalyser.frontend.model.RankedResume;
import com.resumeanalyser.frontend.model.UserSession;

public class ApiClient {

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        mapper.registerModule(new JavaTimeModule());
    }

    public UserSession login(String email, String password) throws IOException, InterruptedException {
        Map<String, String> payload = new HashMap<>();
        payload.put("email", email);
        payload.put("password", password);
        String body = mapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Login failed: " + response.body());
        }
        return mapper.readValue(response.body(), UserSession.class);
    }

    public UserSession register(String email, String password, String fullName) throws IOException, InterruptedException {
        Map<String, String> payload = new HashMap<>();
        payload.put("email", email);
        payload.put("password", password);
        payload.put("fullName", fullName);
        String body = mapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/auth/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201 && response.statusCode() != 200) {
            throw new IOException("Registration failed: " + response.body());
        }
        return mapper.readValue(response.body(), UserSession.class);
    }

    public String startAnalysis(File resume, File jobFile, String jobText, String email, String password) throws IOException, InterruptedException {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.addFile("resume", resume);
        if (jobFile != null) {
            builder.addFile("jobFile", jobFile);
        }
        if (jobText != null && !jobText.isBlank()) {
            builder.addText("jobText", jobText);
        }
        builder.addText("userEmail", email);
        builder.addText("userPassword", password);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/analysis/analyze"))
                .header("Content-Type", "multipart/form-data; boundary=" + builder.getBoundary())
                .POST(builder.build())
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Analysis start failed: " + response.body());
        }
        Map<?, ?> result = mapper.readValue(response.body(), Map.class);
        Object idValue = result.get("analysisId");
        String analysisId = idValue == null ? null : String.valueOf(idValue);
        if (analysisId == null || analysisId.isBlank() || "null".equalsIgnoreCase(analysisId)) {
            throw new IOException("Analysis start failed: missing analysis id");
        }
        return analysisId;
    }

    public AnalysisResult pollStatus(String analysisId) throws IOException, InterruptedException {
        if (analysisId == null || analysisId.isBlank() || "null".equalsIgnoreCase(analysisId)) {
            throw new IOException("Missing analysis id");
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/analysis/status/" + analysisId))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Status check failed: " + response.body());
        }
        Map<?, ?> result = mapper.readValue(response.body(), Map.class);
        Object statusValue = result.get("status");
        String status = statusValue == null ? "" : String.valueOf(statusValue);
        if ("FAILED".equals(status)) {
            Object errorValue = result.get("errorMessage");
            String error = errorValue == null ? "Analysis failed" : String.valueOf(errorValue);
            throw new IOException(error);
        }
        if (!"COMPLETED".equals(result.get("status"))) {
            return null;
        }
        String payload = mapper.writeValueAsString(result.get("result"));
        return mapper.readValue(payload, AnalysisResult.class);
    }

    public List<RankedResume> rankResumes(List<File> resumes, String jobText, String email, String password) throws IOException, InterruptedException {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        for (File resume : resumes) {
            builder.addFile("resumes", resume);
        }
        builder.addText("jobText", jobText);
        builder.addText("userEmail", email);
        builder.addText("userPassword", password);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/comparison/rank"))
                .header("Content-Type", "multipart/form-data; boundary=" + builder.getBoundary())
                .POST(builder.build())
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Map<?, ?> result = mapper.readValue(response.body(), Map.class);
        String payload = mapper.writeValueAsString(result.get("ranking"));
        return mapper.readValue(payload, mapper.getTypeFactory().constructCollectionType(List.class, RankedResume.class));
    }

    public AdminSummary fetchAdminSummary() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/admin/summary"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return mapper.readValue(response.body(), AdminSummary.class);
    }

    public void downloadReport(String analysisId, File destination) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/analysis/" + analysisId + "/report"))
                .GET()
                .build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        Files.write(destination.toPath(), response.body());
    }

    public List<AnalysisResult> getHistory(String email, String password) throws IOException, InterruptedException {
        String url = baseUrl + "/history?userEmail=" + email + "&userPassword=" + password;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("History fetch failed: " + response.body());
        }
        return mapper.readValue(response.body(), mapper.getTypeFactory().constructCollectionType(List.class, AnalysisResult.class));
    }
}
