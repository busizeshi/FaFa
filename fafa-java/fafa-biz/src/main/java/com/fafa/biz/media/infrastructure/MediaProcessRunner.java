package com.fafa.biz.media.infrastructure;

import com.fafa.common.error.*;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.*;
import org.springframework.stereotype.Component;

/** 不经 shell 启动受控命令；限制时长和日志体积，超时终止整个子进程树。 */
@Component
public class MediaProcessRunner {
    public byte[] run(List<String> command,int seconds) {
        Process process;
        try { process=new ProcessBuilder(command).redirectErrorStream(true).start(); }
        catch(java.io.IOException e) { throw new FafaException(CommonErrorCode.DEPENDENCY_UNAVAILABLE,"媒体处理工具不可用"); }
        try(var pool=Executors.newVirtualThreadPerTaskExecutor()) {
            var output=pool.submit(()->{
                try(var input=process.getInputStream();var bytes=new ByteArrayOutputStream()) {
                    byte[] buffer=new byte[8192];int n;
                    while((n=input.read(buffer))!=-1) {
                        if(bytes.size()+n>1048576) { process.destroyForcibly();throw new IllegalStateException("处理输出超限"); }
                        bytes.write(buffer,0,n);
                    }
                    return bytes.toByteArray();
                }
            });
            try {
                if(!process.waitFor(seconds,TimeUnit.SECONDS)) {
                    stop(process);throw new FafaException(CommonErrorCode.INVALID_ARGUMENT,"文件处理超时");
                }
                byte[] bytes=output.get(5,TimeUnit.SECONDS);
                if(process.exitValue()!=0) throw new FafaException(CommonErrorCode.INVALID_ARGUMENT,"文件解析或转码失败");
                return bytes;
            } catch(InterruptedException e) {
                Thread.currentThread().interrupt();stop(process);throw new FafaException(CommonErrorCode.DEPENDENCY_UNAVAILABLE);
            } catch(ExecutionException|TimeoutException e) { stop(process);throw new FafaException(CommonErrorCode.INVALID_ARGUMENT,"文件处理输出异常"); }
            finally { stop(process); }
        }
    }
    private void stop(Process process) {
        process.descendants().forEach(child->child.destroyForcibly());
        if(process.isAlive()) process.destroyForcibly();
    }
}
