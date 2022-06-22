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
import net.minecraft.client.Minecraft;
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
import de.markusbordihn.advancementstracker.client.gui.screens.AdvancementsTrackerScreen;

public class AdvancementCategoryPanel
    extends ObjectSelectionList<AdvancementCategoryPanel.RootAdvancementEntry> {

  protected static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  private final int listLeft;
  private final int listWidth;
  private final Minecraft minecraft;

  private AdvancementsTrackerScreen parent;

  public AdvancementCategoryPanel(AdvancementsTrackerScreen parent, int listWidth, int top,
      int listLeft, int bottom) {
    super(parent.getMinecraftInstance(), listWidth, parent.height, top, bottom,
        parent.getFontRenderer().lineHeight * 3 + 8);
    this.parent = parent;
    this.minecraft = parent.getMinecraft();
    this.listWidth = listWidth;
    this.listLeft = listLeft;
    this.setLeftPos(listLeft);
    this.refreshList();
    this.setRenderBackground(false);
  }

  private static String stripControlCodes(String value) {
    return net.minecraft.util.StringUtil.stripColor(value);
  }

  public void refreshList() {
    this.clearEntries();

    // Build root advancements list.
    parent.buildRootAdvancementsList(this::addEntry,
        mod -> new RootAdvancementEntry(mod, this.parent));

    // Pre-select first entry if we have nothing selected.
    log.info("{}", getEntry(0));
    if (this.getSelected() == null && getItemCount() > 0 && getEntry(0) != null) {
      RootAdvancementEntry rootAdvancementEntry = getEntry(0);
      log.info("Pre-select {}", rootAdvancementEntry);
      parent.setSelectedRootAdvancement(rootAdvancementEntry);
      this.setSelected(rootAdvancementEntry);
    }
  }

  public class RootAdvancementEntry extends ObjectSelectionList.Entry<RootAdvancementEntry> {

    private final AdvancementEntry advancementEntry;
    private final AdvancementsTrackerScreen parent;
    private final ResourceLocation background;
    private final ItemStack icon;
    private final TextComponent title;
    private final TextComponent description;
    private final int descriptionColor;

    RootAdvancementEntry(AdvancementEntry advancementEntry, AdvancementsTrackerScreen parent) {
      this.advancementEntry = advancementEntry;
      this.background = advancementEntry.background;
      this.description = new TextComponent(stripControlCodes(advancementEntry.description));
      this.descriptionColor = advancementEntry.descriptionColor;
      this.icon = advancementEntry.icon;
      this.parent = parent;
      this.title = new TextComponent(stripControlCodes(advancementEntry.title));
    }

    private void renderBackground(PoseStack poseStack, int top, int entryWidth, int entryHeight) {
      if (this.background == null) {
        return;
      }
      RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 1);
      RenderSystem.setShaderTexture(0, this.background);
      poseStack.pushPose();
      GuiComponent.blit(poseStack, getLeft() + 1, top - 1, 0, 0, entryWidth - 2, entryHeight + 2,
          16, 16);
      poseStack.popPose();
    }

    private void renderIcon(int top) {
      if (this.icon == null) {
        return;
      }
      minecraft.getItemRenderer().renderGuiItem(this.icon, getLeft() + 1, top + 6);
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

      int iconWidth = 14;
      int maxFontWidth = listWidth - iconWidth - 4;

      // Background
      this.renderBackground(poseStack, top, entryWidth, entryHeight);

      Font font = this.parent.getFontRenderer();

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
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button != 0) {
        return false;
      }
      parent.setSelectedRootAdvancement(this);
      setSelected(this);
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
    this.parent.renderBackground(poseStack);
  }

}
