package am.loras.backend.config;

import org.springframework.context.annotation.Configuration;

/**
 * Mail configuration.
 *
 * Spring Boot auto-configures {@code JavaMailSender} from the
 * {@code spring.mail.*} properties defined in {@code application.properties}.
 * This class exists as a placeholder for any future customisation (e.g. MIME
 * message templates, custom encodings) without requiring changes to the
 * application bootstrap class.
 */
@Configuration
public class MailConfig {
    // JavaMailSender is auto-configured by Spring Boot via spring-boot-starter-mail.
}
