package am.loras.backend.service;

import am.loras.backend.domain.Category;
import am.loras.backend.dto.response.CategoryResponse;
import am.loras.backend.exception.ConflictException;
import am.loras.backend.exception.NotFoundException;
import am.loras.backend.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Returns all categories with the count of non-deleted projects in each.
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        List<Object[]> rows = categoryRepository.findAllWithProjectCount();
        return rows.stream()
                .map(row -> {
                    Category category = (Category) row[0];
                    Long count = ((Number) row[1]).longValue();
                    return CategoryResponse.builder()
                            .id(category.getId())
                            .name(category.getName())
                            .projectCount(count)
                            .build();
                })
                .toList();
    }

    /**
     * Creates a new category.
     *
     * @throws ConflictException if a category with the same name already exists
     */
    public CategoryResponse createCategory(String name) {
        if (categoryRepository.existsByName(name)) {
            throw new ConflictException("Category with name '" + name + "' already exists");
        }
        Category category = Category.builder()
                .name(name)
                .build();
        categoryRepository.save(category);
        return toCategoryResponse(category);
    }

    /**
     * Renames an existing category.
     *
     * @throws NotFoundException if the category does not exist
     * @throws ConflictException if the new name is already taken
     */
    public CategoryResponse renameCategory(Long id, String name) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found: " + id));
        if (!category.getName().equals(name) && categoryRepository.existsByName(name)) {
            throw new ConflictException("Category with name '" + name + "' already exists");
        }
        category.setName(name);
        categoryRepository.save(category);
        return toCategoryResponse(category);
    }

    /**
     * Deletes a category.
     *
     * @throws NotFoundException if the category does not exist
     * @throws ConflictException if any project is still linked to this category
     */
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found: " + id));
        if (!category.getProjects().isEmpty()) {
            throw new ConflictException(
                    "Cannot delete category '" + category.getName()
                    + "' — it is still referenced by " + category.getProjects().size() + " project(s)");
        }
        categoryRepository.delete(category);
    }

    // -------------------------------------------------------------------------
    // Mappers
    // -------------------------------------------------------------------------

    private CategoryResponse toCategoryResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
}
