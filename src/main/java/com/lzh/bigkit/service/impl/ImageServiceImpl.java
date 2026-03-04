package com.lzh.bigkit.service.impl;

import com.lzh.bigkit.service.ImageService;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ImageServiceImpl implements ImageService {
    
    private static final String[] SUPPORTED_FORMATS = {"jpg", "jpeg", "png", "bmp", "gif", "webp"};
    
    @Value("${file.upload.base-dir:uploads}")
    private String uploadBaseDir;
    
    // 存储会话ID与文件夹路径的映射
    private final Map<String, String> sessionMap = new ConcurrentHashMap<>();
    
    /**
     * 初始化方法，确保上传基础目录存在
     */
    @PostConstruct
    public void init() {
        try {
            Path basePath = Paths.get(uploadBaseDir);
            if (!Files.exists(basePath)) {
                Files.createDirectories(basePath);
                log.info("创建上传基础目录: {}", basePath.toAbsolutePath());
            } else {
                log.info("上传基础目录已存在: {}", basePath.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("初始化上传目录失败: " + uploadBaseDir, e);
            throw new RuntimeException("初始化文件上传目录失败", e);
        }
    }
    
    @Override
    public String createUserSession() {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        String sessionPath = Paths.get(uploadBaseDir, sessionId).toString();
        
        try {
            Files.createDirectories(Paths.get(sessionPath));
            sessionMap.put(sessionId, sessionPath);
            log.info("创建用户会话: {}，路径: {}", sessionId, sessionPath);
            return sessionId;
        } catch (IOException e) {
            log.error("创建会话文件夹失败: " + sessionPath, e);
            throw new RuntimeException("创建会话失败", e);
        }
    }
    
    @Override
    public String getSessionPath(String sessionId) {
        return sessionMap.get(sessionId);
    }
    
    @Override
    public UploadResult uploadFiles(String sessionId, MultipartFile[] files) {
        String sessionPath = getSessionPath(sessionId);

        List<String> uploadedFiles = new ArrayList<>();
        
        try {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String originalFilename = file.getOriginalFilename();
                    String newFileName = System.currentTimeMillis() + "_" + originalFilename;
                    Path filePath = Paths.get(sessionPath, newFileName);
                    
                    Files.write(filePath, file.getBytes());
                    uploadedFiles.add(filePath.toString());
                    log.debug("文件上传成功: {}", filePath);
                }
            }
            
            return new UploadResult(true, sessionId, uploadedFiles, uploadedFiles.size(), null);
            
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return new UploadResult(false, sessionId, null, 0, "文件上传失败: " + e.getMessage());
        }
    }
    
    @Override
    public CompressResult compress(List<String> imagePaths, CompressConfig config) {
        if (imagePaths == null || imagePaths.isEmpty()) {
            CompressResult result = new CompressResult();
            result.setSuccess(false);
            result.setErrorMessage("图片路径列表不能为空");
            return result;
        }
        
        // 设置默认配置
        if (config == null) {
            config = CompressConfig.builder().build();
        }
        
        CompressResult result = new CompressResult();
        int successCount = 0;
        long originalTotalSize = 0;
        long compressedTotalSize = 0;
        
        for (String imagePath : imagePaths) {
            ProcessResult processResult = processSingleImage(imagePath, config);
            
            originalTotalSize += processResult.originalSize;
            compressedTotalSize += processResult.compressedSize;
            
            if (processResult.success) {
                successCount++;
            }
        }
        
        result.setProcessedCount(imagePaths.size());
        result.setSuccessCount(successCount);
        result.setOriginalTotalSize(originalTotalSize);
        result.setCompressedTotalSize(compressedTotalSize);
        
        if (compressedTotalSize > 0 && originalTotalSize > 0) {
            result.setCompressionRatio((double) (originalTotalSize - compressedTotalSize) / originalTotalSize * 100);
        }
        
        result.setSuccess(successCount == imagePaths.size());
        
        log.info("图片压缩完成: 总数={}, 成功={}, 原始大小={}KB, 压缩后大小={}KB, 压缩率={}%%",
                result.getProcessedCount(), result.getSuccessCount(),
                originalTotalSize / 1024, compressedTotalSize / 1024, 
                String.format("%.2f", result.getCompressionRatio()));
        
        return result;
    }
    
    /**
     * 处理单张图片的内部结果类
     */
    private static class ProcessResult {
        boolean success;
        long originalSize;
        long compressedSize;
        String errorMessage;
        
        ProcessResult(boolean success, long originalSize, long compressedSize) {
            this.success = success;
            this.originalSize = originalSize;
            this.compressedSize = compressedSize;
        }
        
        ProcessResult(boolean success, long originalSize, long compressedSize, String errorMessage) {
            this(success, originalSize, compressedSize);
            this.errorMessage = errorMessage;
        }
    }
    
    /**
     * 处理单张图片
     */
    private ProcessResult processSingleImage(String imagePath, CompressConfig config) {
        try {
            File inputFile = new File(imagePath);
            
            // 验证文件是否存在
            if (!inputFile.exists()) {
                return new ProcessResult(false, 0, 0, "文件不存在: " + imagePath);
            }
            
            // 验证是否为支持的图片格式
            String extension = FilenameUtils.getExtension(imagePath).toLowerCase();
            if (!isSupportedFormat(extension)) {
                return new ProcessResult(false, 0, 0, "不支持的图片格式: " + extension);
            }
            
            long originalSize = inputFile.length();
            
            // 检查文件大小阈值
            if (config.getSizeThreshold() != null && 
                originalSize <= config.getSizeThreshold() * 1024) {
                // 文件小于阈值，直接复制
                String outputPath = getOutputPath(inputFile, config);
                Files.copy(inputFile.toPath(), Paths.get(outputPath));
                return new ProcessResult(true, originalSize, originalSize);
            }
            
            // 读取图片
            BufferedImage originalImage = ImageIO.read(inputFile);
            if (originalImage == null) {
                return new ProcessResult(false, originalSize, 0, "无法读取图片文件");
            }
            
            // 构建输出路径
            String outputPath = getOutputPath(inputFile, config);
            
            // 创建Thumbnails构建器
            Thumbnails.Builder<BufferedImage> builder = Thumbnails.of(originalImage);
            
            // 设置压缩质量
            if (extension.equals("png")) {
                // PNG是无损格式，quality参数无效
                builder.scale(1.0);
            } else {
                builder.outputQuality(config.getQuality() / 100.0);
                builder.scale(1.0);
            }
            
            // 设置尺寸限制
            if (config.getMaxWidth() != null || config.getMaxHeight() != null) {
                int targetWidth = config.getMaxWidth() != null ? config.getMaxWidth() : originalImage.getWidth();
                int targetHeight = config.getMaxHeight() != null ? config.getMaxHeight() : originalImage.getHeight();
                builder.size(targetWidth, targetHeight);
            }
            
            // 设置输出格式
            String outputFormat = config.getOutputFormat();
            if (outputFormat == null || outputFormat.isEmpty()) {
                outputFormat = extension;
            }
            builder.outputFormat(outputFormat);
            
            // 执行压缩
            builder.toFile(outputPath);
            
            // 获取压缩后的文件大小
            long compressedSize = new File(outputPath).length();
            
            log.debug("图片压缩成功: {} -> {}, 原始大小: {}KB, 压缩后: {}KB",
                    imagePath, outputPath, originalSize/1024, compressedSize/1024);
            
            return new ProcessResult(true, originalSize, compressedSize);
            
        } catch (Exception e) {
            log.error("图片压缩失败: " + imagePath, e);
            return new ProcessResult(false, 0, 0, "压缩失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取输出文件路径
     */
    private String getOutputPath(File inputFile, CompressConfig config) {
        String fileName = inputFile.getName();
        String baseName = FilenameUtils.getBaseName(fileName);
        String extension = FilenameUtils.getExtension(fileName);
        
        // 确定输出目录
        String outputDir;
        if (config.getOutputDir() != null && !config.getOutputDir().isEmpty()) {
            outputDir = config.getOutputDir();
        } else {
            outputDir = inputFile.getParent();
        }
        
        // 确定文件名
        String outputFileName;
        if (config.isOverwrite()) {
            outputFileName = fileName;
        } else {
            String suffix = config.getOutputFormat() != null ? "." + config.getOutputFormat() : "." + extension;
            outputFileName = baseName + "_compressed_" + System.currentTimeMillis() + suffix;
        }
        
        return Paths.get(outputDir, outputFileName).toString();
    }
    
    /**
     * 检查是否为支持的格式
     */
    private boolean isSupportedFormat(String extension) {
        for (String format : SUPPORTED_FORMATS) {
            if (format.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 判断是否为图片文件
     */
    private boolean isImageFile(String filePath) {
        String extension = FilenameUtils.getExtension(filePath).toLowerCase();
        return isSupportedFormat(extension);
    }
}