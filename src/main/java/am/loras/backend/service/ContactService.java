package am.loras.backend.service;

import am.loras.backend.domain.ContactInquiry;
import am.loras.backend.dto.request.ContactRequest;
import am.loras.backend.dto.response.ContactInquiryResponse;
import am.loras.backend.dto.response.PageResponse;
import am.loras.backend.exception.NotFoundException;
import am.loras.backend.exception.ValidationException;
import am.loras.backend.repository.ContactInquiryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ContactService {

    private final ContactInquiryRepository contactInquiryRepository;
    private final MailService mailService;

    public ContactService(ContactInquiryRepository contactInquiryRepository,
                          MailService mailService) {
        this.contactInquiryRepository = contactInquiryRepository;
        this.mailService = mailService;
    }

    /**
     * Validates the honeypot, persists the inquiry, then fires async emails.
     *
     * @throws ValidationException if the honeypot field is non-blank (bot detected)
     */
    public void submitInquiry(ContactRequest req) {
        if (req.getWebsite() != null && !req.getWebsite().isBlank()) {
            throw new ValidationException("Bot submission detected");
        }

        ContactInquiry inquiry = ContactInquiry.builder()
                .name(req.getName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .message(req.getMessage())
                .emailDelivered(false)
                .read(false)
                .build();

        contactInquiryRepository.save(inquiry);

        // Fire-and-forget — failures do not roll back the DB write
        mailService.sendAutoReply(inquiry.getEmail(), inquiry.getName());
        mailService.sendInquiryNotification(inquiry);
    }

    /**
     * Returns paginated inquiries ordered newest-first.
     */
    @Transactional(readOnly = true)
    public PageResponse<ContactInquiryResponse> getInquiries(int page, int size) {
        Page<ContactInquiry> pageResult = contactInquiryRepository
                .findAllByOrderBySubmittedAtDesc(PageRequest.of(page, size));

        return PageResponse.<ContactInquiryResponse>builder()
                .content(pageResult.getContent().stream().map(this::toResponse).toList())
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .build();
    }

    /**
     * Marks an inquiry as read or unread.
     *
     * @throws NotFoundException if the inquiry does not exist
     */
    public ContactInquiryResponse markRead(Long id, boolean read) {
        ContactInquiry inquiry = contactInquiryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Inquiry not found: " + id));
        inquiry.setRead(read);
        contactInquiryRepository.save(inquiry);
        return toResponse(inquiry);
    }

    // -------------------------------------------------------------------------
    // Mappers
    // -------------------------------------------------------------------------

    private ContactInquiryResponse toResponse(ContactInquiry inquiry) {
        return ContactInquiryResponse.builder()
                .id(inquiry.getId())
                .name(inquiry.getName())
                .email(inquiry.getEmail())
                .phone(inquiry.getPhone())
                .message(inquiry.getMessage())
                .emailDelivered(inquiry.isEmailDelivered())
                .read(inquiry.isRead())
                .submittedAt(inquiry.getSubmittedAt())
                .build();
    }
}
