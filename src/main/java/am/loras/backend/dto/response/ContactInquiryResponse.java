package am.loras.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactInquiryResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private String message;
    private boolean emailDelivered;
    private boolean read;
    private OffsetDateTime submittedAt;
}
