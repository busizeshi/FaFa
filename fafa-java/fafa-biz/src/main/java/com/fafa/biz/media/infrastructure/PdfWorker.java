package com.fafa.biz.media.infrastructure;

import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.rendering.PDFRenderer;

/** 在独立、受限堆内存的 JVM 中读取 PDF；不启动 Spring，不执行脚本或外部链接。 */
public final class PdfWorker {
    private static final Set<String> BLOCKED=Set.of("JS","JavaScript","AA","OpenAction","EmbeddedFiles","Launch","RichMedia","XFA","AcroForm");
    private PdfWorker() { }
    public static void main(String[] args) {
        try { render(args); }
        catch(Exception e) { System.exit(2); }
    }
    private static void render(String[] args) throws Exception {
        try(var doc=Loader.loadPDF(Path.of(args[0]).toFile())) {
            int pages=doc.getNumberOfPages();
            if(doc.isEncrypted()||pages<1||pages>Integer.parseInt(args[3])) throw new IllegalArgumentException();
            var visited=Collections.newSetFromMap(new IdentityHashMap<COSBase,Boolean>());
            inspect(doc.getDocument().getTrailer(),visited);
            var box=doc.getPage(0).getCropBox();
            double width=box.getWidth(),height=box.getHeight();
            if(width<=0||height<=0||width*height>Long.parseLong(args[4])) throw new IllegalArgumentException();
            float scale=(float)Math.min(1d,1024d/Math.max(width,height));
            var renderer=new PDFRenderer(doc);renderer.setSubsamplingAllowed(true);
            var image=renderer.renderImage(0,scale);
            ImageIO.write(image,"png",Path.of(args[1]).toFile());image.flush();
            Files.writeString(Path.of(args[2]),Integer.toString(pages));
        }
    }
    private static void inspect(COSBase value,Set<COSBase> visited) {
        if(value==null||!visited.add(value)) return;
        if(visited.size()>100000) throw new IllegalArgumentException();
        if(value instanceof COSObject object) inspect(object.getObject(),visited);
        else if(value instanceof COSDictionary dict) {
            for(var key:dict.keySet()) {
                if(BLOCKED.contains(key.getName())) throw new IllegalArgumentException();
                inspect(dict.getDictionaryObject(key),visited);
            }
            var action=dict.getNameAsString(COSName.S);
            if("Launch".equals(action)||"JavaScript".equals(action)) throw new IllegalArgumentException();
        } else if(value instanceof COSArray array) array.forEach(item->inspect(item,visited));
    }
}
