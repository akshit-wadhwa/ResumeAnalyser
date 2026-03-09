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
        String part = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
                + value + "\r\n";
        parts.add(part.getBytes(StandardCharsets.UTF_8));
    }

    public void addFile(String name, File file) throws IOException {
        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + file.getName() + "\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n";
        parts.add(header.getBytes(StandardCharsets.UTF_8));
        parts.add(Files.readAllBytes(file.toPath()));
        parts.add("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    public HttpRequest.BodyPublisher build() {
        String end = "--" + boundary + "--\r\n";
        parts.add(end.getBytes(StandardCharsets.UTF_8));
        return HttpRequest.BodyPublishers.ofByteArrays(parts);
    }

    public String getBoundary() {
        return boundary;
    }
}
