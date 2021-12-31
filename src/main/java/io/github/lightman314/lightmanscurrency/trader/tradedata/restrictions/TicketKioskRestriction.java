package io.github.lightman314.lightmanscurrency.trader.tradedata.restrictions;

import com.mojang.datafixers.util.Pair;

import io.github.lightman314.lightmanscurrency.containers.slots.TicketSlot;
import io.github.lightman314.lightmanscurrency.core.ModItems;
import io.github.lightman314.lightmanscurrency.items.TicketItem;
import io.github.lightman314.lightmanscurrency.trader.tradedata.ItemTradeData;
import io.github.lightman314.lightmanscurrency.util.InventoryUtil;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class TicketKioskRestriction extends ItemTradeRestriction{

	public TicketKioskRestriction() {} 
	
	public TicketKioskRestriction(String classicType)
	{
		super(classicType);
	}
	
	@Override
	public ItemStack modifySellItem(ItemStack sellItem, ItemTradeData trade)
	{
		if(sellItem.getItem() instanceof TicketItem && trade.hasCustomName())
			sellItem.setDisplayName(new StringTextComponent(trade.getCustomName()));
		return sellItem;
	}
	
	@Override
	public boolean allowSellItem(ItemStack itemStack)
	{
		if(TicketItem.isMasterTicket(itemStack))
			return true;
		return itemStack.getItem().getTags().contains(TicketItem.TICKET_MATERIAL_TAG) && itemStack.getItem() != ModItems.TICKET;
	}
	
	@Override
	public ItemStack filterSellItem(ItemStack itemStack)
	{
		if(TicketItem.isMasterTicket(itemStack))
			return TicketItem.CreateTicket(TicketItem.GetTicketID(itemStack), 1);
		else if(itemStack.getItem().getTags().contains(TicketItem.TICKET_MATERIAL_TAG) && itemStack.getItem() != ModItems.TICKET)
			return itemStack;
		else
			return ItemStack.EMPTY;
	}
	
	@Override
	public boolean allowItemSelectItem(ItemStack itemStack)
	{
		Item item = itemStack.getItem();
		return item.getTags().contains(TicketItem.TICKET_MATERIAL_TAG) && item != ModItems.TICKET && item != ModItems.TICKET_MASTER;
	}
	
	@Override
	public int getSaleStock(ItemStack sellItem, IInventory traderStorage)
	{
		if(sellItem.getItem() == ModItems.TICKET)
			return InventoryUtil.GetItemTagCount(traderStorage, TicketItem.TICKET_MATERIAL_TAG, ModItems.TICKET_MASTER) / sellItem.getCount();
		return super.getSaleStock(sellItem, traderStorage);
	}
	
	@Override
	public void removeItemsFromStorage(ItemStack sellItem, IInventory traderStorage)
	{
		if(sellItem.getItem() == ModItems.TICKET)
		{
			if(!InventoryUtil.RemoveItemCount(traderStorage, sellItem))
			{
				InventoryUtil.RemoveItemTagCount(traderStorage, TicketItem.TICKET_MATERIAL_TAG, sellItem.getCount(), ModItems.TICKET_MASTER);
			}
		}
		else
			super.removeItemsFromStorage(sellItem, traderStorage);
	}
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public Pair<ResourceLocation,ResourceLocation> getEmptySlotBG()
	{
		return Pair.of(PlayerContainer.LOCATION_BLOCKS_TEXTURE, TicketSlot.EMPTY_TICKET_SLOT);
	}
	
}
