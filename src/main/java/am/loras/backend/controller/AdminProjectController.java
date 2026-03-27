package am.loras.backend.controller;

import am.loras.backend.dto.request.CreateProjectRequest;
import am.loras.backend.dto.request.ReorderImagesRequest;
import am.loras.backend.dto.request.UpdateProjectRequest;
import am.loras.backend.dto.response.AdminStatsResponse;
import am.loras.backend.dto.response.PageResponse;
import am.loras.backend.dto.response.ProjectResponse;
import am.loras.backend.repository.CategoryRepository;
import am.loras.backend.repository.ContactInquiryRepository;
import am.loras.backend.repository.ProjectRepository;
import am.loras.backend.service.CategoryService;
import am.loras.backend.service.ProjectService;
import am.loras.backend.service.StorageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProjectController {

    private final ProjectService projectService;
    private final CategoryService categoryService;
    private final StorageService storageService;
    private final ProjectRepository projectRepository;
    private final CategoryRepository categoryRepository;
    private final ContactInquiryRepository contactInquiryRepository;

    public AdminProjectController(ProjectService projectService,
                                  CategoryService categoryService,
                                  StorageService storageService,
                                  ProjectRepository projectRepository,
                                  CategoryRepository categoryRepository,
                                  ContactInquiryRepository contactInquiryRepository) {
        this.projectService = projectService;
        this.categoryService = categoryService;
        this.storageService = storageService;
        this.projectRepository = projectRepository;
        this.categoryRepository = categoryRepository;
        this.contactInquiryRepository = contactInquiryRepository;
    }

    /**
     * GET /api/v1/admin/projects?status=all|published|draft&page=0&size=12
     */
    @GetMapping("/projects")
    public ResponseEntity<PageResponse<ProjectResponse>> getAllProjects(
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        return ResponseEntity.ok(projectService.getAllProjects(status, page, size));
    }

    /**
     * GET /api/v1/admin/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        long totalPublished = projectRepository.countByPublishedTrueAndDeletedAtIsNull();
        long totalDraft = projectRepository.countByPublishedFalseAndDeletedAtIsNull();
        long totalInquiries = contactInquiryRepository.count();
        long unreadInquiries = contactInquiryRepository.countByReadFalse();

        AdminStatsResponse stats = AdminStatsResponse.builder()
                .totalPublished(totalPublished)
                .totalDraft(totalDraft)
                .byCategory(categoryService.getAllCategories())
                .totalInquiries(totalInquiries)
                .unreadInquiries(unreadInquiries)
                .build();

        return ResponseEntity.ok(stats);
    }

    /**
     * POST /api/v1/admin/projects
     */
    @PostMapping("/projects")
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody CreateProjectRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(req));
    }

    /**
     * PATCH /api/v1/admin/projects/{id}
     */
    @PatchMapping("/projects/{id}")
    public ResponseEntity<ProjectResponse> updateProject(@PathVariable Long id,
                                                         @RequestBody UpdateProjectRequest req) {
        return ResponseEntity.ok(projectService.updateProject(id, req));
    }

    /**
     * PATCH /api/v1/admin/projects/{id}/publish?published=true|false
     */
    @PatchMapping("/projects/{id}/publish")
    public ResponseEntity<ProjectResponse> togglePublish(@PathVariable Long id,
                                                         @RequestParam boolean published) {
        return ResponseEntity.ok(projectService.togglePublish(id, published));
    }

    /**
     * DELETE /api/v1/admin/projects/{id}
     * Soft-deletes the project and removes all associated GCS objects.
     */
    @DeleteMapping("/projects/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        // Fetch the project response before the soft-delete so we have the image/video paths
        ProjectResponse project = projectService.getProjectById(id);

        // Delete all image objects from GCS
        project.getImages().forEach(img -> storageService.deleteObject(img.getObjectPath()));

        // Delete the video object from GCS if one is stored
        if (project.getVideoUrl() != null && !project.getVideoUrl().isBlank()) {
            String videoPath = extractObjectPathFromPublicUrl(project.getVideoUrl());
            if (videoPath != null) {
                storageService.deleteObject(videoPath);
            }
        }

        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/v1/admin/projects/{id}/images/{imageId}
     * Deletes a single image and its GCS object.
     */
    @DeleteMapping("/projects/{id}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id, @PathVariable Long imageId) {
        String objectPath = projectService.deleteImage(id, imageId);
        storageService.deleteObject(objectPath);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/v1/admin/projects/{id}/images/reorder
     */
    @PatchMapping("/projects/{id}/images/reorder")
    public ResponseEntity<Void> reorderImages(@PathVariable Long id,
                                              @Valid @RequestBody ReorderImagesRequest req) {
        projectService.reorderImages(id, req);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Extracts the GCS object path from a public URL like
     * {@code https://storage.googleapis.com/{bucket}/{objectPath}}.
     */
    private String extractObjectPathFromPublicUrl(String publicUrl) {
        if (publicUrl == null) {
            return null;
        }
        // Pattern: https://storage.googleapis.com/<bucket>/<objectPath>
        String prefix = "https://storage.googleapis.com/";
        if (!publicUrl.startsWith(prefix)) {
            return null;
        }
        String withoutPrefix = publicUrl.substring(prefix.length());
        int slashIdx = withoutPrefix.indexOf('/');
        if (slashIdx < 0) {
            return null;
        }
        return withoutPrefix.substring(slashIdx + 1);
    }
}
