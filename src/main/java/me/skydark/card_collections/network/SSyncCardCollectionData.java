package me.skydark.card_collections.network;

import com.google.common.collect.ImmutableMap;
import me.skydark.card_collections.data.CardCollectionData;
import me.skydark.card_collections.data.CardCollectionDataManager;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Map;
import java.util.function.Supplier;

public class SSyncCardCollectionData implements IMessagePacket {
    private final Map<String, CardCollectionData> collections;

    public SSyncCardCollectionData(Map<String, CardCollectionData> collections) {
        this.collections = collections;
    }

    public SSyncCardCollectionData(PacketBuffer buf) {
        ImmutableMap.Builder<String, CardCollectionData> builder = ImmutableMap.builder();
        int size = buf.readInt();
        for (int i=0; i<size; i++) {
            CardCollectionData value = CardCollectionData.readFromBuffer(buf);
            builder.put(value.getId(), value);
        }
        this.collections = builder.build();
    }

    public void writePacketData(PacketBuffer buf) {
        buf.writeInt(collections.size());
        collections.forEach((key, value) -> {
            value.writeToBuffer(buf);
        });
    }

    @Override
    public boolean processPacket(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            CardCollectionDataManager.INSTANCE.setCollections(collections);
        });
        return true;
    }
}
