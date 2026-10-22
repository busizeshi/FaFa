package com.fafa.biz.testing;

import com.fafa.biz.media.application.*;
import com.fafa.biz.media.domain.ObjectStorage;
import com.fafa.biz.media.domain.MediaModels.*;
import com.fafa.biz.media.infrastructure.*;
import com.fafa.biz.usage.application.StorageQuota;
import com.fafa.common.error.*;
import java.util.*;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.*;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class MediaLibraryTest extends IdentityHttpSupport {
    @MockitoBean ObjectStorage objects;
    @Autowired StorageQuota quota;
    @Autowired MediaLibrary library;
    @Autowired MediaJobService jobs;
    @Autowired MediaJobStore jobStore;
    @Autowired MediaStore store;
    @Autowired MultipartUploads multipart;
    String token,owner,pet;
    byte[] png;
    @BeforeEach void prepare() throws Exception {
        var user=register("library@example.com").path("data");
        owner=user.at("/user/id").asText();token=user.at("/session/token").asText();
        pet=call("POST","/v1/pets",Map.of("nickname","雪球","species","CAT"),"pet",201).at("/data/id").asText();
        quota.grant(owner,"library-grant",1000000000,"test","媒体测试额度");
        var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(10,10,BufferedImage.TYPE_INT_RGB),"png",out);png=out.toByteArray();
        when(objects.read(anyString(),anyLong())).thenReturn(png);
    }
    String image() throws Exception {
        String hash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(png));
        String id=call("POST","/v1/upload-sessions",Map.of("petId",pet,"fileName","x.png","mime","image/png","bytes",Integer.toString(png.length),"sha256",hash),
            UUID.randomUUID().toString(),201).at("/data/id").asText();
        call("POST","/v1/upload-sessions/"+id+":complete",null,null,200);return id;
    }
    @Test void albumsMetadataFiltersAndDeletingAlbumPreservesMedia() throws Exception {
        String id=image();
        var body=Map.of("name","早餐","description","");
        String album=call("POST","/v1/albums",body,"album",201).at("/data/id").asText();
        assertThat(call("POST","/v1/albums",body,"album",201).at("/data/id").asText()).isEqualTo(album);
        call("PUT","/v1/albums/"+album+"/items/"+id,null,null,200);
        assertThat(call("PUT","/v1/albums/"+album+"/items/"+id,null,null,200).at("/data/version").asInt()).isEqualTo(2);
        call("PATCH","/v1/media/"+id,Map.of("version",1,"title","早餐100%","description","实际投喂","tags",List.of("早餐","猫咪")),null,200);
        call("PATCH","/v1/media/"+id,Map.of("version",1,"title","旧","description","","tags",List.of()),null,409);
        assertThat(getJson("/v1/media?albumId="+album+"&tag=早餐&type=IMAGE",token).at("/data/items").size()).isEqualTo(1);
        var filtered=mvc.perform(get("/v1/media").param("q","100%").header("Authorization","Bearer "+token)).andReturn().getResponse();
        assertThat(json.readTree(filtered.getContentAsString()).at("/data/items").size()).isEqualTo(1);
        assertThat(getJson("/v1/media?q=不存在",token).at("/data/items").size()).isZero();
        call("DELETE","/v1/albums/"+album+"?version=2",null,null,200);
        assertThat(getResult("/v1/media/"+id,token).getResponse().getStatus()).isEqualTo(200);
        verify(objects,never()).delete(anyString());
    }
    @Test void deletionRequiresAcknowledgementAndReleasesOnlyAfterPhysicalSuccess() throws Exception {
        String id=image();
        call("PUT","/v1/pets/"+pet+"/avatar",Map.of("mediaId",id,"version",1),"avatar",200);
        call("DELETE","/v1/media/"+id,Map.of("version",1,"acknowledgedReferences",false),null,409);
        call("DELETE","/v1/media/"+id,Map.of("version",1,"acknowledgedReferences",true),null,200);
        call("POST","/v1/media/"+id+"/access-tickets",null,null,409);
        assertThat(getJson("/v1/pets/"+pet+"/avatar",token).at("/data/mediaId").isNull()).isTrue();
        library.purge(id);
        assertThat(quota.view(owner).used()).isEqualTo(Integer.toString(png.length));
        when(clock.instant()).thenReturn(NOW.plusSeconds(301));
        doThrow(new FafaException(CommonErrorCode.DEPENDENCY_UNAVAILABLE)).when(objects).delete(anyString());
        assertThatThrownBy(()->library.purge(id)).isInstanceOf(FafaException.class);
        assertThat(quota.view(owner).used()).isEqualTo(Integer.toString(png.length));
        doNothing().when(objects).delete(anyString());
        library.purge(id);library.purge(id);
        assertThat(quota.view(owner).used()).isEqualTo("0");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM quota_ledger WHERE entry_type='FREE'",Integer.class)).isEqualTo(1);
    }
    @Test void jobReceiptLeaseRecoveryAndCancellationFenceStaleWorker() throws Exception {
        when(objects.inspect(anyString())).thenReturn(new ObjectStorage.ObjectInfo(100,"etag"));
        String id=call("POST","/v1/upload-sessions",Map.of("petId",pet,"fileName","x.pdf","mime","application/pdf","bytes","100","sha256","a".repeat(64)),
            "pdf",201).at("/data/id").asText();
        assertThat(call("POST","/v1/upload-sessions/"+id+":complete",null,null,200).at("/data/state").asText()).isEqualTo("PROCESSING");
        String event=jobStore.pending(NOW).getFirst().id();
        assertThat(jobs.claim(UUID.randomUUID().toString(),id)).isNull();
        var lease=jobs.claim(event,id);assertThat(lease).isNotNull();assertThat(jobs.claim(event,id)).isNull();
        when(clock.instant()).thenReturn(NOW.plusSeconds(601));jobs.recover(id);
        assertThat(jobStore.find(id).state()).isEqualTo("QUEUED");
        assertThat(jobs.finish(lease,null)).isFalse();
        call("POST","/v1/upload-sessions/"+id+":cancel",null,null,200);
        assertThat(jobStore.find(id).state()).isEqualTo("CANCELLED");
    }
    @Test void resumePartsReuseReservationAndRejectWrongSize() throws Exception {
        String id=call("POST","/v1/upload-sessions",Map.of("petId",pet,"fileName","x.mp4","mime","video/mp4","bytes","8388609",
            "sha256","a".repeat(64),"uploadMode","MULTIPART"),"video",201).at("/data/id").asText();
        when(objects.inspect(contains("/parts/1"))).thenReturn(new ObjectStorage.ObjectInfo(8388608,"one"));
        when(objects.inspect(contains("/parts/2"))).thenReturn(new ObjectStorage.ObjectInfo(1,"two"));
        assertThat(getJson("/v1/upload-sessions/"+id+"/parts",token).at("/data/parts").size()).isEqualTo(2);
        call("POST","/v1/upload-sessions/"+id+"/upload-policy",null,null,409);
        when(objects.policy(anyString(),anyString(),anyLong(),any())).thenReturn(new ObjectStorage.Policy("https://store.invalid",Map.of("key","part")));
        call("POST","/v1/upload-sessions/"+id+"/part-policies",Map.of("partNumber",2),null,200);
        call("POST","/v1/upload-sessions/"+id+"/part-policies",Map.of("partNumber",3),null,409);
        when(objects.inspect(contains("/parts/2"))).thenReturn(new ObjectStorage.ObjectInfo(2,"bad"));
        call("POST","/v1/upload-sessions/"+id+":complete",null,null,409);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM quota_reservation",Integer.class)).isEqualTo(1);
        verify(objects,never()).compose(anyList(),anyString());
    }
    @Test void ownershipAndConcurrentDeleteCannotDoubleFree() throws Exception {
        String id=image();String stranger=register("library-stranger@example.com").at("/data/session/token").asText();
        var result=mvc.perform(get("/v1/media/"+id+"/deletion-impact").header("Authorization","Bearer "+stranger)).andReturn().getResponse();
        assertThat(result.getStatus()).isEqualTo(404);
        call("DELETE","/v1/media/"+id,Map.of("version",1,"acknowledgedReferences",true),null,200);
        when(clock.instant()).thenReturn(NOW.plus(1,ChronoUnit.HOURS));
        try(var pool=Executors.newFixedThreadPool(2)) {
            var a=pool.submit(()->library.purge(id));var b=pool.submit(()->library.purge(id));a.get(15,TimeUnit.SECONDS);b.get(15,TimeUnit.SECONDS);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM quota_ledger WHERE entry_type='FREE'",Integer.class)).isEqualTo(1);
    }
    private com.fasterxml.jackson.databind.JsonNode call(String method,String path,Object body,String key,int status) throws Exception {
        var request=request(org.springframework.http.HttpMethod.valueOf(method),path).header("Authorization","Bearer "+token);
        if(key!=null) request.header("Idempotency-Key",key);
        if(body!=null) request.contentType("application/json").content(json.writeValueAsBytes(body));
        var response=mvc.perform(request).andReturn().getResponse();
        assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(status);
        return json.readTree(response.getContentAsString());
    }
}
