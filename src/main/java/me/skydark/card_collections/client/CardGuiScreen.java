package me.skydark.card_collections.client;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import me.skydark.card_collections.Mod;
import me.skydark.card_collections.data.CardData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;

import java.util.SortedSet;
import java.util.TreeSet;

public class CardGuiScreen extends Screen {
    private static ResourceLocation GUI_BACKGROUND = new ResourceLocation(Mod.MOD_ID, "textures/gui/card.png");
    private CardData cardData;
    private ResourceLocation cardTexture;

    protected CardGuiScreen(CardData cardData) {
        super(StringTextComponent.EMPTY);
        this.cardData = cardData;
        this.cardTexture = cardData.getTexture();
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        //RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        int GUI_WIDTH = 320;
        int GUI_HEIGHT = 192;
        int GUI_CARD_SIZE = GUI_HEIGHT;
        int GUI_LEFT = (this.width - GUI_WIDTH) / 2;
        int GUI_TOP = (this.height - GUI_HEIGHT) / 2;

        this.minecraft.getTextureManager().bindTexture(GUI_BACKGROUND);
        this.blit(matrixStack, GUI_LEFT, GUI_TOP, this.getBlitOffset(), 0, 0, GUI_WIDTH, GUI_HEIGHT, GUI_HEIGHT, GUI_WIDTH);
        this.minecraft.getTextureManager().bindTexture(cardTexture);
        this.blit(matrixStack, GUI_LEFT, GUI_TOP, this.getBlitOffset(), 0, 0, GUI_CARD_SIZE, GUI_CARD_SIZE, GUI_CARD_SIZE, GUI_CARD_SIZE);

        String collectionId = cardData.getCollectionId();
        TranslationTextComponent text = new TranslationTextComponent(cardData.getTranslationKey());
        text.appendString("\n").appendSibling(new TranslationTextComponent(String.format("card_collections.%s.name", collectionId)).mergeStyle(TextFormatting.ITALIC)).appendString("\n");
        if (cardData.dimensions != null) {
            SortedSet<String> keys = new TreeSet<>(cardData.dimensions.keySet());
            for (String dimKey : keys) {
                Integer value = cardData.dimensions.get(dimKey);
                text.appendString("\n")
                    .appendSibling(new TranslationTextComponent(String.format("card_collections.%s.m_%s", collectionId, dimKey))
                        .mergeStyle(TextFormatting.DARK_GREEN)
                        .appendString(": ")
                        .appendSibling(new TranslationTextComponent(String.format("card_collections.%s.m_%s_%s", collectionId, dimKey, value))
                                .mergeStyle(TextFormatting.AQUA)));
            }
        }
        text.appendString("\n\n").appendSibling(new TranslationTextComponent(String.format("card.%s.%s.desc", collectionId, cardData.getCardId())));
        this.font.func_238418_a_(text, GUI_LEFT + GUI_CARD_SIZE + 4, GUI_TOP + 4, GUI_WIDTH - GUI_CARD_SIZE - 8, 0);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    public static void open(CardData cardData) {
        Minecraft.getInstance().displayGuiScreen(new CardGuiScreen(cardData));
    }
}
