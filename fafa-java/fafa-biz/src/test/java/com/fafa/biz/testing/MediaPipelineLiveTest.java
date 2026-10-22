package com.fafa.biz.testing;

import com.fafa.biz.media.application.*;
import com.fafa.biz.media.infrastructure.*;
import com.fafa.biz.usage.application.StorageQuota;
import com.fasterxml.jackson.databind.JsonNode;
import io.minio.*;
import java.nio.file.*;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import org.apache.pdfbox.pdmodel.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Gateway→真实 Biz HTTP→MySQL/MinIO→RabbitMQ→工具进程；账号邮件及会话仍使用测试适配器。 */
@EnabledIfEnvironmentVariable(named="FAFA_MEDIA_PIPELINE_TEST",matches="true")
@SpringBootTest(classes=IdentityTestApplication.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties="spring.config.location=classpath:identity-test.yml")
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class MediaPipelineLiveTest extends IdentityHttpSupport {
    static final String SUFFIX=UUID.randomUUID().toString().replace("-","");
    static final String BUCKET="fafa-pipeline-"+SUFFIX,EXCHANGE="fafa.test."+SUFFIX,QUEUE=EXCHANGE+".process";
    @LocalServerPort int port;
    @Autowired StorageQuota quota;
    @Autowired MediaLibrary library;
    @Autowired MediaProcessRunner tools;
    @Autowired ConnectionFactory rabbit;
    @Autowired RabbitListenerEndpointRegistry listeners;
    String base,token;
    @DynamicPropertySource static void setup(DynamicPropertyRegistry p) {
        p.add("fafa.media.storage.endpoint",()->System.getenv("FAFA_MEDIA_TEST_ENDPOINT"));
        p.add("fafa.media.storage.access-key",()->System.getenv("FAFA_MEDIA_TEST_ACCESS_KEY"));
        p.add("fafa.media.storage.secret-key",()->System.getenv("FAFA_MEDIA_TEST_SECRET_KEY"));
        p.add("fafa.media.storage.bucket",()->BUCKET);
        p.add("fafa.media.messaging-enabled",()->"true");
        p.add("fafa.media.exchange",()->EXCHANGE);p.add("fafa.media.queue",()->QUEUE);
        p.add("fafa.media.processing.ffmpeg",()->System.getenv("FAFA_FFMPEG"));
        p.add("fafa.media.processing.ffprobe",()->System.getenv("FAFA_FFPROBE"));
        p.add("spring.rabbitmq.host",()->System.getenv("FAFA_RABBITMQ_HOST"));
        p.add("spring.rabbitmq.username",()->System.getenv("FAFA_RABBITMQ_USERNAME"));
        p.add("spring.rabbitmq.password",()->System.getenv("FAFA_RABBITMQ_PASSWORD"));
        p.add("spring.rabbitmq.virtual-host",()->"/fafa");
        p.add("spring.rabbitmq.listener.simple.prefetch",()->"1");
    }
    @Test void pdfAndMultipartVideoReachReadyAndPhysicalDeleteBalancesStorage() throws Exception {
        when(clock.instant()).thenReturn(Instant.now());
        var admin=MinioClient.builder().endpoint(System.getenv("FAFA_MEDIA_TEST_ENDPOINT"))
            .credentials(System.getenv("FAFA_MEDIA_TEST_ACCESS_KEY"),System.getenv("FAFA_MEDIA_TEST_SECRET_KEY")).build();
        admin.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
        Path folder=Files.createTempDirectory("fafa-live-fixture-");
        try(var gateway=new GatewayProcess(port)) {
            base=gateway.base;
            var registered=register("pipeline@example.com").path("data");
            token=registered.at("/session/token").asText();String owner=registered.at("/user/id").asText();
            quota.grant(owner,"pipeline-grant",500000000,"test","完整媒体验收");
            String pet=api("POST","/pets",Map.of("nickname","雪球","species","CAT"),"pet",201).path("id").asText();
            Path pdf=folder.resolve("medical.pdf");
            try(var doc=new PDDocument()) { doc.addPage(new PDPage());doc.save(pdf.toFile()); }
            String document=upload(pet,"application/pdf",Files.readAllBytes(pdf),false);
            waitReady(document);
            assertThat(api("GET","/media/"+document,null,null,200).path("pageCount").asInt()).isEqualTo(1);
            var fileTicket=api("POST","/media/"+document+"/access-tickets?variant=DOCUMENT",null,null,200);
            var download=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(fileTicket.path("url").asText())).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
            assertThat(download.statusCode()).isEqualTo(200);
            assertThat(download.headers().firstValue("content-disposition").orElse("")).contains("attachment");
            Path mp4=folder.resolve("short.mp4");
            tools.run(List.of(System.getenv("FAFA_FFMPEG"),"-nostdin","-v","error","-f","lavfi","-i","color=c=blue:s=160x120:d=1",
                "-c:v","libx264","-pix_fmt","yuv420p","-threads","1",mp4.toString()),30);
            String video=upload(pet,"video/mp4",Files.readAllBytes(mp4),true);
            waitReady(video);
            api("POST","/upload-sessions/"+video+":complete",null,null,200);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM quota_ledger WHERE entry_type='SETTLE'",Integer.class)).isEqualTo(2);
            var ticket=api("POST","/media/"+video+"/access-tickets?variant=PLAYBACK",null,null,200);
            assertThat(HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(ticket.path("url").asText())).GET().build(),
                HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(200);
            String album=api("POST","/albums",Map.of("name","视频","description",""),"album",201).path("id").asText();
            api("PUT","/albums/"+album+"/items/"+video,null,null,200);
            api("DELETE","/albums/"+album+"?version=2",null,null,200);
            api("GET","/media/"+video,null,null,200);
            for(String id:List.of(document,video)) api("DELETE","/media/"+id,Map.of("version",1,"acknowledgedReferences",true),null,200);
            assertThat(Long.parseLong(quota.view(owner).used())).isPositive();
            when(clock.instant()).thenReturn(Instant.now().plusSeconds(901));
            library.purge(document);library.purge(video);
            assertThat(quota.view(owner).used()).isEqualTo("0");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM quota_ledger WHERE entry_type='FREE'",Integer.class)).isEqualTo(2);
        } finally {
            listeners.stop();
            var control=new RabbitAdmin(rabbit);
            control.deleteQueue(QUEUE);control.deleteQueue(QUEUE+".dead");
            control.deleteExchange(EXCHANGE);control.deleteExchange(EXCHANGE+".dead");
            var keys=new ArrayList<String>();
            for(var item:admin.listObjects(ListObjectsArgs.builder().bucket(BUCKET).recursive(true).build())) keys.add(item.get().objectName());
            for(String key:keys) admin.removeObject(RemoveObjectArgs.builder().bucket(BUCKET).object(key).build());
            admin.removeBucket(RemoveBucketArgs.builder().bucket(BUCKET).build());admin.close();
            try(var paths=Files.walk(folder)) { for(Path file:paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(file); }
        }
    }
    private String upload(String pet,String mime,byte[] bytes,boolean multipart) throws Exception {
        String hash=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
        String id=api("POST","/upload-sessions",Map.of("petId",pet,"fileName","fixture","mime",mime,"bytes",Integer.toString(bytes.length),
            "sha256",hash,"uploadMode",multipart?"MULTIPART":"SINGLE"),UUID.randomUUID().toString(),201).path("id").asText();
        var signed=api("POST","/upload-sessions/"+id+(multipart?"/part-policies":"/upload-policy"),
            multipart?Map.of("partNumber",1):null,null,200);
        Map<String,String> fields=json.convertValue(signed.path("formFields"),new com.fasterxml.jackson.core.type.TypeReference<>(){});
        StorageUpload.send(signed.path("uploadUrl").asText(),fields,bytes);
        if(multipart) assertThat(api("GET","/upload-sessions/"+id+"/parts",null,null,200).path("parts").size()).isEqualTo(1);
        assertThat(api("POST","/upload-sessions/"+id+":complete",null,null,200).path("state").asText()).isEqualTo("PROCESSING");
        return id;
    }
    private void waitReady(String id) throws Exception {
        long until=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(90);
        while(System.nanoTime()<until) {
            var session=api("GET","/upload-sessions/"+id,null,null,200);
            if("READY".equals(session.path("state").asText())) return;
            if("FAILED".equals(session.path("state").asText())) fail("处理失败："+api("GET","/upload-sessions/"+id+"/processing-job",null,null,200));
            Thread.sleep(250);
        }
        fail("处理超时："+api("GET","/upload-sessions/"+id+"/processing-job",null,null,200));
    }
    private JsonNode api(String method,String path,Object body,String key,int expected) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create(base+path)).timeout(Duration.ofSeconds(30)).header("Authorization","Bearer "+token);
        if(key!=null) builder.header("Idempotency-Key",key);
        if(body!=null) builder.header("Content-Type","application/json");
        var response=HttpClient.newHttpClient().send(builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():
            HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(body))).build(),HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(response.body()).isEqualTo(expected);return json.readTree(response.body()).path("data");
    }
}
