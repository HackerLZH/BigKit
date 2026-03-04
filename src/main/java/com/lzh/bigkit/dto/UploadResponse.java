package com.lzh.bigkit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {
    private boolean success;
    private String sessionId;
    private List<String> uploadedFiles;
    private int count;
    private String message;
}