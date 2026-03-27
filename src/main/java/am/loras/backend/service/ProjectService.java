package am.loras.backend.service;

import am.loras.backend.domain.Category;
import am.loras.backend.domain.Project;
import am.loras.backend.domain.ProjectImage;
import am.loras.backend.dto.request.CreateProjectRequest;
import am.loras.backend.dto.request.ReorderImagesRequest;
import am.loras.backend.dto.request.UpdateProjectRequest;
import am.loras.backend.dto.response.CategoryResponse;
import am.loras.backend.dto.response.ImageResponse;
import am.loras.backend.dto.response.PageResponse;
import am.loras.backend.dto.response.ProjectResponse;
import am.loras.backend.exception.NotFoundException;
import am.loras.backend.repository.CategoryRepository;
import am.loras.backend.repository.ProjectImageRepository;
import am.loras.backend.repository.ProjectRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectImageRepository projectImageRepository;
    private final CategoryRepository categoryRepository;
    private final SlugService slugService;
    private final StorageService storageService;

    public ProjectService(ProjectRepository projectRepository,
                          ProjectImageRepository projectImageRepository,
                          CategoryRepository categoryRepository,
                          SlugService slugService,
                          StorageService storageService) {
        this.projectRepository = projectRepository;
        this.projectImageRepository = projectImageRepository;
        this.categoryRepository = categoryRepository;
        this.slugService = slugService;
        this.storageService = storageService;
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Returns published projects, optionally filtered by one or more categories.
     */
    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> getPublishedProjects(List<Long> categoryIds, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Project> result;

        if (categoryIds == null || categoryIds.isEmpty()) {
            result = projectRepository.findByPublishedTrueAndDeletedAtIsNull(pageable);
        } else {
            result = projectRepository.findPublishedByCategoryIds(categoryIds, pageable);
        }

        return toPageResponse(result);
    }

    /**
     * Returns a single published project by its slug.
     *
     * @throws NotFoundException if no published project with that slug exists
     */
    @Transactional(readOnly = true)
    public ProjectResponse getPublishedBySlug(String slug) {
        Project project = projectRepository.findBySlugAndDeletedAtIsNull(slug)
                .filter(Project::isPublished)
                .orElseThrow(() -> new NotFoundException("Project not found: " + slug));
        return toProjectResponse(project);
    }

    // =========================================================================
    // Admin API
    // =========================================================================

    /**
     * Returns all projects (published, draft, or all) for the admin panel.
     *
     * @param status "published" | "draft" | "all" (default)
     */
    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> getAllProjects(String status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Project> result;

        if ("published".equalsIgnoreCase(status)) {
            result = projectRepository.findByPublishedTrueAndDeletedAtIsNull(pageable);
        } else if ("draft".equalsIgnoreCase(status)) {
            result = projectRepository.findByPublishedFalseAndDeletedAtIsNull(pageable);
        } else {
            result = projectRepository.findByDeletedAtIsNull(pageable);
        }

        return toPageResponse(result);
    }

    /**
     * Creates a new project with images and categories.
     */
    public ProjectResponse createProject(CreateProjectRequest req) {
        String slug = (req.getSlug() != null && !req.getSlug().isBlank())
                ? req.getSlug()
                : slugService.generateUniqueSlug(req.getTitle(), projectRepository);

        Set<Category> categories = resolveCategories(req.getCategoryIds());

        Project project = Project.builder()
                .title(req.getTitle())
                .slug(slug)
                .description(req.getDescription())
                .published(req.isPublished())
                .featured(req.isFeatured())
                .videoUrl(req.getVideoUrl())
                .categories(categories)
                .images(new ArrayList<>())
                .build();

        projectRepository.save(project);

        if (req.getImageObjectPaths() != null) {
            addImages(project, req.getImageObjectPaths());
        }

        return toProjectResponse(projectRepository.save(project));
    }

    /**
     * Partially updates a project. Null fields in the request are left unchanged.
     *
     * @throws NotFoundException if the project does not exist or is deleted
     */
    public ProjectResponse updateProject(Long id, UpdateProjectRequest req) {
        Project project = findActiveProject(id);

        if (req.getTitle() != null && !req.getTitle().isBlank()) {
            project.setTitle(req.getTitle());
        }
        if (req.getDescription() != null) {
            project.setDescription(req.getDescription());
        }
        if (req.getSlug() != null && !req.getSlug().isBlank()) {
            project.setSlug(req.getSlug());
        }
        if (req.getPublished() != null) {
            project.setPublished(req.getPublished());
        }
        if (req.getFeatured() != null) {
            project.setFeatured(req.getFeatured());
        }
        if (req.getVideoUrl() != null) {
            project.setVideoUrl(req.getVideoUrl().isBlank() ? null : req.getVideoUrl());
        }
        if (req.getCategoryIds() != null) {
            project.setCategories(resolveCategories(req.getCategoryIds()));
        }
        if (req.getImageObjectPaths() != null) {
            // Replace images: remove all existing, add new ones
            project.getImages().clear();
            projectRepository.save(project); // flush removal via orphanRemoval
            addImages(project, req.getImageObjectPaths());
        }

        return toProjectResponse(projectRepository.save(project));
    }

    /**
     * Soft-deletes a project by setting deletedAt. GCS deletion is handled by the caller.
     *
     * @throws NotFoundException if the project does not exist or is already deleted
     */
    public void deleteProject(Long id) {
        Project project = findActiveProject(id);
        project.setDeletedAt(OffsetDateTime.now());
        projectRepository.save(project);
    }

    /**
     * Returns a project by ID regardless of published status (admin use).
     *
     * @throws NotFoundException if the project does not exist or is deleted
     */
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        return toProjectResponse(findActiveProject(id));
    }

    /**
     * Publishes or unpublishes a project.
     *
     * @throws NotFoundException if the project does not exist or is deleted
     */
    public ProjectResponse togglePublish(Long id, boolean published) {
        Project project = findActiveProject(id);
        project.setPublished(published);
        return toProjectResponse(projectRepository.save(project));
    }

    /**
     * Reorders images for a project by applying the sort orders in the request.
     *
     * @throws NotFoundException if the project or any referenced image does not exist
     */
    public void reorderImages(Long id, ReorderImagesRequest req) {
        Project project = findActiveProject(id);

        Map<Long, ProjectImage> imageMap = project.getImages().stream()
                .collect(Collectors.toMap(ProjectImage::getId, img -> img));

        for (ReorderImagesRequest.ImageOrderItem item : req.getItems()) {
            ProjectImage image = imageMap.get(item.getImageId());
            if (image == null) {
                throw new NotFoundException("Image not found: " + item.getImageId()
                        + " for project: " + id);
            }
            image.setSortOrder(item.getSortOrder());
        }

        projectRepository.save(project);
    }

    /**
     * Deletes a single image from a project.
     *
     * @return the GCS objectPath of the deleted image so the caller can clean up GCS
     * @throws NotFoundException if the project or image does not exist
     */
    public String deleteImage(Long projectId, Long imageId) {
        Project project = findActiveProject(projectId);

        ProjectImage image = project.getImages().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Image not found: " + imageId
                        + " for project: " + projectId));

        String objectPath = image.getObjectPath();
        project.getImages().remove(image);
        projectRepository.save(project);
        return objectPath;
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    private Project findActiveProject(Long id) {
        return projectRepository.findById(id)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("Project not found: " + id));
    }

    private Set<Category> resolveCategories(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Category> found = categoryRepository.findAllById(categoryIds);
        return new HashSet<>(found);
    }

    private void addImages(Project project, List<String> objectPaths) {
        int sortOrder = project.getImages().size();
        for (String objectPath : objectPaths) {
            ProjectImage image = ProjectImage.builder()
                    .project(project)
                    .objectPath(objectPath)
                    .sortOrder(sortOrder++)
                    .build();
            project.getImages().add(image);
        }
    }

    private PageResponse<ProjectResponse> toPageResponse(Page<Project> page) {
        return PageResponse.<ProjectResponse>builder()
                .content(page.getContent().stream().map(this::toProjectResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    private ProjectResponse toProjectResponse(Project project) {
        List<ImageResponse> images = project.getImages().stream()
                .map(img -> ImageResponse.builder()
                        .id(img.getId())
                        .objectPath(img.getObjectPath())
                        .publicUrl(storageService.getPublicUrl(img.getObjectPath()))
                        .sortOrder(img.getSortOrder())
                        .build())
                .toList();

        List<CategoryResponse> categories = project.getCategories().stream()
                .map(cat -> CategoryResponse.builder()
                        .id(cat.getId())
                        .name(cat.getName())
                        .build())
                .toList();

        return ProjectResponse.builder()
                .id(project.getId())
                .title(project.getTitle())
                .slug(project.getSlug())
                .description(project.getDescription())
                .published(project.isPublished())
                .featured(project.isFeatured())
                .videoUrl(project.getVideoUrl())
                .createdAt(project.getCreatedAt())
                .images(images)
                .categories(categories)
                .build();
    }
}
