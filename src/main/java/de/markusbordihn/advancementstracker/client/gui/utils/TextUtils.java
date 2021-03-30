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

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.LanguageMap;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;

public class TextUtils {
  private FontRenderer fontRenderer;

  public TextUtils(Minecraft minecraft) {
    this.fontRenderer = minecraft.font;
  }

  public int drawText(MatrixStack matrixStack, String text, int x, int y, int width, int height, int color) {
    return drawText(matrixStack, text, x, y, width, height, color, false, false);
  }

  public int drawText(MatrixStack matrixStack, String text, int x, int y, int width, int color) {
    return drawText(matrixStack, text, x, y, width, 0, color, false, true);
  }

  public int drawTextWithShadow(MatrixStack matrixStack, String text, int x, int y, int width, int height, int color) {
    return drawText(matrixStack, text, x, y, width, height, color, true, false);
  }

  public int drawTextWithShadow(MatrixStack matrixStack, String text, double x, double y, double width, double height,
      int color) {
    return drawText(matrixStack, text, (int) x, (int) y, (int) width, (int) height, color, true, false);
  }

  public int drawText(MatrixStack matrixStack, String text, int x, int y, int width, int height, int color,
      boolean shadow, boolean fullHeight) {
    int maxTextLength = width - 2;
    if (this.fontRenderer.width(text) > maxTextLength) {
      List<IReorderingProcessor> textList = new ArrayList<>();
      textList.addAll(LanguageMap.getInstance()
          .getVisualOrder(this.fontRenderer.getSplitter().splitLines(text, maxTextLength, Style.EMPTY)));
      Float ySplitPosition = (float) y;
      for (IReorderingProcessor textLine : textList) {
        if (fullHeight || ySplitPosition + this.fontRenderer.lineHeight < y + height) {
          if (shadow) {
            this.fontRenderer.drawShadow(matrixStack, textLine, (float) x, ySplitPosition, color);
          } else {
            this.fontRenderer.draw(matrixStack, textLine, (float) x, ySplitPosition, color);
          }
          ySplitPosition = ySplitPosition + this.fontRenderer.lineHeight + 2;
        } else {
          drawTextFromRight(matrixStack, "...", x + width - 2,
              Math.round(ySplitPosition - (this.fontRenderer.lineHeight + 2)), color, shadow);
          break;
        }
      }
      return Math.round(ySplitPosition);
    } else {
      if (shadow) {
        this.fontRenderer.drawShadow(matrixStack, text, (float) x, (float) y, color);
      } else {
        this.fontRenderer.draw(matrixStack, text, (float) x, (float) y, color);
      }
    }
    return y + this.fontRenderer.lineHeight;
  }

  public int drawTextFromRight(MatrixStack matrixStack, String text, int x, int y, int color) {
    return drawTextFromRight(matrixStack, text, x, y, color, false);
  }

  public int drawTextFromRightWithShadow(MatrixStack matrixStack, String text, int x, int y, int color) {
    return drawTextFromRight(matrixStack, text, x, y, color, true);
  }

  public int drawTextFromRight(MatrixStack matrixStack, String text, int x, int y, int color, boolean shadow) {
    int textWidth = this.fontRenderer.width(text);
    if (shadow) {
      this.fontRenderer.drawShadow(matrixStack, text, (float) x - textWidth, (float) y, color);
    } else {
      this.fontRenderer.draw(matrixStack, text, (float) x - textWidth, (float) y, color);
    }
    return textWidth;
  }

  public int drawTrimmedText(MatrixStack matrixStack, String text, int x, int y, int width, int color) {
    return drawTrimmedText(matrixStack, text, x, y, width, color, false);
  }

  public int drawTrimmedTextWithShadow(MatrixStack matrixStack, String text, int x, int y, int width, int color) {
    return drawTrimmedText(matrixStack, text, x, y, width, color, true);
  }

  public int drawTrimmedText(MatrixStack matrixStack, String text, int x, int y, int width, int color, boolean shadow) {
    if (this.fontRenderer.width(text) >= width) {
      ITextComponent textComponent = new StringTextComponent(text);
      ITextProperties trimTextComponent = this.fontRenderer.substrByWidth(textComponent, width - 3);
      if (shadow) {
        this.fontRenderer.drawShadow(matrixStack, trimTextComponent.getString() + "...", (float) x, (float) y, color);
      } else {
        this.fontRenderer.draw(matrixStack, trimTextComponent.getString() + "...", (float) x, (float) y, color);
      }
    } else {
      if (shadow) {
        this.fontRenderer.drawShadow(matrixStack, text, (float) x, (float) y, color);
      } else {
        this.fontRenderer.draw(matrixStack, text, (float) x, (float) y, color);
      }
    }
    return y + this.fontRenderer.lineHeight;
  }

  public static int calculateTextHeight(String text, int width) {
    int maxTextLength = width - 2;
    Minecraft minecraft = Minecraft.getInstance();
    FontRenderer fontRenderer = minecraft.font;
    if (fontRenderer.width(text) > maxTextLength) {
      List<IReorderingProcessor> textList = new ArrayList<>();
      textList.addAll(LanguageMap.getInstance()
          .getVisualOrder(fontRenderer.getSplitter().splitLines(text, maxTextLength, Style.EMPTY)));
      return textList.size() * (fontRenderer.lineHeight + 2);
    }
    return fontRenderer.lineHeight;
  }

}
