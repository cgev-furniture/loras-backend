package am.loras.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageResponse {

    private Long id;
    private String objectPath;
    private String publicUrl;
    private int sortOrder;
}
