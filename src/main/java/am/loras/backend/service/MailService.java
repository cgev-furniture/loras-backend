package am.loras.backend.service;

import am.loras.backend.config.AppProperties;
import am.loras.backend.domain.ContactInquiry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final String contactEmail;

    public MailService(JavaMailSender mailSender, AppProperties appProperties) {
        this.mailSender = mailSender;
        this.contactEmail = appProperties.getContact().getEmail();
    }

    /**
     * Sends a branded auto-reply confirmation email to the visitor who submitted the inquiry.
     * Failures are logged and not re-thrown — the DB write has already committed.
     */
    @Async("mailTaskExecutor")
    public void sendAutoReply(String toEmail, String toName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(contactEmail);
            message.setTo(toEmail);
            message.setSubject("We received your message — LORAS Furniture");
            message.setText(
                    "Hello " + toName + ",\n\n"
                    + "Thank you for reaching out to LORAS Furniture. We have received your message "
                    + "and will get back to you as soon as possible.\n\n"
                    + "In the meantime, feel free to browse our portfolio at our website "
                    + "or reach us on WhatsApp at +374 98 11 08 95.\n\n"
                    + "Warm regards,\n"
                    + "The LORAS Furniture Team\n"
                    + "Yerevan, Armenia\n"
                    + "https://instagram.com/lorasfurniture"
            );
            mailSender.send(message);
            log.info("Auto-reply sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send auto-reply to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    /**
     * Notifies the admin email address about a new contact inquiry.
     * Failures are logged and not re-thrown.
     */
    @Async("mailTaskExecutor")
    public void sendInquiryNotification(ContactInquiry inquiry) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(contactEmail);
            message.setTo(contactEmail);
            message.setSubject("New Contact Inquiry — " + inquiry.getName());
            message.setText(
                    "A new inquiry has been submitted via the LORAS Furniture website.\n\n"
                    + "Name:    " + inquiry.getName() + "\n"
                    + "Email:   " + inquiry.getEmail() + "\n"
                    + "Phone:   " + (inquiry.getPhone() != null ? inquiry.getPhone() : "—") + "\n\n"
                    + "Message:\n" + inquiry.getMessage() + "\n\n"
                    + "Submitted at: " + inquiry.getSubmittedAt()
            );
            mailSender.send(message);
            log.info("Inquiry notification sent to admin for inquiry id={}", inquiry.getId());
        } catch (Exception e) {
            log.error("Failed to send inquiry notification for inquiry id={}: {}", inquiry.getId(), e.getMessage(), e);
        }
    }
}
