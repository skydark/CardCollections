package me.skydark.card_collections.network;

import me.skydark.card_collections.data.CardCollectionData;
import me.skydark.card_collections.data.CardData;
import me.skydark.card_collections.init.ModItems;
import me.skydark.card_collections.item.CardItem;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class CPopCardFromCollectionBook implements IMessagePacket {
    private int slot;
    private String cardId;

    public CPopCardFromCollectionBook(int slot, String cardId) {
        this.slot = slot;
        this.cardId = cardId;
    }

    public CPopCardFromCollectionBook(PacketBuffer buf) {
        this.slot = buf.readInt();
        this.cardId = buf.readString(32767);
    }

    @Override
    public void writePacketData(PacketBuffer buf) {
        buf.writeInt(slot);
        buf.writeString(cardId);
    }

    @Override
    public boolean processPacket(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (!PlayerInventory.isHotbar(this.slot) && this.slot != 40) return;
            ServerPlayerEntity player = ctx.get().getSender();
            if (player == null) { return; }

            ItemStack handStack = player.inventory.getStackInSlot(this.slot);
            if (handStack.getItem() != ModItems.COLLECTION_BOOK.get()) return;
            CardCollectionData collectionData = CardCollectionData.getCollectionData(handStack);
            if (collectionData == null) return;
            CardData cardData = collectionData.getCard(this.cardId);
            if (cardData == null) return;
            ItemStack cardStack = CardItem.createStack(cardData);

            CompoundNBT compoundNBT = handStack.getTag();
            if (compoundNBT == null) return;
            ListNBT listnbt = compoundNBT.getList("cards", 8).copy();
            ListNBT listnbtNew = new ListNBT();
            boolean founded = false;
            for (int i = 0; i < listnbt.size(); ++i) {
                String _cardId = listnbt.getString(i);
                if (this.cardId.equals(_cardId)) {
                    founded = true;
                    continue;
                }
                listnbtNew.add(StringNBT.valueOf(_cardId));
            }
            if (!founded) return;

            handStack.setTagInfo("cards", listnbtNew);
            player.entityDropItem(cardStack, 0.0F);
        });
        return true;
    }
}
