package com.lzh.bigkit.service;

import lombok.Builder;
import lombok.Data;

import java.util.List;

public interface ImageService {
    
    /**
     * 创建用户会话，生成专属文件夹
     * @return 会话ID（文件夹名称）
     */
    String createUserSession();
    
    /**
     * 获取会话对应的文件夹路径
     * @param sessionId 会话ID
     * @return 文件夹路径
     */
    String getSessionPath(String sessionId);
    
    /**
     * 批量压缩图片
     * @param imagePaths 图片路径列表
     * @param config 压缩配置
     * @return 压缩结果
     */
    CompressResult compress(List<String> imagePaths, CompressConfig config);
    
    /**
     * 上传文件到指定会话
     * @param sessionId 会话ID
     * @param files 文件数组
     * @return 上传结果
     */
    UploadResult uploadFiles(String sessionId, org.springframework.web.multipart.MultipartFile[] files);
    
    /**
     * 压缩配置类
     */
    @Data
    @Builder
    class CompressConfig {
        /**
         * 输出目录，默认为原目录
         */
        private String outputDir;
        
        /**
         * 压缩质量 (1-100)，默认80
         */
        @Builder.Default // 该注解使得属性带默认值
        private int quality = 80;
        
        /**
         * 最大宽度，超过则等比缩放
         */
        private Integer maxWidth;
        
        /**
         * 最大高度，超过则等比缩放
         */
        private Integer maxHeight;
        
        /**
         * 是否覆盖原文件，默认false
         */
        @Builder.Default
        private boolean overwrite = false;
        
        /**
         * 输出格式 (jpg/png/webp)，默认与原格式相同
         */
        private String outputFormat;
        
        /**
         * 文件大小限制(KB)，超过此大小才压缩
         */
        private Long sizeThreshold;
    }
    
    /**
     * 上传结果类
     */
    @Data
    class UploadResult {
        /**
         * 是否成功
         */
        private boolean success;
        
        /**
         * 会话ID
         */
        private String sessionId;
        
        /**
         * 上传的文件路径列表
         */
        private java.util.List<String> uploadedFiles;
        
        /**
         * 上传文件数量
         */
        private int count;
        
        /**
         * 错误信息
         */
        private String errorMessage;
        
        public UploadResult() {}
        
        public UploadResult(boolean success, String sessionId, java.util.List<String> uploadedFiles, int count, String errorMessage) {
            this.success = success;
            this.sessionId = sessionId;
            this.uploadedFiles = uploadedFiles;
            this.count = count;
            this.errorMessage = errorMessage;
        }
    }
    
    /**
     * 压缩结果类
     */
    @Data
    class CompressResult {
        /**
         * 是否成功
         */
        private boolean success;
        
        /**
         * 处理的文件数量
         */
        private int processedCount;
        
        /**
         * 成功压缩的文件数量
         */
        private int successCount;
        
        /**
         * 原始总大小(bytes)
         */
        private long originalTotalSize;
        
        /**
         * 压缩后总大小(bytes)
         */
        private long compressedTotalSize;
        
        /**
         * 压缩比率
         */
        private double compressionRatio;
        
        /**
         * 错误信息
         */
        private String errorMessage;
    }
}
