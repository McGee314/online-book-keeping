package com.samudera.bookkeeping.controller;

import com.samudera.bookkeeping.common.Result;
import com.samudera.bookkeeping.dto.PasswordUpdateRequest;
import com.samudera.bookkeeping.dto.ProfileUpdateRequest;
import com.samudera.bookkeeping.exception.BusinessException;
import com.samudera.bookkeeping.service.UserService;
import com.samudera.bookkeeping.vo.UserInfoVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public Result<UserInfoVO> getProfile() {
        return Result.success(userService.getCurrentUserInfo());
    }

    @PutMapping("/profile")
    public Result<UserInfoVO> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return Result.success(userService.updateProfile(request));
    }

    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BusinessException(400, "File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/gif") && !contentType.equals("image/webp"))) {
            throw new BusinessException(400, "Only image files (JPEG, PNG, GIF, WebP) are allowed");
        }

        if (file.getSize() > 2 * 1024 * 1024) {
            throw new BusinessException(400, "File size must be less than 2MB");
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = "avatar_" + UUID.randomUUID().toString() + extension;

        // Ensure upload directory exists
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        // Save file
        Path targetPath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        // Update user avatar in database
        String avatarUrl = "/uploads/" + filename;
        userService.updateAvatar(avatarUrl);

        return Result.success(Map.of("avatar", avatarUrl));
    }

    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody PasswordUpdateRequest request) {
        userService.updatePassword(request);
        return Result.success();
    }
}
