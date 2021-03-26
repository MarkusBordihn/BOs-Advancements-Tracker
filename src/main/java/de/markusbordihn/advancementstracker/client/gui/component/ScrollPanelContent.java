package de.markusbordihn.advancementstracker.client.gui.component;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.LanguageMap;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;

public abstract class ScrollPanelContent extends AbstractGui {

  private static final int BAR_WIDTH = 6;
  protected FontRenderer fontRenderer;
  protected Minecraft minecraft;
  protected ResourceLocation background;
  protected String name;
  protected TextureManager textureManager;
  protected boolean hasScrollBar = false;
  protected boolean isActive = false;
  protected float scrollDistance;
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

  protected ScrollPanelContent(String contentName, int width, int height) {
    this.name = contentName;
    this.width = width;
    this.height = height;
  }

  protected void drawContent(MatrixStack matrixStack, int entryRight, int relativeY, Tessellator tessellator,
      int mouseX, int mouseY) {
  }

  protected void drawBackground(MatrixStack matrixStack, Tessellator tessellator) {
    if (this.background != null && this.background != TextureManager.INTENTIONAL_MISSING_TEXTURE) {
      this.textureManager.bind(this.background);
      BufferBuilder buffer = tessellator.getBuilder();
      buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
      buffer.vertex(this.x, this.yMax, 0.0D).color(0x50, 0x50, 0x50, 0xFF)
          .uv(this.x / TEXTURE_SCALE, (this.yMax + (int) this.scrollDistance) / TEXTURE_SCALE).endVertex();
      buffer.vertex(this.xMax, this.yMax, 0.0D).color(0x50, 0x50, 0x50, 0xFF)
          .uv(this.xMax / TEXTURE_SCALE, (this.yMax + (int) this.scrollDistance) / TEXTURE_SCALE).endVertex();
      buffer.vertex(this.xMax, this.y, 0.0D).color(0x50, 0x50, 0x50, 0xFF)
          .uv(this.xMax / TEXTURE_SCALE, (this.y + (int) this.scrollDistance) / TEXTURE_SCALE).endVertex();
      buffer.vertex(this.x, this.y, 0.0D).color(0x50, 0x50, 0x50, 0xFF)
          .uv(this.x / TEXTURE_SCALE, (this.y + (int) this.scrollDistance) / TEXTURE_SCALE).endVertex();
      tessellator.end();
    }
  }

  protected int drawText(MatrixStack matrixStack, String text, int x, int y, int color) {
    int maxTextLength = this.xMax - x - 5;
    if (this.fontRenderer.width(text) > maxTextLength) {
      List<IReorderingProcessor> textList = new ArrayList<>();
      textList.addAll(LanguageMap.getInstance().getVisualOrder(this.fontRenderer.getSplitter().splitLines(text, maxTextLength, Style.EMPTY)));
      Float ySplitPosition = (float) y;
      for (IReorderingProcessor textLine : textList) {
        if (ySplitPosition + fontRenderer.lineHeight < yMax) {
          RenderSystem.enableBlend();
          this.fontRenderer.draw(matrixStack, textLine, (float) x, ySplitPosition, color);
          ySplitPosition = ySplitPosition + fontRenderer.lineHeight + 2;
          RenderSystem.disableBlend();
        }
      }
      return Math.round(ySplitPosition);
    } else {
      this.fontRenderer.draw(matrixStack, text, (float) x, (float) y, color);
    }
    return y + fontRenderer.lineHeight;
  }

  protected int drawTextWithShadow(MatrixStack matrixStack, String text, int x, int y, int color) {
    fontRenderer.drawShadow(matrixStack, text, (float) x, (float) y, color);
    return y + fontRenderer.lineHeight;
  }

  protected int drawTrimmedTextWithShadow(MatrixStack matrixStack, String text, int width, int x, int y, int color) {
    if (fontRenderer.width(text) >= width) {
      ITextComponent textComponent = new StringTextComponent(text);
      ITextProperties trimTextComponent = fontRenderer.substrByWidth(textComponent, width - 3);
      fontRenderer.drawShadow(matrixStack, trimTextComponent.getString() + "...", (float) x, (float) y, color);
    } else {
      fontRenderer.drawShadow(matrixStack, text, (float) x, (float) y, color);
    }
    return y + fontRenderer.lineHeight;
  }

  protected void handleClick(double mouseX, double mouseY, int button) {

  }

  public void setMinecraftInstance(Minecraft minecraft) {
    this.minecraft = minecraft;
    this.fontRenderer = minecraft.font;
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
