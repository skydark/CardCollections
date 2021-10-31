package me.skydark.card_collections.proxy;

import me.skydark.card_collections.client.CardGuiScreen;
import me.skydark.card_collections.client.CollectionBookGuiScreen;
import me.skydark.card_collections.data.CardData;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public class ClientProxy implements IProxy {
    @Override
    public void openCardGui(CardData cardData) {
        Minecraft.getInstance().displayGuiScreen(new CardGuiScreen(cardData));
    }

    @Override
    public void openCollectionBookGui(PlayerEntity playerIn, ItemStack handStack, Hand handIn) {
        Minecraft.getInstance().displayGuiScreen(new CollectionBookGuiScreen(playerIn, handStack, handIn));
    }
}
