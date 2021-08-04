package me.skydark.card_collections.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public interface IMessagePacket {
    void writePacketData(PacketBuffer buffer);

    boolean processPacket(Supplier<NetworkEvent.Context> ctx);
}
