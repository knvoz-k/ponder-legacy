package net.createmod.ponder.editor;

import java.util.Map;
import java.util.WeakHashMap;
import io.netty.buffer.ByteBuf;
import net.createmod.ponder.api.editor.CuboidSelection;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class EditorNetwork {
    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("ponder_editor");
    private EditorNetwork() {}
    public static void register() {
        CHANNEL.registerMessage(CaptureHandler.class, CaptureRequest.class, 0, Side.SERVER);
        CHANNEL.registerMessage(ResultHandler.class, CaptureResult.class, 1, Side.CLIENT);
    }

    public static final class CaptureRequest implements IMessage {
        public long request;
        public BlockPos from;
        public BlockPos to;
        public CaptureRequest() {}
        public CaptureRequest(long request, CuboidSelection selection) {
            this.request = request; from = selection.min(); to = selection.max();
        }
        @Override public void fromBytes(ByteBuf buf) {
            request = buf.readLong(); from = BlockPos.fromLong(buf.readLong()); to = BlockPos.fromLong(buf.readLong());
        }
        @Override public void toBytes(ByteBuf buf) { buf.writeLong(request); buf.writeLong(from.toLong()); buf.writeLong(to.toLong()); }
    }

    public static final class CaptureResult implements IMessage {
        public long request;
        public String error = "";
        public byte[] bytes = new byte[0];
        public CaptureResult() {}
        CaptureResult(long request, byte[] bytes, String error) { this.request = request; this.bytes = bytes; this.error = error; }
        @Override public void fromBytes(ByteBuf buf) {
            request = buf.readLong(); error = ByteBufUtils.readUTF8String(buf);
            int size = buf.readInt();
            if (size < 0 || size > StructureCapture.MAX_COMPRESSED_BYTES || size > buf.readableBytes())
                throw new IllegalArgumentException("Invalid Ponder structure response length");
            bytes = new byte[size]; buf.readBytes(bytes);
        }
        @Override public void toBytes(ByteBuf buf) {
            buf.writeLong(request); ByteBufUtils.writeUTF8String(buf, error); buf.writeInt(bytes.length); buf.writeBytes(bytes);
        }
    }

    public static final class CaptureHandler implements IMessageHandler<CaptureRequest, IMessage> {
        private static final Map<EntityPlayerMP, Long> LAST = new WeakHashMap<>();
        @Override public IMessage onMessage(CaptureRequest message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                CaptureResult response;
                try {
                    if (!player.capabilities.isCreativeMode && !player.canUseCommand(2, "pondereditor"))
                        throw new IllegalArgumentException("Creative mode or operator permission is required to capture structures");
                    long now = System.currentTimeMillis();
                    if (now - LAST.getOrDefault(player, 0L) < 1000) throw new IllegalArgumentException("Wait a second before capturing again");
                    LAST.put(player, now);
                    CuboidSelection selection = new CuboidSelection(message.from, message.to);
                    selection.checkCaptureSize();
                    if (player.getDistanceSq(selection.min()) > 128 * 128 || player.getDistanceSq(selection.max()) > 128 * 128)
                        throw new IllegalArgumentException("Stay within 128 blocks of both selection corners");
                    response = new CaptureResult(message.request, StructureCapture.capture(player.world, selection), "");
                } catch (Exception failure) {
                    response = new CaptureResult(message.request, new byte[0],
                        failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage());
                }
                CHANNEL.sendTo(response, player);
            });
            return null;
        }
    }

    public static final class ResultHandler implements IMessageHandler<CaptureResult, IMessage> {
        @Override public IMessage onMessage(CaptureResult message, MessageContext context) {
            net.createmod.ponder.PonderMod.proxy.receiveEditorCapture(message.request, message.bytes, message.error);
            return null;
        }
    }
}
