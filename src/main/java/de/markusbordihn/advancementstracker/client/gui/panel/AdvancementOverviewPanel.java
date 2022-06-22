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
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.ObjectSelectionList;
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
import de.markusbordihn.advancementstracker.client.advancements.TrackedAdvancementsManager;
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

    // Build child advancements list.
    parent.buildChildAdvancementsList(this::addEntry,
        mod -> new ChildAdvancementEntry(mod, this.parent));

    // Reset scroll bar
    setScrollAmount(0);
  }

  public class ChildAdvancementEntry extends ObjectSelectionList.Entry<ChildAdvancementEntry> {

    private static final ResourceLocation icons =
        new ResourceLocation("minecraft:textures/gui/icons.png");
    private static final ResourceLocation miscTexture =
        new ResourceLocation(Constants.MOD_ID, "textures/gui/misc.png");

    private final AdvancementEntry advancementEntry;
    private final AdvancementsTrackerScreen parent;
    private final Font font;
    private final ItemStack icon;
    private final ResourceLocation background;
    private final TextComponent description;
    private final TextComponent title;
    private final boolean isDone;
    private final int completedCriteriaNumber;
    private final int descriptionColor;
    private final int remainingCriteriaNumber;
    private int relativeLeftPosition;
    private int relativeTopPosition;

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
      RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 0.25f);
      RenderSystem.setShaderTexture(0, this.background);
      poseStack.pushPose();
      GuiComponent.blit(poseStack, getLeft() + 1, top - 1, 0, 0, entryWidth - 2, entryHeight + 1,
          16, 16);
      poseStack.popPose();
    }

    private void renderIcon(int top) {
      if (this.icon == null) {
        return;
      }
      minecraft.getItemRenderer().renderGuiItem(this.icon, getLeft() + 3, top + 2);
    }

    private void renderProgress(PoseStack poseStack, int top, int entryWidth, int entryHeight,
        int iconWidth) {
      int progressPositionLeft = getLeft() + iconWidth + 5;
      int progressPositionTop = top + 33;
      int progressWidth = 182;

      // Render empty bar.
      RenderSystem.setShaderColor(1, 1, 1, 1);
      RenderSystem.setShaderTexture(0, this.icons);
      poseStack.pushPose();
      GuiComponent.blit(poseStack, progressPositionLeft, progressPositionTop, 0, 64, progressWidth,
          5, 256, 256);
      poseStack.popPose();

      // Render Green bar with progress and numbers.
      if (this.remainingCriteriaNumber > 0 || this.isDone) {
        int progressTotal = this.completedCriteriaNumber + this.remainingCriteriaNumber;
        int progressDone = this.completedCriteriaNumber;
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderTexture(0, this.icons);
        poseStack.pushPose();
        GuiComponent.blit(poseStack, progressPositionLeft, progressPositionTop, 0, 69,
            isDone ? progressWidth : (progressWidth / progressTotal * progressDone), 5, 256, 256);
        poseStack.popPose();

        // Only render numbers if we have enough space.
        if (entryWidth > progressWidth + 42) {
          float scaling = 0.75f;
          float positionScaling = 1.33f;
          poseStack.pushPose();
          poseStack.scale(scaling, scaling, scaling);
          font.draw(poseStack, new TextComponent(progressDone + "/" + progressTotal),
              (progressPositionLeft + progressWidth + 5) * positionScaling,
              (progressPositionTop) * positionScaling,
              this.remainingCriteriaNumber >= 1 ? ChatFormatting.YELLOW.getColor()
                  : ChatFormatting.GREEN.getColor());
          poseStack.popPose();
        }
      }
    }

    private void renderSeparator(PoseStack poseStack, int top, int entryWidth, int entryHeight) {
      int topPosition = top - 2;
      int leftPosition = getLeft();
      int rightPosition = leftPosition + entryWidth - 2;
      int bottomPosition = top + entryHeight;

      RenderSystem.setShaderColor(1, 1, 1, 1);
      RenderSystem.setShaderTexture(0, miscTexture);
      poseStack.pushPose();
      GuiComponent.blit(poseStack, leftPosition, topPosition, 0, 0, entryWidth - 1, 1,
          entryWidth - 1, 256);
      GuiComponent.blit(poseStack, rightPosition, topPosition + 1, 255, 0, 1, entryHeight + 2, 256,
          entryHeight);
      GuiComponent.blit(poseStack, leftPosition, bottomPosition, 0, 255, entryWidth - 1, 1,
          entryWidth - 1, 256);
      GuiComponent.blit(poseStack, leftPosition, topPosition + 1, 0, 0, 1, entryHeight + 2, 256,
          entryHeight);
      poseStack.popPose();
    }

    private void renderTrackingCheckbox(PoseStack poseStack, int top, int left) {
      int iconPosition = 22;
      if (this.isDone) {
        iconPosition = 3;
      } else if (TrackedAdvancementsManager.isTrackedAdvancement(this.advancementEntry)) {
        iconPosition = 42;
      }
      RenderSystem.setShaderColor(1, 1, 1, 1);
      RenderSystem.setShaderTexture(0, miscTexture);
      poseStack.pushPose();
      GuiComponent.blit(poseStack, left + 2, top + 23, iconPosition, 6, 15, 15, 256, 256);
      poseStack.popPose();
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
      int iconWidth = 18;
      int maxFontWidth = listWidth - iconWidth - 4;
      this.relativeLeftPosition = left;
      this.relativeTopPosition = top;

      // Background
      this.renderBackground(poseStack, top, entryWidth, entryHeight);

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

      // Checkbox for enabling tracking
      this.renderTrackingCheckbox(poseStack, top, left);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button != 0) {
        return false;
      }
      double relativeX = mouseX - this.relativeLeftPosition;
      double relativeY = mouseY - this.relativeTopPosition;
      if ((relativeX > 3 && relativeX < 15) && (relativeY > 25 && relativeX < 35)) {
        TrackedAdvancementsManager.toggleTrackedAdvancement(this.getAdvancementEntry());
      }
      parent.setSelectedChildAdvancement(this);
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
    // this.parent.renderBackground(poseStack);
  }

}
