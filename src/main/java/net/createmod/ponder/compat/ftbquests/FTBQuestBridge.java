package net.createmod.ponder.compat.ftbquests;

import java.lang.reflect.*;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.*;

/** Optional client bridge. No FTB class appears in a signature or a class literal. */
@SideOnly(Side.CLIENT)
public final class FTBQuestBridge {
    private FTBQuestBridge() {}
    public static boolean available(){return Loader.isModLoaded("ftbquests");}
    public static boolean openQuest(String code)throws ReflectiveOperationException {
        if(!available())return false;
        Class<?> fileClass=Class.forName("com.feed_the_beast.ftbquests.client.ClientQuestFile");
        Object file=fileClass.getField("INSTANCE").get(null);if(file==null)return false;
        Object tree=fileClass.getField("questTreeGui").get(file);if(tree==null)return false;
        return (Boolean)tree.getClass().getMethod("handleClick",String.class,String.class).invoke(tree,"quest",code);
    }
    @SuppressWarnings("unchecked")
    public static CompletableFuture<Path> resolveMedia(String name)throws ReflectiveOperationException {
        if(!available())throw new IllegalStateException("FTB Quests is needed for media: sources");
        return (CompletableFuture<Path>)Class.forName("com.feed_the_beast.ftbquests.client.ClientMediaAssets")
            .getMethod("resolve",String.class).invoke(null,name);
    }
    public static int[] imageSize(String source)throws ReflectiveOperationException {
        Object dimensions=Class.forName("com.feed_the_beast.ftbquests.client.CanvasMedia").getMethod("dimensions",String.class).invoke(null,source);
        return new int[]{(Integer)dimensions.getClass().getMethod("width").invoke(dimensions),(Integer)dimensions.getClass().getMethod("height").invoke(dimensions)};
    }
    public static void drawImage(String source,int x,int y,int w,int h)throws ReflectiveOperationException {
        Class.forName("com.feed_the_beast.ftbquests.client.CanvasMedia").getMethod("drawFit",String.class,int.class,int.class,int.class,int.class,int.class)
            .invoke(null,source,x,y,w,h,255);
    }
}
