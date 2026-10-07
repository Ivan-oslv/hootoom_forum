package com.hootoom.forum.user.controller;

import com.hootoom.forum.common.api.ApiResponse;
import com.hootoom.forum.user.dto.UpdateUserProfileDTO;
import com.hootoom.forum.user.service.UserProfileService;
import com.hootoom.forum.user.vo.UserProfileVO;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserProfileController {
    private final UserProfileService service;

    public UserProfileController(UserProfileService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileVO>> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(service.getCurrentUser(Long.valueOf(jwt.getSubject()))));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserProfileVO>> updateCurrentUser(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateUserProfileDTO command) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(service.updateCurrentUser(Long.valueOf(jwt.getSubject()), command)));
    }
}
