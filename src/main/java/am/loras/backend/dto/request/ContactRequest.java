package am.loras.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ContactRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotBlank
    @Email
    private String email;

    private String phone;

    @NotBlank
    @Size(max = 2000)
    private String message;

    /** Honeypot field — must be blank/null. */
    private String website;
}
