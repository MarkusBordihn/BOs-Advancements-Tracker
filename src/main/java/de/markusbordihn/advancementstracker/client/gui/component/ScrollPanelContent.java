/**
 * Copyright 2021 Markus Bordihn
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

package de.markusbordihn.advancementstracker.client.gui.component;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import de.markusbordihn.advancementstracker.client.gui.utils.BackgroundUtils;
import de.markusbordihn.advancementstracker.client.gui.utils.TextUtils;

public abstract class ScrollPanelContent extends AbstractGui {

  private static final int BAR_WIDTH = 6;
  protected FontRenderer fontRenderer;
  protected Minecraft minecraft;
  protected ResourceLocation background;
  protected String name;
  protected TextUtils textUtils;
  protected TextureManager textureManager;
  protected boolean hasScrollBar = false;
  protected boolean isActive = false;
  protected float scrollDistance;
  protected int backgroundAlpha = 0x80;
  protected int baseX;
  protected int baseY;
  protected int height;
  protected int relativeX = 0;
  protected int relativeY = 0;
  protected int width;
  protected int x;
  protected int xMax;
  protected int y;
  protected int yMax;
  static final float TEXTURE_SCALE = 32.0F;
  private BackgroundUtils backgroundUtils;

  protected ScrollPanelContent(String contentName, int width, int height) {
    this.name = contentName;
    this.width = width;
    this.height = height;
  }

  protected void drawContent(MatrixStack matrixStack, int entryRight, int relativeY, Tessellator tessellator,
      int mouseX, int mouseY) {
  }

  protected void drawBackground(MatrixStack matrixStack, Tessellator tessellator) {
    this.backgroundUtils.drawBackground(tessellator, this.background, this.x, this.y, this.width, this.height);
  }

  protected int drawText(MatrixStack matrixStack, String text, int x, int y, int color) {
    return textUtils.drawText(matrixStack, text, x, y, width - (x - this.x), height - (y - this.y), color);
  }

  protected int drawTextWithShadow(MatrixStack matrixStack, String text, int x, int y, int color) {
    return textUtils.drawTextWithShadow(matrixStack, text, x, y, width - (x - this.x), height - (y - this.y), color);
  }

  protected int drawTrimmedTextWithShadow(MatrixStack matrixStack, String text, int x, int y, int width, int color) {
    return this.textUtils.drawTrimmedTextWithShadow(matrixStack, text, x, y, width, color);
  }

  protected void handleClick(double mouseX, double mouseY, int button) {

  }

  public void setMinecraftInstance(Minecraft minecraft) {
    this.backgroundUtils = new BackgroundUtils(minecraft);
    this.fontRenderer = minecraft.font;
    this.minecraft = minecraft;
    this.textUtils = new TextUtils(minecraft);
    this.textureManager = minecraft.getTextureManager();
  }

  public void setPosition(int x, int y) {
    this.baseX = x;
    this.baseY = y;
    this.x = this.baseX + this.relativeX;
    this.y = this.baseY + this.relativeY;
    this.xMax = this.x + this.width;
    this.yMax = this.y + this.height;
  }

  public void setRelativeX(int x) {
    if (this.relativeX == x) {
      return;
    }
    this.relativeX = x;
    this.x = this.baseX + this.relativeX;
    this.xMax = this.x + this.width;
  }

  public void setRelativeY(int y) {
    if (this.relativeY == y) {
      return;
    }
    this.relativeY = y;
    this.y = this.baseY + this.relativeY;
    this.yMax = this.y + this.height;
  }

  public boolean isInsideEventArea(int mouseX, int mouseY) {
    return this.x <= mouseX && mouseX < this.xMax && this.y <= mouseY && mouseY < this.yMax;
  }

  public boolean isInsideEventAreaY(int mouseY) {
    return this.y <= mouseY && mouseY < this.yMax;
  }

  public String getContentName() {
    return this.name;
  }

  public void hasScrollBar(boolean hasScrollBar) {
    if (this.hasScrollBar == hasScrollBar) {
      return;
    }
    this.hasScrollBar = hasScrollBar;
    if (hasScrollBar) {
      this.width -= BAR_WIDTH;
      this.xMax = this.x + this.width;
    } else {
      this.width += BAR_WIDTH;
      this.xMax = this.x + this.width;
    }
  }

}
