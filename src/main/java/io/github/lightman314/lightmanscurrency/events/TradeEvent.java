package io.github.lightman314.lightmanscurrency.events;

import io.github.lightman314.lightmanscurrency.common.universal_traders.TradingOffice;
import io.github.lightman314.lightmanscurrency.containers.UniversalContainer;
import io.github.lightman314.lightmanscurrency.tradedata.TradeData;
import io.github.lightman314.lightmanscurrency.util.MathUtil;
import io.github.lightman314.lightmanscurrency.util.MoneyUtil.CoinValue;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.world.World;
import net.minecraftforge.eventbus.api.Event;

public abstract class TradeEvent extends Event{

	private final PlayerEntity player;
	public final PlayerEntity getPlayer() { return this.player; }
	private final TradeData trade;
	public final TradeData getTrade() { return this.trade; }
	private final Container container;
	public final Container getContainer() { return this.container; }
	private final World world;
	public final World getWorld() { return this.world; }
	
	protected TradeEvent(PlayerEntity player, TradeData trade, Container container)
	{
		this.player = player;
		this.trade = trade;
		this.container = container;
		if(container instanceof UniversalContainer)
			this.world = TradingOffice.getWorld();
		else
			this.world = player.world;
	}
	
	public static class PreTradeEvent extends TradeEvent
	{
		public PreTradeEvent(PlayerEntity player, TradeData trade, Container container)
		{
			super(player, trade, container);
		}
		
		@Override
		public boolean isCancelable() { return true; }
		
	}
	
	public static class TradeCostEvent extends TradeEvent
	{
		
		private double costMultiplier;
		public double getCostMultiplier() { return this.costMultiplier; }
		public void setCostMultiplier(double newCostMultiplier) { this.costMultiplier = MathUtil.clamp(newCostMultiplier, 0d, 1d); }
		
		CoinValue currentCost;
		public CoinValue getCost() { return this.currentCost; }
		public CoinValue multipliedCost() { return this.currentCost.ApplyMultiplier(this.costMultiplier); }
		
		public TradeCostEvent(PlayerEntity player, TradeData trade, Container container)
		{
			super(player, trade, container);
			this.costMultiplier = 1f;
			this.currentCost = trade.getCost();
		}
	}
	
	public static class PostTradeEvent extends TradeEvent
	{
		
		private boolean isDirty = false;
		
		public PostTradeEvent(PlayerEntity player, TradeData trade, Container container)
		{
			super(player, trade, container);
		}
		
		public boolean isDirty()
		{
			return this.isDirty;
		}
		
		public void markDirty()
		{
			this.isDirty = true;
		}
		
		public void clean()
		{
			this.isDirty = false;
		}
		
	}
	
}
