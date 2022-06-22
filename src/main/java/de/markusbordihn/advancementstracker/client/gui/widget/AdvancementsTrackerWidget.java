/**
 * Copyright 2022 Markus Bordihn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.markusbordihn.advancementstracker.client.gui.widget;

import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import de.markusbordihn.advancementstracker.Constants;
import de.markusbordihn.advancementstracker.client.advancements.AdvancementEntry;
import de.markusbordihn.advancementstracker.client.advancements.TrackedAdvancementsManager;
import de.markusbordihn.advancementstracker.config.ClientConfig;
import de.markusbordihn.advancementstracker.utils.gui.PositionManager;

@EventBusSubscriber(value = Dist.CLIENT)
public class AdvancementsTrackerWidget extends GuiComponent {

  protected static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  private static final ClientConfig.Config CLIENT = ClientConfig.CLIENT;

  private static final int TEXT_COLOR_WHITE = ChatFormatting.WHITE.getColor();
  private static final int TEXT_COLOR_YELLOW = ChatFormatting.YELLOW.getColor();

  private static boolean hudVisible = true;
  private static Set<AdvancementEntry> trackedAdvancements;

  private final PositionManager positionManager;
  private final Font fontRender;
  private final ItemRenderer itemRenderer;
  private final TextureManager textureManager;
  private final Minecraft minecraft;

  private int x;
  private int y;

  public AdvancementsTrackerWidget(Minecraft minecraft) {
    this.minecraft = minecraft;
    this.fontRender = minecraft.font;
    this.itemRenderer = minecraft.getItemRenderer();
    this.textureManager = minecraft.getTextureManager();
    positionManager = new PositionManager(minecraft);
    positionManager.setInstance(minecraft);
    positionManager.setWidth(81);
    positionManager.setHeight(22);
  }

  @SubscribeEvent
  public static void handleWorldEventLoad(WorldEvent.Load event) {}

  @SubscribeEvent()
  public void renderOverlay(RenderGameOverlayEvent.Pre event) {
    if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
      return;
    }
    if (!hudVisible) {
      return;
    }

    // Position Manager Updates
    positionManager.updateWindow();
    positionManager.setPosition(positionManager.getMiddleRight());
    positionManager.setWidth(140);
    positionManager.setHeight(160);
    x = positionManager.getPositionX();
    y = positionManager.getPositionY();

    PoseStack poseStack = event.getMatrixStack();
    MultiBufferSource.BufferSource multiBufferSource =
        Minecraft.getInstance().renderBuffers().bufferSource();
    float partialTicks = event.getPartialTicks();

    poseStack.pushPose();
    renderBackground(poseStack);
    renderTitle(poseStack);
    poseStack.popPose();

    // List tracked advancement
    poseStack.pushPose();
    renderAdvancements(poseStack, multiBufferSource, x, y + this.fontRender.lineHeight + 4,
        partialTicks);
    poseStack.popPose();
  }

  public void renderBackground(PoseStack poseStack) {
    fill(poseStack, x, y, positionManager.getPositionXWidth(), positionManager.getPositionYHeight(),
        1325400064);
  }

  public void renderTitle(PoseStack poseStack) {
    fill(poseStack, x, y, positionManager.getPositionXWidth(), y + this.fontRender.lineHeight + 2,
        1325400064);
    fontRender.draw(poseStack, "Advancements Tracker", x + 2.0f, y + 2.0f, TEXT_COLOR_WHITE);
  }

  public void renderAdvancements(PoseStack poseStack,
      MultiBufferSource.BufferSource multiBufferSource, int x, int y, float partialTicks) {
    int topPos = y;
    for (AdvancementEntry advancementEntry : trackedAdvancements) {
      topPos += renderAdvancement(poseStack, multiBufferSource, x + 2, topPos, advancementEntry,
          partialTicks);
    }
  }

  public int renderAdvancement(PoseStack poseStack,
      MultiBufferSource.BufferSource multiBufferSource, int x, int y,
      AdvancementEntry advancementEntry, float partialTicks) {

    // Title
    float titleScale = 0.75f;
    poseStack.pushPose();
    poseStack.scale(titleScale, titleScale, titleScale);
    fontRender.drawShadow(poseStack, advancementEntry.title, (x + 10) / titleScale, y / titleScale,
        TEXT_COLOR_YELLOW);
    fontRender.draw(poseStack, advancementEntry.title, (x + 10) / titleScale, y / titleScale,
        TEXT_COLOR_YELLOW);
    poseStack.popPose();

    // Icon
    if (advancementEntry.icon != null) {
      poseStack.pushPose();
      renderGuiItem(advancementEntry.icon, multiBufferSource, x - 4, y - 5, 0.65f);
      poseStack.popPose();
    }

    // Description
    float descriptionScale = 0.75f;
    poseStack.pushPose();
    poseStack.scale(descriptionScale, descriptionScale, descriptionScale);
    fontRender.drawShadow(poseStack, advancementEntry.description, x / descriptionScale,
        (y + fontRender.lineHeight) / descriptionScale, advancementEntry.descriptionColor);
    fontRender.draw(poseStack, advancementEntry.description, x / descriptionScale,
        (y + fontRender.lineHeight) / descriptionScale, advancementEntry.descriptionColor);
    poseStack.popPose();

    return fontRender.lineHeight * 2 + 2;
  }

  public static void updateTrackedAdvancements() {
    trackedAdvancements = TrackedAdvancementsManager.getTrackedAdvancements();
  }

  public static void toggleVisibility() {
    hudVisible = !hudVisible;
  }

  public void renderGuiItem(ItemStack itemStack, MultiBufferSource.BufferSource multiBufferSource,
      int x, int y, float scale) {
    this.renderGuiItem(itemStack, multiBufferSource, x, y, scale,
        itemRenderer.getModel(itemStack, (Level) null, (LivingEntity) null, 0));
  }

  private void renderGuiItem(ItemStack itemStack, MultiBufferSource.BufferSource multiBufferSource,
      int x, int y, float scale, BakedModel model) {
    this.textureManager.getTexture(TextureAtlas.LOCATION_BLOCKS).setFilter(false, false);
    RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
    RenderSystem.enableBlend();
    RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    PoseStack modelPoseStack = RenderSystem.getModelViewStack();
    modelPoseStack.pushPose();
    modelPoseStack.translate((double) x, (double) y, (double) (100.0F + itemRenderer.blitOffset));
    modelPoseStack.translate(8.0D, 8.0D, 0.0D);
    modelPoseStack.scale(scale, -scale, scale);
    modelPoseStack.scale(16.0F, 16.0F, 16.0F);
    RenderSystem.applyModelViewMatrix();
    PoseStack modelPoseStack1 = new PoseStack();
    boolean flag = !model.usesBlockLight();
    if (flag) {
      Lighting.setupForFlatItems();
    }
    itemRenderer.render(itemStack, ItemTransforms.TransformType.GUI, false, modelPoseStack1,
        multiBufferSource, 15728880, OverlayTexture.NO_OVERLAY, model);
    multiBufferSource.endBatch();
    RenderSystem.enableDepthTest();
    if (flag) {
      Lighting.setupFor3DItems();
    }
    modelPoseStack.popPose();
    RenderSystem.applyModelViewMatrix();
  }

}
