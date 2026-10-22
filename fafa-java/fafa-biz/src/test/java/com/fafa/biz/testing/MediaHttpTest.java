package com.fafa.biz.testing;

import com.fafa.biz.media.domain.ObjectStorage;
import com.fafa.biz.media.application.MediaService;
import com.fafa.biz.usage.application.StorageQuota;
import com.fafa.common.error.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class MediaHttpTest extends IdentityHttpSupport {
    @MockitoBean ObjectStorage storage;
    @Autowired StorageQuota quota;
    @Autowired MediaService media;
    byte[] image;
    String token,owner,pet;
    @BeforeEach void setupMedia() throws Exception {
        var bytes=new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(20,10,BufferedImage.TYPE_INT_RGB),"png",bytes);
        image=bytes.toByteArray();
        var registration=register("media@example.com").path("data");
        token=registration.at("/session/token").asText();owner=registration.at("/user/id").asText();
        pet=write("/v1/pets",Map.of("nickname","雪球","species","CAT"),"pet",201).at("/data/id").asText();
        quota.grant(owner,"grant-media",100000,"test","测试赠送");
        when(storage.read(anyString(),anyLong())).thenReturn(image);
        when(storage.policy(anyString(),anyString(),anyLong(),any())).thenReturn(new ObjectStorage.Policy("https://storage.invalid/bucket",Map.of("key","example")));
        when(storage.download(anyString(),anyInt())).thenReturn("https://storage.invalid/preview?signature=example");
    }
    Map<String,Object> request(String hash,String mime) {
        return Map.of("petId",pet,"fileName","猫咪.png","mime",mime,"bytes",Integer.toString(image.length),"sha256",hash);
    }
    String digest() throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(image)); }
    String create(String key) throws Exception {
        return write("/v1/upload-sessions",request(digest(),"image/png"),key,201).at("/data/id").asText();
    }
    @Test void uploadCompletesOnceAndPrivatePreviewCanBecomeAvatar() throws Exception {
        String id=create("image");
        assertThat(create("image")).isEqualTo(id);
        write("/v1/upload-sessions/"+id+"/upload-policy",null,null,200);
        write("/v1/upload-sessions/"+id+":complete",null,null,200);
        write("/v1/upload-sessions/"+id+":complete",null,null,200);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM media_asset",Integer.class)).isEqualTo(1);
        assertThat(quota.view(owner).used()).isEqualTo(Integer.toString(image.length));
        var response=mvc.perform(put("/v1/pets/"+pet+"/avatar").header("Authorization","Bearer "+token)
            .header("Idempotency-Key","avatar").contentType("application/json")
            .content(json.writeValueAsBytes(Map.of("mediaId",id,"version",1)))).andReturn().getResponse();
        assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(200);
        assertThat(getJson("/v1/pets/"+pet+"/avatar",token).at("/data/mediaId").asText()).isEqualTo(id);
        write("/v1/media/"+id+"/access-tickets",null,null,200);
        when(clock.instant()).thenReturn(NOW.plusSeconds(301));
        media.cleanup(id);media.cleanup(id);
        assertThat(quota.view(owner).used()).isEqualTo(Integer.toString(image.length));
        verify(storage,times(1)).delete(contains("staging/"));
    }
    @Test void cancellationHoldsReservationUntilPolicyExpiryAndCleanupIsRetryable() throws Exception {
        String id=create("cancel");
        write("/v1/upload-sessions/"+id+":cancel",null,null,200);
        assertThat(quota.view(owner).reserved()).isEqualTo(Integer.toString(image.length));
        write("/v1/upload-sessions/"+id+":complete",null,null,409);
        when(clock.instant()).thenReturn(NOW.plusSeconds(301));
        doThrow(new FafaException(CommonErrorCode.DEPENDENCY_UNAVAILABLE)).when(storage).delete(anyString());
        assertThatThrownBy(()->media.cleanup(id)).isInstanceOf(FafaException.class);
        assertThat(quota.view(owner).reserved()).isEqualTo(Integer.toString(image.length));
        doNothing().when(storage).delete(anyString());
        media.cleanup(id);
        assertThat(quota.view(owner).reserved()).isEqualTo("0");
    }
    @Test void forgedChecksumAndMimeNeverBecomeReady() throws Exception {
        String id=write("/v1/upload-sessions",request("a".repeat(64),"image/png"),"bad-checksum",201).at("/data/id").asText();
        write("/v1/upload-sessions/"+id+":complete",null,null,400);
        assertThat(getJson("/v1/upload-sessions/"+id,token).at("/data/state").asText()).isEqualTo("FAILED");
        String jpeg=write("/v1/upload-sessions",request(digest(),"image/jpeg"),"bad-mime",201).at("/data/id").asText();
        write("/v1/upload-sessions/"+jpeg+":complete",null,null,400);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM media_asset",Integer.class)).isZero();
    }
    @Test void quotaValidationAndOwnershipFailBeforeSigning() throws Exception {
        write("/v1/upload-sessions",Map.of("petId",pet,"fileName","x.png","mime","image/png","bytes","20971521","sha256",digest()),"huge",413);
        write("/v1/upload-sessions",Map.of("petId",pet,"fileName","x.png","mime","image/png","bytes","100001","sha256",digest()),"quota",409);
        String id=create("private");
        var invalidAvatar=mvc.perform(put("/v1/pets/"+pet+"/avatar").header("Authorization","Bearer "+token)
            .header("Idempotency-Key","no-version").contentType("application/json")
            .content(json.writeValueAsBytes(Map.of("mediaId",id)))).andReturn().getResponse();
        assertThat(invalidAvatar.getStatus()).isEqualTo(400);
        String stranger=register("media-stranger@example.com").at("/data/session/token").asText();
        var response=mvc.perform(post("/v1/upload-sessions/"+id+"/upload-policy")
            .header("Authorization","Bearer "+stranger)).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(404);
        verify(storage,never()).policy(anyString(),anyString(),anyLong(),any());
    }
    @Test void objectWriteFailurePreservesReservationAndCanRetry() throws Exception {
        String id=create("storage-failure");
        doThrow(new FafaException(CommonErrorCode.DEPENDENCY_UNAVAILABLE)).when(storage).put(anyString(),any(),anyString());
        write("/v1/upload-sessions/"+id+":complete",null,null,503);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM media_asset",Integer.class)).isZero();
        assertThat(quota.view(owner).reserved()).isEqualTo(Integer.toString(image.length));
        doNothing().when(storage).put(anyString(),any(),anyString());
        write("/v1/upload-sessions/"+id+":complete",null,null,200);
        assertThat(getJson("/v1/pets/"+pet+"/deletion-impact",token).at("/data/resourceCounts/media").asInt()).isEqualTo(1);
    }
    @Test void concurrentCompletionDoesNotDoubleSettle() throws Exception {
        String id=create("concurrent");
        try(var pool=Executors.newFixedThreadPool(2)) {
            var a=pool.submit(()->write("/v1/upload-sessions/"+id+":complete",null,null,200));
            var b=pool.submit(()->write("/v1/upload-sessions/"+id+":complete",null,null,200));
            a.get(20,TimeUnit.SECONDS);b.get(20,TimeUnit.SECONDS);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM quota_ledger WHERE entry_type='SETTLE'",Integer.class)).isEqualTo(1);
    }
    private com.fasterxml.jackson.databind.JsonNode write(String path,Object body,String key,int status) throws Exception {
        var request=post(path).header("Authorization","Bearer "+token);
        if(key!=null) request.header("Idempotency-Key",key);
        if(body!=null) request.contentType("application/json").content(json.writeValueAsBytes(body));
        var response=mvc.perform(request).andReturn().getResponse();
        assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(status);
        return json.readTree(response.getContentAsString());
    }
}
