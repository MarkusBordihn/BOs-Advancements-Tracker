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

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FormattedCharSequence;

public class TextUtils {
  private Font fontRenderer;

  public TextUtils(Minecraft minecraft) {
    this.fontRenderer = minecraft.font;
  }

  public int drawText(PoseStack matrix, String text, int x, int y, int width, int height,
      int color) {
    return drawText(matrix, text, x, y, width, height, color, false, false);
  }

  public int drawText(PoseStack matrix, String text, int x, int y, int width, int color) {
    return drawText(matrix, text, x, y, width, 0, color, false, true);
  }

  public int drawTextWithShadow(PoseStack matrix, String text, int x, int y, int width, int height,
      int color) {
    return drawText(matrix, text, x, y, width, height, color, true, false);
  }

  public int drawTextWithShadow(PoseStack matrix, String text, double x, double y, double width,
      double height, int color) {
    return drawText(matrix, text, (int) x, (int) y, (int) width, (int) height, color, true, false);
  }

  public int drawText(PoseStack matrix, String text, int x, int y, int width, int height, int color,
      boolean shadow, boolean fullHeight) {
    int maxTextLength = width - 2;
    if (this.fontRenderer.width(text) > maxTextLength) {
      List<FormattedCharSequence> textList = new ArrayList<>();
      textList.addAll(Language.getInstance().getVisualOrder(
          this.fontRenderer.getSplitter().splitLines(text, maxTextLength, Style.EMPTY)));
      Float ySplitPosition = (float) y;
      for (FormattedCharSequence textLine : textList) {
        if (fullHeight || ySplitPosition + this.fontRenderer.lineHeight < y + height) {
          if (shadow) {
            this.fontRenderer.drawShadow(matrix, textLine, (float) x, ySplitPosition, color);
          } else {
            this.fontRenderer.draw(matrix, textLine, (float) x, ySplitPosition, color);
          }
          ySplitPosition = ySplitPosition + this.fontRenderer.lineHeight + 2;
        } else {
          drawTextFromRight(matrix, "...", x + width - 2,
              Math.round(ySplitPosition - (this.fontRenderer.lineHeight + 2)), color, shadow);
          break;
        }
      }
      return Math.round(ySplitPosition);
    } else {
      if (shadow) {
        this.fontRenderer.drawShadow(matrix, text, (float) x, (float) y, color);
      } else {
        this.fontRenderer.draw(matrix, text, (float) x, (float) y, color);
      }
    }
    return y + this.fontRenderer.lineHeight;
  }

  public int drawTextFromRight(PoseStack matrix, String text, int x, int y, int color) {
    return drawTextFromRight(matrix, text, x, y, color, false);
  }

  public int drawTextFromRightWithShadow(PoseStack matrix, String text, int x, int y, int color) {
    return drawTextFromRight(matrix, text, x, y, color, true);
  }

  public int drawTextFromRight(PoseStack matrix, String text, int x, int y, int color,
      boolean shadow) {
    int textWidth = this.fontRenderer.width(text);
    if (shadow) {
      this.fontRenderer.drawShadow(matrix, text, (float) x - textWidth, (float) y, color);
    } else {
      this.fontRenderer.draw(matrix, text, (float) x - textWidth, (float) y, color);
    }
    return textWidth;
  }

  public int drawTrimmedText(PoseStack matrix, String text, int x, int y, int width, int color) {
    return drawTrimmedText(matrix, text, x, y, width, color, false);
  }

  public int drawTrimmedTextWithShadow(PoseStack matrix, String text, int x, int y, int width,
      int color) {
    return drawTrimmedText(matrix, text, x, y, width, color, true);
  }

  public int drawTrimmedText(PoseStack matrix, String text, int x, int y, int width, int color,
      boolean shadow) {
    if (this.fontRenderer.width(text) >= width) {
      TextComponent textComponent = new TextComponent(text);
      FormattedText trimTextComponent = this.fontRenderer.substrByWidth(textComponent, width - 3);
      if (shadow) {
        this.fontRenderer.drawShadow(matrix, trimTextComponent.getString() + "...", (float) x,
            (float) y, color);
      } else {
        this.fontRenderer.draw(matrix, trimTextComponent.getString() + "...", (float) x, (float) y,
            color);
      }
    } else {
      if (shadow) {
        this.fontRenderer.drawShadow(matrix, text, (float) x, (float) y, color);
      } else {
        this.fontRenderer.draw(matrix, text, (float) x, (float) y, color);
      }
    }
    return y + this.fontRenderer.lineHeight;
  }

  public static int calculateTextHeight(String text, int width) {
    int maxTextLength = width - 2;
    Minecraft minecraft = Minecraft.getInstance();
    Font fontRenderer = minecraft.font;
    if (fontRenderer.width(text) > maxTextLength) {
      List<FormattedCharSequence> textList = new ArrayList<>();
      textList.addAll(Language.getInstance()
          .getVisualOrder(fontRenderer.getSplitter().splitLines(text, maxTextLength, Style.EMPTY)));
      return textList.size() * (fontRenderer.lineHeight + 2);
    }
    return fontRenderer.lineHeight;
  }

}
