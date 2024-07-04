package io.github.lightman314.lightmanscurrency.network.message.bank;

import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.common.items.PortableATMItem;
import io.github.lightman314.lightmanscurrency.common.menus.validation.EasyMenu;
import io.github.lightman314.lightmanscurrency.network.packet.ClientToServerPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;

public class CPacketOpenATM extends ClientToServerPacket {

	private static final Type<CPacketOpenATM> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LightmansCurrency.MODID,"c_open_atm"));
	private static final CPacketOpenATM INSTANCE = new CPacketOpenATM();
	public static Handler<CPacketOpenATM> HANDLER = new H();

	public static void sendToServer() { INSTANCE.send(); }

	private CPacketOpenATM() { super(TYPE); }

	private static class H extends SimpleHandler<CPacketOpenATM>
	{
		protected H() { super(TYPE, INSTANCE); }
		@Override
		public void handle(@Nonnull CPacketOpenATM message, @Nonnull IPayloadContext context, @Nonnull Player player) {
			player.openMenu(PortableATMItem.getMenuProvider(), EasyMenu.nullEncoder());
		}
	}

}
