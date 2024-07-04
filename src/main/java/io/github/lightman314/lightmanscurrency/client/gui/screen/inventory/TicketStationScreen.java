package io.github.lightman314.lightmanscurrency.client.gui.screen.inventory;

import io.github.lightman314.lightmanscurrency.LCText;
import io.github.lightman314.lightmanscurrency.client.gui.easy.EasyMenuScreen;
import io.github.lightman314.lightmanscurrency.api.misc.client.rendering.EasyGuiGraphics;
import io.github.lightman314.lightmanscurrency.client.gui.easy.rendering.Sprite;
import io.github.lightman314.lightmanscurrency.client.gui.widget.ScrollListener;
import io.github.lightman314.lightmanscurrency.client.gui.widget.easy.EasyAddonHelper;
import io.github.lightman314.lightmanscurrency.client.gui.widget.easy.EasyButton;
import io.github.lightman314.lightmanscurrency.client.gui.widget.scroll.IScrollable;
import io.github.lightman314.lightmanscurrency.client.util.ScreenArea;
import io.github.lightman314.lightmanscurrency.common.crafting.TicketStationRecipe;
import io.github.lightman314.lightmanscurrency.api.misc.EasyText;

import io.github.lightman314.lightmanscurrency.client.gui.widget.button.PlainButton;
import io.github.lightman314.lightmanscurrency.common.menus.TicketStationMenu;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import net.minecraft.world.item.crafting.RecipeHolder;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class TicketStationScreen extends EasyMenuScreen<TicketStationMenu> implements IScrollable {

	public static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath(LightmansCurrency.MODID, "textures/gui/container/ticket_machine.png");

	public static final Sprite SPRITE_ARROW = Sprite.SimpleSprite(GUI_TEXTURE, 176, 0, 24, 16);

	private static final ScreenArea SELECTION_AREA = ScreenArea.of(153, 7, 16, 16);

	private RecipeHolder<TicketStationRecipe> selectedRecipe = null;
	public List<RecipeHolder<TicketStationRecipe>> getMatchingRecipes() { return this.menu.getAllRecipes().stream().filter(r -> r.value().matches(this.menu.blockEntity.getStorage(), this.menu.blockEntity.getLevel())).toList(); }

	public TicketStationScreen(TicketStationMenu container, Inventory inventory, Component title)
	{
		super(container, inventory, title);
		this.resize(176,138);
	}
	
	@Override
	protected void renderBG(@Nonnull EasyGuiGraphics gui)
	{

		gui.renderNormalBackground(GUI_TEXTURE, this);

		gui.drawString(this.title, 8, 6, 0x404040);
		gui.drawString(this.playerInventoryTitle, 8, (this.getYSize() - 94), 0x404040);

		if(this.selectedRecipe != null)
			gui.renderItem(this.selectedRecipe.value().peekAtResult(this.menu.blockEntity.getStorage()), SELECTION_AREA.pos);
		
	}

	@Override
	protected void renderAfterWidgets(@Nonnull EasyGuiGraphics gui) {
		//Render tooltip
		if(this.selectedRecipe != null && SELECTION_AREA.offsetPosition(this.getCorner()).isMouseInArea(gui.mousePos))
		{
			List<Component> tooltip = new ArrayList<>();
			tooltip.add(LCText.TOOLTIP_TICKET_STATION_RECIPE_INFO.get(this.selectedRecipe.value().peekAtResult(this.menu.blockEntity.getStorage()).getHoverName()));
			if(this.getMatchingRecipes().size() > 1)
				tooltip.add(LCText.TOOLTIP_TICKET_STATION_SELECT_RECIPE.get());
			gui.renderComponentTooltip(tooltip);
		}
	}

	@Override
	protected void initialize(ScreenArea screenArea)
	{
		this.addChild(new PlainButton(screenArea.x + 79, screenArea.y + 21, this::craftTicket, SPRITE_ARROW)
				.withAddons(
						EasyAddonHelper.visibleCheck(() -> this.menu.validInputs() && this.selectedRecipe != null && this.menu.roomForOutput(this.selectedRecipe.value())),
						EasyAddonHelper.tooltip(this::getArrowTooltip)
				));
		//Add scroll area for recipe selection.
		this.addChild(new ScrollListener(SELECTION_AREA.offsetPosition(screenArea.pos), this));
		this.validateSelectedRecipe();
	}
	@Override
	protected void screenTick() { this.validateSelectedRecipe(); }

	private Component getArrowTooltip()
	{
		if(this.selectedRecipe != null)
			return LCText.TOOLTIP_TICKET_STATION_CRAFT.get(this.selectedRecipe.value().peekAtResult(this.menu.blockEntity.getStorage()).getHoverName());
		return EasyText.empty();
	}

	private void validateSelectedRecipe()
	{
		//Auto-void and select matching recipes
		List<RecipeHolder<TicketStationRecipe>> matchingRecipes = this.getMatchingRecipes();
		if(this.selectedRecipe != null && !matchingRecipes.contains(this.selectedRecipe))
		{
			if(!matchingRecipes.isEmpty())
				this.selectedRecipe = matchingRecipes.getFirst();
			else
				this.selectedRecipe = null;
			return;
		}
		if(this.selectedRecipe == null && !matchingRecipes.isEmpty())
			this.selectedRecipe = matchingRecipes.getFirst();
	}

	private void craftTicket(EasyButton button) {
		this.validateSelectedRecipe();
		if(this.selectedRecipe == null)
			return;
		this.menu.SendCraftTicketsMessage(Screen.hasShiftDown(), this.selectedRecipe.id());
	}

	@Override
	public int currentScroll() {
		this.validateSelectedRecipe();
		if(this.selectedRecipe == null)
			return 0;
		return this.getMatchingRecipes().indexOf(this.selectedRecipe);
	}

	@Override
	public void setScroll(int newScroll) {
		List<RecipeHolder<TicketStationRecipe>> matchingRecipes = this.getMatchingRecipes();
		if(matchingRecipes.isEmpty())
			this.selectedRecipe = null;
		else if(newScroll < 0 || newScroll >= matchingRecipes.size())
			this.setScroll(0);
		else
			this.selectedRecipe = matchingRecipes.get(newScroll);
	}

    @Override
	public int getMaxScroll() { return this.getMatchingRecipes().size() - 1; }

}
