package am.loras.backend.service;

import am.loras.backend.config.AppProperties;
import am.loras.backend.dto.response.SignedUrlResponse;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.SignUrlOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class StorageService {

    private final Storage storage;
    private final String bucketName;

    public StorageService(Storage storage, AppProperties appProperties) {
        this.storage = storage;
        this.bucketName = appProperties.getGcs().getBucketName();
    }

    /**
     * Generates a V4 signed PUT URL that allows a client to upload directly to GCS.
     *
     * @param objectPath  the GCS object name (e.g. "images/uuid.jpg")
     * @param contentType the MIME type the client will use in the PUT request
     * @param ttl         how long the signed URL remains valid
     * @return a {@link SignedUrlResponse} containing the signed URL, object path, and public URL
     */
    public SignedUrlResponse generateSignedUploadUrl(String objectPath, String contentType, Duration ttl) {
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, objectPath))
                .setContentType(contentType)
                .build();

        URL signedUrl = storage.signUrl(
                blobInfo,
                ttl.toMinutes(),
                TimeUnit.MINUTES,
                SignUrlOption.httpMethod(HttpMethod.PUT),
                SignUrlOption.withV4Signature(),
                SignUrlOption.withContentType()
        );

        String publicUrl = getPublicUrl(objectPath);

        return SignedUrlResponse.builder()
                .signedUrl(signedUrl.toString())
                .objectPath(objectPath)
                .publicUrl(publicUrl)
                .build();
    }

    /**
     * Deletes a GCS object. Errors are swallowed and logged — a missing object is not an error.
     *
     * @param objectPath the GCS object name to delete
     */
    public void deleteObject(String objectPath) {
        try {
            storage.delete(BlobId.of(bucketName, objectPath));
        } catch (Exception e) {
            log.warn("Failed to delete GCS object '{}': {}", objectPath, e.getMessage());
        }
    }

    /**
     * Returns the public URL for a GCS object.
     *
     * @param objectPath the GCS object name
     * @return the public URL string
     */
    public String getPublicUrl(String objectPath) {
        return "https://storage.googleapis.com/" + bucketName + "/" + objectPath;
    }
}
