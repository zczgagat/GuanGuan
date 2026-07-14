package com.familytree.controller;

import com.familytree.model.Person;
import com.familytree.repository.PersonRepository;
import com.familytree.service.FamilyService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class AvatarController {

    @Value("${app.upload-dir:uploads/avatars}")
    private String uploadDir;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private FamilyService familyService;

    @PostConstruct
    public void init() {
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();
    }

    @PostMapping("/upload-avatar/{personId}")
    public ResponseEntity<?> uploadAvatar(@PathVariable Long personId,
                                           @RequestParam("file") MultipartFile file) {
        Person person = personRepository.findById(personId).orElse(null);
        if (person == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "人物不存在"));
        }

        // 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("error", "仅支持图片文件"));
        }

        // 生成唯一文件名
        String ext = "";
        String origName = file.getOriginalFilename();
        if (origName != null && origName.contains(".")) {
            ext = origName.substring(origName.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + ext;

        try {
            Path path = Paths.get(uploadDir, fileName);
            Files.copy(file.getInputStream(), path);

            // 删除旧头像
            String oldAvatar = person.getAvatar();
            if (oldAvatar != null && !oldAvatar.isEmpty()) {
                try {
                    Files.deleteIfExists(Paths.get(uploadDir, oldAvatar));
                } catch (IOException ignored) {}
            }

            person.setAvatar(fileName);
            personRepository.save(person);
            familyService.rebuildGraph();

            return ResponseEntity.ok(Map.of(
                "avatar", fileName,
                "url", "/uploads/avatars/" + fileName
            ));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "上传失败"));
        }
    }

    @DeleteMapping("/upload-avatar/{personId}")
    public ResponseEntity<?> deleteAvatar(@PathVariable Long personId) {
        Person person = personRepository.findById(personId).orElse(null);
        if (person == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "人物不存在"));
        }
        String oldAvatar = person.getAvatar();
        if (oldAvatar != null && !oldAvatar.isEmpty()) {
            try {
                Files.deleteIfExists(Paths.get(uploadDir, oldAvatar));
            } catch (IOException ignored) {}
        }
        person.setAvatar(null);
        personRepository.save(person);
        familyService.rebuildGraph();
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
