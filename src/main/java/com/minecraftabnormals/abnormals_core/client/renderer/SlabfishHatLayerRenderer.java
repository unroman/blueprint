package com.minecraftabnormals.abnormals_core.client.renderer;

import com.minecraftabnormals.abnormals_core.client.RewardHandler;
import com.minecraftabnormals.abnormals_core.client.model.SlabfishHatModel;
import com.minecraftabnormals.abnormals_core.common.world.storage.tracking.IDataManager;
import com.minecraftabnormals.abnormals_core.core.AbnormalsCore;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.TimeUnit;

public class SlabfishHatLayerRenderer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	public static OnlineImageCache REWARD_CACHE = new OnlineImageCache(AbnormalsCore.MODID, 1, TimeUnit.DAYS);
	private final SlabfishHatModel model;

	public SlabfishHatLayerRenderer(PlayerRenderer renderer) {
		super(renderer);
		this.model = new SlabfishHatModel(SlabfishHatModel.createBodyModel().bakeRoot());
	}

	@Override
	public void render(PoseStack stack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
		RewardHandler.RewardProperties properties = RewardHandler.getRewardProperties();
		if (properties == null)
			return;

		RewardHandler.RewardProperties.SlabfishProperties slabfishProperties = properties.getSlabfishProperties();
		if (slabfishProperties == null)
			return;

		String defaultTypeUrl = slabfishProperties.getDefaultTypeUrl();
		IDataManager data = (IDataManager) entity;

		if (entity.isInvisible() || entity.isSpectator() || !(RewardHandler.SlabfishSetting.getSetting(data, RewardHandler.SlabfishSetting.ENABLED)) || defaultTypeUrl == null || !RewardHandler.REWARDS.containsKey(entity.getUUID()))
			return;

		RewardHandler.RewardData reward = RewardHandler.REWARDS.get(entity.getUUID());

		if (reward.getSlabfish() == null || reward.getTier() < 2)
			return;

		RewardHandler.RewardData.SlabfishData slabfish = reward.getSlabfish();
		ResourceLocation typeLocation = REWARD_CACHE.getTextureLocation(reward.getTier() >= 4 && slabfish.getTypeUrl() != null && RewardHandler.SlabfishSetting.getSetting(data, RewardHandler.SlabfishSetting.TYPE) ? slabfish.getTypeUrl() : defaultTypeUrl);
		if (typeLocation == null)
			return;
		
		ResourceLocation sweaterLocation = reward.getTier() >= 3 && slabfish.getSweaterUrl() != null && RewardHandler.SlabfishSetting.getSetting(data, RewardHandler.SlabfishSetting.SWEATER) ? REWARD_CACHE.getTextureLocation(slabfish.getSweaterUrl()) : null;
		ResourceLocation backpackLocation = slabfish.getBackpackUrl() != null && RewardHandler.SlabfishSetting.getSetting(data, RewardHandler.SlabfishSetting.BACKPACK) ? REWARD_CACHE.getTextureLocation(slabfish.getBackpackUrl()) : null;
		ModelPart body = this.model.body;
		ModelPart backpack = this.model.backpack;

		body.copyFrom(this.getParentModel().head);
		body.render(stack, buffer.getBuffer(slabfish.isTranslucent() ? RenderType.entityTranslucent(typeLocation) : RenderType.entityCutout(typeLocation)), packedLight, OverlayTexture.NO_OVERLAY);

		if (sweaterLocation != null)
			body.render(stack, buffer.getBuffer(RenderType.entityCutout(sweaterLocation)), packedLight, OverlayTexture.NO_OVERLAY);

		if (backpackLocation != null) {
			backpack.copyFrom(body);
			backpack.render(stack, buffer.getBuffer(RenderType.entityCutout(backpackLocation)), packedLight, OverlayTexture.NO_OVERLAY);
		}
	}
}
