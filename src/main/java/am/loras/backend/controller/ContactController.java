package am.loras.backend.controller;

import am.loras.backend.dto.request.ContactRequest;
import am.loras.backend.dto.response.ContactInquiryResponse;
import am.loras.backend.dto.response.PageResponse;
import am.loras.backend.ratelimit.RateLimitService;
import am.loras.backend.service.ContactService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1")
public class ContactController {

    private static final int RATE_LIMIT_CAPACITY = 5;
    private static final Duration RATE_LIMIT_DURATION = Duration.ofHours(1);

    private final ContactService contactService;
    private final RateLimitService rateLimitService;

    public ContactController(ContactService contactService, RateLimitService rateLimitService) {
        this.contactService = contactService;
        this.rateLimitService = rateLimitService;
    }

    /**
     * POST /api/v1/contact — public, rate-limited 5 req/IP/hour
     */
    @PostMapping("/contact")
    public ResponseEntity<Void> submitInquiry(@Valid @RequestBody ContactRequest req,
                                              HttpServletRequest httpRequest) {
        String clientIp = resolveClientIp(httpRequest);
        if (!rateLimitService.tryConsume(clientIp, RATE_LIMIT_CAPACITY, RATE_LIMIT_DURATION)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        contactService.submitInquiry(req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * GET /api/v1/admin/inquiries — admin only, paginated
     */
    @GetMapping("/admin/inquiries")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<ContactInquiryResponse>> getInquiries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(contactService.getInquiries(page, size));
    }

    /**
     * PATCH /api/v1/admin/inquiries/{id}/read?read=true|false — admin only
     */
    @PatchMapping("/admin/inquiries/{id}/read")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ContactInquiryResponse> markRead(@PathVariable Long id,
                                                           @RequestParam boolean read) {
        return ResponseEntity.ok(contactService.markRead(id, read));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
