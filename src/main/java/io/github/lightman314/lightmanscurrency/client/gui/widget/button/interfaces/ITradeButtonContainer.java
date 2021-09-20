package io.github.lightman314.lightmanscurrency.client.gui.widget.button.interfaces;

import io.github.lightman314.lightmanscurrency.tradedata.ItemTradeData;
import io.github.lightman314.lightmanscurrency.util.MoneyUtil.CoinValue;
import net.minecraft.inventory.IInventory;

public interface ITradeButtonContainer {

	public long GetCoinValue();
	
	public IInventory GetItemInventory();
	
	public CoinValue TradeCostEvent(ItemTradeData trade);
	
	public boolean PermissionToTrade(int tradeIndex);
	
	public ItemTradeData GetTrade(int tradeIndex);
	
}
