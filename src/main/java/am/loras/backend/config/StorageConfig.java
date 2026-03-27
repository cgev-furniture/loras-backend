package am.loras.backend.config;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    /**
     * Creates the Google Cloud Storage client using Application Default Credentials (ADC).
     * On Cloud Run, ADC resolves automatically via the service account attached to the instance.
     * Locally, run {@code gcloud auth application-default login} before starting the service.
     */
    @Bean
    public Storage storage() {
        return StorageOptions.getDefaultInstance().getService();
    }
}
