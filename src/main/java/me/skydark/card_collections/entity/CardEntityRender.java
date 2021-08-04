package me.skydark.card_collections.entity;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import me.skydark.card_collections.Mod;
import me.skydark.card_collections.data.CardCollectionDataManager;
import me.skydark.card_collections.data.CardData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.item.PaintingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix3f;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;

public class CardEntityRender extends EntityRenderer<CardEntity> {
    public static ResourceLocation TEXTURE_ARD_NOT_FOUND = new ResourceLocation(Mod.MOD_ID, "textures/gui/missing.png");
    public CardEntityRender(EntityRendererManager renderManagerIn) {
        super(renderManagerIn);
    }

    public void render(CardEntity entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn) {
        matrixStackIn.push();
        matrixStackIn.rotate(Vector3f.YP.rotationDegrees(180.0F - entityYaw));
        matrixStackIn.scale(0.0625F, 0.0625F, 0.0625F);
        this.renderCard(matrixStackIn, bufferIn, entityIn);
        matrixStackIn.pop();
        super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
    }

    /**
     * Returns the location of an entity's texture.
     */
    public ResourceLocation getEntityTexture(CardEntity entity) {
        CardData cardData = CardCollectionDataManager.INSTANCE.getCard(entity.card);
        return cardData == null ? TEXTURE_ARD_NOT_FOUND : cardData.getTexture();
    }

    private void renderCard(MatrixStack matrixStack, IRenderTypeBuffer bufferIn, CardEntity cardEntity) {
        MatrixStack.Entry matrixStackEntry = matrixStack.getLast();
        Matrix4f matrix = matrixStackEntry.getMatrix();
        Matrix3f normal = matrixStackEntry.getNormal();

        float right = 8;
        float left = -8;
        float bottom = 8;
        float top = -8;
        float front = -0.5f;
        float back = 0.5f;

        int posX = MathHelper.floor(cardEntity.getPosX());
        int posY = MathHelper.floor(cardEntity.getPosY());
        int posZ = MathHelper.floor(cardEntity.getPosZ());
        int lightMapUV = WorldRenderer.getCombinedLight(cardEntity.world, new BlockPos(posX, posY, posZ));

        IVertexBuilder frontBuilder = bufferIn.getBuffer(RenderType.getEntityTranslucent(this.getEntityTexture(cardEntity)));
        add(matrix, normal, frontBuilder, right, top, front, 0, 1, 0, 0, -1, lightMapUV);
        add(matrix, normal, frontBuilder, left, top, front, 1, 1, 0, 0, -1, lightMapUV);
        add(matrix, normal, frontBuilder, left, bottom, front, 1, 0, 0, 0, -1, lightMapUV);
        add(matrix, normal, frontBuilder, right, bottom, front, 0, 0, 0, 0, -1, lightMapUV);

        IVertexBuilder backBuilder = bufferIn.getBuffer(RenderType.getEntitySolid(new ResourceLocation("minecraft", "textures/painting/back.png")));
        add(matrix, normal, backBuilder, right, bottom, back, 0, 0, 0, 0, 1, lightMapUV);
        add(matrix, normal, backBuilder, left, bottom, back, 1, 0, 0, 0, 1, lightMapUV);
        add(matrix, normal, backBuilder, left, top, back, 1, 1, 0, 0, 1, lightMapUV);
        add(matrix, normal, backBuilder, right, top, back, 0, 1, 0, 0, 1, lightMapUV);

        add(matrix, normal, backBuilder, right, bottom, front, 0, 0, 0, 1, 0, lightMapUV);
        add(matrix, normal, backBuilder, left, bottom, front, 1, 0, 0, 1, 0, lightMapUV);
        add(matrix, normal, backBuilder, left, bottom, back, 1, 1, 0, 1, 0, lightMapUV);
        add(matrix, normal, backBuilder, right, bottom, back, 0, 1, 0, 1, 0, lightMapUV);

        add(matrix, normal, backBuilder, right, top, back, 0, 0, 0, -1, 0, lightMapUV);
        add(matrix, normal, backBuilder, left, top, back, 1, 0, 0, -1, 0, lightMapUV);
        add(matrix, normal, backBuilder, left, top, front, 1, 1, 0, -1, 0, lightMapUV);
        add(matrix, normal, backBuilder, right, top, front, 0, 1, 0, -1, 0, lightMapUV);

        add(matrix, normal, backBuilder, right, bottom, back, 1, 0, -1, 0, 0, lightMapUV);
        add(matrix, normal, backBuilder, right, top, back, 1, 1, -1, 0, 0, lightMapUV);
        add(matrix, normal, backBuilder, right, top, front, 0, 1, -1, 0, 0, lightMapUV);
        add(matrix, normal, backBuilder, right, bottom, front, 0, 0, -1, 0, 0, lightMapUV);

        add(matrix, normal, backBuilder, left, bottom, front, 1, 0, 1, 0, 0, lightMapUV);
        add(matrix, normal, backBuilder, left, top, front, 1, 1, 1, 0, 0, lightMapUV);
        add(matrix, normal, backBuilder, left, top, back, 0, 1, 1, 0, 0, lightMapUV);
        add(matrix, normal, backBuilder, left, bottom, back, 0, 0, 1, 0, 0, lightMapUV);
    }

    private void add(Matrix4f matrix4f, Matrix3f normal, IVertexBuilder vertexBuilder,
                     float x, float y, float z,
                     float u, float v,
                     int normalX, int normalY, int normalZ,
                     int lightMapUV) {
        vertexBuilder.pos(matrix4f, x, y, z)
                .color(255, 255, 255, 255)
                .tex(u, v)
                .overlay(OverlayTexture.NO_OVERLAY)
                .lightmap(lightMapUV)
                .normal(normal, (float)normalX, (float)normalY, (float)normalZ)
                .endVertex();
    }
}
