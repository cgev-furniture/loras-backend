package am.loras.backend.repository;

import am.loras.backend.domain.ProjectImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ProjectImageRepository extends JpaRepository<ProjectImage, Long> {

    List<ProjectImage> findByProjectIdOrderBySortOrderAsc(Long projectId);

    @Transactional
    void deleteByProjectId(Long projectId);
}
