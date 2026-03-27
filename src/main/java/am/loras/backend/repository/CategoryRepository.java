package am.loras.backend.repository;

import am.loras.backend.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

    boolean existsByName(String name);

    @Query("SELECT c, COUNT(p) FROM Category c LEFT JOIN c.projects p WHERE p.deletedAt IS NULL OR p IS NULL GROUP BY c")
    List<Object[]> findAllWithProjectCount();
}
