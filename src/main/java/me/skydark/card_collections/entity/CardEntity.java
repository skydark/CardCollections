package me.skydark.card_collections.entity;

import me.skydark.card_collections.Mod;
import me.skydark.card_collections.data.CardCollectionDataManager;
import me.skydark.card_collections.data.CardData;
import me.skydark.card_collections.init.ModEntities;
import me.skydark.card_collections.item.CardItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.HangingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.FMLPlayMessages;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;

public class CardEntity extends HangingEntity implements IEntityAdditionalSpawnData {
    public ResourceLocation card;
    public CardEntity(EntityType<? extends CardEntity> type, World worldIn) {
        super(type, worldIn);
    }

    public CardEntity(World world, BlockPos hangingPos, Direction direction, CardData cardData) {
        this(world, hangingPos, direction, cardData.getResourceLocation());
    }

    public CardEntity(World world, BlockPos hangingPos, Direction direction, ResourceLocation card) {
        super(ModEntities.CARD.get(), world, hangingPos);
        this.card = card;
        this.updateFacingWithBoundingBox(direction);
    }

    public CardEntity(FMLPlayMessages.SpawnEntity spawnEntity, World world) {
        super(ModEntities.CARD.get(), world);

        /*
        this(worldIn, spawnEntity.getAdditionalData().readBlockPos(),
                Direction.byHorizontalIndex(spawnEntity.getAdditionalData().readByte()),
                spawnEntity.getAdditionalData().readResourceLocation());
         */
    }

    public void writeAdditional(CompoundNBT compound) {
        compound.putString("Card", this.card.toString());
        compound.putByte("Facing", (byte)this.facingDirection.getHorizontalIndex());
        super.writeAdditional(compound);
    }

    public void readAdditional(CompoundNBT compound) {
        this.card = ResourceLocation.tryCreate(compound.getString("Card"));
        this.facingDirection = Direction.byHorizontalIndex(compound.getByte("Facing"));
        super.readAdditional(compound);
        this.updateFacingWithBoundingBox(this.facingDirection);
    }

    public int getWidthPixels() {
        return 16;
    }

    public int getHeightPixels() {
        return 16;
    }

    public void onBroken(@Nullable Entity brokenEntity) {
        if (this.world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)) {
            this.playSound(SoundEvents.ENTITY_PAINTING_BREAK, 1.0F, 1.0F);
            if (brokenEntity instanceof PlayerEntity) {
                PlayerEntity playerentity = (PlayerEntity)brokenEntity;
                if (playerentity.abilities.isCreativeMode) {
//                    return;
                }
            }

            CardData cardData = CardCollectionDataManager.INSTANCE.getCard(this.card);
            if (cardData == null) {
                Mod.LOGGER.warn("Invalid card to drop: {}", this.card);
                return;
            }
            ItemStack stack = CardItem.createStack(cardData);
            this.entityDropItem(stack);
        }
    }

    public void playPlaceSound() {
        this.playSound(SoundEvents.ENTITY_PAINTING_PLACE, 1.0F, 1.0F);
    }

    public void setLocationAndAngles(double x, double y, double z, float yaw, float pitch) {
        this.setPosition(x, y, z);
    }

    @OnlyIn(Dist.CLIENT)
    public void setPositionAndRotationDirect(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
        BlockPos blockpos = this.hangingPosition.add(x - this.getPosX(), y - this.getPosY(), z - this.getPosZ());
        this.setPosition((double)blockpos.getX(), (double)blockpos.getY(), (double)blockpos.getZ());
    }

    @Override
    public IPacket<?> createSpawnPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(PacketBuffer buffer) {
        buffer.writeBlockPos(this.getHangingPosition());
        buffer.writeByte((byte)this.getHorizontalFacing().getHorizontalIndex());
        buffer.writeResourceLocation(this.card);
    }

    @Override
    public void readSpawnData(PacketBuffer additionalData) {
        this.hangingPosition = additionalData.readBlockPos();
        this.facingDirection = Direction.byHorizontalIndex(additionalData.readByte());
        this.card = additionalData.readResourceLocation();
        this.updateFacingWithBoundingBox(this.facingDirection);
    }
}
