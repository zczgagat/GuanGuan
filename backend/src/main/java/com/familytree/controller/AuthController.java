package com.familytree.controller;

import com.familytree.model.User;
import com.familytree.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private static final Map<String, Long> TOKENS = new ConcurrentHashMap<>();

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || username.trim().isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "用户名不能为空"));
        if (password == null || password.length() < 3)
            return ResponseEntity.badRequest().body(Map.of("error", "密码至少3位"));
        if (userRepository.existsByUsername(username.trim()))
            return ResponseEntity.badRequest().body(Map.of("error", "用户名已存在"));
        userRepository.save(new User(username.trim(), encoder.encode(password)));
        return ResponseEntity.ok(Map.of("message", "注册成功"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null)
            return ResponseEntity.badRequest().body(Map.of("error", "请输入用户名和密码"));
        User user = userRepository.findByUsername(username.trim()).orElse(null);
        if (user == null || !encoder.matches(password, user.getPassword()))
            return ResponseEntity.badRequest().body(Map.of("error", "用户名或密码错误"));

        String token = UUID.randomUUID().toString();
        TOKENS.put(token, user.getId());
        return ResponseEntity.ok(Map.of(
            "token", token, "userId", user.getId(),
            "username", user.getUsername(),
            "avatar", user.getAvatar() != null ? user.getAvatar() : ""
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer "))
            TOKENS.remove(authHeader.substring(7));
        return ResponseEntity.ok(Map.of("message", "已退出"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
        return ResponseEntity.ok(Map.of(
            "userId", user.getId(), "username", user.getUsername(),
            "avatar", user.getAvatar() != null ? user.getAvatar() : ""
        ));
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
        String oldPwd = body.get("oldPassword");
        String newPwd = body.get("newPassword");
        if (!encoder.matches(oldPwd, user.getPassword()))
            return ResponseEntity.badRequest().body(Map.of("error", "原密码错误"));
        if (newPwd == null || newPwd.length() < 3)
            return ResponseEntity.badRequest().body(Map.of("error", "新密码至少3位"));
        user.setPassword(encoder.encode(newPwd));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "密码已修改"));
    }

    @PutMapping("/username")
    public ResponseEntity<?> changeUsername(@RequestBody Map<String, String> body, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
        String newName = body.get("username");
        if (newName == null || newName.trim().isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "用户名不能为空"));
        if (userRepository.existsByUsername(newName.trim()))
            return ResponseEntity.badRequest().body(Map.of("error", "用户名已存在"));
        user.setUsername(newName.trim());
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("username", user.getUsername()));
    }

    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/"))
            return ResponseEntity.badRequest().body(Map.of("error", "仅支持图片文件"));
        try {
            String ext = "";
            String origName = file.getOriginalFilename();
            if (origName != null && origName.contains("."))
                ext = origName.substring(origName.lastIndexOf("."));
            String fileName = "user_" + userId + ext;
            Path path = Paths.get("uploads/avatars", fileName);
            Files.createDirectories(path.getParent());
            Files.copy(file.getInputStream(), path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            user.setAvatar(fileName);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("avatar", fileName, "url", "/uploads/avatars/" + fileName));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "上传失败"));
        }
    }

    public static Long getUserIdFromToken(String token) { return TOKENS.get(token); }
}
