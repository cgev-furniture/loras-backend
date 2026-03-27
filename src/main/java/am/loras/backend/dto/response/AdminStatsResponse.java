package am.loras.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsResponse {

    private long totalPublished;
    private long totalDraft;
    private List<CategoryResponse> byCategory;
    private long totalInquiries;
    private long unreadInquiries;
}
