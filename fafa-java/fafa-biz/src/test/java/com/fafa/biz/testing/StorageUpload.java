package com.fafa.biz.testing;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

/** 测试用真实 multipart/form-data 上传；从接口原样复制所有签名字段。 */
final class StorageUpload {
    private StorageUpload() { }
    static void send(String url,Map<String,String> fields,byte[] file) throws Exception {
        String boundary="fafa"+java.util.UUID.randomUUID();
        var out=new java.io.ByteArrayOutputStream();
        for(var entry:fields.entrySet()) out.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\""+entry.getKey()+"\"\r\n\r\n"+entry.getValue()+"\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"test.bin\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(file);out.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));
        var result=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(url)).header("Content-Type","multipart/form-data; boundary="+boundary)
            .POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray())).build(),HttpResponse.BodyHandlers.discarding());
        assertThat(result.statusCode()).isIn(200,204);
    }
}
