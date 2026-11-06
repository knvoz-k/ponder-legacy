package net.createmod.ponder.client.projection;

import java.util.*;
import net.minecraft.command.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.*;

@SideOnly(Side.CLIENT)
public final class ProjectionCommand extends CommandBase {
    @Override public String getName(){return "ponderprojection";}
    @Override public String getUsage(ICommandSender sender){return "/ponderprojection [layout|reset|clear|unpin|here|rotate|layer <all|y>|offset <x> <y> <z>]";}
    @Override public int getRequiredPermissionLevel(){return 0;}
    @Override public void execute(MinecraftServer server,ICommandSender sender,String[] args)throws CommandException{
        ProjectionSession s=ProjectionService.current();if(s==null)throw new CommandException("ponder.projection.none");
        if(args.length==0){Minecraft.getMinecraft().displayGuiScreen(new ProjectionScreen(s,null));return;}
        switch(args[0]){
            case "layout":s.pinned=true;Minecraft.getMinecraft().displayGuiScreen(new ProjectionLayoutScreen(null));return;
            case "reset":ProjectionOverlay.INSTANCE.reset(false);ProjectionOverlay.INSTANCE.reset(true);return;
            case "clear":ProjectionService.clear();return;
            case "unpin":s.pinned=false;return;
            case "here":s.anchor=ProjectionService.target();s.refresh();return;
            case "rotate":s.turns++;s.rebuild();return;
            case "layer":if(args.length!=2)throw new WrongUsageException(getUsage(sender));s.layer=args[1].equals("all")?-1:parseInt(args[1],0,s.maxLayer());s.refresh();return;
            case "offset":if(args.length!=4)throw new WrongUsageException(getUsage(sender));s.anchor=s.anchor.add(parseInt(args[1],-1024,1024),parseInt(args[2],-256,256),parseInt(args[3],-1024,1024));s.refresh();return;
            default:throw new WrongUsageException(getUsage(sender));
        }
    }
    @Override public List<String> getTabCompletions(MinecraftServer server,ICommandSender sender,String[] args,BlockPos pos){return args.length==1?getListOfStringsMatchingLastWord(args,"layout","reset","clear","unpin","here","rotate","layer","offset"):List.of();}
}
