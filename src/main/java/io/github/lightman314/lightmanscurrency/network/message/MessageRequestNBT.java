package io.github.lightman314.lightmanscurrency.network.message;

import java.util.function.Supplier;

import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.util.TileEntityUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fmllegacy.network.NetworkEvent.Context;

public class MessageRequestNBT {

	private BlockPos pos;
	
	public MessageRequestNBT(BlockEntity tileEntity)
	{
		this.pos = tileEntity.getBlockPos();
	}
	
	public MessageRequestNBT(BlockPos pos)
	{
		this.pos = pos;
	}
	
	public static void encode(MessageRequestNBT message, FriendlyByteBuf buffer) {
		buffer.writeBlockPos(message.pos);
	}

	public static MessageRequestNBT decode(FriendlyByteBuf buffer) {
		return new MessageRequestNBT(buffer.readBlockPos());
	}

	public static void handle(MessageRequestNBT message, Supplier<Context> supplier) {
		supplier.get().enqueueWork(() ->
		{
			LightmansCurrency.LogInfo("Data request received.");
			ServerPlayer player = supplier.get().getSender();
			if(player != null)
			{
				BlockEntity blockEntity = player.level.getBlockEntity(message.pos);
				if(blockEntity != null)
				{
					TileEntityUtil.sendUpdatePacket(blockEntity);
				}
			}
		});
		supplier.get().setPacketHandled(true);
	}

}
