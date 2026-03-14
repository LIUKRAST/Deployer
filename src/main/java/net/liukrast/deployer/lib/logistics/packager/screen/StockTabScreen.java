package net.liukrast.deployer.lib.logistics.packager.screen;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.jei.CreateJEI;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestMenu;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.theme.Color;
import net.liukrast.deployer.lib.logistics.packager.AbstractInventorySummary;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.logistics.stockTicker.GenericOrderContained;
import net.liukrast.deployer.lib.mixin.accessors.StockKeeperRequestScreenAccessor;
import net.liukrast.deployer.lib.mixinExtensions.STBEExtension;
import net.liukrast.deployer.lib.registry.DeployerRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.*;

public abstract class StockTabScreen<K,V> extends KeeperTabScreen implements ProvidesOrder<V> {
    private static final List<Component> UNFINISHED_ORDER = List.of(
            Component.translatable("stock_inventory_type.unfinished_order").withStyle(style -> style.withColor(0x5391e1)),
            Component.translatable("stock_inventory_type.unfinished_order_line_1").withStyle(ChatFormatting.GRAY),
            Component.translatable("stock_inventory_type.unfinished_order_line_2").withStyle(ChatFormatting.GRAY)
    );
    protected final StockInventoryType<K, V, ?> type;

    public LerpedFloat itemScroll = LerpedFloat.linear()
            .startWithValue(0);

    protected final int rows = 9;
    protected final int cols = 9;
    protected final int rowHeight = 20;
    protected final int colWidth = 20;
    protected final Couple<Integer> noneHovered = Couple.create(-1, -1);
    protected static final int itemsX = 6;
    protected static final int itemsY = 17;
    protected int orderY;
    protected int lockX;
    protected int lockY;
    protected int windowWidth;
    protected int windowHeight;

    public EditBox searchBox;

    int emptyTicks = 0;

    public List<List<V>> currentItemSource;
    public List<List<V>> displayedItems = new ArrayList<>();
    public List<StockKeeperRequestScreenAccessor.CategoryEntryAccessor> categories = new ArrayList<>();

    public List<V> itemsToOrder = new ArrayList<>();

    private boolean ignoreTextInput;
    private boolean scrollHandleActive;

    public boolean refreshSearchNextTick = false;
    public boolean moveToTopNextTick = false;

    private final Set<Integer> hiddenCategories;
    protected AbstractInventorySummary<K,V> forcedEntries;

    public StockTabScreen(StockTickerBlockEntity blockEntity, StockKeeperRequestMenu menu, Component title, Item icon, StockInventoryType<K, V,?> type) {
        super(blockEntity, menu, title, icon);
        this.type = type;
        hiddenCategories =
                new HashSet<>(beAccess.getHiddenCategoriesByPlayer().getOrDefault(menu.player.getUUID(), List.of()));
        forcedEntries = type.networkHandler().createSummary();
    }

    public StockTabScreen(StockTickerBlockEntity blockEntity, StockKeeperRequestMenu menu, Item icon, StockInventoryType<K,V,?> type) {
        this(
                blockEntity, menu,
                Component.translatable(
                        "stock_inventory_type."
                                + Objects.requireNonNull(DeployerRegistries.STOCK_INVENTORY.getKey(type)).getNamespace()
                                + "." + Objects.requireNonNull(DeployerRegistries.STOCK_INVENTORY.getKey(type)).getPath()),
                icon,
                type
        );
    }

    /* IMPLEMENTATIONS */
    public abstract void renderTooltip(@NotNull GuiGraphics graphics, V entry, int mouseX, int mouseY);
    public abstract void renderEntry(@NotNull GuiGraphics graphics, int scale, V entry, boolean isStackHovered, boolean isRenderingOrders);
    public abstract int clickAmount(boolean ctrlDown, boolean shiftDown, boolean altDown);
    public int scrollAmount(boolean ctrlDown, boolean shiftDown, boolean altDown) {
        return clickAmount(ctrlDown, shiftDown, altDown);
    }

    public boolean matchesSearch(V stack, String value) {
        return false;
    }

    protected final void addCategory(StockKeeperRequestScreen.CategoryEntry categoryEntry) {
        categories.add((StockKeeperRequestScreenAccessor.CategoryEntryAccessor) categoryEntry);
    }

    @Override
    protected void init() {
        orderY = height-39;
        lockX = 186;
        lockY = 18;

        var id = DeployerRegistries.STOCK_INVENTORY.getKey(type);
        assert id != null;
        MutableComponent searchLabel = Component.translatable("stock_inventory_type." + id.getNamespace() + "." + id.getPath() + ".search");
        searchBox = new EditBox(new NoShadowFontWrapper(font), 53, 6, 100, 9, searchLabel);
        searchBox.setMaxLength(50);
        searchBox.setBordered(false);
        searchBox.setTextColor(0x4A2D31);
        addWidget(searchBox);
    }

    private void refreshSearchResults(boolean scrollBackUp) {
        displayedItems = Collections.emptyList();
        if (scrollBackUp)
            itemScroll.startWithValue(0);

        if (currentItemSource == null) {
            clampScrollBar();
            return;
        }

        categories = new ArrayList<>();

        for (int i = 0; i < beAccess.getCategories().size(); i++) {
            ItemStack stack = beAccess.getCategories().get(i);
            StockKeeperRequestScreen.CategoryEntry entry = new StockKeeperRequestScreen.CategoryEntry(i, stack.isEmpty() ? ""
                    : stack.getHoverName()
                    .getString(),
                    0);
            ((StockKeeperRequestScreenAccessor.CategoryEntryAccessor)entry).setHidden(hiddenCategories.contains(i));
            addCategory(entry);
        }

        StockKeeperRequestScreen.CategoryEntry unsorted = new StockKeeperRequestScreen.CategoryEntry(-1, CreateLang.translate("gui.stock_keeper.unsorted_category")
                .string(), 0);
        ((StockKeeperRequestScreenAccessor.CategoryEntryAccessor)unsorted).setHidden(hiddenCategories.contains(-1));
        addCategory(unsorted);

        String valueWithPrefix = searchBox.getValue();
        boolean anyItemsInCategory = false;

        // Nothing is being filtered out
        if (valueWithPrefix.isBlank()) {
            displayedItems = new ArrayList<>(currentItemSource);

            int categoryY = 0;
            for (int categoryIndex = 0; categoryIndex < currentItemSource.size(); categoryIndex++) {
                categories.get(categoryIndex).setY(categoryY);
                List<V> displayedItemsInCategory = displayedItems.get(categoryIndex);
                if (displayedItemsInCategory.isEmpty())
                    continue;
                if (categoryIndex < currentItemSource.size() - 1)
                    anyItemsInCategory = true;

                categoryY += rowHeight;
                if (!categories.get(categoryIndex).getHidden())
                    categoryY += (int) (Math.ceil(displayedItemsInCategory.size() / (float) cols) * rowHeight);
            }

            if (!anyItemsInCategory)
                categories.clear();

            clampScrollBar();
            return;
        }

        // Filter by search string
        displayedItems = new ArrayList<>();
        currentItemSource.forEach($ -> displayedItems.add(new ArrayList<>()));

        int categoryY = 0;
        for (int categoryIndex = 0; categoryIndex < displayedItems.size(); categoryIndex++) {
            List<V> category = currentItemSource.get(categoryIndex);
            categories.get(categoryIndex).setY(categoryY);

            if (displayedItems.size() <= categoryIndex)
                break;

            List<V> displayedItemsInCategory = displayedItems.get(categoryIndex);
            for (V stack : category) {
                if(matchesSearch(stack, valueWithPrefix))
                    displayedItemsInCategory.add(stack);
            }

            if (displayedItemsInCategory.isEmpty())
                continue;
            if (categoryIndex < currentItemSource.size() - 1)
                anyItemsInCategory = true;

            categoryY += rowHeight;

            if (!(categories.get(categoryIndex)).getHidden())
                categoryY += (int) (Math.ceil(displayedItemsInCategory.size() / (float) cols) * rowHeight);
        }

        if (!anyItemsInCategory)
            categories.clear();

        clampScrollBar();
    }

    @Override
    public void containerTick() {
        if(!forcedEntries.isEmpty()) {
            AbstractInventorySummary<K,V> summary = beAccess.deployer$getLastClientsideStockSnapshotAsSummary(type);
            for(V stack : forcedEntries.getStacks()) {
                int limitedAmount = -type.valueHandler().getCount(stack)-1;
                int actualAmount = summary.getCountOf(stack);
                if(actualAmount <= limitedAmount)
                    forcedEntries.erase(stack);
            }
        }

        boolean allEmpty = true;
        for(List<V> list : displayedItems)
            allEmpty &= list.isEmpty();
        if(allEmpty)
            emptyTicks++;
        else
            emptyTicks = 0;
        //TODO: Check if successTicks if needed

        List<List<V>> clientStockSnapshot = beAccess.deployer$getClientStockSnapshot(type);
        if(clientStockSnapshot != currentItemSource) {
            currentItemSource = clientStockSnapshot;
            refreshSearchResults(false);
            revalidateOrders();
        }

        if(refreshSearchNextTick) {
            refreshSearchNextTick = false;
            refreshSearchResults(moveToTopNextTick);
        }

        itemScroll.tickChaser();
        if (Math.abs(itemScroll.getValue() - itemScroll.getChaseTarget()) < 1 / 16f)
            itemScroll.setValue(itemScroll.getChaseTarget());
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        /* debug outline */
        //graphics.renderOutline(0,0, width, height, 0xFFFF00FF);
        //graphics.renderOutline(1, 1, width-2, height-39, 0xFF00FFFF);
        PoseStack ms = graphics.pose();
        float currentScroll = itemScroll.getValue(partialTicks);
        Couple<Integer> hoveredSlot = getHoveredSlot(mouseX, mouseY);

        // Render ordered items
        for (int index = 0; index < cols; index++) {
            if (itemsToOrder.size() <= index)
                break;

            V entry = itemsToOrder.get(index);
            boolean isStackHovered = index == hoveredSlot.getSecond() && hoveredSlot.getFirst() == -1;

            ms.pushPose();
            ms.translate(itemsX + index * colWidth, height-30, 0);
            renderEntry(graphics, 1, entry, isStackHovered, true);
            ms.popPose();
        }

        if (itemsToOrder.size() > 9) {
            graphics.drawString(font, Component.literal("[+" + (itemsToOrder.size() - 9) + "]"), windowWidth - 40,
                    height + 21, 0xF8F8EC);
        }

        graphics.enableScissor(getGuiLeft(), getGuiTop()+1, getGuiLeft()+width, getGuiTop()+height);

        ms.pushPose();
        ms.translate(0, -currentScroll * rowHeight, 0);

        // Search bar
        AllGuiTextures.STOCK_KEEPER_REQUEST_SEARCH.render(graphics, 42-18, 1);
        searchBox.render(graphics, mouseX, mouseY, partialTicks);
        if (searchBox.getValue()
                .isBlank() && !searchBox.isFocused())
            graphics.drawString(font, searchBox.getMessage(),
                    width / 2 - font.width(searchBox.getMessage()) / 2, searchBox.getY(), 0xff4A2D31, false);

        // Something isnt right
        boolean allEmpty = true;
        for (List<V> list : displayedItems)
            allEmpty &= list.isEmpty();
        if (allEmpty) {
            Component msg = getTroubleshootingMessage();
            float alpha = Mth.clamp((emptyTicks - 10f) / 5f, 0f, 1f);
            if (alpha > 0) {
                List<FormattedCharSequence> split = font.split(msg, 160);
                for (int i = 0; i < split.size(); i++) {
                    FormattedCharSequence sequence = split.get(i);
                    int lineWidth = font.width(sequence);
                    graphics.drawString(font, sequence, width / 2 - lineWidth / 2 + 1,
                            itemsY + 20 + 1 + i * (font.lineHeight + 1), new Color(0x4A2D31).setAlpha(alpha)
                                    .getRGB(),
                            false);
                    graphics.drawString(font, sequence, width / 2 - lineWidth / 2,
                            itemsY + 20 + i * (font.lineHeight + 1), new Color(0xF8F8EC).setAlpha(alpha)
                                    .getRGB(),
                            false);
                }
            }
        }

        // Items
        for (int categoryIndex = 0; categoryIndex < displayedItems.size(); categoryIndex++) {
            List<V> category = displayedItems.get(categoryIndex);
            var categoryEntry = categories.isEmpty() ? null : categories.get(categoryIndex);
            int categoryY = categories.isEmpty() ? 0 : categoryEntry.getY();
            if (category.isEmpty())
                continue;

            if (!categories.isEmpty()) {
                (categoryEntry.getHidden() ? AllGuiTextures.STOCK_KEEPER_CATEGORY_HIDDEN
                        : AllGuiTextures.STOCK_KEEPER_CATEGORY_SHOWN).render(graphics, 6, 17 + categoryY + 6);
                graphics.drawString(font, categoryEntry.getName(), 6 + 10, 17 + categoryY + 8, 0x4A2D31, false);
                graphics.drawString(font, categoryEntry.getName(), 6 + 9, 17 + categoryY + 7, 0xF8F8EC, false);
                if (categoryEntry.getHidden())
                    continue;
            }

            for (int index = 0; index < category.size(); index++) {
                int pY = 17 + categoryY + (categories.isEmpty() ? 4 : rowHeight) + (index / cols) * rowHeight;
                float cullY = pY - currentScroll * rowHeight;

                if (cullY < 0)
                    continue;
                if (cullY > height - 39)
                    break;

                boolean isStackHovered = index == hoveredSlot.getSecond() && categoryIndex == hoveredSlot.getFirst();
                V entry = category.get(index);

                ms.pushPose();
                ms.translate(6 + (index % cols) * colWidth, pY, 0);
                renderEntry(graphics, 1, entry, isStackHovered, false);
                ms.popPose();
            }
        }

        ms.popPose();
        graphics.disableScissor();

        // Scroll bar
        int windowH = height-39;
        int totalH = getMaxScroll() * rowHeight + windowH;
        int barSize = Math.max(5, Mth.floor((float) windowH / totalH * (windowH - 2)));
        if (barSize < windowH - 2) {
            int barX = width-4;
            int barY = 0;
            ms.pushPose();
            ms.translate(0, (currentScroll * rowHeight) / totalH * (windowH - 2), 0);
            AllGuiTextures pad = AllGuiTextures.STOCK_KEEPER_REQUEST_SCROLL_PAD;
            graphics.blit(pad.location, barX, barY, pad.getWidth(), barSize, pad.getStartX(), pad.getStartY(),
                    pad.getWidth(), pad.getHeight(), 256, 256);
            AllGuiTextures.STOCK_KEEPER_REQUEST_SCROLL_TOP.render(graphics, barX, barY);
            if (barSize > 16)
                AllGuiTextures.STOCK_KEEPER_REQUEST_SCROLL_MID.render(graphics, barX, barY + barSize / 2 - 4);
            AllGuiTextures.STOCK_KEEPER_REQUEST_SCROLL_BOT.render(graphics, barX, barY + barSize - 5);
            ms.popPose();
        }

        //TODO: JEI Imported?

        // Render tooltip of hovered item
        if (hoveredSlot != noneHovered) {
            int slot = hoveredSlot.getSecond();
            boolean orderHovered = hoveredSlot.getFirst() == -1;
            V entry = orderHovered ? itemsToOrder.get(slot)
            : displayedItems.get(hoveredSlot.getFirst())
            .get(slot);
            renderTooltip(graphics, entry, mouseX, mouseY);
        }


    }

    @Nullable
    private V getOrderForItem(V stack) {
        for (V entry : itemsToOrder)
            if(type.valueHandler().hashStrategy().equals(stack, entry))
                return entry;
        return null;
    }

    private void revalidateOrders() {
        Set<V> invalid = new HashSet<>(itemsToOrder);
        AbstractInventorySummary<K,V> summary = ((STBEExtension)blockEntity).deployer$getLastClientsideStockSnapshotAsSummary(type);
        if(currentItemSource == null || summary == null) {
            itemsToOrder.removeAll(invalid);
            return;
        }
        for(V entry : itemsToOrder) {
            type.valueHandler().setCount(entry, Math.min(summary.getCountOf(entry), type.valueHandler().getCount(entry)));
            if(type.valueHandler().getCount(entry) > 0)
                invalid.remove(entry);
        }
        itemsToOrder.removeAll(invalid);
    }

    private Couple<Integer> getHoveredSlot(int x, int y) {
        x += 1;
        if (x < itemsX || x >= itemsX + cols * colWidth)
            return noneHovered;

        // Ordered item is hovered
        if (y >= height-30 && y < height-30 + rowHeight) {
            int col = (x - itemsX) / colWidth;
            if (itemsToOrder.size() <= col)
                return noneHovered;
            return Couple.create(-1, col);
        }

        if (y < 16 || y > height-39)
            return noneHovered;
        if (!itemScroll.settled())
            return noneHovered;

        int localY = y - itemsY;

        for (int categoryIndex = 0; categoryIndex < displayedItems.size(); categoryIndex++) {
            StockKeeperRequestScreenAccessor.CategoryEntryAccessor entry = categories.isEmpty() ? (StockKeeperRequestScreenAccessor.CategoryEntryAccessor)new StockKeeperRequestScreen.CategoryEntry(0, "", 0) : categories.get(categoryIndex);
            if (entry.getHidden())
                continue;

            int row = Mth.floor((localY - (categories.isEmpty() ? 4 : rowHeight) - entry.getY()) / (float) rowHeight
                    + itemScroll.getChaseTarget());

            int col = (x - itemsX) / colWidth;
            int slot = row * cols + col;

            if (slot < 0)
                return noneHovered;
            if (displayedItems.get(categoryIndex)
                    .size() <= slot)
                continue;

            return Couple.create(categoryIndex, slot);
        }

        return noneHovered;
    }

    //TODO: JEI -> getHoveredIngredient

    private Component getTroubleshootingMessage() {
        if (currentItemSource == null)
            return CreateLang.translate("gui.stock_keeper.checking_stocks")
                    .component();
        if (beAccess.getActiveLinks() == 0)
            return CreateLang.translate("gui.stock_keeper.no_packagers_linked")
                    .component();
        if (currentItemSource.isEmpty())
            return CreateLang.translate("gui.stock_keeper.inventories_empty")
                    .component();
        return CreateLang.translate("gui.stock_keeper.no_search_results")
                .component();
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        assert minecraft != null;
        boolean lmb = pButton == GLFW.GLFW_MOUSE_BUTTON_LEFT;
        boolean rmb = pButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT;

        if (rmb && searchBox.isMouseOver(pMouseX, pMouseY)) {
            searchBox.setValue("");
            refreshSearchNextTick = true;
            moveToTopNextTick = true;
            searchBox.setFocused(true);
            syncJEI();
            return true;
        }

        if (searchBox.isFocused()) {
            if (searchBox.isHovered())
                return searchBox.mouseClicked(pMouseX, pMouseY, pButton);
            searchBox.setFocused(false);
        }

        // Scroll bar
        int barX = itemsX + cols * colWidth - 1;
        if (getMaxScroll() > 0 && lmb && pMouseX > barX && pMouseX <= barX + 8 && pMouseY > 15
                && pMouseY < windowHeight - 82) {
            scrollHandleActive = true;
            if (minecraft.isWindowActive())
                GLFW.glfwSetInputMode(minecraft.getWindow()
                        .getWindow(), 208897, GLFW.GLFW_CURSOR_HIDDEN);
            return true;
        }

        Couple<Integer> hoveredSlot = getHoveredSlot((int) pMouseX, (int) pMouseY);
        // Category hiding
        int localY = (int) (pMouseY - itemsY);
        if (itemScroll.settled() && lmb && !categories.isEmpty() && pMouseX >= itemsX
                && pMouseX < itemsX + cols * colWidth && pMouseY >= 16
                && pMouseY <= height-39) {
            for (int categoryIndex = 0; categoryIndex < displayedItems.size(); categoryIndex++) {
                StockKeeperRequestScreenAccessor.CategoryEntryAccessor entry = categories.get(categoryIndex);
                if (Mth.floor((localY - entry.getY()) / (float) rowHeight + itemScroll.getChaseTarget()) != 0)
                    continue;
                if (displayedItems.get(categoryIndex)
                        .isEmpty())
                    continue;
                int indexOf = entry.getTargetBECategory();
                if (indexOf >= beAccess.getCategories().size())
                    continue;

                if (!entry.getHidden()) {
                    hiddenCategories.add(indexOf);
                    playUiSound(SoundEvents.ITEM_FRAME_ROTATE_ITEM, 1f, 1.5f);
                } else {
                    hiddenCategories.remove(indexOf);
                    playUiSound(SoundEvents.ITEM_FRAME_ROTATE_ITEM, 1f, 0.675f);
                }

                refreshSearchNextTick = true;
                moveToTopNextTick = false;
                return true;
            }
        }

        if (hoveredSlot == noneHovered || !lmb && !rmb)
            return super.mouseClicked(pMouseX, pMouseY, pButton);

        // Items
        boolean orderClicked = hoveredSlot.getFirst() == -1;
        V entry = orderClicked ? itemsToOrder.get(hoveredSlot.getSecond())
        : displayedItems.get(hoveredSlot.getFirst())
        .get(hoveredSlot.getSecond());

        int transfer = clickAmount(hasControlDown(), hasShiftDown(), hasAltDown());

        V existingOrder = getOrderForItem(entry);

        if (existingOrder == null) {
            if (itemsToOrder.size() >= cols || rmb)
                return true;

            existingOrder = type.valueHandler().copyWithCount(entry, 0);
            itemsToOrder.add(existingOrder);

            playUiSound(SoundEvents.WOOL_STEP, 0.75f, 1.2f);
            playUiSound(SoundEvents.BAMBOO_WOOD_STEP, 0.75f, 0.8f);
        }

        int current = type.valueHandler().getCount(existingOrder);

        if (rmb || orderClicked) {
            int newCount = current - transfer;

            if (newCount <= 0) {
                itemsToOrder.remove(existingOrder);

                playUiSound(SoundEvents.WOOL_STEP, 0.75f, 1.8f);
                playUiSound(SoundEvents.BAMBOO_WOOD_STEP, 0.75f, 1.8f);
            } else {
                type.valueHandler().setCount(existingOrder, newCount);
            }

            return true;
        }

        type.valueHandler().setCount(existingOrder, current + Math.min(transfer, type.valueHandler().getCount(entry) - current));
        return true;
    }

    @Override
    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        assert minecraft != null;
        if (pButton == GLFW.GLFW_MOUSE_BUTTON_LEFT && scrollHandleActive) {
            scrollHandleActive = false;
            if (minecraft.isWindowActive())
                GLFW.glfwSetInputMode(minecraft.getWindow()
                        .getWindow(), 208897, GLFW.GLFW_CURSOR_NORMAL);
        }
        return super.mouseReleased(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Couple<Integer> hoveredSlot = getHoveredSlot((int) mouseX, (int) mouseY);
        boolean noHover = hoveredSlot == noneHovered;

        if (noHover || hoveredSlot.getFirst() >= 0 && !hasShiftDown() && getMaxScroll() != 0) {
            int maxScroll = getMaxScroll();
            int direction = (int) (Math.ceil(Math.abs(scrollY)) * -Math.signum(scrollY));
            float newTarget = Mth.clamp(Math.round(itemScroll.getChaseTarget() + direction), 0, maxScroll);
            itemScroll.chase(newTarget, 0.5, LerpedFloat.Chaser.EXP);
            return true;
        }

        boolean orderClicked = hoveredSlot.getFirst() == -1;
        V entry = orderClicked ? itemsToOrder.get(hoveredSlot.getSecond())
        : displayedItems.get(hoveredSlot.getFirst())
        .get(hoveredSlot.getSecond());

        boolean remove = scrollY < 0;
        int transfer = scrollAmount(hasControlDown(), hasShiftDown(), hasAltDown());

        V existingOrder = orderClicked ? entry : getOrderForItem(entry);

        if (existingOrder == null) {
            if (itemsToOrder.size() >= cols || remove)
                return true;

            existingOrder = type.valueHandler().copyWithCount(entry, 0);
            itemsToOrder.add(existingOrder);

            playUiSound(SoundEvents.WOOL_STEP, 0.75f, 1.2f);
            playUiSound(SoundEvents.BAMBOO_WOOD_STEP, 0.75f, 0.8f);
        }

        int current = type.valueHandler().getCount(existingOrder);

        if (remove) {
            int newCount = current - transfer;

            if (newCount <= 0) {
                itemsToOrder.remove(existingOrder);

                playUiSound(SoundEvents.WOOL_STEP, 0.75f, 1.8f);
                playUiSound(SoundEvents.BAMBOO_WOOD_STEP, 0.75f, 1.8f);
            } else {
                type.valueHandler().setCount(existingOrder, newCount);

                if (newCount != current)
                    playUiSound(AllSoundEvents.SCROLL_VALUE.getMainEvent(), 0.25f, 1.2f);
            }

            return true;
        }

        int stock = beAccess.deployer$getLastClientsideStockSnapshotAsSummary(type).getCountOf(entry);
        int add = Math.min(transfer, stock - current);
        int newCount = current + Math.max(add, 0);

        type.valueHandler().setCount(existingOrder, newCount);

        if (newCount != current && current != 0)
            playUiSound(AllSoundEvents.SCROLL_VALUE.getMainEvent(), 0.25f, 1.2f);

        return true;
    }

    private void clampScrollBar() {
        int maxScroll = getMaxScroll();
        float prevTarget = itemScroll.getChaseTarget();
        float newTarget = Mth.clamp(prevTarget, 0, maxScroll);
        if (prevTarget != newTarget)
            itemScroll.startWithValue(newTarget);
    }

    private int getMaxScroll() {
        int visibleHeight = height-39;
        int totalRows = 2;
        for (int i = 0; i < displayedItems.size(); i++) {
            List<V> list = displayedItems.get(i);
            if (list.isEmpty())
                continue;
            totalRows++;
            if (categories.size() > i && categories.get(i).getHidden())
                continue;
            totalRows += (int) Math.ceil(list.size() / (float) cols);
        }
        return Math.max(0, (totalRows * rowHeight - visibleHeight + 50) / rowHeight);
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        assert minecraft != null;
        if (pButton != GLFW.GLFW_MOUSE_BUTTON_LEFT || !scrollHandleActive)
            return super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);

        Window window = minecraft.getWindow();
        double scaleX = window.getGuiScaledWidth() / (double) window.getScreenWidth();
        double scaleY = window.getGuiScaledHeight() / (double) window.getScreenHeight();

        int windowH = windowHeight - 92;
        int totalH = getMaxScroll() * rowHeight + windowH;
        int barSize = Math.max(5, Mth.floor((float) windowH / totalH * (windowH - 2)));

        int minY = 15 + barSize / 2;
        int maxY = 15 + windowH - barSize / 2;

        if (barSize >= windowH - 2)
            return true;

        int barX = itemsX + cols * colWidth;
        double target = (pMouseY - 15 - barSize / 2.0) * totalH / (windowH - 2) / rowHeight;
        itemScroll.chase(Mth.clamp(target, 0, getMaxScroll()), 0.8, LerpedFloat.Chaser.EXP);

        if (minecraft.isWindowActive()) {
            double forceX = (barX + 2) / scaleX;
            double forceY = Mth.clamp(pMouseY, minY, maxY) / scaleY;
            GLFW.glfwSetCursorPos(window.getWindow(), forceX, forceY);
        }

        return true;
    }

    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        if (ignoreTextInput)
            return false;
        String s = searchBox.getValue();
        if (!searchBox.charTyped(pCodePoint, pModifiers))
            return false;
        if (!Objects.equals(s, searchBox.getValue())) {
            refreshSearchNextTick = true;
            moveToTopNextTick = true;
            syncJEI();
        }
        return true;
    }

    @Override
    public void switchFocused() {
        ignoreTextInput = true;
        searchBox.setFocused(true);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        ignoreTextInput = false;

        if (pKeyCode == GLFW.GLFW_KEY_ENTER && searchBox.isFocused()) {
            searchBox.setFocused(false);
            return true;
        }

        String s = searchBox.getValue();
        if (!searchBox.keyPressed(pKeyCode, pScanCode, pModifiers)) {
            return searchBox.isFocused() && searchBox.isVisible() && pKeyCode != 256
                    || super.keyPressed(pKeyCode, pScanCode, pModifiers);
        }
        if (!Objects.equals(s, searchBox.getValue())) {
            refreshSearchNextTick = true;
            moveToTopNextTick = true;
            syncJEI();
        }
        return true;
    }

    @Override
    public boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
        ignoreTextInput = false;
        return super.keyReleased(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public @Nullable GenericOrderContained<V> addToSendQueue() {
        revalidateOrders();
        if (itemsToOrder.isEmpty()) return null;
        forcedEntries = type.networkHandler().createSummary();
        AbstractInventorySummary<K, V> summary = ((STBEExtension) blockEntity).deployer$getLastClientsideStockSnapshotAsSummary(type);
        for (V value : itemsToOrder) {
            int countOf = summary.getCountOf(value);
            //TODO: Check for max count
            forcedEntries.add(type.valueHandler().copy(value), -1 - Math.max(0, countOf - type.valueHandler().getCount(value)));
        }
        return type.valueHandler().createContained(itemsToOrder);
    }

    @Override
    public @NotNull StockInventoryType<K, V, ?> getType() {
        return type;
    }

    @Override
    public List<Component> getWarnTooltip() {
        return itemsToOrder.isEmpty() ? null : UNFINISHED_ORDER;
    }

    @Override
    public void onSendIt() {
        itemsToOrder = new ArrayList<>();
    }

    private void syncJEI() {
        if (Mods.JEI.isLoaded() && AllConfigs.client().syncJeiSearch.get())
            CreateJEI.runtime.getIngredientFilter().setFilterText(searchBox.getValue());
    }

    protected void playUiSound(SoundEvent sound, float volume, float pitch) {
        Minecraft.getInstance()
                .getSoundManager()
                .play(SimpleSoundInstance.forUI(sound, pitch, volume * 0.25f));
    }
}
