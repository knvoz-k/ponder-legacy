package net.createmod.ponder.client.guide;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.*;
import net.createmod.ponder.compat.ftbquests.FTBQuestBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.*;

/** Bounded asynchronous bitmap loading; optional FTBQ supplies its GIF/APNG and shared-media handling. */
@SideOnly(Side.CLIENT)
public final class GuideImages implements AutoCloseable {
    private static final ExecutorService IO=Executors.newFixedThreadPool(2,r->{Thread t=new Thread(r,"Ponder guide images");t.setDaemon(true);return t;});
    private final Map<String,Entry> entries=new LinkedHashMap<>(16,.75f,true);
    private static final class Entry {
        CompletableFuture<BufferedImage> pending;
        ResourceLocation texture;
        int width,height;
        String error="";
    }
    private boolean ftb(String source){return !source.startsWith("local:")&&FTBQuestBridge.available();}
    public int[] dimensions(String source){
        if(ftb(source))try{return FTBQuestBridge.imageSize(source);}catch(ReflectiveOperationException|LinkageError ignored){}
        Entry entry=entries.get(source);return entry==null?new int[]{0,0}:new int[]{entry.width,entry.height};
    }
    public String draw(String source,int x,int y,int w,int h){
        if(ftb(source))try{FTBQuestBridge.drawImage(source,x,y,w,h);return "";}catch(ReflectiveOperationException|LinkageError ignored){}
        if(source.startsWith("media:")||source.startsWith("asset:"))return net.minecraft.client.resources.I18n.format("ponder.guide.quest_media_missing");
        Entry entry=entries.computeIfAbsent(source,key->{Entry e=new Entry();e.pending=CompletableFuture.supplyAsync(()->{
            try(var stream=GuideMediaSources.openImage(key)){
                byte[] bytes=stream.readNBytes(8*1024*1024+1);if(bytes.length>8*1024*1024)throw new IOException("Image exceeds 8 MiB");
                try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){
                    var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new IOException("Unsupported image");
                    ImageReader reader=readers.next();try{reader.setInput(input);int iw=reader.getWidth(0),ih=reader.getHeight(0);if(iw<1||ih<1||(long)iw*ih>4_194_304)throw new IOException("Image exceeds 4 megapixels");return reader.read(0);}finally{reader.dispose();}
                }
            }catch(IOException|RuntimeException failure){throw new CompletionException(failure);}
        },IO);return e;});
        if(entry.texture==null&&entry.error.isEmpty()&&entry.pending.isDone())try{
            BufferedImage image=entry.pending.join();entry.width=image.getWidth();entry.height=image.getHeight();
            entry.texture=Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("ponder_guide",new DynamicTexture(image));
            entry.pending=null;
        }catch(CompletionException failure){entry.error=net.minecraft.client.resources.I18n.format("ponder.guide.image_failed");entry.pending=null;}
        if(entry.texture!=null){
            float scale=Math.min(w/(float)entry.width,h/(float)entry.height);int iw=Math.max(1,(int)(entry.width*scale)),ih=Math.max(1,(int)(entry.height*scale));
            Minecraft.getMinecraft().getTextureManager().bindTexture(entry.texture);GlStateManager.color(1,1,1,1);Gui.drawModalRectWithCustomSizedTexture(x+(w-iw)/2,y+(h-ih)/2,0,0,iw,ih,iw,ih);
        }
        while(entries.size()>8){var iterator=entries.entrySet().iterator();var oldest=iterator.next();release(oldest.getValue());iterator.remove();}
        return entry.error.isEmpty()&&entry.texture==null?net.minecraft.client.resources.I18n.format("ponder.guide.media_loading"):entry.error;
    }
    private void release(Entry e){if(e.pending!=null)e.pending.cancel(true);if(e.texture!=null)Minecraft.getMinecraft().getTextureManager().deleteTexture(e.texture);}
    @Override public void close(){entries.values().forEach(this::release);entries.clear();}
}
