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

package de.markusbordihn.advancementstracker.client.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class SmallButton extends Button {

  private final Minecraft minecraft;
  private final Font font;

  private static final float SCALING = 0.55f;
  private final int scaledX;
  private final int scaledY;
  private final int scaledWidth;
  private final int scaledHeight;

  public SmallButton(int x, int y, int width, int height, Component component, OnPress onPress) {
    super(x, y, width, height, component, onPress, Button.DEFAULT_NARRATION);
    this.minecraft = Minecraft.getInstance();
    this.font = this.minecraft.font;
    this.scaledX = Math.round(this.getX() / SCALING);
    this.scaledY = Math.round(this.getY() / SCALING);
    this.scaledWidth = Math.round(this.width / SCALING);
    this.scaledHeight = Math.round(this.height / SCALING);
  }

  @Override
  public void renderWidget(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    RenderSystem.enableDepthTest();

    // Scaling down the button images
    int buttonPosTop = getTextureY() / 2;
    blit(poseStack, this.getX(), this.getY(), 0, buttonPosTop, this.width / 2, this.height, 256,
        128);
    blit(poseStack, this.getX() + this.width / 2, this.getY(), 200 - this.width / 2.0f,
        buttonPosTop, this.width / 2, this.height, 256, 128);

    // Scaling down button text
    poseStack.pushPose();
    poseStack.scale(SCALING, SCALING, SCALING);
    drawCenteredString(poseStack, font, this.getMessage(), this.scaledX + this.scaledWidth / 2,
        this.scaledY + (this.scaledHeight - 8) / 2,
        getFGColor() | Mth.ceil(this.alpha * 255.0F) << 24);
    poseStack.popPose();
  }

  private int getTextureY() {
    int i = 1;
    if (!this.active) {
      i = 0;
    } else if (this.isHoveredOrFocused()) {
      i = 2;
    }

    return 46 + i * 20;
  }

}
