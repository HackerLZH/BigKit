package com.lzh.bigkit.dto;

import lombok.Data;
import java.util.List;

@Data
public class LocalCompressRequest {
    private String sessionId;
    private List<String> fileNames;
    private Integer quality = 80;
    private Integer maxWidth;
    private Integer maxHeight;
    private Long sizeThreshold;
    private String outputDir;
    private boolean overwrite = false;
    private String outputFormat;
    
    private List<String> imagePaths; // 添加缺失的字段
}