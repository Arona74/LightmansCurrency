package io.github.lightman314.lightmanscurrency.network.message.wallet;

import io.github.lightman314.lightmanscurrency.common.menus.wallet.WalletMenu;
import io.github.lightman314.lightmanscurrency.network.packet.ClientToServerPacket;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class CPacketWalletQuickCollect extends ClientToServerPacket.Simple {

	private static final CPacketWalletQuickCollect INSTANCE = new CPacketWalletQuickCollect();
	public static final Handler<CPacketWalletQuickCollect> HANDLER = new H();

	private CPacketWalletQuickCollect() {}

	public static void sendToServer() { INSTANCE.send(); }

	private static class H extends SimpleHandler<CPacketWalletQuickCollect>
	{
		protected H() { super(INSTANCE); }
		@Override
		protected void handle(@Nonnull CPacketWalletQuickCollect message, @Nullable ServerPlayer sender) {
			if(sender != null)
			{
				if(sender.containerMenu instanceof WalletMenu menu)
					menu.QuickCollectCoins();
			}
		}
	}

}
