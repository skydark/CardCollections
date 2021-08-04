package me.skydark.card_collections.init;

import me.skydark.card_collections.Mod;
import me.skydark.card_collections.network.CPopCardFromCollectionBook;
import me.skydark.card_collections.network.IMessagePacket;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.function.Function;

public class ModMessages {
    private static SimpleChannel INSTANCE;
    private static int ID = 0;

    public static void register() {
        INSTANCE = NetworkRegistry.newSimpleChannel(new ResourceLocation(Mod.MOD_ID, "main"),
                () -> "1.0",
                c -> true,
                s -> true);
        register(CPopCardFromCollectionBook.class, CPopCardFromCollectionBook::new);
    }

    private static <T extends IMessagePacket> void register(Class<T> packetClass, Function<PacketBuffer, T> packetToMessage)
    {
        INSTANCE.registerMessage(ID++, packetClass, T::writePacketData, packetToMessage, T::processPacket);
    }

    public static void sendToClient(Object packet, ServerPlayerEntity player) {
        INSTANCE.sendTo(packet, player.connection.netManager, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void sendToServer(Object packet) {
        INSTANCE.sendToServer(packet);
    }
}
