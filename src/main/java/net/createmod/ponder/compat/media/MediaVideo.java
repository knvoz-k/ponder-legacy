package net.createmod.ponder.compat.media;

import java.lang.reflect.*;
import java.util.concurrent.Callable;
import net.createmod.ponder.client.guide.GuideMediaSources;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.*;

/** Isolated, optional Media API 1.x adapter. Ponder never embeds a decoder or native library. */
@SideOnly(Side.CLIENT)
public final class MediaVideo implements AutoCloseable {
    private Object player;
    private Class<?> contract;
    private String error="";
    private boolean closed;
    public MediaVideo(String source) {
        if(!Loader.isModLoaded("media_api")){error=I18n.format("ponder.guide.media_missing");return;}
        try{
            Class<?> api=Class.forName("dev.yudream.media.api.client.MediaAPI");contract=Class.forName("dev.yudream.media.api.client.VideoPlayer");
            if(source.startsWith("https://")||source.startsWith("http://")||source.matches("(?:BV[0-9A-Za-z]+|av[0-9]+)(?:[?].*)?"))player=api.getMethod("open",String.class).invoke(null,source);
            else {
                var quest=GuideMediaSources.questMedia(source);
                Constructor<?> videoSource=Class.forName("dev.yudream.media.api.client.VideoSource").getConstructor(String.class);
                Callable<Object> resolver=()->videoSource.newInstance(GuideMediaSources.resolveVideo(source,quest));
                player=api.getMethod("openAsync",Callable.class).invoke(null,resolver);
            }
        }catch(ReflectiveOperationException|RuntimeException|LinkageError failure){error=I18n.format("ponder.guide.media_failed");}
    }
    private Object call(String method,Class<?>[] types,Object... args){
        if(player==null||closed)return null;
        try{return contract.getMethod(method,types).invoke(player,args);}
        catch(ReflectiveOperationException|LinkageError failure){error=I18n.format("ponder.guide.media_failed");return null;}
    }
    private Object call(String method){return call(method,new Class<?>[0]);}
    public int texture(){Object value=call("textureId");return value instanceof Integer n?n:-1;}
    public int width(){Object v=call("width");return v instanceof Integer n?n:0;}
    public int height(){Object v=call("height");return v instanceof Integer n?n:0;}
    public long position(){Object v=call("positionMillis");return v instanceof Long n?n:0;}
    public long duration(){Object v=call("durationMillis");return v instanceof Long n?n:0;}
    public boolean seekable(){return Boolean.TRUE.equals(call("isSeekable"));}
    public boolean playing(){return Boolean.TRUE.equals(call("isPlaying"));}
    public String status(){Object value=call("status");return !error.isEmpty()?error:value==null?"":value.toString();}
    public void toggle(){if(player!=null)call("toggle");}
    public void replay(){call("replay");}
    public void seek(long time){if(seekable())call("seekTo",new Class<?>[]{long.class},time);}
    @Override public void close(){if(!closed){call("close");closed=true;player=null;}}
}
