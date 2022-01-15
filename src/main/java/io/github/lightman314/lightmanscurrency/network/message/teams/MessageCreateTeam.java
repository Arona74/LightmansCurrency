package io.github.lightman314.lightmanscurrency.network.message.teams;

import java.util.function.Supplier;

import io.github.lightman314.lightmanscurrency.common.teams.Team;
import io.github.lightman314.lightmanscurrency.common.universal_traders.TradingOffice;
import io.github.lightman314.lightmanscurrency.network.LightmansCurrencyPacketHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent.Context;

public class MessageCreateTeam {
	
	String teamName;
	
	public MessageCreateTeam(String teamName)
	{
		this.teamName = teamName;
	}
	
	public static void encode(MessageCreateTeam message, PacketBuffer buffer) {
		buffer.writeString(message.teamName, Team.MAX_NAME_LENGTH);
	}

	public static MessageCreateTeam decode(PacketBuffer buffer) {
		return new MessageCreateTeam(buffer.readString(Team.MAX_NAME_LENGTH));
	}

	public static void handle(MessageCreateTeam message, Supplier<Context> supplier) {
		supplier.get().enqueueWork(() -> {
			Team newTeam = TradingOffice.registerTeam(supplier.get().getSender(), message.teamName);
			if(newTeam != null)
			{
				LightmansCurrencyPacketHandler.instance.send(LightmansCurrencyPacketHandler.getTarget(supplier.get().getSender()), new MessageCreateTeamResponse(newTeam.getID()));
			}
		});
		supplier.get().setPacketHandled(true);
	}

}
