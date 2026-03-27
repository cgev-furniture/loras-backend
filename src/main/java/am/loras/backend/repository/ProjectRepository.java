package am.loras.backend.repository;

import am.loras.backend.domain.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findBySlugAndDeletedAtIsNull(String slug);

    Page<Project> findByPublishedTrueAndDeletedAtIsNull(Pageable pageable);

    Page<Project> findByDeletedAtIsNull(Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p JOIN p.categories c WHERE p.published = true AND p.deletedAt IS NULL AND c.id IN :categoryIds")
    Page<Project> findPublishedByCategoryIds(@Param("categoryIds") List<Long> categoryIds, Pageable pageable);

    boolean existsBySlug(String slug);

    long countByPublishedTrueAndDeletedAtIsNull();

    long countByPublishedFalseAndDeletedAtIsNull();

    Page<Project> findByPublishedFalseAndDeletedAtIsNull(Pageable pageable);
}
