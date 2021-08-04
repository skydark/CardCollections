package me.skydark.card_collections.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import me.skydark.card_collections.Mod;
import me.skydark.card_collections.data.CardCollectionData;
import me.skydark.card_collections.data.CardData;
import me.skydark.card_collections.init.ModMessages;
import me.skydark.card_collections.item.CardItem;
import me.skydark.card_collections.network.CPopCardFromCollectionBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.list.ExtendedList;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class CollectionBookGuiScreen extends Screen {
    private static final ResourceLocation GUI_BACKGROUND = new ResourceLocation(Mod.MOD_ID, "textures/gui/collection_book.png");
    private static final ResourceLocation GUI_NOTHING = new ResourceLocation(Mod.MOD_ID, "textures/gui/collection_book_nothing.png");
    private static final ResourceLocation GUI_MISSING = new ResourceLocation(Mod.MOD_ID, "textures/gui/missing.png");

    private static final int GUI_WIDTH = 308;
    private static final int GUI_HEIGHT = 228;
    private static final int GUI_CARD_SIZE = 148;
    private static final int GUI_CARD_BORDER = 2;
    private static final int GUI_DIM_PANEL_WIDTH = 144;

    private final PlayerEntity playerEntity;
    private final ItemStack collectionBook;
    private final Hand hand;
    @Nullable
    private final CardCollectionData collectionData;
    private final List<String> dimensions = new ArrayList<>();

    private List<CardData> cardDataList = new ArrayList<>();
    private CollectionBookGuiScreen.CardList cardList;

    @Nullable
    private CardData activeCard;

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public CollectionBookGuiScreen(PlayerEntity player, ItemStack collectionBook, Hand handIn) {
        super(StringTextComponent.EMPTY);
        this.playerEntity = player;
        this.collectionBook = collectionBook;
        this.hand = handIn;

        this.collectionData = CardCollectionData.getCollectionData(collectionBook);
        if (this.collectionData != null) {
            CompoundNBT compoundnbt = collectionBook.getTag();
            if (compoundnbt != null) {
                ListNBT listnbt = compoundnbt.getList("cards", 8).copy();

                for (int i = 0; i < listnbt.size(); ++i) {
                    String cardId = listnbt.getString(i);
                    CardData cardData = collectionData.getCard(cardId);
                    if (cardData == null) {
                        Mod.LOGGER.warn("Invalid card {} in collection[{}]", cardId, i);
                        continue;
                    }
                    this.cardDataList.add(cardData);
                }
            }
            this.dimensions.addAll(new TreeSet<>(this.collectionData.dimensions.keySet()));
        }
    }

    @Override
    protected void init() {
        super.init();
        int GUI_LEFT = (this.width - GUI_WIDTH) / 2;
        int GUI_TOP = (this.height - GUI_HEIGHT) / 2;

        this.activeCard = this.cardDataList.size() > 0 ? this.cardDataList.get(0) : null;

        this.cardList = new CardList(Minecraft.getInstance(), GUI_LEFT, GUI_WIDTH - GUI_CARD_SIZE - 2, GUI_CARD_SIZE - 32, GUI_TOP + 16, 12);
        this.addListener(this.cardList);
        //this.setListenerDefault(this.cardList);

        this.addButton(new Button(GUI_LEFT + GUI_WIDTH - 32, GUI_TOP, 32, 20, DialogTexts.GUI_BACK, (btn) -> this.closeScreen()));
        this.addButton(new Button(GUI_LEFT + GUI_WIDTH - 32, GUI_TOP + GUI_CARD_SIZE - 16, 32, 20, new TranslationTextComponent("gui.card_collections.button.pop"), (btn) -> {
            if (this.activeCard != null) {
                Mod.LOGGER.debug("Pop card: {}", this.activeCard.getResourceLocation());
                this.popCard();
            }
        }));

        this.addButton(new Button(
                GUI_LEFT + 4, GUI_TOP + GUI_CARD_SIZE + 5, 12, 9, new StringTextComponent("+"), (btn) -> {
            this.sortByName(true);
            this.cardList.centerScrollOn(this.activeCard);
        }));
        this.addButton(new Button(
                GUI_LEFT + 16, GUI_TOP + GUI_CARD_SIZE + 5, 12, 9, new StringTextComponent("-"), (btn) -> {
            this.sortByName(false);
            this.cardList.centerScrollOn(this.activeCard);
        }));
        for (int i = 0; i < dimensions.size(); i++) {
            String dimKey = dimensions.get(i);
            this.addButton(new Button(
                    GUI_LEFT + 4, GUI_TOP + GUI_CARD_SIZE + 9 * (i + 1) + 5, 12, 9, new StringTextComponent("+"), (btn) -> {
                this.sort(dimKey, true);
                this.cardList.centerScrollOn(this.activeCard);
            }));
            this.addButton(new Button(
                    GUI_LEFT + 16, GUI_TOP + GUI_CARD_SIZE + 9 * (i + 1) + 5, 12, 9, new StringTextComponent("-"), (btn) -> {
                this.sort(dimKey, false);
                this.cardList.centerScrollOn(this.activeCard);
            }));
        }
    }

    private void sortByName(boolean asc) {
        Mod.LOGGER.debug("Sort by {}, order: {}", "name", asc ? "ASC" : "DSC");
        this.cardDataList.sort((o1, o2) -> {
            String value1 = I18n.format(o1.getTranslationKey());
            String value2 = I18n.format(o2.getTranslationKey());
            return asc ? value1.compareToIgnoreCase(value2) : value2.compareToIgnoreCase(value1);
        });
        this.cardList.refill(this.cardDataList);
    }

    private void sort(String dim, boolean asc) {
        Mod.LOGGER.debug("Sort by {}, order: {}", dim, asc ? "ASC" : "DSC");
        this.cardDataList.sort((o1, o2) -> {
            Integer value1 = o1.dimensions.get(dim);
            Integer value2 = o2.dimensions.get(dim);
            if (value1 == null) value1 = 0;
            if (value2 == null) value2 = 0;
            return asc ? value1 - value2 : value2 - value1;
        });
        this.cardList.refill(this.cardDataList);
    }

    private void popCard() {
        if (this.activeCard == null) {
            Mod.LOGGER.info("No active card to pop.");
            return;
        }
        CompoundNBT compoundNBT = this.collectionBook.getTag();
        if (compoundNBT == null) {
            Mod.LOGGER.warn("Cannot pop card from collection book without nbt");
            return;
        }
        String popCardId = this.activeCard.getCardId();
        ListNBT listNBT = compoundNBT.getList("cards", 8).copy();
        ListNBT listNBTNew = new ListNBT();
        boolean founded = false;
        for (int i = 0; i < listNBT.size(); ++i) {
            String cardId = listNBT.getString(i);
            if (popCardId.equals(cardId)) {
                founded = true;
                continue;
            }
            listNBTNew.add(StringNBT.valueOf(cardId));
        }
        if (!founded) return;

        this.collectionBook.setTagInfo("cards", listNBTNew);
        this.playerEntity.entityDropItem(CardItem.createStack(this.activeCard), 0.0F);

        int slot = this.hand == Hand.MAIN_HAND ? this.playerEntity.inventory.currentItem : 40;
        ModMessages.sendToServer(new CPopCardFromCollectionBook(slot, popCardId));

        this.cardDataList = this.cardDataList.stream().filter(card -> !popCardId.equals(card.getCardId())).collect(Collectors.toList());
        this.cardList.refill(this.cardDataList);
        this.activeCard = null;
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        int GUI_LEFT = (this.width - GUI_WIDTH) / 2;
        int GUI_TOP = (this.height - GUI_HEIGHT) / 2;

        // draw background
        if (this.minecraft == null) {
            return;
        }
        this.minecraft.getTextureManager().bindTexture(GUI_BACKGROUND);
        blit(matrixStack, GUI_LEFT, GUI_TOP, this.getBlitOffset(), 0, 0, GUI_WIDTH, GUI_HEIGHT, GUI_HEIGHT, GUI_WIDTH);
        // draw card list panel, TODO: search
        this.cardList.render(matrixStack, 0, 0, 0);
        // draw dimension panel texts, sort buttons done in init
        if (this.collectionData != null) {
            this.font.drawText(matrixStack, new TranslationTextComponent("gui.card_collections.text.name"),
                    GUI_LEFT + 30, GUI_TOP + GUI_CARD_SIZE + 5, 0x0010F0);
            for (int i = 0; i < dimensions.size(); i++) {
                String dimKey = dimensions.get(i);
                this.font.drawText(matrixStack, new TranslationTextComponent(this.collectionData.getTranslationKeyOfDim(dimKey)),
                        GUI_LEFT + 30, GUI_TOP + GUI_CARD_SIZE + 9 * (i + 1) + 5, 0x0010F0);
                if (this.activeCard != null) {
                    Integer value = this.activeCard.dimensions.get(dimKey);
                    this.font.drawText(matrixStack, new TranslationTextComponent(this.collectionData.getTranslationKeyOfDimValue(dimKey, value)),
                            GUI_LEFT + 80, GUI_TOP + GUI_CARD_SIZE + 9 * (i + 1) + 5, 0x000000);
                }
            }
        }
        // draw card
        this.minecraft.getTextureManager().bindTexture(this.activeCard != null ? this.activeCard.getTexture() : GUI_MISSING);
        blit(matrixStack, GUI_LEFT + GUI_WIDTH - GUI_CARD_SIZE + GUI_CARD_BORDER, GUI_TOP + GUI_CARD_BORDER, this.getBlitOffset(), 0, 0,
                GUI_CARD_SIZE - GUI_CARD_BORDER * 2, GUI_CARD_SIZE - GUI_CARD_BORDER * 2,
                GUI_CARD_SIZE - GUI_CARD_BORDER * 2, GUI_CARD_SIZE - GUI_CARD_BORDER * 2);
        // draw desc
        if (this.collectionData == null || this.activeCard == null) {
            this.font.func_238418_a_(new TranslationTextComponent("gui.card_collections.text.no_active_card"),
                    GUI_LEFT + GUI_DIM_PANEL_WIDTH + 2, GUI_TOP + GUI_CARD_SIZE + 5, GUI_WIDTH - GUI_DIM_PANEL_WIDTH - 4, 0);
        } else {
            this.font.func_238418_a_(new TranslationTextComponent(this.activeCard.getTranslationKeyOfDesc()),
                    GUI_LEFT + GUI_DIM_PANEL_WIDTH + 2, GUI_TOP + GUI_CARD_SIZE + 5, GUI_WIDTH - GUI_DIM_PANEL_WIDTH - 4, 0);
        }
        // draw pop / close button, done in init

        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    public void setSelected(CardData cardData) {
        this.activeCard = cardData;
    }

    @OnlyIn(Dist.CLIENT)
    class CardList extends ExtendedList<CollectionBookGuiScreen.CardList.CardEntry> {
        public CardList(Minecraft mcIn, int left, int widthIn, int heightIn, int topIn, int slotHeightIn) {
            super(mcIn, widthIn, heightIn, topIn, topIn + heightIn, slotHeightIn);
            this.setLeftPos(left);
            this.func_244605_b(false);
            this.func_244606_c(false);

            String activeCardId = CollectionBookGuiScreen.this.activeCard == null ? "" : CollectionBookGuiScreen.this.activeCard.getCardId();
            for (CardData cardData : CollectionBookGuiScreen.this.cardDataList) {
                CardEntry cardEntry = new CardEntry(cardData);
                this.addEntry(cardEntry);
                if (activeCardId.equals(cardData.getCardId())) {
                    this.setSelected(cardEntry);
                }
            }

            if (this.getSelected() != null) {
                this.centerScrollOn(this.getSelected());
            }
        }

        public void centerScrollOn(@Nullable CardData cardData) {
            if (cardData != null) {
                String cardId = cardData.getCardId();
                List<CardEntry> entries = this.getEventListeners();
                for (int i = 0; i < entries.size(); i++) {
                    CardEntry entry = entries.get(i);
                    if (cardId.equals(entry.cardData.getCardId())) {
                        this.setScrollAmount(i * this.itemHeight + this.itemHeight / 2.0 - (this.y1 - this.y0) / 2.0);
                        this.setSelected(entry);
                        return;
                    }
                }
                Mod.LOGGER.debug("CardData not found in scroll, ignore: {}", cardId);
            }
            this.setScrollAmount(0);
        }

        public void refill(List<CardData> cardDataList) {
            replaceEntries(cardDataList.stream().map(CardEntry::new).collect(Collectors.toList()));
        }

        public int getRowWidth() {
            return this.width;
        }

        protected int getScrollbarPosition() {
            return this.x1 - 6;
        }

        public void setSelected(@Nullable CollectionBookGuiScreen.CardList.CardEntry entry) {
            super.setSelected(entry);
            if (entry != null) {
                NarratorChatListener.INSTANCE.say((new TranslationTextComponent("narrator.select", entry.cardData.getCardId())).getString());
                Mod.LOGGER.debug("Selected {}", entry.cardData.getResourceLocation());
                CollectionBookGuiScreen.this.setSelected(entry.cardData);
            }
        }

        protected void renderBackground(MatrixStack matrixStack) {
            //CollectionBookGuiScreen.this.renderBackground(matrixStack);
        }

        @Override
        protected void renderList(MatrixStack matrixStack, int x, int y, int mouseX, int mouseY, float partialTicks) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();
            this.minecraft.getTextureManager().bindTexture(GUI_NOTHING);
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            bufferbuilder.pos(this.x0, this.y1, 0.0D).tex(this.x0 / 32.0F, (this.y1 + (int)this.getScrollAmount()) / 32.0F).color(32, 32, 32, 255).endVertex();
            bufferbuilder.pos(this.x1, this.y1, 0.0D).tex(this.x1 / 32.0F, (this.y1 + (int)this.getScrollAmount()) / 32.0F).color(32, 32, 32, 255).endVertex();
            bufferbuilder.pos(this.x1, this.y0, 0.0D).tex(this.x1 / 32.0F, (this.y0 + (int)this.getScrollAmount()) / 32.0F).color(32, 32, 32, 255).endVertex();
            bufferbuilder.pos(this.x0, this.y0, 0.0D).tex(this.x0 / 32.0F, (this.y0 + (int)this.getScrollAmount()) / 32.0F).color(32, 32, 32, 255).endVertex();
            tessellator.draw();

            super.renderList(matrixStack, x, y, mouseX, mouseY, partialTicks);

            this.minecraft.getTextureManager().bindTexture(GUI_NOTHING);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(519);
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);

            int border = itemHeight;
            int z = -100;
            int r = 0XFF; int g = 0xFF; int b = 0xFF;
            addVertex(bufferbuilder, x0, y0, z, 0.0F, 1.0F, r, g, b, 255);
            addVertex(bufferbuilder, x1, y0, z, 1.0F, 1.0F, r, g, b, 255);
            addVertex(bufferbuilder, x1, y0 - border, z, 1.0F, 0.0F, r, g, b, 255);
            addVertex(bufferbuilder, x0, y0 - border, z, 0.0F, 0.0F, r, g, b, 255);

            addVertex(bufferbuilder, x0, y1 + border, z, 0.0F, 1.0F, r, g, b, 255);
            addVertex(bufferbuilder, x1, y1 + border, z, 1.0F, 1.0F, r, g, b, 255);
            addVertex(bufferbuilder, x1, y1, z, 1.0F, 0.0F, r, g, b, 255);
            addVertex(bufferbuilder, x0, y1, z, 0.0F, 0.0F, r, g, b, 255);

            tessellator.draw();
            RenderSystem.depthFunc(515);
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
            RenderSystem.disableAlphaTest();
            RenderSystem.shadeModel(7425);
            RenderSystem.disableTexture();
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);

            int shadow = 4;
            addVertex(bufferbuilder, x0, y0 + shadow, 0.0D, 0.0F, 1.0F, 0, 0, 0, 0);
            addVertex(bufferbuilder, x1, y0 + shadow, 0.0D, 1.0F, 1.0F, 0, 0, 0, 0);
            addVertex(bufferbuilder, x1, y0, 0.0D, 1.0F, 0.0F, 0, 0, 0, 255);
            addVertex(bufferbuilder, x0, y0, 0.0D, 0.0F, 0.0F, 0, 0, 0, 255);

            addVertex(bufferbuilder, x0, y1, 0.0D, 0.0F, 1.0F, 0, 0, 0, 255);
            addVertex(bufferbuilder, x1, y1, 0.0D, 1.0F, 1.0F, 0, 0, 0, 255);
            addVertex(bufferbuilder, x1, y1 - shadow, 0.0D, 1.0F, 0.0F, 0, 0, 0, 0);
            addVertex(bufferbuilder, x0, y1 - shadow, 0.0D, 0.0F, 0.0F, 0, 0, 0, 0);

            tessellator.draw();
        }

        private void addVertex(BufferBuilder bufferbuilder, double x, double y, double z, float u, float v, int r, int g, int b, int a) {
            bufferbuilder.pos(x, y, z).tex(u, v).color(r, g, b, a).endVertex();
        }

        protected boolean isFocused() {
            return CollectionBookGuiScreen.this.getListener() == this;
        }

        @OnlyIn(Dist.CLIENT)
        public class CardEntry extends ExtendedList.AbstractListEntry<CollectionBookGuiScreen.CardList.CardEntry> {
            private final CardData cardData;
            public final int ICON_SIZE = 8;

            public CardEntry(CardData cardData) {
                this.cardData = cardData;
            }

            public void render(MatrixStack matrixStack, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTicks) {
                AbstractGui.fill(matrixStack, left, top, left + ICON_SIZE, top + ICON_SIZE, 0xFF000000 | cardData.getColor());
                CollectionBookGuiScreen.this.font.drawText(matrixStack, new TranslationTextComponent(cardData.getTranslationKey()), left + ICON_SIZE + 4, top, 0xFFFFFF);
            }

            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) {
                    CollectionBookGuiScreen.CardList.this.setSelected(this);
                    return true;
                } else {
                    return false;
                }
            }
        }
    }
}
