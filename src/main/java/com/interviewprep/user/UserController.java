package com.interviewprep.user;

import com.interviewprep.common.PageResponse;
import com.interviewprep.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return userService.getByEmail(jwt.getSubject());
    }

    @GetMapping
    public PageResponse<UserResponse> list(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return userService.list(pageable);
    }
}
