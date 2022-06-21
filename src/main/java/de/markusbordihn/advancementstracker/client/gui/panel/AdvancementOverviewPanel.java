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

package de.markusbordihn.advancementstracker.client.gui.panel;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.TextComponent;

import de.markusbordihn.advancementstracker.Constants;
import de.markusbordihn.advancementstracker.client.advancements.AdvancementEntry;
import de.markusbordihn.advancementstracker.client.gui.screens.AdvancementsTrackerScreen;

public class AdvancementOverviewPanel
    extends ObjectSelectionList<AdvancementOverviewPanel.ChildAdvancementEntry> {

  protected static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  private final int listLeft;
  private final int listWidth;
  private ResourceLocation background;

  private AdvancementsTrackerScreen parent;

  public AdvancementOverviewPanel(AdvancementsTrackerScreen parent, int listWidth, int top,
      int listLeft, int bottom) {
    super(parent.getMinecraftInstance(), listWidth, parent.height, top, bottom,
        parent.getFontRenderer().lineHeight * 4 + 8);
    this.parent = parent;
    this.listWidth = listWidth;
    this.listLeft = listLeft;
    this.setLeftPos(listLeft + 1);
    this.refreshList();
    this.setRenderBackground(false);
  }

  private static String stripControlCodes(String value) {
    return net.minecraft.util.StringUtil.stripColor(value);
  }

  public void refreshList() {
    this.clearEntries();
    if (this.parent.getSelectedRootAdvancement() != null) {
      this.background = this.parent.getSelectedRootAdvancement().background;
    }
    parent.buildChildAdvancementsList(this::addEntry,
        mod -> new ChildAdvancementEntry(mod, this.parent));
  }

  public class ChildAdvancementEntry extends ObjectSelectionList.Entry<ChildAdvancementEntry> {

    private final AdvancementEntry advancementEntry;
    private final AdvancementsTrackerScreen parent;
    private final Font font;
    private final ItemStack icon;
    private final ResourceLocation background;
    private final ResourceLocation icons = new ResourceLocation("minecraft:textures/gui/icons.png");
    private final TextComponent description;
    private final TextComponent title;
    private final boolean isDone;
    private final int completedCriteriaNumber;
    private final int descriptionColor;
    private final int remainingCriteriaNumber;

    ChildAdvancementEntry(AdvancementEntry advancementEntry, AdvancementsTrackerScreen parent) {
      this.advancementEntry = advancementEntry;
      this.background = advancementEntry.background;
      this.completedCriteriaNumber = advancementEntry.completedCriteriaNumber;
      this.description = new TextComponent(stripControlCodes(advancementEntry.description));
      this.descriptionColor = advancementEntry.descriptionColor;
      this.font = parent.getFontRenderer();
      this.icon = advancementEntry.icon;
      this.isDone = advancementEntry.isDone;
      this.parent = parent;
      this.remainingCriteriaNumber = advancementEntry.remainingCriteriaNumber;
      this.title = new TextComponent(stripControlCodes(advancementEntry.title));
    }

    private void renderBackground(PoseStack poseStack, int top, int entryWidth, int entryHeight) {
      if (this.background == null) {
        return;
      }
      RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 1);
      RenderSystem.setShaderTexture(0, this.background);
      poseStack.pushPose();
      GuiComponent.blit(poseStack, getLeft() + 1, top - (2 + font.lineHeight), 0, 0, entryWidth - 2,
          entryHeight + 2, 16, 16);
      poseStack.popPose();
    }

    private void renderIcon(int top) {
      if (this.icon == null) {
        return;
      }
      minecraft.getItemRenderer().renderGuiItem(this.icon, getLeft() + 1, top + 11);
    }

    private void renderProgress(PoseStack poseStack, int top, int entryWidth, int entryHeight,
        int iconWidth) {
      if (this.icons == null) {
        return;
      }

      int progressPositionLeft = getLeft() + iconWidth + 5;
      int progressPositionTop = top + 33;
      int progressWidth = 182;
      double progressTextScale = 0.75;

      // Render empty bar.
      RenderSystem.setShaderColor(1, 1, 1, 1);
      RenderSystem.setShaderTexture(0, this.icons);
      poseStack.pushPose();
      GuiComponent.blit(poseStack, progressPositionLeft,
          progressPositionTop, 0, 64,
          progressWidth, 5, 256, 256);
      poseStack.popPose();

      // Render Green bar with progress and numbers.
      if (this.remainingCriteriaNumber > 0 || this.isDone) {
        int progressTotal = this.completedCriteriaNumber + this.remainingCriteriaNumber;
        int progressDone = this.completedCriteriaNumber;
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderTexture(0, this.icons);
        poseStack.pushPose();
        GuiComponent.blit(poseStack,
            progressPositionLeft,
            progressPositionTop, 0, 69,
            isDone ? progressWidth : (progressWidth / progressTotal * progressDone), 5, 256, 256);
        poseStack.popPose();

        // Only render numbers if we have enough space.
        if (entryWidth > progressWidth + 44) {
        font.draw(poseStack, new TextComponent(progressDone + "/" + progressTotal),
            progressPositionLeft + progressWidth + 5, progressPositionTop -1, ChatFormatting.GREEN.getColor());
        }
      }
    }

    private void renderSeparator(PoseStack poseStack, int top, int entryWidth, int entryHeight) {
      int topPosition = top - 2;
      int leftPosition = getLeft();
      int rightPosition = leftPosition + entryWidth - 1;
      int bottomPosition = top + 1 + entryHeight;

      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder bufferBuilder = tesselator.getBuilder();

      RenderSystem.disableTexture();
      RenderSystem.setShader(GameRenderer::getPositionShader);
      RenderSystem.setShaderColor(1, 1, 1, 1);
      bufferBuilder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
      bufferBuilder.vertex(leftPosition, topPosition, 0).color(1f, 1f, 1f, 1f).endVertex();
      bufferBuilder.vertex(rightPosition, topPosition, 0).color(1f, 1f, 1f, 1f).endVertex();
      bufferBuilder.vertex(rightPosition, topPosition, 0).color(1f, 1f, 1f, 1f).endVertex();
      bufferBuilder.vertex(rightPosition, bottomPosition, 0).color(1f, 1f, 1f, 1f).endVertex();
      bufferBuilder.vertex(rightPosition, bottomPosition, 0).color(1f, 1f, 1f, 1f).endVertex();
      bufferBuilder.vertex(leftPosition, bottomPosition, 0).color(1f, 1f, 1f, 1f).endVertex();
      bufferBuilder.vertex(leftPosition, bottomPosition, 0).color(1f, 1f, 1f, 1f).endVertex();
      bufferBuilder.vertex(leftPosition, topPosition, 0).color(1f, 1f, 1f, 1f).endVertex();
      tesselator.end();
      RenderSystem.enableTexture();
    }

    public AdvancementEntry getAdvancementEntry() {
      return advancementEntry;
    }

    @Override
    public Component getNarration() {
      return new TranslatableComponent("narrator.select", advancementEntry.title);
    }

    @Override
    public void render(PoseStack poseStack, int entryIdx, int top, int left, int entryWidth,
        int entryHeight, int mouseX, int mouseY, boolean p_194999_5_, float partialTick) {
      int iconWidth = 16;
      int maxFontWidth = listWidth - iconWidth - 4;

      // Icon
      this.renderIcon(top);

      // Title (only one line)
      int titleWidth = font.width(title) > maxFontWidth ? maxFontWidth - 6 : maxFontWidth;
      font.drawShadow(poseStack,
          Language.getInstance()
              .getVisualOrder(FormattedText.composite(font.substrByWidth(title, titleWidth))),
          left + iconWidth + (float) 3, top + (float) 1, 0xFFFFFF);
      font.draw(poseStack,
          Language.getInstance()
              .getVisualOrder(FormattedText.composite(font.substrByWidth(title, titleWidth))),
          left + iconWidth + (float) 3, top + (float) 1, 0xFFFFFF);
      if (titleWidth != maxFontWidth) {
        font.draw(poseStack, new TextComponent("…"), left + iconWidth + titleWidth, top + (float) 1,
            0xFFFFFF);
      }

      // Description (two lines)
      List<FormattedCharSequence> descriptionParts = font.split(description, maxFontWidth);
      int descriptionLines = 1;
      for (FormattedCharSequence descriptionPart : descriptionParts) {
        float descriptionTopPosition = top + (float) (2 + font.lineHeight) * descriptionLines;
        font.drawShadow(poseStack, descriptionPart, left + iconWidth + (float) 3,
            descriptionTopPosition, this.descriptionColor);
        font.draw(poseStack, descriptionPart, left + iconWidth + (float) 3, descriptionTopPosition,
            this.descriptionColor);
        if (descriptionParts.size() == 3 && descriptionLines == 2) {
          font.draw(poseStack, new TextComponent("…"),
              left + iconWidth
                  + (float) (font.width(descriptionPart) < maxFontWidth - 6
                      ? font.width(descriptionPart) + 6
                      : maxFontWidth - 6),
              descriptionTopPosition, 0xFFFFFF);
          break;
        }
        descriptionLines++;
      }

      // Progress
      this.renderProgress(poseStack, top, entryWidth, entryHeight, iconWidth);

      // Separator
      this.renderSeparator(poseStack, top, entryWidth, entryHeight);
    }

    @Override
    public boolean mouseClicked(double p_mouseClicked_1_, double p_mouseClicked_3_,
        int p_mouseClicked_5_) {
      parent.setSelectedChildAdvancement(this);
      //AdvancementOverviewPanel.this.setSelected(this);
      return false;
    }
  }

  @Override
  protected int getScrollbarPosition() {
    return this.listWidth + this.listLeft;
  }

  @Override
  public int getRowWidth() {
    return this.listWidth;
  }

  @Override
  protected void renderBackground(PoseStack poseStack) {
    if (this.background == null) {
      return;
    }
    RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 1);
    RenderSystem.setShaderTexture(0, this.background);
    poseStack.pushPose();
    GuiComponent.blit(poseStack, getLeft(), getTop(), 0, 0, getWidth(), getHeight(), 16, 16);
    poseStack.popPose();
  }

}
