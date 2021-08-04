package me.skydark.card_collections.network;

import me.skydark.card_collections.entity.CardEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.network.play.IClientPlayNetHandler;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.IOException;
import java.util.UUID;

public class SSpawnCardPacket implements IPacket<IClientPlayNetHandler> {
    private int entityID;
    private UUID uniqueId;
    private BlockPos position;
    private Direction facing;
    private ResourceLocation card;

    public SSpawnCardPacket(CardEntity cardEntity) {
        this.entityID = cardEntity.getEntityId();
        this.uniqueId = cardEntity.getUniqueID();
        this.position = cardEntity.getHangingPosition();
        this.facing = cardEntity.getHorizontalFacing();
        this.card = cardEntity.card;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public void readPacketData(PacketBuffer buf) throws IOException {
        this.entityID = buf.readVarInt();
        this.uniqueId = buf.readUniqueId();
        this.card = buf.readResourceLocation();
        this.position = buf.readBlockPos();
        this.facing = Direction.byHorizontalIndex(buf.readUnsignedByte());
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer buf) throws IOException {
        buf.writeVarInt(this.entityID);
        buf.writeUniqueId(this.uniqueId);
        buf.writeResourceLocation(this.card);
        buf.writeBlockPos(this.position);
        buf.writeByte(this.facing.getHorizontalIndex());
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void processPacket(IClientPlayNetHandler handler) {
        ClientPlayNetHandler clientHandler = (ClientPlayNetHandler) handler;
        PacketThreadUtil.checkThreadAndEnqueue(this, clientHandler, Minecraft.getInstance());
        CardEntity cardEntity = new CardEntity(clientHandler.getWorld(), this.getPosition(), this.getFacing(), this.getCardResourceLocation());
        cardEntity.setEntityId(this.getEntityID());
        cardEntity.setUniqueId(this.getUniqueId());
        clientHandler.getWorld().addEntity(this.getEntityID(), cardEntity);
    }

    @OnlyIn(Dist.CLIENT)
    public int getEntityID() {
        return this.entityID;
    }

    @OnlyIn(Dist.CLIENT)
    public UUID getUniqueId() {
        return this.uniqueId;
    }

    @OnlyIn(Dist.CLIENT)
    public BlockPos getPosition() {
        return this.position;
    }

    @OnlyIn(Dist.CLIENT)
    public Direction getFacing() {
        return this.facing;
    }

    @OnlyIn(Dist.CLIENT)
    public ResourceLocation getCardResourceLocation() {
        return this.card;
    }
}