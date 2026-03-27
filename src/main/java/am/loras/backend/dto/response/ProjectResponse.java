package am.loras.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {

    private Long id;
    private String title;
    private String slug;
    private String description;
    private boolean published;
    private boolean featured;
    private String videoUrl;
    private OffsetDateTime createdAt;
    private List<ImageResponse> images;
    private List<CategoryResponse> categories;
}
