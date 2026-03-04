package com.lzh.bigkit.controller;

import com.lzh.bigkit.dto.LocalCompressRequest;
import com.lzh.bigkit.dto.SessionResponse;
import com.lzh.bigkit.service.ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor // 只为final或@NonNull属性生成对应的构造函数
public class ImageController {
    
    private final ImageService imageService;

    @Value("${file.image.size-threshold}")
    private Long sizeThreshold;
    
    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public ImageService.UploadResult uploadFiles(
            @RequestParam("files") MultipartFile[] files) {
        String sessionId = imageService.createUserSession();
        return imageService.uploadFiles(sessionId, files);
    }
    
    /**
     * 压缩用户会话中的所有图片（自动检查并上传文件）
     */
    @PostMapping("/compress")
    public ImageService.CompressResult compressImages(
            @RequestParam("sessionId") String sessionId,
            @RequestParam(value = "quality", defaultValue = "80") int quality,
            @RequestParam(value = "maxWidth", required = false) Integer maxWidth,
            @RequestParam(value = "maxHeight", required = false) Integer maxHeight) {
        
        String sessionPath = imageService.getSessionPath(sessionId);
        List<String> filePaths = new ArrayList<>();
        
        try {
            // 自动获取会话目录下所有图片文件
            Files.list(Paths.get(sessionPath))
                .filter(path -> isImageFile(path.toString()))
                .forEach(path -> filePaths.add(path.toString()));
            
            if (filePaths.isEmpty()) {
                ImageService.CompressResult result = new ImageService.CompressResult();
                result.setSuccess(false);
                result.setErrorMessage("未找到可压缩的图片文件");
                return result;
            }
            
            // 配置压缩参数
            ImageService.CompressConfig config = ImageService.CompressConfig.builder()
                    .quality(quality)
                    .maxWidth(maxWidth)
                    .maxHeight(maxHeight)
                    .sizeThreshold(sizeThreshold)
                    .outputDir(sessionPath) // 输出到会话目录
                    .build();
            
            // 执行压缩
            return imageService.compress(filePaths, config);
            
        } catch (IOException e) {
            log.error("文件处理失败", e);
            ImageService.CompressResult result = new ImageService.CompressResult();
            result.setSuccess(false);
            result.setErrorMessage("文件处理失败: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * 获取支持的格式列表
     */
    @GetMapping("/formats")
    public String[] getSupportedFormats() {
        return new String[]{"jpg", "jpeg", "png", "bmp", "gif", "webp"};
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        return FilenameUtils.getExtension(fileName).toLowerCase();
    }
    
    /**
     * 判断是否为图片文件
     */
    private boolean isImageFile(String filePath) {
        String extension = getFileExtension(filePath);
        String[] imageExtensions = {"jpg", "jpeg", "png", "bmp", "gif", "webp"};
        for (String imgExt : imageExtensions) {
            if (imgExt.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }
}