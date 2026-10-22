package com.fafa.biz.media.application;

import com.fafa.biz.media.infrastructure.MediaProcessRunner;
import com.fafa.biz.media.domain.MediaModels.Session;
import com.fafa.common.error.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** 处理本地受控临时文件；不允许远程 URL、任意协议、用户命令或模板。 */
@Component
public class FileProcessor {
    public record Processed(Path preview,Path playback,int width,int height,Long durationMillis,Integer pageCount) { }
    private final ProcessingPolicy policy;private final MediaPolicy images;private final MediaProcessRunner runner;private final ObjectMapper json;
    public FileProcessor(ProcessingPolicy policy,MediaPolicy images,MediaProcessRunner runner,ObjectMapper json) {
        this.policy=policy;this.images=images;this.runner=runner;this.json=json;
    }
    public Processed process(Session s,Path input,Path dir) throws Exception {
        var digest=MessageDigest.getInstance("SHA-256");
        try(var stream=new DigestInputStream(Files.newInputStream(input),digest)) { stream.transferTo(java.io.OutputStream.nullOutputStream()); }
        if(!HexFormat.of().formatHex(digest.digest()).equals(s.sha256())) throw new FafaException(CommonErrorCode.INVALID_ARGUMENT,"文件摘要不匹配");
        return "application/pdf".equals(s.mime())?pdf(input,dir):video(input,dir);
    }
    private Processed pdf(Path input,Path dir) throws Exception {
        Path preview=dir.resolve("preview.png"),info=dir.resolve("pages.txt");
        String java=Path.of(System.getProperty("java.home"),"bin",isWindows()?"java.exe":"java").toString();
        var command=new ArrayList<>(List.of(java,"-Xmx256m","-Djava.awt.headless=true","-Dfafa.media.pdf-child=true"));
        var home=new org.springframework.boot.system.ApplicationHome(com.fafa.biz.BizApplication.class).getSource();
        if(home!=null&&home.isFile()&&home.getName().endsWith(".jar")) command.addAll(List.of("-jar",home.toString()));
        else command.addAll(List.of("-cp",System.getProperty("surefire.test.class.path",System.getProperty("java.class.path")),
            "com.fafa.biz.media.infrastructure.PdfWorker"));
        command.addAll(List.of(input.toString(),preview.toString(),info.toString(),Integer.toString(policy.maxPdfPages()),Long.toString(images.maxPixels())));
        runner.run(command,policy.processSeconds());
        return new Processed(preview,null,0,0,null,Integer.parseInt(Files.readString(info).strip()));
    }
    private Processed video(Path input,Path dir) throws Exception {
        var info=json.readTree(runner.run(List.of(policy.ffprobe(),"-v","error","-protocol_whitelist","file,pipe","-show_entries",
            "format=duration,format_name:stream=codec_type,width,height","-of","json",input.toString()),15));
        double duration=info.path("format").path("duration").asDouble(-1);
        String format=info.path("format").path("format_name").asText();
        int w=0,h=0;
        for(var stream:info.path("streams")) if("video".equals(stream.path("codec_type").asText())) {
            w=stream.path("width").asInt();h=stream.path("height").asInt();break;
        }
        if(!Double.isFinite(duration)||duration<=0||duration>policy.maxVideoSeconds()||!format.contains("mp4")||
           w<=0||h<=0||(long)w*h>images.maxPixels()) throw new FafaException(CommonErrorCode.INVALID_ARGUMENT,"视频格式、时长或尺寸超限");
        Path playback=dir.resolve("playback.mp4"),preview=dir.resolve("preview.png");
        runner.run(List.of(policy.ffmpeg(),"-nostdin","-v","error","-protocol_whitelist","file,pipe","-i",input.toString(),
            "-map","0:v:0","-map","0:a:0?","-map_metadata","-1","-map_chapters","-1",
            "-vf","scale=1280:720:force_original_aspect_ratio=decrease:force_divisible_by=2",
            "-c:v","libx264","-preset","fast","-pix_fmt","yuv420p","-b:v","1500k","-maxrate","1800k","-bufsize","3600k",
            "-threads","1","-c:a","aac","-b:a","96k","-t",Integer.toString(policy.maxVideoSeconds()),
            "-fs",Long.toString(policy.maxPlaybackBytes()),"-movflags","+faststart",playback.toString()),policy.processSeconds());
        if(Files.size(playback)>=policy.maxPlaybackBytes()) throw new FafaException(CommonErrorCode.INVALID_ARGUMENT,"转码成品超出预占上限");
        runner.run(List.of(policy.ffmpeg(),"-nostdin","-v","error","-protocol_whitelist","file,pipe","-i",playback.toString(),
            "-map_metadata","-1","-frames:v","1","-vf","scale=1024:-1","-threads","1",preview.toString()),30);
        return new Processed(preview,playback,w,h,(long)(duration*1000),null);
    }
    private boolean isWindows() { return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win"); }
}
