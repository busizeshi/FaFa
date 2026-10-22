package com.fafa.biz.testing;

import com.fafa.biz.media.infrastructure.MinioObjectStorage;
import io.minio.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.assertj.core.api.Assertions.*;

/** 可选真实 MinIO 验收；仅创建和删除本次随机命名的测试 bucket。 */
@EnabledIfEnvironmentVariable(named="FAFA_MEDIA_TEST_ENDPOINT",matches=".+")
class MinioStorageTest {
    @Test void signedPostConstraintsPrivatePreviewAndDeletion() throws Exception {
        String endpoint=System.getenv("FAFA_MEDIA_TEST_ENDPOINT");
        String access=System.getenv("FAFA_MEDIA_TEST_ACCESS_KEY"),secret=System.getenv("FAFA_MEDIA_TEST_SECRET_KEY");
        String bucket="fafa-wp04-"+UUID.randomUUID().toString().replace("-","");
        var admin=MinioClient.builder().endpoint(endpoint).credentials(access,secret).build();
        admin.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        var storage=new MinioObjectStorage(endpoint,endpoint,access,secret,bucket);
        var http=HttpClient.newHttpClient();
        try {
            byte[] bytes="verified-test".getBytes(StandardCharsets.UTF_8);
            var policy=storage.policy("staging/test","image/png",bytes.length,Instant.now().plusSeconds(120));
            String boundary="fafaBoundary";
            var body=new java.io.ByteArrayOutputStream();
            for(var entry:policy.fields().entrySet()) {
                body.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\""+entry.getKey()+"\"\r\n\r\n"+entry.getValue()+"\r\n").getBytes(StandardCharsets.UTF_8));
            }
            body.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"test.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(bytes);body.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));
            var response=http.send(HttpRequest.newBuilder(URI.create(policy.url()))
                .header("Content-Type","multipart/form-data; boundary="+boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(),HttpResponse.BodyHandlers.discarding());
            assertThat(response.statusCode()).isIn(200,204);
            assertThat(storage.read("staging/test",bytes.length)).isEqualTo(bytes);
            storage.put("private/preview.png",bytes,"image/png");
            assertThat(http.send(HttpRequest.newBuilder(URI.create(endpoint+"/"+bucket+"/private/preview.png")).GET().build(),
                HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(403);
            assertThat(http.send(HttpRequest.newBuilder(URI.create(storage.download("private/preview.png",60))).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray()).body()).isEqualTo(bytes);
        } finally {
            storage.delete("staging/test");storage.delete("private/preview.png");
            admin.removeBucket(RemoveBucketArgs.builder().bucket(bucket).build());
            admin.close();
        }
    }
}
