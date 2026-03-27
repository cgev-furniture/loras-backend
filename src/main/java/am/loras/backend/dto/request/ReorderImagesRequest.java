package am.loras.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReorderImagesRequest {

    @NotNull
    @Valid
    private List<ImageOrderItem> items;

    @Data
    public static class ImageOrderItem {

        @NotNull
        private Long imageId;

        private int sortOrder;
    }
}
