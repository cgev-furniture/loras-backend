package am.loras.backend.service;

import am.loras.backend.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.regex.Pattern;

@Service
public class SlugService {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern LEADING_TRAILING_HYPHENS = Pattern.compile("^-+|-+$");

    /**
     * Converts a title into a URL-safe slug:
     * lowercase → NFD-normalize (strips diacritics) → replace non-alphanumeric with hyphens →
     * strip leading/trailing hyphens → collapse consecutive hyphens.
     */
    public String generateSlug(String title) {
        if (title == null || title.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(title.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String slug = NON_ALPHANUMERIC.matcher(normalized).replaceAll("-");
        slug = LEADING_TRAILING_HYPHENS.matcher(slug).replaceAll("");
        return slug;
    }

    /**
     * Generates a slug that is unique among all projects.
     * Appends -2, -3, … until no collision is found.
     */
    public String generateUniqueSlug(String title, ProjectRepository repo) {
        String base = generateSlug(title);
        if (base.isBlank()) {
            base = "project";
        }
        String candidate = base;
        int suffix = 2;
        while (repo.existsBySlug(candidate)) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }
}
