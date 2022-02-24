package io.github.lightman314.lightmanscurrency.money;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.io.Files;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;

import io.github.lightman314.lightmanscurrency.Config;
import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.core.ModBlocks;
import io.github.lightman314.lightmanscurrency.core.ModItems;
import io.github.lightman314.lightmanscurrency.events.GetDefaultMoneyDataEvent;
import io.github.lightman314.lightmanscurrency.items.CoinBlockItem;
import io.github.lightman314.lightmanscurrency.items.CoinItem;
import io.github.lightman314.lightmanscurrency.items.WalletItem;
import io.github.lightman314.lightmanscurrency.money.CoinValue.CoinValuePair;
import io.github.lightman314.lightmanscurrency.network.LightmansCurrencyPacketHandler;
import io.github.lightman314.lightmanscurrency.util.FileUtil;
import io.github.lightman314.lightmanscurrency.util.InventoryUtil;
import io.github.lightman314.lightmanscurrency.util.MathUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.NonNullList;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.network.PacketDistributor;

@Mod.EventBusSubscriber
public class MoneyUtil {
	
	public static final String MONEY_FILE_LOCATION = "config/lightmanscurrency/MasterCoinList.json";
	
	public static final String MAIN_CHAIN = "main";
	
	private static MoneyData moneyData = null;
	public static MoneyData getMoneyData() { return moneyData; }
	public static void receiveMoneyData(MoneyData data) { moneyData = data; }
	
	@SubscribeEvent
	public static void onServerStart(FMLServerStartedEvent event) {
		LightmansCurrency.LogInfo("Setting up Money System for server.");
		reloadMoneyData();
	}
	
	public static void reloadMoneyData() {
		LightmansCurrency.LogInfo("Reloading Money Data");
		File mcl = new File(MONEY_FILE_LOCATION);
		if(!mcl.exists())
		{
			createMoneyDataFile(mcl);
		}
		try { 
			JsonObject fileData = JSONUtils.fromJson(Files.toString(mcl, StandardCharsets.UTF_8));
			moneyData = MoneyData.fromJson(fileData);
		} catch(Throwable e) {
			LightmansCurrency.LogError("Error loading Master Coin List. Using default values for now.", e);
			moneyData = MoneyData.generateDefault();
		}
		LightmansCurrencyPacketHandler.instance.send(PacketDistributor.ALL.noArg(), moneyData);
	}
	
	private static void createMoneyDataFile(File mcl) {
		File dir = new File(mcl.getParent());
		if(!dir.exists())
			dir.mkdirs();
		if(dir.exists())
		{
			try {
				
				mcl.createNewFile();
				
				MoneyData defaultData = MoneyData.generateDefault();
				
				FileUtil.writeStringToFile(mcl, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(defaultData.toJson()));
				
				LightmansCurrency.LogInfo("MasterCoinList.json does not exist. Creating a fresh copy.");
				
			} catch(Throwable e) { LightmansCurrency.LogError("Error attempting to create 'MasterCoinList.json' file.", e); }
		}
	}
	
	@SubscribeEvent
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		//Send the player the Money Data
		LightmansCurrencyPacketHandler.instance.send(LightmansCurrencyPacketHandler.getTarget(event.getPlayer()), moneyData);
	}
	
	/**
     * Initializes the default master coin list from order of highest to lowest values.
     */
	@SubscribeEvent
    public static void initializeDefaultCoins(GetDefaultMoneyDataEvent event)
    {
    	
    	LightmansCurrency.LogInfo("Initializing the Money Utilities.");
    	
    	//Copper Coin
    	event.dataCollector.addCoinBuilder(CoinData.getBuilder(ModItems.COIN_COPPER, MAIN_CHAIN)
    			.defineInitial("item.lightmanscurrency.coin_copper.initial"));
    	//Iron Coin
    	event.dataCollector.addCoinBuilder(CoinData.getBuilder(ModItems.COIN_IRON, MAIN_CHAIN)
    			.defineInitial("item.lightmanscurrency.coin_iron.initial")
    			.defineConversion(ModItems.COIN_COPPER, 10));
    	//Gold Coin
    	event.dataCollector.addCoinBuilder(CoinData.getBuilder(ModItems.COIN_GOLD, MAIN_CHAIN)
    			.defineInitial("item.lightmanscurrency.coin_gold.initial")
    			.defineConversion(ModItems.COIN_IRON, 10));
    	//Emerald Coin
    	event.dataCollector.addCoinBuilder(CoinData.getBuilder(ModItems.COIN_EMERALD, MAIN_CHAIN)
    			.defineInitial("item.lightmanscurrency.coin_emerald.initial")
    			.defineConversion(ModItems.COIN_GOLD, 10));
    	//Diamond Coin
    	event.dataCollector.addCoinBuilder(CoinData.getBuilder(ModItems.COIN_DIAMOND, MAIN_CHAIN)
    			.defineInitial("item.lightmanscurrency.coin_diamond.initial")
    			.defineConversion(ModItems.COIN_EMERALD, 10));
    	//Netherite Coin
    	event.dataCollector.addCoinBuilder(CoinData.getBuilder(ModItems.COIN_NETHERITE, MAIN_CHAIN)
    			.defineInitial("item.lightmanscurrency.coin_netherite.initial")
    			.defineConversion(ModItems.COIN_DIAMOND, 10));
    	
    	//Hidden coins
    	//Copper Coinpile
    	event.dataCollector.addCoinBuilder(CoinData.getBuilder(ModBlocks.COINPILE_COPPER, MAIN_CHAIN)
    			.defineConversion(ModItems.COIN_COPPER, 9)
    			.setHidden());
    	//Copper Coin Block
    	event.dataCollector.addCoinBuilder(CoinData.getBuilder(ModBlocks.COINBLOCK_COPPER, MAIN_CHAIN)
    			.defineConversion(ModBlocks.COINPILE_COPPER, 4)
    			.setHidden());
    	
    	//Iron Coinpile
    	event.dataCollector.addCoinBuilder(CoinData.getBuilder(ModBlocks.COINPILE_IRON, MAIN_CHAIN)
    			.defineConversion(ModItems.COIN_IRON, 9)
    			.setHidden());
    	//Iron Coin Block
    	event.dataCollector.addCoinBuilder(CoinData.getBuilder(ModBlocks.COINBLOCK_IRON, MAIN_CHAIN)
    			.defineConversion(ModBlocks.COINPILE_IRON, 4)
    			.setHidden());
    	
    	//Gold Coinpile
    	event.dataCollector.addCoinBuilder(CoinData.getBuilder(ModBlocks.COINPILE_GOLD, MAIN_CHAIN)
    			.defineConversion(ModItems.COIN_GOLD, 9)
    			.setHidden());
    	//Gold Coin Block
    	event.dataCollector.addCoinBuilder(CoinData.getBuilder(ModBlocks.COINBLOCK_GOLD, MAIN_CHAIN)
    			.defineConversion(ModBlocks.COINPILE_GOLD, 4)
    			.setHidden());
    	
    	//Emerald Coinpile
    	event.dataCollector.addCoinBuilder(CoinData.getBuilder(ModBlocks.COINPILE_EMERALD, MAIN_CHAIN)
    			.defineConversion(ModItems.COIN_EMERALD, 9)
    			.setHidden());
    	//Emerald Coin Block
    	event.dataCollector.addCoinBuilder(CoinData.getBuilder(ModBlocks.COINBLOCK_EMERALD, MAIN_CHAIN)
    			.defineConversion(ModBlocks.COINPILE_EMERALD, 4)
    			.setHidden());
    	
    	//Diamond Coinpile
    	event.dataCollector.addCoinBuilder(CoinData.getBuilder(ModBlocks.COINPILE_DIAMOND, MAIN_CHAIN)
    			.defineConversion(ModItems.COIN_DIAMOND, 9)
    			.setHidden());
    	//Diamond Coin Block
    	event.dataCollector.addCoinBuilder(CoinData.getBuilder(ModBlocks.COINBLOCK_DIAMOND, MAIN_CHAIN)
    			.defineConversion(ModBlocks.COINPILE_DIAMOND, 4)
    			.setHidden());
    	
    	//Netherite Coinpile
    	event.dataCollector.addCoinBuilder(CoinData.getBuilder(ModBlocks.COINPILE_NETHERITE, MAIN_CHAIN)
    			.defineConversion(ModItems.COIN_NETHERITE, 9)
    			.setHidden());
    	//Netherite Coin Block
    	event.dataCollector.addCoinBuilder(CoinData.getBuilder(ModBlocks.COINBLOCK_NETHERITE, MAIN_CHAIN)
    			.defineConversion(ModBlocks.COINPILE_NETHERITE, 4)
    			.setHidden());
		
    }
	
	@SubscribeEvent
	//Add coin value tooltips to non CoinItem coins.
	public static void onItemTooltip(ItemTooltipEvent event) {
		Item item = event.getItemStack().getItem();
		CoinData coinData = getData(item);
		if(coinData != null && !(item instanceof CoinItem || item instanceof CoinBlockItem))
		{
			CoinItem.addCoinTooltips(event.getItemStack(), event.getToolTip());
		}
	}
	
    /**
     * Checks if the given item is in the master coin list.
     * By default allows hidden coins
     * @param item The item to check.
     */
    public static boolean isCoin(Item item)
    {
    	return isCoin(item, true);
    }
    
    /**
     * Checks if the given item is in the master coin list.
     * @param item The item to check.
     * @param allowHidden Whether hidden coins should return true.
     */
    public static boolean isCoin(Item item, boolean allowHidden)
    {
    	if(item == null)
    		return false;
    	CoinData data = getData(item);
    	if(data == null)
    		return false;
    	return allowHidden || !data.isHidden;
    }
    
    /**
     * Checks if the given item is both a coin & that the coin is considered hidden.
     * Used to blacklist "hidden" coins from certain slots (such as the ATM slots) while still letting them be considered coins in the default isCoin check
     */
    public static boolean isCoinHidden(Item item)
    {
    	if(item == null)
    		return false;
    	CoinData data = getData(item);
    	if(data == null)
    		return false;
    	return data.isHidden;
    }
    
    /**
     * Checks if the given item is in the master coin list.
     * @param stack The ItemStack to check.
     */
    public static boolean isCoin(@Nonnull ItemStack stack)
    {
    	return isCoin(stack, true);
    }
	
    /**
     * Checks if the given item is in the master coin list.
     * @param stack The ItemStack to check.
     * @param allowHidden Whether hidden coins should return true.
     */
    public static boolean isCoin(@Nonnull ItemStack stack, boolean allowHidden)
    {
    	return isCoin(stack.getItem(), allowHidden);
    }
    
    /**
     * Gets the value of the given item.
     * @param coinItem The coin to get the value of.
     */
    public static long getValue(Item coinItem)
    {
    	CoinData coinData = getData(coinItem);
    	if(coinData != null)
    		return coinData.getValue();
    	return 0;
    }
    
    /**
     * Gets the total value of the item stack.
     * @param coinStack The item stack to get the value of.
     */
    public static CoinValue getCoinValue(ItemStack coinStack) { return new CoinValue(getValue(coinStack)); }
    
    /**
     * Gets the total value of the item stack.
     * @param coinStack The item stack to get the value of.
     */
    public static long getValue(ItemStack coinStack)
    {
    	return getValue(coinStack.getItem()) * coinStack.getCount();
    }
    
    /**
	 * Gets the total value of the items in the given ItemStack list.
	 * @param inventory The list full of coins from which to get the value of.
	 */
    public static CoinValue getCoinValue(NonNullList<ItemStack> inventory) { return new CoinValue(getValue(inventory)); }
    
    /**
	 * Gets the total value of the items in the given ItemStack list.
	 * @param inventory The list full of coins from which to get the value of.
	 */
    public static long getValue(NonNullList<ItemStack> inventory)
	{
    	long value = 0;
		for(int i = 0; i < inventory.size(); i++)
		{
			value += getValue(inventory.get(i));
		}
		return value;
	}
    
    /**
	 * Gets the total value of the items in the given inventory.
	 * @param inventory The inventory full of coins with which to get the value of.
	 */
    public static CoinValue getCoinValue(IInventory inventory) { return new CoinValue(getValue(inventory)); }
    
    /**
	 * Gets the total value of the items in the given inventory.
	 * @param inventory The inventory full of coins with which to get the value of.
	 */
    public static long getValue(IInventory inventory)
	{
    	long value = 0;
		for(int i = 0; i < inventory.getSizeInventory(); i++)
		{
			value += getValue(inventory.getStackInSlot(i));
		}
		return value;
	}
    
    /**
     * Converts all coins in the inventory to as large a coin as humanly possible
     */
    public static void ConvertAllCoinsUp(IInventory inventory)
    {
    	if(moneyData == null)
    		return;
    	List<Item> coinList = getAllCoins(false);
    	for(int i = 1; i < coinList.size(); i++)
    	{
    		ConvertCoinsUp(inventory, coinList.get(i));
    	}
    	for(int i = coinList.size() - 1; i > 0; i--)
    	{
    		ConvertCoinsUp(inventory, coinList.get(i));
    	}
    }
    
    /**
     * Converts all coins in the inventory to as large a coin as humanly possible
     */
    public static NonNullList<ItemStack> ConvertAllCoinsUp(NonNullList<ItemStack> inventoryList)
    {
    	IInventory inventory = InventoryUtil.buildInventory(inventoryList);
    	ConvertAllCoinsUp(inventory);
    	return InventoryUtil.buildList(inventory);
    }
    
    /**
     * Converts as many of the small coin that it can into its next largest coin
     */
    public static void ConvertCoinsUp(IInventory inventory, Item smallCoin)
    {
    	//Get next-higher coin data
    	Pair<Item,Integer> upwardConversion = getUpwardConversion(smallCoin);
    	if(upwardConversion == null)
    		return;
    	Item largeCoin = upwardConversion.getFirst();
    	int smallCoinCount = upwardConversion.getSecond();
    	
    	if(!isCoin(largeCoin))
    		return;
    	while(InventoryUtil.GetItemCount(inventory, smallCoin) >= smallCoinCount)
		{
			//Remove the smaller coins
    		InventoryUtil.RemoveItemCount(inventory, smallCoin, smallCoinCount);
			//Put the new coin into the inventory
			ItemStack newCoinStack = new ItemStack(largeCoin, 1);
			if(!InventoryUtil.PutItemStack(inventory, newCoinStack))
			{
				//Could not merge the inventory. Re-add the smaller coins & break the loop;
				InventoryUtil.TryPutItemStack(inventory, new ItemStack(smallCoin, smallCoinCount));
				return;
			}
		}
    }
    
    /**
     * Converts all coins in the inventory to as small a coin as humanly possible
     */
    public static void ConvertAllCoinsDown(IInventory inventory)
    {
    	ConvertAllCoinsDown(inventory, 2);
    }
    
    /**
     * Converts all coins in the inventory to as small a coin as humanly possible
     * @param iterations The number of times to repeatedly convert to ensure that the available space is used. Default is 2.
     */
    private static void ConvertAllCoinsDown(IInventory inventory, int iterations)
    {
    	if(moneyData == null)
    		return;
    	List<CoinData> coinList = moneyData.getCoinList();
    	for(int x = 0; x < iterations; x++)
    	{
    		for(int i = 0; i < (coinList.size() - 1); i++)
    		{
    			if(!coinList.get(i).isHidden)
    				ConvertCoinsDown(inventory, coinList.get(i).coinItem);
    		}
    	}
    }
    
    /**
     * Converts as many of the large coin that it can into its defined smaller coin
     */
    public static void ConvertCoinsDown(IInventory inventory, Item largeCoin)
    {
    	CoinData coinData = getData(largeCoin);
    	Item smallCoin = coinData.worthOtherCoin;
    	int smallCoinCount = coinData.worthOtherCoinCount;
    	if(!isCoin(smallCoin))
    		return;
    	while(InventoryUtil.GetItemCount(inventory, largeCoin) > 0)
		{
			//Remove the large coin
    		InventoryUtil.RemoveItemCount(inventory, largeCoin, 1);
    		//Merge the new coins into the container
			ItemStack newCoinStack = new ItemStack(smallCoin, smallCoinCount);
			if(!InventoryUtil.PutItemStack(inventory, newCoinStack))
			{
				//Could not merge the inventory. Re-add the large coin & break the loop;
				InventoryUtil.TryPutItemStack(inventory, new ItemStack(largeCoin, 1));
				return;
			}
		}
    }
    
    public static void SortCoins(IInventory inventory)
	{
		
		InventoryUtil.MergeStacks(inventory);
		
		List<ItemStack> oldInventory = new ArrayList<>();
		for(int i = 0; i < inventory.getSizeInventory(); i++)
		{
			if(!inventory.getStackInSlot(i).isEmpty())
				oldInventory.add(inventory.getStackInSlot(i));
		}
		
		inventory.clear();
		
		int index = 0;
		while(oldInventory.size() > 0)
		{
			int highestIndex = 0;
			long highestIndividualValue = MoneyUtil.getValue(oldInventory.get(0).getItem());
			long highestWholeValue = highestIndividualValue * oldInventory.get(0).getCount();
			//LightmansCurrency.LOGGER.info("Starting sort values. HI: " + highestIndividualValue + " HW: " + highestWholeValue);
			
			for(int i = 1; i < oldInventory.size(); i++)
			{
				ItemStack stack = oldInventory.get(i);
				long thisIndividualValue = MoneyUtil.getValue(stack.getItem());
				long thisWholeValue = thisIndividualValue * stack.getCount();
				//Value is higher 
				if(thisIndividualValue > highestIndividualValue)
				{
					//LightmansCurrency.LOGGER.info("Larger Individual Value at index " + i + ": " + thisIndividualValue + " > " + highestIndividualValue + "Whole Values: " + thisWholeValue + " & " + highestWholeValue);
					highestIndex = i;
					highestIndividualValue = thisIndividualValue;
					highestWholeValue = thisWholeValue;
				}
				else if(thisIndividualValue == highestIndividualValue && thisWholeValue > highestWholeValue)
				{
					//LightmansCurrency.LOGGER.info("Same Individual Value but larger whole at index " + i + ": " + thisWholeValue + " > " + highestWholeValue + "Individual Values: " + thisIndividualValue + " = " + highestIndividualValue );
					highestIndex = i;
					highestWholeValue = thisWholeValue;
				}
			}
			
			inventory.setInventorySlotContents(index, oldInventory.get(highestIndex));
			index++;
			oldInventory.remove(highestIndex);
		}
		
	}
    
    public static NonNullList<ItemStack> SortCoins(NonNullList<ItemStack> inventory)
    {
    	IInventory tempInventory = InventoryUtil.buildInventory(inventory);
    	SortCoins(tempInventory);
    	return InventoryUtil.buildList(tempInventory);
    }
    
    /**
     * Process a payment from the given coin slot inventory & player.
     * @param inventory An inventory that may or may not contain coins that payment can be taken from. Can be null, but then payment will only be taken from the players wallet.
     * @param player The player making the payment. Required for item overflow, and wallet aquisition.
     * @param price The price of the payment that we are attempting to process.
     * @return Whether the payment went through. If false is returned, no money was taken from the wallet nor the inventory.
     */
    public static boolean ProcessPayment(@Nullable IInventory inventory, @Nonnull PlayerEntity player, @Nonnull CoinValue price)
    {
    	return ProcessPayment(inventory, player, price, false);
    }
    
    /**
     * Process a payment from the given coin slot inventory & player.
     * @param inventory An inventory that may or may not contain coins that payment can be taken from. Can be null, but then payment will only be taken from the players wallet.
     * @param player The player making the payment. Required for item overflow, and wallet aquisition.
     * @param price The price of the payment that we are attempting to process.
     * @param ignoreWallet Whether we should ignore the players wallet in terms of taking payment or giving change.
     * @return Whether the payment went through. If false is returned, no money was taken from the wallet nor the inventory.
     */
    public static boolean ProcessPayment(@Nullable IInventory inventory, @Nonnull PlayerEntity player, @Nonnull CoinValue price, boolean ignoreWallet)
    {
    	//Get the players wallet
    	ItemStack wallet = ignoreWallet ? ItemStack.EMPTY : LightmansCurrency.getWalletStack(player);
    	
    	long valueToTake = price.getRawValue();
    	//Get value from the wallet
    	long rawInventoryValue = 0;
    	if(inventory != null)
    		rawInventoryValue += getValue(inventory);
    	if(!wallet.isEmpty())
    		rawInventoryValue += getValue(WalletItem.getWalletInventory(wallet));
    	if(rawInventoryValue < valueToTake)
    		return false;
    	
    	//Otherwise take the payment
    	//Take from the inventory first
    	if(inventory != null)
    		valueToTake = takeObjectsOfValue(valueToTake, inventory, true);
    	//Then take from the wallet
    	if(valueToTake > 0 && !wallet.isEmpty())
    	{
    		NonNullList<ItemStack> walletInventory = WalletItem.getWalletInventory(wallet);
    		valueToTake = takeObjectsOfValue(valueToTake, walletInventory);
    		WalletItem.putWalletInventory(wallet, walletInventory);
    	}
    	
    	//Give change if necessary
    	if(valueToTake < 0)
    	{
    		List<ItemStack> change = getCoinsOfValue(Math.abs(valueToTake));
    		for(ItemStack coinStack : change)
    		{
    			//Put them in the wallet first
    			if(!wallet.isEmpty())
    			{
    				coinStack = WalletItem.PickupCoin(wallet, coinStack);
    			}
    			if(!coinStack.isEmpty() && inventory != null)
    			{
    				//TryPutItemStack allows partial placement unlike PutItemStack which is used for cancelable placements
    				coinStack = InventoryUtil.TryPutItemStack(inventory, coinStack);
    			}
    			//Out of room to place it, throw it at the player
    			if(!coinStack.isEmpty())
    			{
    				player.inventory.placeItemBackInInventory(player.world, coinStack);
    			}
    		}
    	}
    	
    	return true;
    }
    
    /**
     * Put money into the coin slot inventory & player.
     * @param inventory An inventory in which to give the coins to by default should the player not have an equipped wallet. Can be null, but then payment will only be given to the players wallet/inventory.
     * @param player The player receiving the money. Required for item overflow, and wallet aquisition.
     * @param price The amount of money we're attempting to give.
     */
    public static void ProcessChange(@Nullable IInventory inventory, @Nonnull PlayerEntity player, @Nonnull CoinValue change)
    {
    	ProcessChange(inventory, player, change, false);
    }
    
    public static void ProcessChange(@Nullable IInventory inventory, @Nonnull PlayerEntity player, @Nonnull CoinValue change, boolean ignoreWallet)
    {
    	//Get the players wallet
    	ItemStack wallet = ignoreWallet ? ItemStack.EMPTY : LightmansCurrency.getWalletStack(player);
    	
    	List<ItemStack> changeCoins = getCoinsOfValue(change);
    	for(ItemStack coinStack : changeCoins)
		{
			//Put them in the wallet first
			if(!wallet.isEmpty())
			{
				coinStack = WalletItem.PickupCoin(wallet, coinStack);
			}
			if(!coinStack.isEmpty() && inventory != null)
			{
				//TryPutItemStack allows partial placement unlike PutItemStack which is used for cancelable placements
				coinStack = InventoryUtil.TryPutItemStack(inventory, coinStack);
			}
			//Out of room to place it, throw it at the player
			if(!coinStack.isEmpty())
			{
				player.inventory.placeItemBackInInventory(player.world, coinStack);
			}
		}
    	
    }
    
    /**
     * Removes coins of the given value from the given inventory.
     * Used to process a payment.
     * @param value The amount of money to remove from the inventory.
     * @param inventory The inventory from which to take away the coins.
     * @param forceTake Whether we should take as much as we can should the inventory not contain enough money.
     * @return Returns the given value if there is not enough money to take.
     * Returns 0 if exact change was taken, and returns a negative value if more money was taken than what was requested so that change can be calculated separately.
     */
    public static long takeObjectsOfValue(long value, IInventory inventory, boolean forceTake)
	{
    	if(moneyData == null)
    		return value;
		//Check to ensure that the inventory has enough 'value' to remove
		if(MoneyUtil.getValue(inventory) < value && !forceTake)
			return value;
		else
		{
			List<CoinData> coinList = moneyData.getCoinList();
			//Remove objects from the inventory.
			for(CoinData coinData : coinList)
			{
				long coinValue = coinData.getValue();
				if(coinValue <= value)
				{
					//Search the inventory for this coin
					for(int i = 0; i < inventory.getSizeInventory() && coinValue <= value; i++)
					{
						ItemStack itemStack = inventory.getStackInSlot(i);
						if(inventory.getStackInSlot(i).getItem().equals(coinData.coinItem))
						{
							//Remove the coins until they would be too much money or until the stack is empty.
							while(coinValue <= value && !itemStack.isEmpty())
							{
								value -= coinValue;
								itemStack.setCount(itemStack.getCount() - 1);
								if(itemStack.isEmpty())
									inventory.setInventorySlotContents(i, ItemStack.EMPTY);
							}
						}
					}
				}
			}
			//Took all we could without over-taking, so we'll just go through the coinList backwards until we have what we need.
			if(value > 0)
			{
				for(int c = coinList.size() - 1; c >= 0; c--)
				{
					Item coin = coinList.get(c).coinItem;
					long coinValue = coinList.get(c).getValue();
					//Search the inventory for this coin
					for(int i = 0; i < inventory.getSizeInventory() && value > 0; i++)
					{
						ItemStack itemStack = inventory.getStackInSlot(i);
						if(itemStack.getItem() == coin)
						{
							//Remove the coins until they would be too much money or until the stack is empty.
							while(value > 0 && !itemStack.isEmpty())
							{
								value -= coinValue;
								itemStack.setCount(itemStack.getCount() - 1);
								if(itemStack.isEmpty())
									inventory.setInventorySlotContents(i, ItemStack.EMPTY);
							}
						}
					}
				}
			}
			
			//Inform the user if we were exact, or if too many items were taken and a refund is required via the getObjectsOfValue function
			return value; 
			
		}
	}
    
    private static long takeObjectsOfValue(long value, NonNullList<ItemStack> inventory)
    {
    	return takeObjectsOfValue(value, inventory, false);
    }
    
    /**
     * Removes coins of the given value from the given itemstack list.
     * Used to process a payment from a wallet or other non-inventory source.
     * @param value The amount of money to remove from the inventory.
     * @param inventory The ItemStack list from which to take away the coins.
     * @return Returns the given value if there is not enough money to take.
     * Returns 0 if exact change was taken, and returns a negative value if more money was taken than what was requested so that change can be calculated separately.
     */
    private static long takeObjectsOfValue(long value, NonNullList<ItemStack> inventory, boolean forceTake)
	{
    	if(moneyData == null)
    		return value;
		//Check to ensure that the inventory has enough 'value' to remove
		if(MoneyUtil.getValue(inventory) < value && !forceTake)
			return value;
		else
		{
			//Remove objects from the inventory.
			List<CoinData> coinList = moneyData.getCoinList();
			for(CoinData coinData : coinList)
			{
				long coinValue = coinData.getValue();
				if(coinValue <= value)
				{
					//Search the inventory for this coin
					for(int i = 0; i < inventory.size() && coinValue <= value; i++)
					{
						ItemStack itemStack = inventory.get(i);
						if(inventory.get(i).getItem().equals(coinData.coinItem))
						{
							//Remove the coins until they would be too much money or until the stack is empty.
							while(coinValue <= value && !itemStack.isEmpty())
							{
								value -= coinValue;
								itemStack.setCount(itemStack.getCount() - 1);
								if(itemStack.isEmpty())
									inventory.set(i, ItemStack.EMPTY);
							}
						}
					}
				}
			}
			//Took all we could without over-taking, so we'll just go through the coinList backwards until we have what we need.
			if(value > 0)
			{
				for(int c = coinList.size() - 1; c >= 0; c--)
				{
					Item coin = coinList.get(c).coinItem;
					long coinValue = coinList.get(c).getValue();
					//Search the inventory for this coin
					for(int i = 0; i < inventory.size() && value > 0; i++)
					{
						ItemStack itemStack = inventory.get(i);
						if(itemStack.getItem() == coin)
						{
							//Remove the coins until they would be too much money or until the stack is empty.
							while(value > 0 && !itemStack.isEmpty())
							{
								value -= coinValue;
								itemStack.setCount(itemStack.getCount() - 1);
								if(itemStack.isEmpty())
									inventory.set(i, ItemStack.EMPTY);
							}
						}
					}
				}
			}
			
			//Inform the user if we were exact, or if too many items were taken and a refund is required via the getObjectsOfValue function
			return value; 
			
		}
	}
    
    /**
     * Returns a list of coins in Item Stack format from the given value.
     * Used to convert from a stored value back into coins, or give change after processing a payment.
     */
    //@Deprecated //No longer used in favor of the new CoinValue data type
    //Do not replace, as the CoinValue uses this to read from long values
    public static List<ItemStack> getCoinsOfValue(long value)
	{
		List<ItemStack> items = new ArrayList<>();
		if(value <= 0 || moneyData == null)
			return items;
		
		//Search through each coin in the coinList
		List<CoinData> coinList = moneyData.getCoinList(MAIN_CHAIN);
		for(int i = 0; i < coinList.size(); i++)
		{
			if(!coinList.get(i).isHidden)
			{
				Item coin = coinList.get(i).coinItem;
				int coinsToGive = 0;
				long coinValue = coinList.get(i).getValue();
				while(coinValue <= value)
				{
					value -= coinValue;
					coinsToGive++;
				}
				while(coinsToGive > 0)
				{
					int giveCount = coinsToGive;
					if(giveCount > 64)
						giveCount = 64;
					coinsToGive -= giveCount;
					ItemStack newStack = new ItemStack(coin, giveCount);
					items.add(newStack);
				}
			}
		}
		
		return items;
	}
    
    /**
     * Returns a list of coins in Item Stack format from the given value.
     * Used to convert from a stored value back into coins, or give change after processing a payment.
     */
    public static List<ItemStack> getCoinsOfValue(CoinValue value)
    {
    	List<ItemStack> items = new ArrayList<>();
    	for(CoinValuePair pricePair : value.coinValues)
    	{
    		int amount = pricePair.amount;
    		while(amount > 0)
    		{
    			ItemStack newStack = new ItemStack(pricePair.coin);
        		int amountToAdd = MathUtil.clamp(amount, 0, newStack.getMaxStackSize());
        		newStack.setCount(amountToAdd);
        		
        		items.add(newStack);
        		amount -= amountToAdd;
    		}
    	}
    	return items;
    }
    
    /**
     * Gets a short display string for the given monetary value in the format of '1n2d..'
     * @param value The amount of monetary value to display.
     */
    public static String getStringOfValue(long value)
    {
    	return new CoinValue(value).getString();
    }
    
    /**
     * Gets the coin data for the given coin item
     */
    public static CoinData getData(Item coinItem)
    {
    	if(moneyData == null)
    		return null;
    	return moneyData.getData(coinItem);
    }
    
    /**
     * Gets a list of all of the registered coin items
     * By default, hidden coins will not be included in this list.
     */
    public static List<Item> getAllCoins()
    {
    	return getAllCoins(false);
    }
    
    /**
     * Gets a list of all of the registered coin items
     * @param Whether hidden coins will be included in the list.
     */
    public static List<Item> getAllCoins(boolean includeHidden)
    {
    	if(moneyData == null)
    		return new ArrayList<>();
    	List<Item> coinItems = new ArrayList<>();
    	List<CoinData> coinList = moneyData.getCoinList();
    	for(int i = 0; i < coinList.size(); i++)
    	{
    		if(!coinList.get(i).isHidden || includeHidden)
    			coinItems.add(coinList.get(i).coinItem);
    	}
    	return coinItems;
    }
    
    /**
     * Gets a list of all of the registered coin items
     * By default, hidden coins will not be included in this list.
     * @param chain The coin chain to receive coins from
     */
    public static List<Item> getAllCoins(String chain)
    {
    	return getAllCoins(chain, false);
    }
    
    /**
     * Gets a list of all of the registered coin items
     * @param chain The coin chain to receive coins from
     * @param includeHidden Whether hidden coins will be included in the list.
     */
    public static List<Item> getAllCoins(String chain, boolean includeHidden)
    {
    	if(moneyData == null)
    		return new ArrayList<>();
    	List<Item> coinItems = new ArrayList<>();
    	List<CoinData> coinList = moneyData.getCoinList();
    	for(int i = 0; i < coinList.size(); i++)
    	{
    		if(coinList.get(i).chain.contentEquals(chain) && (!coinList.get(i).isHidden || includeHidden))
    			coinItems.add(coinList.get(i).coinItem);
    	}
    	return coinItems;
    }
    
    /**
     * Gets a sorted list of all of the coin data
     * By default CoinData for hidden coins will not be included in this list.
     */
    public static List<CoinData> getAllData()
    {
    	return getAllData(false);
    }
    
    /**
     * Gets a sorted list of all of the coin data
     * @param includeHidden Whether hidden CoinData will be included in the list.
     */
    public static List<CoinData> getAllData(boolean includeHidden)
    {
    	if(moneyData == null)
    		return new ArrayList<>();
    	List<CoinData> coinList = moneyData.getCoinList();
    	if(includeHidden)
    		return coinList;
    	List<CoinData> publicCoinList = new ArrayList<>();
    	for(int i = 0; i < coinList.size(); ++i)
    	{
    		if(!coinList.get(i).isHidden)
    			publicCoinList.add(coinList.get(i));
    	}
    	return publicCoinList;
    }
    
    /**
     * Gets a sorted list of all of the coin data in the given chain
     * By default CoinData for hidden coins will not be included in this list.
     * @param chain The coin chain to receive coins from
     */
    public static List<CoinData> getAllData(String chain)
    {
    	return getAllData(chain, false);
    }
    
    /**
     * Gets a sorted list of all of the coin data
     * @param chain The coin chain to receive coins from
     * @param includeHidden Whether hidden CoinData will be included in the list.
     */
    public static List<CoinData> getAllData(String chain, boolean includeHidden)
    {
    	if(moneyData == null)
    		return new ArrayList<>();
    	List<CoinData> coinList = moneyData.getCoinList(chain);
    	if(includeHidden)
    		return coinList;
    	List<CoinData> publicCoinList = new ArrayList<>();
    	for(int i = 0; i < coinList.size(); ++i)
    	{
    		if(!coinList.get(i).isHidden)
    			publicCoinList.add(coinList.get(i));
    	}
    	return publicCoinList;
    }
    
    public static Pair<Item,Integer> getUpwardConversion(Item coinItem)
    {
    	if(moneyData == null)
    		return null;
    	Item largeCoin = null;
    	int amount = Integer.MAX_VALUE;
    	List<CoinData> coinList = moneyData.getCoinList();
    	CoinData smallCoinData = moneyData.getData(coinItem);
    	if(smallCoinData == null)
    		return null;
    	for(CoinData coinData : coinList)
    	{
    		//Don't get upward conversion for hidden coins or coins from a different chain
    		if(coinData.worthOtherCoin == coinItem && coinData.worthOtherCoinCount < amount && !coinData.isHidden && smallCoinData.chain.contentEquals(coinData.chain))
    		{
    			largeCoin = coinData.coinItem;
    			amount = coinData.worthOtherCoinCount;
    		}
    	}
    	if(largeCoin != null)
    		return new Pair<Item,Integer>(largeCoin, amount);
    	return null;
    }
    
    public static Pair<Item,Integer> getDownwardConversion(Item coinItem)
    {
    	if(moneyData == null)
    		return null;
    	CoinData data = moneyData.getData(coinItem);
    	if(data == null)
    		return null;
    	if(data.convertsDownwards())
    		return data.getDownwardConversion();
    	return null;
    }
    
    public static long displayValueToLong(double displayValue)
    {
    	long baseCoinValue = getValue(Config.getBaseCoinItem());
    	double totalValue = displayValue * baseCoinValue;
    	long value = (long)totalValue;
    	return totalValue % 1d >= 0.5d ? value + 1 : value;
    }
    
    public static CoinValue displayValueToCoinValue(double displayValue)
    {
    	return new CoinValue(displayValueToLong(displayValue));
    }
    
}
