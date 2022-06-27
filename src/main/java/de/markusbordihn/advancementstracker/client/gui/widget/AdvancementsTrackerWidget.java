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

import java.util.ConcurrentModificationException;
import java.util.List;
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
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.FormattedCharSequence;
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
import de.markusbordihn.advancementstracker.client.advancements.AdvancementsManager;
import de.markusbordihn.advancementstracker.client.advancements.TrackedAdvancementsManager;
import de.markusbordihn.advancementstracker.client.keymapping.ModKeyMapping;
import de.markusbordihn.advancementstracker.config.ClientConfig;
import de.markusbordihn.advancementstracker.utils.gui.PositionManager;

@EventBusSubscriber(value = Dist.CLIENT)
public class AdvancementsTrackerWidget extends GuiComponent {

  protected static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  private static final ClientConfig.Config CLIENT = ClientConfig.CLIENT;

  // Pre-defined colors and placeholders
  private static final int TEXT_COLOR_WHITE = ChatFormatting.WHITE.getColor();
  private static final int TEXT_COLOR_YELLOW = ChatFormatting.YELLOW.getColor();
  private static final int TEXT_COLOR_GRAY = ChatFormatting.GRAY.getColor();

  // Pre-defined texts
  private static MutableComponent noAdvancementsText =
      new TranslatableComponent(Constants.MOD_PREFIX + "advancementsWidget.noAdvancements")
          .append(ModKeyMapping.KEY_SHOW_WIDGET.getTranslatedKeyMessage())
          .withStyle(ChatFormatting.WHITE);
  private static MutableComponent noTrackedAdvancementsText =
      new TranslatableComponent(Constants.MOD_PREFIX + "advancementsWidget.noTrackedAdvancements")
          .append(new TranslatableComponent(
              Constants.MOD_PREFIX + "advancementsWidget.hotkeyAdvancementOverview",
              ModKeyMapping.KEY_SHOW_OVERVIEW.getTranslatedKeyMessage())
                  .withStyle(ChatFormatting.YELLOW))
          .withStyle(ChatFormatting.WHITE);

  private static boolean hudVisible = true;
  private static Set<AdvancementEntry> trackedAdvancements;

  private final Font font;
  private final ItemRenderer itemRenderer;
  private final Minecraft minecraft;
  private final PositionManager positionManager;
  private final TextureManager textureManager;

  private int x;
  private int y;

  public AdvancementsTrackerWidget(Minecraft minecraft) {
    this.font = minecraft.font;
    this.itemRenderer = minecraft.getItemRenderer();
    this.minecraft = minecraft;
    this.textureManager = minecraft.getTextureManager();
    positionManager = new PositionManager(minecraft);
    positionManager.setInstance(minecraft);
    positionManager.setWidth(80);
    positionManager.setHeight(22);
  }

  @SubscribeEvent
  public static void handleWorldEventLoad(WorldEvent.Load event) {
    updatePredefinedText();
    hudVisible = CLIENT.widgetEnabled.get() && CLIENT.widgetVisible.get();
  }

  @SubscribeEvent()
  public void renderOverlay(RenderGameOverlayEvent.Pre event) {
    if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
      return;
    }

    // Disable overlay if visibility is disabled or if there is another screen open.
    if (!hudVisible || (this.minecraft != null && this.minecraft.screen != null)) {
      return;
    }

    // Use Position Manager for Updates
    positionManager.updateWindow();
    positionManager.setPosition(positionManager.getMiddleRight());
    positionManager.setWidth(120);
    positionManager.setHeight(160);
    x = positionManager.getPositionX();
    y = positionManager.getPositionY();

    PoseStack poseStack = event.getMatrixStack();
    MultiBufferSource.BufferSource multiBufferSource =
        Minecraft.getInstance().renderBuffers().bufferSource();

    // Render background and GUI
    renderTitle(poseStack);

    // List tracked advancement
    if (TrackedAdvancementsManager.hasTrackedAdvancements()) {
      renderAdvancements(poseStack, multiBufferSource, x, y + this.font.lineHeight + 4);
    } else if (AdvancementsManager.hasAdvancements()) {
      renderNoTrackedAdvancements(poseStack, x, y + this.font.lineHeight + 4);
    } else {
      renderNoAdvancements(poseStack, x, y + this.font.lineHeight + 4);
    }
  }

  public static void updateTrackedAdvancements() {
    trackedAdvancements = TrackedAdvancementsManager.getTrackedAdvancements();
  }

  public static void toggleVisibility() {
    hudVisible = !hudVisible;
  }

  private void renderTitle(PoseStack poseStack) {
    poseStack.pushPose();
    fill(poseStack, x, y, positionManager.getPositionXWidth(), y + this.font.lineHeight + 2,
        1325400064);
    font.draw(poseStack, "Advancements Tracker", x + 2.0f, y + 2.0f, TEXT_COLOR_WHITE);
    poseStack.popPose();
  }

  private void renderNoTrackedAdvancements(PoseStack poseStack, int x, int y) {
    int textContentHeight = (this.font.lineHeight + 2) * 9;
    int textContentWidth = positionManager.getWidth();
    poseStack.pushPose();
    fill(poseStack, x, y, x + textContentWidth, y + textContentHeight, 1325400064);
    font.drawWordWrap(noTrackedAdvancementsText, x + 5, y + 5, textContentWidth - 10,
        textContentHeight - 10);
    poseStack.popPose();
  }

  private void renderNoAdvancements(PoseStack poseStack, int x, int y) {
    int textContentHeight = (this.font.lineHeight + 2) * 9;
    int textContentWidth = positionManager.getWidth();
    poseStack.pushPose();
    fill(poseStack, x, y, x + textContentWidth, y + textContentHeight, 1325400064);
    font.drawWordWrap(noAdvancementsText, x + 5, y + 5, textContentWidth - 10,
        textContentHeight - 10);
    poseStack.popPose();
  }

  private void renderAdvancements(PoseStack poseStack,
      MultiBufferSource.BufferSource multiBufferSource, int x, int y) {
    poseStack.pushPose();
    int topPos = y;
    int numberOfAdvancementsRendered = 0;
    try {
      for (AdvancementEntry advancementEntry : trackedAdvancements) {
        // Check if the screen space is big enough to render all advancements.
        if (topPos + (font.lineHeight * 4) < positionManager.getWindowHeightScaled()) {
          topPos +=
              renderAdvancement(poseStack, multiBufferSource, x, topPos, advancementEntry) + 2;
          numberOfAdvancementsRendered++;
        } else {
          renderAdvancementEllipsis(poseStack, x, topPos, trackedAdvancements.size(),
              numberOfAdvancementsRendered);
          break;
        }
      }
    } catch (ConcurrentModificationException exception) {
      log.debug("Advancement list was modified during rendering. This is expected in some cases.");
    }
    poseStack.popPose();
  }

  private void renderAdvancementEllipsis(PoseStack poseStack, int x, int y,
      int numberOfAdvancements, int numberOfAdvancementsRendered) {

    // Background
    poseStack.pushPose();
    fill(poseStack, x, y, positionManager.getPositionXWidth(), y + font.lineHeight, 1325400064);
    poseStack.popPose();

    // Text
    float textScale = 0.75f;
    TranslatableComponent text =
        new TranslatableComponent(Constants.ADVANCEMENTS_WIDGET_PREFIX + "notAllVisible",
            numberOfAdvancementsRendered, numberOfAdvancements);
    poseStack.pushPose();
    poseStack.scale(textScale, textScale, textScale);
    font.drawShadow(poseStack, text, (x + 16) / textScale, (y + 2) / textScale, TEXT_COLOR_GRAY);
    font.draw(poseStack, text, (x + 16) / textScale, (y + 2) / textScale, TEXT_COLOR_GRAY);
    poseStack.popPose();
  }

  private int renderAdvancement(PoseStack poseStack,
      MultiBufferSource.BufferSource multiBufferSource, int x, int y,
      AdvancementEntry advancementEntry) {

    // Positions
    int maxFontWidth = positionManager.getWidth() - 2;
    int referenceTopPosition = y + 3;
    int referenceLeftPosition = x + 2;

    // Pre-calculations
    float titleScale = 0.75f;
    int titlePaddingLeft = 10;
    int titlePaddingRight = advancementEntry.getProgressTotal() > 1 ? 20 : 0;
    int titleWidth = font.width(advancementEntry.getTitle()) * titleScale > maxFontWidth
        - titlePaddingLeft - titlePaddingRight
            ? maxFontWidth - titlePaddingLeft - titlePaddingRight - Math.round(7 * titleScale)
            : maxFontWidth - titlePaddingLeft - titlePaddingRight;
    int titleWidthScaled = Math.round(titleWidth / titleScale);
    FormattedCharSequence titleText = Language.getInstance().getVisualOrder(
        FormattedText.composite(font.substrByWidth(advancementEntry.getTitle(), titleWidthScaled)));

    float descriptionScale = 0.75f;
    List<FormattedCharSequence> descriptionParts = font.split(advancementEntry.getDescription(),
        Math.round(maxFontWidth / descriptionScale) - 3);
    int descriptionLines = 1;

    // Calculate expected content size
    int expectedContentSize =
        Math.round((font.lineHeight * titleScale + 3) + ((font.lineHeight * descriptionScale + 3)
            * (descriptionParts.size() <= 3 ? descriptionParts.size() : 3)));

    // Background
    poseStack.pushPose();
    fill(poseStack, x, y, positionManager.getPositionXWidth(), y + expectedContentSize, 1325400064);
    poseStack.popPose();

    // Title (only one line)
    poseStack.pushPose();
    poseStack.scale(titleScale, titleScale, titleScale);
    font.drawShadow(poseStack, titleText, (referenceLeftPosition + titlePaddingLeft) / titleScale,
        referenceTopPosition / titleScale, TEXT_COLOR_YELLOW);
    font.draw(poseStack, titleText, (referenceLeftPosition + titlePaddingLeft) / titleScale,
        referenceTopPosition / titleScale, TEXT_COLOR_YELLOW);

    // Show ellipsis if title is to long.
    if (titleWidth != maxFontWidth - titlePaddingLeft - titlePaddingRight) {
      font.draw(poseStack, Constants.ELLIPSIS,
          ((referenceLeftPosition + titlePaddingLeft) / titleScale) + titleWidthScaled,
          referenceTopPosition / titleScale, TEXT_COLOR_YELLOW);
    }
    poseStack.popPose();

    // Show Progress, if we have more than one requirements.
    if (advancementEntry.getProgressTotal() > 1) {
      float progressScale = 0.6f;
      int progressPositionLeft = referenceLeftPosition + maxFontWidth - titlePaddingRight + 1;
      poseStack.pushPose();
      poseStack.scale(progressScale, progressScale, progressScale);
      font.drawShadow(poseStack, advancementEntry.getProgressString(),
          progressPositionLeft / progressScale, (referenceTopPosition - 1) / progressScale,
          TEXT_COLOR_YELLOW);
      font.draw(poseStack, advancementEntry.getProgressString(),
          progressPositionLeft / progressScale, (referenceTopPosition - 1) / progressScale,
          TEXT_COLOR_YELLOW);
      poseStack.popPose();
    }
    referenceTopPosition += font.lineHeight * titleScale + 3;

    // Icon
    if (advancementEntry.icon != null) {
      poseStack.pushPose();
      renderGuiItem(advancementEntry.icon, multiBufferSource, referenceLeftPosition - 4,
          referenceTopPosition - 14, 0.65f);
      poseStack.popPose();
    }

    // Description (max three lines)
    poseStack.pushPose();
    poseStack.scale(descriptionScale, descriptionScale, descriptionScale);
    for (FormattedCharSequence descriptionPart : descriptionParts) {
      boolean shouldEnd = false;
      font.drawShadow(poseStack, descriptionPart, referenceLeftPosition / descriptionScale,
          referenceTopPosition / descriptionScale, advancementEntry.descriptionColor);
      font.draw(poseStack, descriptionPart, referenceLeftPosition / descriptionScale,
          referenceTopPosition / descriptionScale, advancementEntry.descriptionColor);
      if ((descriptionParts.size() >= 3 && descriptionLines == 3)) {
        font.draw(poseStack, Constants.ELLIPSIS, (referenceLeftPosition / descriptionScale)
            + ((font.width(descriptionPart) / descriptionScale) < maxFontWidth / descriptionScale
                - 3 ? (font.width(descriptionPart) / descriptionScale) - 7
                    : (maxFontWidth / descriptionScale) - 7),
            referenceTopPosition / descriptionScale, 0xFFFFFF);
        shouldEnd = true;
      } else if (descriptionParts.size() == 2 && descriptionLines == 2) {
        shouldEnd = true;
      }
      referenceTopPosition += font.lineHeight * descriptionScale + 3;
      if (shouldEnd) {
        break;
      }
      descriptionLines++;
    }
    poseStack.popPose();

    // Return actual content position
    return referenceTopPosition - y;
  }

  private static void updatePredefinedText() {
    // Update text for custom key-mapping.
    noAdvancementsText =
        new TranslatableComponent(Constants.MOD_PREFIX + "advancementsWidget.noAdvancements")
            .withStyle(ChatFormatting.WHITE);
    noTrackedAdvancementsText =
        new TranslatableComponent(Constants.MOD_PREFIX + "advancementsWidget.noTrackedAdvancements")
            .append(new TranslatableComponent(
                Constants.MOD_PREFIX + "advancementsWidget.hotkeyAdvancementOverview",
                ModKeyMapping.KEY_SHOW_OVERVIEW.getTranslatedKeyMessage())
                    .withStyle(ChatFormatting.YELLOW))
            .withStyle(ChatFormatting.WHITE);
  }

  private void renderGuiItem(ItemStack itemStack, MultiBufferSource.BufferSource multiBufferSource,
      int x, int y, float scale) {
    this.renderGuiItem(itemStack, multiBufferSource, x, y, scale,
        itemRenderer.getModel(itemStack, (Level) null, (LivingEntity) null, 0));
  }

  /**
   * @deprecated
   */
  @Deprecated
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
    modelPoseStack.translate(x, y, 100.0F + itemRenderer.blitOffset);
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
