package com.fafa.biz.testing;

import com.fafa.biz.media.domain.ObjectStorage;
import com.fafa.biz.usage.application.StorageQuota;
import io.minio.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** 真实存储、真实数据源与 HTTP 用例组合；移动认证与 SMTP 仍为测试适配器。 */
@EnabledIfEnvironmentVariable(named="FAFA_MEDIA_TEST_ENDPOINT",matches=".+")
class MediaLiveTest extends IdentityHttpSupport {
    static final String BUCKET="fafa-live-"+UUID.randomUUID().toString().replace("-","");
    @Autowired ObjectStorage storage;
    @Autowired StorageQuota quota;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry p) {
        p.add("fafa.media.storage.endpoint",()->System.getenv("FAFA_MEDIA_TEST_ENDPOINT"));
        p.add("fafa.media.storage.access-key",()->System.getenv("FAFA_MEDIA_TEST_ACCESS_KEY"));
        p.add("fafa.media.storage.secret-key",()->System.getenv("FAFA_MEDIA_TEST_SECRET_KEY"));
        p.add("fafa.media.storage.bucket",()->BUCKET);
    }
    @Test void realObjectVerificationAndDatabaseSettlement() throws Exception {
        var admin=MinioClient.builder().endpoint(System.getenv("FAFA_MEDIA_TEST_ENDPOINT"))
            .credentials(System.getenv("FAFA_MEDIA_TEST_ACCESS_KEY"),System.getenv("FAFA_MEDIA_TEST_SECRET_KEY")).build();
        admin.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
        String original=null,preview=null,staging=null;
        try {
            var registered=register("media-live@example.com").path("data");
            String owner=registered.at("/user/id").asText(),token=registered.at("/session/token").asText();
            quota.grant(owner,"live-grant",100000,"test","独立图片验收");
            var petResponse=mvc.perform(post("/v1/pets").header("Authorization","Bearer "+token).header("Idempotency-Key","live-pet")
                .contentType("application/json").content("{\"nickname\":\"雪球\",\"species\":\"CAT\"}")).andReturn().getResponse();
            assertThat(petResponse.getStatus()).isEqualTo(201);
            String pet=json.readTree(petResponse.getContentAsString()).at("/data/id").asText();
            var data=new ByteArrayOutputStream();
            ImageIO.write(new BufferedImage(32,16,BufferedImage.TYPE_INT_RGB),"png",data);
            String hash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(data.toByteArray()));
            var response=mvc.perform(post("/v1/upload-sessions").header("Authorization","Bearer "+token)
                .header("Idempotency-Key","live-image").contentType("application/json")
                .content(json.writeValueAsBytes(Map.of("petId",pet,"fileName","live.png","mime","image/png","bytes",Integer.toString(data.size()),"sha256",hash))))
                .andReturn().getResponse();
            assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(201);
            String id=json.readTree(response.getContentAsString()).at("/data/id").asText();
            staging="staging/"+owner+"/"+id; original="private/"+owner+"/"+id+"/original";preview="private/"+owner+"/"+id+"/preview.png";
            storage.put(staging,data.toByteArray(),"image/png");
            var complete=mvc.perform(post("/v1/upload-sessions/"+id+":complete").header("Authorization","Bearer "+token)).andReturn().getResponse();
            assertThat(complete.getStatus()).as(complete.getContentAsString()).isEqualTo(200);
            assertThat(json.readTree(complete.getContentAsString()).at("/data/state").asText()).isEqualTo("READY");
            assertThat(quota.view(owner).used()).isEqualTo(Integer.toString(data.size()));
            assertThat(storage.read(original,data.size())).isEqualTo(data.toByteArray());
        } finally {
            if(staging!=null) storage.delete(staging);
            if(original!=null) storage.delete(original);
            if(preview!=null) storage.delete(preview);
            admin.removeBucket(RemoveBucketArgs.builder().bucket(BUCKET).build());admin.close();
        }
    }
}
