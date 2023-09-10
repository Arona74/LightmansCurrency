package io.github.lightman314.lightmanscurrency.client.gui.screen.inventory.atm;

import java.util.List;

import com.google.common.collect.Lists;

import io.github.lightman314.lightmanscurrency.client.gui.easy.rendering.EasyGuiGraphics;
import io.github.lightman314.lightmanscurrency.client.gui.screen.inventory.ATMScreen;
import io.github.lightman314.lightmanscurrency.client.gui.widget.CoinValueInput;
import io.github.lightman314.lightmanscurrency.client.gui.widget.TeamSelectWidget;
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.icon.IconButton;
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.TeamButton.Size;
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.icon.IconData;
import io.github.lightman314.lightmanscurrency.client.gui.widget.easy.EasyAddonHelper;
import io.github.lightman314.lightmanscurrency.client.gui.widget.easy.EasyButton;
import io.github.lightman314.lightmanscurrency.client.gui.widget.easy.EasyTextButton;
import io.github.lightman314.lightmanscurrency.client.util.IconAndButtonUtil;
import io.github.lightman314.lightmanscurrency.client.util.ScreenArea;
import io.github.lightman314.lightmanscurrency.client.util.TextRenderUtil;
import io.github.lightman314.lightmanscurrency.common.bank.reference.BankReference;
import io.github.lightman314.lightmanscurrency.common.bank.reference.types.TeamBankReference;
import io.github.lightman314.lightmanscurrency.common.easy.EasyText;
import io.github.lightman314.lightmanscurrency.common.teams.Team;
import io.github.lightman314.lightmanscurrency.common.teams.TeamSaveData;
import io.github.lightman314.lightmanscurrency.common.menus.slots.SimpleSlot;
import io.github.lightman314.lightmanscurrency.common.money.CoinValue;
import io.github.lightman314.lightmanscurrency.network.LightmansCurrencyPacketHandler;
import io.github.lightman314.lightmanscurrency.network.message.bank.MessageBankTransferPlayer;
import io.github.lightman314.lightmanscurrency.network.message.bank.MessageBankTransferTeam;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Items;

import javax.annotation.Nonnull;

public class TransferTab extends ATMTab {

	public TransferTab(ATMScreen screen) { super(screen); }
	
	//Response should be 100 ticks or 5 seconds
	public static final int RESPONSE_DURATION = 100;
	
	private int responseTimer = 0;
	
	CoinValueInput amountWidget;
	
	EditBox playerInput;
	TeamSelectWidget teamSelection;
	
	IconButton buttonToggleMode;
	EasyButton buttonTransfer;
	
	long selectedTeam = -1;
	
	boolean playerMode = true;
	
	@Nonnull
	@Override
	public IconData getIcon() { return IconAndButtonUtil.ICON_STORE_COINS; }

	@Override
	public MutableComponent getTooltip() { return EasyText.translatable("tooltip.lightmanscurrency.atm.transfer"); }

	@Override
	public void initialize(ScreenArea screenArea, boolean firstOpen) {
		
		SimpleSlot.SetInactive(this.screen.getMenu());
		
		this.responseTimer = 0;
		if(firstOpen)
			this.screen.getMenu().clearMessage();
		
		this.amountWidget = this.addChild(new CoinValueInput(screenArea.pos, EasyText.translatable("gui.lightmanscurrency.bank.transfertip"), CoinValue.EMPTY, this.getFont(), CoinValueInput.EMPTY_CONSUMER));
		this.amountWidget.allowFreeToggle = false;
		this.amountWidget.drawBG = false;
		
		this.buttonToggleMode = this.addChild(new IconButton(screenArea.pos.offset(screen.width - 30, 64), this::ToggleMode, () -> this.playerMode ? IconData.of(Items.PLAYER_HEAD) : IconAndButtonUtil.ICON_ALEX_HEAD)
				.withAddons(EasyAddonHelper.toggleTooltip(() -> this.playerMode, EasyText.translatable("tooltip.lightmanscurrency.atm.transfer.mode.team"), EasyText.translatable("tooltip.lightmanscurrency.atm.transfer.mode.player"))));
		
		this.playerInput = this.addChild(new EditBox(this.getFont(), screenArea.x + 10, screenArea.y + 104, screenArea.width - 20, 20, EasyText.empty()));
		this.playerInput.visible = this.playerMode;
		
		this.teamSelection = this.addChild(new TeamSelectWidget(screenArea.pos.offset(10, 84), 2, Size.NORMAL, this::getTeamList, this::selectedTeam, this::SelectTeam));
		this.teamSelection.visible = !this.playerMode;
		
		this.buttonTransfer = this.addChild(new EasyTextButton(screenArea.pos.offset(10, 126), screenArea.width - 20, 20, () -> EasyText.translatable(this.playerMode ? "gui.button.bank.transfer.player" : "gui.button.bank.transfer.team"), this::PressTransfer));
		this.buttonTransfer.active = false;
		
	}
	
	private List<Team> getTeamList()
	{
		List<Team> results = Lists.newArrayList();
		BankReference source = this.screen.getMenu().getBankAccountReference();
		Team blockTeam = null;
		if(source instanceof TeamBankReference teamBankReference)
			blockTeam = TeamSaveData.GetTeam(true, teamBankReference.teamID);
		for(Team team : TeamSaveData.GetAllTeams(true))
		{
			if(team.hasBankAccount() && team != blockTeam)
				results.add(team);
		}
		return results;
	}
	
	public Team selectedTeam()
	{
		if(this.selectedTeam >= 0)
			return TeamSaveData.GetTeam(true, this.selectedTeam);
		return null;
	}
	
	public void SelectTeam(int teamIndex)
	{
		try {
			Team team = this.getTeamList().get(teamIndex);
			if(team.getID() == this.selectedTeam)
				return;
			this.selectedTeam = team.getID();
		} catch(Throwable ignored) { }
	}
	
	private void PressTransfer(EasyButton button)
	{
		if(this.playerMode)
		{
			LightmansCurrencyPacketHandler.instance.sendToServer(new MessageBankTransferPlayer(this.playerInput.getValue(), this.amountWidget.getCoinValue()));
			this.playerInput.setValue("");
			this.amountWidget.setCoinValue(CoinValue.EMPTY);
		}
		else if(this.selectedTeam >= 0)
		{
			LightmansCurrencyPacketHandler.instance.sendToServer(new MessageBankTransferTeam(this.selectedTeam, this.amountWidget.getCoinValue()));
			this.amountWidget.setCoinValue(CoinValue.EMPTY);
		}
	}

	private void ToggleMode(EasyButton button) {
		this.playerMode = !this.playerMode;
		this.buttonTransfer.setMessage(EasyText.translatable(this.playerMode ? "gui.button.bank.transfer.player" : "gui.button.bank.transfer.team"));
		this.teamSelection.visible = !this.playerMode;
		this.playerInput.visible = this.playerMode;
		//this.buttonToggleMode.setIcon(this.playerMode ? IconData.of(Items.PLAYER_HEAD) : IconData.of(ItemRenderUtil.getAlexHead()));
	}
	
	@Override
	public void renderBG(@Nonnull EasyGuiGraphics gui) {
		
		this.hideCoinSlots(gui);
		
		//this.screen.getFont().draw(pose, this.getTooltip(), this.screen.getGuiLeft() + 8f, this.screen.getGuiTop() + 6f, 0x404040);
		Component balance = this.screen.getMenu().getBankAccount() == null ? EasyText.translatable("gui.lightmanscurrency.bank.null") : EasyText.translatable("gui.lightmanscurrency.bank.balance", this.screen.getMenu().getBankAccount().getCoinStorage().getString("0"));
		gui.drawString(balance, 8, 72, 0x404040);
		
		if(this.hasMessage())
		{
			//Draw a message background
			TextRenderUtil.drawCenteredMultilineText(gui, this.getMessage(), 2, this.screen.getXSize() - 4, 5, 0x404040);
			this.amountWidget.visible = false;
		}
		else
			this.amountWidget.visible = true;
	}
	
	@Override
	public void tick() {
		
		if(this.playerMode)
			this.buttonTransfer.active = !this.playerInput.getValue().isBlank() && this.amountWidget.getCoinValue().isValid();
		else
		{
			Team team = this.selectedTeam();
			this.buttonTransfer.active = team != null && team.hasBankAccount() && this.amountWidget.getCoinValue().isValid();
		}
		
		
		if(this.hasMessage())
		{
			this.responseTimer++;
			if(this.responseTimer >= RESPONSE_DURATION)
			{
				this.responseTimer = 0;
				this.screen.getMenu().clearMessage();
			}
		}
	}
	
	private boolean hasMessage() { return this.screen.getMenu().hasTransferMessage(); }
	
	private MutableComponent getMessage() { return this.screen.getMenu().getTransferMessage(); }

	@Override
	public void closeAction() {
		SimpleSlot.SetActive(this.screen.getMenu());
		this.responseTimer = 0;
		this.screen.getMenu().clearMessage();
	}

	@Override
	public boolean blockInventoryClosing() { return this.playerMode; }
	
}
