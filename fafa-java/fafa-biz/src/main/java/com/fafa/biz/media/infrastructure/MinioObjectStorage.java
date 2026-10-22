package com.fafa.biz.media.infrastructure;

import com.fafa.biz.media.domain.ObjectStorage;
import com.fafa.common.error.*;
import io.minio.*;
import io.minio.http.Method;
import java.io.ByteArrayInputStream;
import java.time.*;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** MinIO 仅用于私有隔离区和正式对象；SDK 异常不透传供应商细节。 */
@Component
public class MinioObjectStorage implements ObjectStorage {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(MinioObjectStorage.class);
    private final MinioClient internal;
    private final MinioClient external;
    private final String endpoint,bucket;
    public MinioObjectStorage(@Value("${fafa.media.storage.endpoint:}") String endpoint,
            @Value("${fafa.media.storage.public-endpoint:}") String publicEndpoint,
            @Value("${fafa.media.storage.access-key:}") String access,
            @Value("${fafa.media.storage.secret-key:}") String secret,
            @Value("${fafa.media.storage.bucket:fafa-media}") String bucket) {
        this.endpoint=publicEndpoint.isBlank()?endpoint:publicEndpoint; this.bucket=bucket;
        if(endpoint.isBlank() || access.isBlank() || secret.isBlank()) { internal=null;external=null;return; }
        var http=new OkHttpClient.Builder().callTimeout(15,TimeUnit.SECONDS).build();
        internal=MinioClient.builder().endpoint(endpoint).credentials(access,secret).region("us-east-1").httpClient(http).build();
        external=MinioClient.builder().endpoint(this.endpoint).credentials(access,secret).region("us-east-1").httpClient(http).build();
    }
    private MinioClient client() {
        if(internal==null) throw new FafaException(CommonErrorCode.DEPENDENCY_UNAVAILABLE,"未配置私有对象存储");
        return internal;
    }
    public Policy policy(String key,String mime,long bytes,Instant expiresAt) {
        client();
        try {
            var policy=new PostPolicy(bucket,ZonedDateTime.ofInstant(expiresAt,ZoneOffset.UTC));
            policy.addEqualsCondition("key",key);
            policy.addEqualsCondition("Content-Type",mime);
            policy.addContentLengthRangeCondition(bytes,bytes);
            var fields=new java.util.HashMap<>(external.getPresignedPostFormData(policy));
            fields.put("key",key);fields.put("Content-Type",mime);
            return new Policy(endpoint+"/"+bucket,fields);
        } catch(Exception e) { throw unavailable(e); }
    }
    public ObjectInfo inspect(String key) {
        try {
            var info=client().statObject(StatObjectArgs.builder().bucket(bucket).object(key).build());
            return new ObjectInfo(info.size(),info.etag());
        } catch(io.minio.errors.ErrorResponseException e) {
            if("NoSuchKey".equals(e.errorResponse().code())) return null;
            throw unavailable(e);
        } catch(Exception e) { throw unavailable(e); }
    }
    public void copy(String source,String target) {
        try { client().copyObject(CopyObjectArgs.builder().bucket(bucket).object(target)
                .source(CopySource.builder().bucket(bucket).object(source).build()).build()); }
        catch(Exception e) { throw unavailable(e); }
    }
    public void compose(java.util.List<Fragment> parts,String target) {
        try { client().composeObject(ComposeObjectArgs.builder().bucket(bucket).object(target)
            .sources(parts.stream().map(part -> ComposeSource.builder().bucket(bucket).object(part.key()).matchETag(part.etag()).build()).toList()).build()); }
        catch(Exception e) { throw unavailable(e); }
    }
    public void fetch(String key,long expected,java.nio.file.Path target) {
        var info=inspect(key);
        if(info==null || info.bytes()!=expected) throw new FafaException(CommonErrorCode.INVALID_ARGUMENT,"对象大小不匹配");
        try(var input=client().getObject(GetObjectArgs.builder().bucket(bucket).object(key).build());
            var output=java.nio.file.Files.newOutputStream(target)) {
            byte[] buffer=new byte[65536];long count=0;int length;
            while((length=input.read(buffer))!=-1) {
                count+=length;
                if(count>expected) throw new FafaException(CommonErrorCode.INVALID_ARGUMENT,"对象大小不匹配");
                output.write(buffer,0,length);
            }
            if(count!=expected) throw new FafaException(CommonErrorCode.INVALID_ARGUMENT,"对象大小不匹配");
        } catch(FafaException e) { throw e; } catch(Exception e) { throw unavailable(e); }
    }
    public void putFile(String key,java.nio.file.Path file,String mime) {
        try(var input=java.nio.file.Files.newInputStream(file)) {
            client().putObject(PutObjectArgs.builder().bucket(bucket).object(key)
                .stream(input,java.nio.file.Files.size(file),-1).contentType(mime).build());
        } catch(Exception e) { throw unavailable(e); }
    }
    public byte[] read(String key,long expected) {
        try {
            var stat=client().statObject(StatObjectArgs.builder().bucket(bucket).object(key).build());
            if(stat.size()!=expected) throw new FafaException(CommonErrorCode.INVALID_ARGUMENT,"对象大小不匹配");
            try(var input=client().getObject(GetObjectArgs.builder().bucket(bucket).object(key).build())) {
                byte[] data=input.readNBytes(Math.toIntExact(expected)+1);
                if(data.length!=expected) throw new FafaException(CommonErrorCode.INVALID_ARGUMENT,"对象大小不匹配");
                return data;
            }
        } catch(FafaException e) { throw e; } catch(Exception e) { throw unavailable(e); }
    }
    public void put(String key,byte[] bytes,String mime) {
        try { client().putObject(PutObjectArgs.builder().bucket(bucket).object(key)
            .stream(new ByteArrayInputStream(bytes),bytes.length,-1).contentType(mime).build()); }
        catch(Exception e) { throw unavailable(e); }
    }
    public void delete(String key) {
        try {
            var versioning=client().getBucketVersioning(GetBucketVersioningArgs.builder().bucket(bucket).build());
            if(versioning.status()!=null) throw new FafaException(CommonErrorCode.CONFLICT,"媒体桶必须关闭且未启用过版本控制");
            client().removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build()); }
        catch(FafaException e) { throw e; } catch(Exception e) { throw unavailable(e); }
    }
    public String downloadAttachment(String key,int seconds) {
        client();
        try { return external.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().bucket(bucket).object(key)
            .method(Method.GET).expiry(seconds).extraQueryParams(java.util.Map.of(
                "response-content-disposition","attachment; filename=document.pdf","response-content-type","application/octet-stream")).build()); }
        catch(Exception e) { throw unavailable(e); }
    }
    public String download(String key,int seconds) {
        client();
        try { return external.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().bucket(bucket).object(key)
            .method(Method.GET).expiry(seconds).build()); } catch(Exception e) { throw unavailable(e); }
    }
    private FafaException unavailable(Exception exception) {
        LOG.warn("对象存储调用失败 errorType={}",exception.getClass().getSimpleName());
        return new FafaException(CommonErrorCode.DEPENDENCY_UNAVAILABLE,"对象存储暂不可用，请重试"); }
}
