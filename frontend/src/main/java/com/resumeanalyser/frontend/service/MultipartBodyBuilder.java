package com.resumeanalyser.frontend.service;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MultipartBodyBuilder {

    private final String boundary = "----Boundary" + UUID.randomUUID();
    private final List<byte[]> parts = new ArrayList<>();

    public void addText(String name, String value) {
        StringBuilder part = new StringBuilder();
        part.append("--").append(boundary).append("\r\n");
        part.append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n\r\n");
        part.append(value).append("\r\n");
        parts.add(toBytes(part.toString()));
    }

    public void addFile(String name, File file) throws IOException {
        StringBuilder header = new StringBuilder();
        header.append("--").append(boundary).append("\r\n");
        header.append("Content-Disposition: form-data; name=\"").append(name)
                .append("\"; filename=\"").append(file.getName()).append("\"\r\n");
        header.append("Content-Type: application/octet-stream\r\n\r\n");
        parts.add(toBytes(header.toString()));
        parts.add(Files.readAllBytes(file.toPath()));
        parts.add(toBytes("\r\n"));
    }

    public HttpRequest.BodyPublisher build() {
        List<byte[]> allParts = new ArrayList<>(parts);
        allParts.add(toBytes("--" + boundary + "--\r\n"));
        return HttpRequest.BodyPublishers.ofByteArrays(allParts);
    }

    public String getBoundary() {
        return boundary;
    }

    private byte[] toBytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
