package com.fafa.biz.media.application;

import com.fafa.common.error.*;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

/** 先限制像素再解码；重新编码预览，避免向访问者传播原始 EXIF。 */
@Component
public class ImageVerifier {
    public record Verified(int width,int height,byte[] preview) { }
    private final MediaPolicy policy;
    public ImageVerifier(MediaPolicy policy) { this.policy=policy; }
    public Verified verify(byte[] data,String mime,String checksum) {
        try {
            if(!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)).equals(checksum)) invalid();
            try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
                var readers=ImageIO.getImageReaders(input);
                if(!readers.hasNext()) { invalid(); }
                var reader=readers.next();
                try {
                    reader.setInput(input,true,true);
                    String format=reader.getFormatName();
                    if(!((mime.equals("image/png") && format.equalsIgnoreCase("png")) ||
                         (mime.equals("image/jpeg") && format.equalsIgnoreCase("JPEG")))) invalid();
                    int w=reader.getWidth(0),h=reader.getHeight(0);
                    if(w<=0 || h<=0 || (long)w*h>policy.maxPixels()) invalid();
                    var original=reader.read(0);
                    double scale=Math.min(1d,(double)policy.previewEdge()/Math.max(w,h));
                    var preview=new BufferedImage(Math.max(1,(int)(w*scale)),Math.max(1,(int)(h*scale)),BufferedImage.TYPE_INT_RGB);
                    var graphics=preview.createGraphics();
                    try {
                        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        graphics.drawImage(original,0,0,preview.getWidth(),preview.getHeight(),null);
                    } finally { graphics.dispose(); original.flush(); }
                    var output=new ByteArrayOutputStream();
                    ImageIO.write(preview,"png",output); preview.flush();
                    return new Verified(w,h,output.toByteArray());
                } finally { reader.dispose(); }
            }
        } catch(FafaException e) { throw e; }
        catch(Exception e) { throw new FafaException(CommonErrorCode.INVALID_ARGUMENT,"图片校验失败"); }
    }
    private void invalid() { throw new FafaException(CommonErrorCode.INVALID_ARGUMENT,"图片格式、像素或摘要不匹配"); }
}
