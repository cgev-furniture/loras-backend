package am.loras.backend.dto.request;

import lombok.Data;

import java.util.List;

/**
 * All fields are nullable to support partial updates.
 * A null value means "do not change this field".
 */
@Data
public class UpdateProjectRequest {

    private String title;

    private String description;

    private List<Long> categoryIds;

    private Boolean published;

    private Boolean featured;

    private String videoUrl;

    private List<String> imageObjectPaths;

    private String slug;
}
