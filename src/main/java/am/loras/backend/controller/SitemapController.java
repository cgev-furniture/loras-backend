package am.loras.backend.controller;

import am.loras.backend.config.AppProperties;
import am.loras.backend.domain.Project;
import am.loras.backend.repository.ProjectRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SitemapController {

    private final ProjectRepository projectRepository;
    private final AppProperties appProperties;

    public SitemapController(ProjectRepository projectRepository, AppProperties appProperties) {
        this.projectRepository = projectRepository;
        this.appProperties = appProperties;
    }

    /**
     * GET /sitemap.xml
     * Generates an XML sitemap listing all published project slugs.
     * The frontend URL is read from {@code app.frontend-url} so the sitemap links to the SPA,
     * not the backend API.
     */
    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() {
        String frontendUrl = appProperties.getFrontendUrl();

        // Fetch all published projects (no pagination limit — typically < 1000 for a portfolio site)
        List<Project> projects = projectRepository
                .findByPublishedTrueAndDeletedAtIsNull(PageRequest.of(0, 10_000))
                .getContent();

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // Static pages
        appendUrl(sb, frontendUrl + "/");
        appendUrl(sb, frontendUrl + "/portfolio");
        appendUrl(sb, frontendUrl + "/contact");

        // Dynamic project pages
        for (Project project : projects) {
            appendUrl(sb, frontendUrl + "/portfolio/" + project.getSlug());
        }

        sb.append("</urlset>");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(sb.toString());
    }

    private void appendUrl(StringBuilder sb, String loc) {
        sb.append("  <url>\n");
        sb.append("    <loc>").append(escapeXml(loc)).append("</loc>\n");
        sb.append("  </url>\n");
    }

    private String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
