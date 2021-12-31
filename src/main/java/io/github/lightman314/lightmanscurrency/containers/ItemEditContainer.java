package io.github.lightman314.lightmanscurrency.containers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import io.github.lightman314.lightmanscurrency.containers.slots.DisplaySlot;
import io.github.lightman314.lightmanscurrency.core.ModContainers;
import io.github.lightman314.lightmanscurrency.network.LightmansCurrencyPacketHandler;
import io.github.lightman314.lightmanscurrency.network.message.item_trader.MessageItemEditClose;
import io.github.lightman314.lightmanscurrency.network.message.item_trader.MessageItemEditSet;
import io.github.lightman314.lightmanscurrency.trader.IItemTrader;
import io.github.lightman314.lightmanscurrency.trader.tradedata.ItemTradeData;
import io.github.lightman314.lightmanscurrency.trader.tradedata.restrictions.ItemTradeRestriction;
import io.github.lightman314.lightmanscurrency.util.InventoryUtil;
import io.github.lightman314.lightmanscurrency.util.MathUtil;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.NonNullList;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

public class ItemEditContainer extends Container{
	
	
	public static List<ItemGroup> ITEM_GROUP_BLACKLIST = ImmutableList.of(ItemGroup.HOTBAR, ItemGroup.INVENTORY, ItemGroup.SEARCH);
	public static final int columnCount = 9;
	public static final int rowCount = 6;
	
	private static List<ItemStack> allItems = null;
	
	public final PlayerEntity player;
	public final Supplier<IItemTrader> traderSource;
	public final int tradeIndex;
	public final ItemTradeData tradeData;
	
	List<ItemStack> filteredResultItems;
	List<ItemStack> searchResultItems;
	//IInventory tradeDisplay;
	IInventory displayInventory;
	
	private String searchString;
	private int stackCount = 1;
	public int getStackCount() { return this.stackCount; }
	private int page = 0;
	public int getPage() { return this.page; }
	private int editSlot = 0;
	public int getEditSlot() { return this.editSlot; }
	
	final List<Slot> tradeSlots;
	
	protected boolean isClient() { return this.player.world.isRemote; }
	
	public ItemEditContainer(int windowId, PlayerInventory inventory, Supplier<IItemTrader> traderSource, int tradeIndex)
	{
		this(ModContainers.ITEM_EDIT, windowId, inventory, traderSource, tradeIndex, traderSource.get().getTrade(tradeIndex));
	}
	
	protected ItemEditContainer(ContainerType<?> type, int windowId, PlayerInventory inventory, Supplier<IItemTrader> traderSource, int tradeIndex, ItemTradeData tradeData)
	{
		super(type, windowId);
		
		this.player = inventory.player;
		this.tradeData = tradeData;
		this.tradeIndex = tradeIndex;
		this.traderSource = traderSource;
		this.tradeSlots = new ArrayList<>();
		
		//this.tradeDisplay = new Inventory(1);
		//this.tradeDisplay.setInventorySlotContents(0, tradeData.getSellItem());
		this.displayInventory = new Inventory(columnCount * rowCount);
		
		//Trade slot
		//this.addSlot(new DisplaySlot(this.tradeDisplay, 0, ItemTradeButton.SLOT_OFFSET1_X, ItemTradeButton.SLOT_OFFSET_Y - ItemTradeButton.HEIGHT));
		
		if(!this.isClient())
			return;
		
		//Display Slots
		for(int y = 0; y < rowCount; y++)
		{
			for(int x = 0; x < columnCount; x++)
			{
				this.addSlot(new Slot(this.displayInventory, x + y * columnCount, 8 + x * 18, 18 + y * 18));
			}
		}
		
		//Load the item list from the item groups
		initItemList();
		this.filteredResultItems = this.getFilteredItems();
		
		//Set the search to the default value to initialize the inventory
		this.modifySearch("");
		
	}
	
	@Override
	public ItemStack slotClick(int slotId, int dragType, ClickType clickType, PlayerEntity player)
	{
		
		if(!this.isClient()) //Only function on client, as the server will be desynchronized
			return ItemStack.EMPTY;
		
		//LightmansCurrency.LOGGER.info("ItemTraderStorageContainer.slotClick(" + slotId + ", " + dragType + ", " + clickType + ", " + player.getName().getString() + ")");
		if(slotId >= 0 && slotId < inventorySlots.size())
		{
			Slot slot = inventorySlots.get(slotId);
			if(slot == null)
				return ItemStack.EMPTY;
			if(slot instanceof DisplaySlot) //Ignore the display slot as it's purely for display purposes
				return ItemStack.EMPTY;
			//Get the item stack in the slot
			ItemStack stack = slot.getStack();
			//Define the item
			if(!stack.isEmpty())
			{
				this.setItem(stack, this.editSlot);
				return ItemStack.EMPTY;
			}
		}
		return ItemStack.EMPTY;
		
	}
	
	@OnlyIn(Dist.CLIENT)
	protected static void initItemList()
	{
		if(allItems != null)
			return;
		
		allItems = new ArrayList<>();
		
		//Go through all of the item groups to avoid allowing sales of hidden items
		for(ItemGroup group : ItemGroup.GROUPS)
		{
			if(!ITEM_GROUP_BLACKLIST.contains(group))
			{
				//LightmansCurrency.LogInfo("Getting items from " + group.getGroupName().getString() + ".");
				//Get all of the items in this group
				NonNullList<ItemStack> items = NonNullList.create();
				group.fill(items);
				//Add them to the list after confirming we don't already have it in the list
				for(ItemStack stack : items)
				{
					
					if(!itemListAlreadyContains(stack))
						allItems.add(stack);
					
					if(stack.getItem() == Items.ENCHANTED_BOOK)
					{
						//LightmansCurrency.LogInfo("Attempting to add lower levels of an enchanted book.");
						Map<Enchantment,Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
						enchantments.forEach((enchantment, level) ->{
							for(int newLevel = level - 1; newLevel > 0; newLevel--)
							{
								ItemStack newBook = new ItemStack(Items.ENCHANTED_BOOK);
								EnchantmentHelper.setEnchantments(ImmutableMap.of(enchantment, newLevel), newBook);
								if(!itemListAlreadyContains(newBook))
								{
									//LightmansCurrency.LogInfo("Adding enchanted book with " + enchantment.getDisplayName(newLevel).getString() + ".");
									allItems.add(newBook);
								}
								//else
								//	LightmansCurrency.LogWarning("Enchanted book with " + enchantment.getDisplayName(newLevel).getString() + " is already present.");
							}
						});
					}
					
				}
			}
		}
	}
	
	
	
	private static boolean itemListAlreadyContains(ItemStack stack)
	{
		for(ItemStack s : allItems)
		{
			if(InventoryUtil.ItemMatches(s, stack))
				return true;
		}
		return false;
	}
	
	//Nothing to do here
	@Override
	public ItemStack transferStackInSlot(PlayerEntity playerEntity, int index)
	{
		return ItemStack.EMPTY;
	}
	
	@Override
	public boolean canInteractWith(PlayerEntity playerIn)
	{
		return true;
	}

	public void modifySearch(String newSearch)
	{
		this.searchString = newSearch.toLowerCase();
		
		//Repopulate the searchResultItems list
		if(this.searchString.length() > 0)
		{
			this.searchResultItems = new ArrayList<>();
			List<ItemStack> validItems = this.editSlot == 0 ? this.filteredResultItems : allItems;
			for(ItemStack stack : validItems)
			{
				//Search the display name
				if(stack.getDisplayName().getString().toLowerCase().contains(this.searchString))
				{
					this.searchResultItems.add(stack);
				}
				//Search the registry name
				else if(stack.getItem().getRegistryName().toString().contains(this.searchString))
				{
					this.searchResultItems.add(stack);
				}
				//Search the enchantments?
				else
				{
					AtomicReference<Boolean> enchantmentMatch = new AtomicReference<>(false);
					Map<Enchantment,Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
					enchantments.forEach((enchantment, level) ->{
						if(enchantment.getRegistryName().toString().contains(this.searchString))
							enchantmentMatch.set(true);
						else if(enchantment.getDisplayName(level).getString().toLowerCase().contains(this.searchString))
							enchantmentMatch.set(true);
					});
					if(enchantmentMatch.get())
						this.searchResultItems.add(stack);
				}
			}
		}
		else //No search string, so the result is just the filtered items list
		{
			this.searchResultItems = this.editSlot == 0 ? this.filteredResultItems : allItems;
		}
		
		//Run refresh page code to validate the page # and repopulate the display inventory
		this.refreshPage();
		
	}
	
	private List<ItemStack> getFilteredItems()
	{
		List<ItemStack> results = Lists.newArrayList();
		ItemTradeRestriction restriction = this.tradeData.getRestriction();
		for(int i = 0; i < allItems.size(); ++i)
		{
			if(restriction.allowItemSelectItem(allItems.get(i)))
				results.add(allItems.get(i));
		}
		return results;
	}
	
	public void modifyStackSize(int deltaCount)
	{
		this.stackCount = MathUtil.clamp(this.stackCount + deltaCount, 1, 64);
		for(int i = 0; i < this.displayInventory.getSizeInventory(); i++)
		{
			ItemStack stack = this.displayInventory.getStackInSlot(i);
			if(!stack.isEmpty())
				stack.setCount(MathUtil.clamp(this.stackCount, 1, stack.getMaxStackSize()));
		}
	}
	
	public int maxPage()
	{
		return (this.searchResultItems.size() - 1) / this.displayInventory.getSizeInventory();
	}
	
	public void modifyPage(int deltaPage)
	{
		this.page += deltaPage;
		refreshPage();
	}
	
	public void refreshPage()
	{
		
		if(this.page < 0)
			this.page = 0;
		if(this.page > maxPage())
			this.page = maxPage();
		
		//LightmansCurrency.LogInfo("Refreshing page " + this.page + ". Max Page: " + maxPage());
		
		int startIndex = this.page * columnCount * rowCount;
		//Define the display inventories contents
		for(int i = 0; i < this.displayInventory.getSizeInventory(); i++)
		{
			int thisIndex = startIndex + i;
			if(thisIndex < this.searchResultItems.size()) //Set to search result item
			{
				ItemStack stack = this.searchResultItems.get(thisIndex).copy();
				stack.setCount(MathUtil.clamp(this.stackCount, 1, stack.getMaxStackSize()));
				this.displayInventory.setInventorySlotContents(i, stack);
			}
			else
			{
				this.displayInventory.setInventorySlotContents(i, ItemStack.EMPTY);
			}
		}
		
	}
	
	public void toggleEditSlot()
	{
		if(this.tradeData.isBarter())
		{
			this.editSlot = this.editSlot == 1 ? 0 : 1;
			this.modifySearch(this.searchString);
		}
			
	}
	
	public void setItem(ItemStack stack, int slot)
	{
		if(isClient())
		{
			//Send message to server
			if(this.editSlot == 1)
				this.tradeData.setBarterItem(stack);
			else
				this.tradeData.setSellItem(stack);
			LightmansCurrencyPacketHandler.instance.sendToServer(new MessageItemEditSet(stack, this.editSlot));
		}
		else
		{
			//Set the trade
			if(slot == 1)
				this.traderSource.get().getTrade(this.tradeIndex).setBarterItem(stack);
			else
				this.traderSource.get().getTrade(this.tradeIndex).setSellItem(stack);
			this.traderSource.get().markTradesDirty();
		}
	}
	
	public void openTraderStorage()
	{
		if(isClient())
		{
			LightmansCurrencyPacketHandler.instance.sendToServer(new MessageItemEditClose());
		}
		else
		{
			this.traderSource.get().openStorageMenu(this.player);
		}
	}
	
}
