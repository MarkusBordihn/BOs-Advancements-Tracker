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

package de.markusbordihn.advancementstracker.client.gui.utils;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

public class BackgroundUtils {

  private TextureManager textureManager;
  static final float TEXTURE_SCALE = 32.0F;
  static final int BACKGROUND_ALPHA = 0x80;

  public BackgroundUtils(Minecraft minecraft) {
    this.textureManager = minecraft.getTextureManager();
  }

  public void drawBackground(Tessellator tessellator, ResourceLocation background, int x, int y, int width,
      int height) {
    drawBackground(tessellator, background, x, y, width, height, BACKGROUND_ALPHA);
  }

  public void drawBackground(Tessellator tessellator, ResourceLocation background, int x, int y, int width, int height,
      int alpha) {
    if (background != null && background != TextureManager.INTENTIONAL_MISSING_TEXTURE) {
      int xMax = x + width;
      int yMax = y + height;
      textureManager.bind(background);
      BufferBuilder buffer = tessellator.getBuilder();
      buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
      buffer.vertex(x, yMax, 0.0D).color(0x80, 0x80, 0x80, alpha).uv(x / TEXTURE_SCALE, yMax / TEXTURE_SCALE)
          .endVertex();
      buffer.vertex(xMax, yMax, 0.0D).color(0x80, 0x80, 0x80, alpha).uv(xMax / TEXTURE_SCALE, yMax / TEXTURE_SCALE)
          .endVertex();
      buffer.vertex(xMax, y, 0.0D).color(0x80, 0x80, 0x80, alpha).uv(xMax / TEXTURE_SCALE, y / TEXTURE_SCALE)
          .endVertex();
      buffer.vertex(x, y, 0.0D).color(0x80, 0x80, 0x80, alpha).uv(x / TEXTURE_SCALE, y / TEXTURE_SCALE).endVertex();
      tessellator.end();
    }
  }

}
