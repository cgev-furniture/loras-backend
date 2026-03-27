package am.loras.backend.controller;

import am.loras.backend.dto.response.PageResponse;
import am.loras.backend.dto.response.ProjectResponse;
import am.loras.backend.service.ProjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /**
     * GET /api/v1/projects
     * Returns paginated published projects, optionally filtered by category IDs.
     */
    @GetMapping("/projects")
    public ResponseEntity<PageResponse<ProjectResponse>> getPublishedProjects(
            @RequestParam(required = false) List<Long> categoryIds,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        return ResponseEntity.ok(projectService.getPublishedProjects(categoryIds, page, size));
    }

    /**
     * GET /api/v1/projects/{slug}
     * Returns a single published project by slug.
     */
    @GetMapping("/projects/{slug}")
    public ResponseEntity<ProjectResponse> getPublishedBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(projectService.getPublishedBySlug(slug));
    }
}
