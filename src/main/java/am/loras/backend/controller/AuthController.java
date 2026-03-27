package am.loras.backend.controller;

import am.loras.backend.config.AppProperties;
import am.loras.backend.domain.AdminUser;
import am.loras.backend.dto.request.ChangePasswordRequest;
import am.loras.backend.dto.request.LoginRequest;
import am.loras.backend.dto.response.AdminUserResponse;
import am.loras.backend.ratelimit.RateLimitService;
import am.loras.backend.repository.AdminUserRepository;
import am.loras.backend.security.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String COOKIE_NAME = "loras_token";
    private static final int RATE_LIMIT_CAPACITY = 10;
    private static final Duration RATE_LIMIT_DURATION = Duration.ofMinutes(1);

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;
    private final RateLimitService rateLimitService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          AdminUserRepository adminUserRepository,
                          PasswordEncoder passwordEncoder,
                          AppProperties appProperties,
                          RateLimitService rateLimitService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/login")
    public ResponseEntity<AdminUserResponse> login(@Valid @RequestBody LoginRequest request,
                                                   HttpServletRequest httpRequest,
                                                   HttpServletResponse httpResponse) {
        String clientIp = resolveClientIp(httpRequest);
        if (!rateLimitService.tryConsume(clientIp, RATE_LIMIT_CAPACITY, RATE_LIMIT_DURATION)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        String username = authentication.getName();
        String token = jwtUtil.generateToken(username);

        AdminUser adminUser = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Admin user not found: " + username));

        addJwtCookie(httpResponse, token);

        AdminUserResponse response = new AdminUserResponse(
                adminUser.getId(),
                adminUser.getUsername(),
                adminUser.isMustChangePassword()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse httpResponse) {
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setDomain(appProperties.getJwt().getCookieDomain());
        httpResponse.addCookie(cookie);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminUserResponse> me() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        AdminUser adminUser = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Admin user not found: " + username));

        AdminUserResponse response = new AdminUserResponse(
                adminUser.getId(),
                adminUser.getUsername(),
                adminUser.isMustChangePassword()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminUserResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        AdminUser adminUser = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Admin user not found: " + username));

        adminUser.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        adminUser.setMustChangePassword(false);
        adminUserRepository.save(adminUser);

        AdminUserResponse response = new AdminUserResponse(
                adminUser.getId(),
                adminUser.getUsername(),
                adminUser.isMustChangePassword()
        );
        return ResponseEntity.ok(response);
    }

    private void addJwtCookie(HttpServletResponse response, String token) {
        // Build Set-Cookie header manually to support SameSite=Strict, which
        // the Servlet Cookie API does not expose until Servlet 6.0 / Tomcat 10.1.
        String cookieValue = COOKIE_NAME + "=" + token
                + "; Path=/"
                + "; Max-Age=86400"
                + "; HttpOnly"
                + "; Secure"
                + "; SameSite=Strict"
                + "; Domain=" + appProperties.getJwt().getCookieDomain();
        response.addHeader("Set-Cookie", cookieValue);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
