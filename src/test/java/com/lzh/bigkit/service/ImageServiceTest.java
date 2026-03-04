package com.lzh.bigkit.service;

import com.lzh.bigkit.BigKitApplication;
import com.lzh.bigkit.service.impl.ImageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = BigKitApplication.class)
@ActiveProfiles("test")
public class ImageServiceTest {

    @Autowired
    private ImageService imageService;

    private String testImagesDir;

}