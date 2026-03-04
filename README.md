# BigKit - 图片压缩工具

一个基于Spring Boot的高性能图片压缩服务，支持多种格式和灵活的压缩配置。

## 🚀 特性

- **多格式支持**：JPG、PNG、BMP、GIF、WebP
- **灵活配置**：可调节压缩质量、尺寸限制、文件大小阈值
- **批量处理**：支持同时处理多个图片文件
- **智能压缩**：可根据文件大小自动决定是否压缩
- **详细统计**：提供完整的压缩效果统计信息

## 🛠️ 技术栈

- Spring Boot 3.5.11
- Java 17
- Thumbnailator 0.4.20
- Apache Commons IO 2.15.1
- Lombok

## 📖 使用方法

### 1. REST API调用

#### 创建会话
```bash
POST /api/images/session

返回:
{
  "success": true,
  "sessionId": "unique-session-id",
  "message": "会话创建成功"
}
```

#### 上传文件
```bash
POST /api/images/upload
Content-Type: multipart/form-data

参数:
- sessionId: 会话ID (必填)
- files: 文件数组 (必填)
```

#### 压缩图片（支持自动上传）
```bash
POST /api/images/compress
Content-Type: multipart/form-data

参数:
- sessionId: 会话ID (必填)
- files: 文件数组 (可选，如果会话中没有文件则自动上传)
- quality: 压缩质量 (1-100, 默认80)
- maxWidth: 最大宽度 (可选)
- maxHeight: 最大高度 (可选)

说明: 
- 如果会话中已有文件，直接进行压缩
- 如果会话中没有文件但提供了files参数，先上传再压缩
- 如果会话中没有文件且未提供files参数，返回错误
```

#### 压缩本地文件
```bash
POST /api/images/compress/local
Content-Type: application/json

{
  "imagePaths": ["D:/images/photo1.jpg", "D:/images/photo2.png"],
  "quality": 75,
  "maxWidth": 1920,
  "maxHeight": 1080,
  "outputDir": "D:/compressed",
  "overwrite": false,
  "outputFormat": "jpg"
}
```

### 2. Java代码调用

```java
@Autowired
private ImageService imageService;

// 单张图片压缩
ImageService.CompressConfig config = ImageService.CompressConfig.builder()
    .quality(80)
    .maxWidth(1200)
    .outputDir("/output/path")
    .build();

ImageService.CompressResult result = imageService.compress("/path/to/image.jpg", config);

// 批量压缩
List<String> imagePaths = Arrays.asList("/path1.jpg", "/path2.png");
ImageService.CompressResult result = imageService.compress(imagePaths, config);
```

## 📁 文件上传配置

### 配置方式

文件上传路径可以通过以下方式配置：

1. **application.yml 配置文件**
```yaml
file:
  upload:
    base-dir: ${FILE_UPLOAD_BASE_DIR:uploads}
```

2. **环境变量**
```bash
export FILE_UPLOAD_BASE_DIR=/data/uploads
```

3. **启动参数**
```bash
java -jar bigkit.jar --file.upload.base-dir=/data/uploads
```

### 路径说明

- **开发环境**：默认使用相对路径 `uploads`
- **生产环境**：建议使用绝对路径，如 `/data/uploads` 或 `D:\data\uploads`

### 目录结构

```
上传基础目录/
├── session-id-1/
│   ├── timestamp_filename1.jpg
│   └── timestamp_filename2.png
├── session-id-2/
│   └── timestamp_filename3.gif
└── ...
```

### 注意事项

1. 确保应用程序对指定目录有读写权限
2. 生产环境建议使用独立的文件存储服务
3. 定期清理过期的会话文件

## ⚙️ 配置选项

| 参数 | 类型 | 默认值 | 描述 |
|------|------|--------|------|
| quality | int | 80 | 压缩质量 (1-100) |
| maxWidth | Integer | null | 最大宽度 |
| maxHeight | Integer | null | 最大高度 |
| outputDir | String | 原目录 | 输出目录 |
| overwrite | boolean | false | 是否覆盖原文件 |
| outputFormat | String | 原格式 | 输出格式 |
| sizeThreshold | Long | null | 大小阈值(KB) |

## 📊 返回结果

```json
{
  "success": true,
  "processedCount": 2,
  "successCount": 2,
  "failureCount": 0,
  "originalTotalSize": 5242880,
  "compressedTotalSize": 1048576,
  "compressionRatio": 80.0,
  "fileResults": [
    {
      "filePath": "/path/to/image1.jpg",
      "success": true,
      "originalSize": 2621440,
      "compressedSize": 524288,
      "compressionRatio": 80.0,
      "outputPath": "/output/image1_compressed.jpg"
    }
  ]
}
```

## 🧪 测试

运行测试用例：
```bash
mvn test
```

## 📈 压缩效果示例

| 原始文件 | 压缩后 | 压缩率 | 质量 |
|----------|--------|--------|------|
| 2.5MB JPG | 500KB JPG | 80% | 高 |
| 1.2MB PNG | 300KB PNG | 75% | 无损 |
| 800KB BMP | 150KB JPG | 81% | 中等 |

## 🔧 开发指南

### 项目结构
```
src/main/java/com/lzh/bigkit/
├── controller/          # REST控制器
├── service/            # 核心服务接口
│   └── impl/           # 服务实现
└── BigKitApplication.java  # 启动类
```

### 扩展功能
- 添加新的图片格式支持
- 实现AI智能压缩
- 添加水印功能
- 支持批量下载压缩包

## 📝 注意事项

1. PNG格式为无损压缩，quality参数对PNG无效
2. 建议压缩质量设置在70-85之间平衡质量和文件大小
3. 大尺寸图片建议先设置maxWidth/maxHeight再压缩
4. WebP格式需要较新的JDK版本支持

## 🤝 贡献

欢迎提交Issue和Pull Request！

## 📄 许可证

MIT License