package net.createmod.ponder.client.guide;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.*;
import net.createmod.ponder.compat.ftbquests.FTBQuestBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.*;

@SideOnly(Side.CLIENT)
public final class GuideMediaSources {
    private GuideMediaSources(){}
    public static Path root(){return Minecraft.getMinecraft().gameDir.toPath().resolve("scripts/ponder/media").toAbsolutePath().normalize();}
    public static Path local(String name)throws IOException {
        Path root=root(),file=root.resolve(name).normalize();
        if(!file.startsWith(root)||name.contains(".."))throw new IOException("Media path escaped scripts/ponder/media");
        for(Path p=file;p!=null&&p.startsWith(root);p=p.getParent())if(Files.isSymbolicLink(p))throw new IOException("Media symlinks are unsupported");
        if(!Files.isRegularFile(file))throw new FileNotFoundException(name);return file;
    }
    public static InputStream openImage(String source)throws IOException {
        if(source.startsWith("local:"))return Files.newInputStream(local(source.substring(6)));
        if(source.startsWith("http://")||source.startsWith("https://")) {
            URLConnection connection=URI.create(source).toURL().openConnection();connection.setConnectTimeout(8000);connection.setReadTimeout(8000);return connection.getInputStream();
        }
        return Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation(source)).getInputStream();
    }
    /** Called on the client thread so optional quest media can request server assets safely. */
    public static CompletableFuture<Path> questMedia(String source)throws ReflectiveOperationException {
        return source.startsWith("media:")?FTBQuestBridge.resolveMedia(source.substring(6)):null;
    }
    public static String resolveVideo(String source,CompletableFuture<Path> quest)throws Exception {
        if(quest!=null)return quest.get(45,TimeUnit.SECONDS).toAbsolutePath().toUri().toString();
        if(source.startsWith("local:"))return local(source.substring(6)).toUri().toString();
        Path cache=Minecraft.getMinecraft().gameDir.toPath().resolve("config/ponder/.media-cache");Files.createDirectories(cache);
        String extension=source.substring(source.lastIndexOf('.')+1);if(!extension.matches("[a-zA-Z0-9]{1,8}"))extension="mp4";
        String hash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(source.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        Path file=cache.resolve(hash+"."+extension),temp=Files.createTempFile(cache,"video-",".tmp");
        try(var in=Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation(source)).getInputStream();var out=Files.newOutputStream(temp)){
            byte[] data=new byte[65536];int read;long total=0;
            while((read=in.read(data))>=0){if(Thread.currentThread().isInterrupted())throw new IOException("Video loading cancelled");total+=read;if(total>256L*1024*1024)throw new IOException("Video resource exceeds 256 MiB");out.write(data,0,read);}
        }catch(Exception error){Files.deleteIfExists(temp);throw error;}
        Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);return file.toAbsolutePath().toUri().toString();
    }
}
