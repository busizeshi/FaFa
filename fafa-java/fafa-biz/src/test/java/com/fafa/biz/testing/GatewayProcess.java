package com.fafa.biz.testing;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

/** 只管理本测试创建的 Gateway 子进程，使用独立端口和明确下游地址。 */
final class GatewayProcess implements AutoCloseable {
    private final Process process;final String base;
    GatewayProcess(int bizPort) throws Exception {
        int port;
        try(var socket=new java.net.ServerSocket(0)) { port=socket.getLocalPort(); }
        base="http://127.0.0.1:"+port+"/api/v1";
        Path jar=Path.of("..","fafa-gateway","target","fafa-gateway-0.1.0-SNAPSHOT.jar").toAbsolutePath();
        if(!Files.exists(jar)) throw new IllegalStateException("先构建 Gateway jar");
        String javaBinary=Path.of(System.getProperty("java.home"),"bin","java").toString();
        Path config=Path.of("..","fafa-gateway","src","test","resources","gateway-test.yml").toAbsolutePath();
        process=new ProcessBuilder(javaBinary,"-jar",jar.toString(),"--spring.config.location=file:"+config,"--server.port="+port,
            "--fafa.gateway.biz-uri=http://127.0.0.1:"+bizPort,"--fafa.gateway.legacy-routes-enabled=false")
            .redirectErrorStream(true).redirectOutput(Path.of("target","wp04-gateway.log").toFile()).start();
        try {
            var client=HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(2)).build();
            long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(60);
            while(System.nanoTime()<until) {
                if(!process.isAlive()) throw new IllegalStateException("测试 Gateway 提前退出");
                try {
                    var response=client.send(HttpRequest.newBuilder(URI.create(base+"/bootstrap")).timeout(java.time.Duration.ofSeconds(2)).GET().build(),HttpResponse.BodyHandlers.discarding());
                    if(response.statusCode()==200) return;
                } catch(java.io.IOException ignored) { }
                Thread.sleep(250);
            }
            throw new IllegalStateException("测试 Gateway 启动超时");
        } catch(Exception e) { close();throw e; }
    }
    public void close() {
        process.destroy();
        try { if(!process.waitFor(5,TimeUnit.SECONDS)) process.destroyForcibly(); }
        catch(InterruptedException e) { Thread.currentThread().interrupt();process.destroyForcibly(); }
    }
}
