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

  private static float scaling = 0.5f;
  private int scaledX;
  private int scaledY;
  private int scaledWidth;
  private int scaledHeight;

  public SmallButton(int x, int y, int width, int height, Component component, OnPress onPress) {
    super(x, y, width, height, component, onPress);
    // calculateScaledPositionAndSize();
  }

  public SmallButton(int x, int y, int width, int height, Component component,
      Button.OnPress onPress, Button.OnTooltip onTooltip) {
    super(x, y, width, height, component, onPress, onTooltip);
    // calculateScaledPositionAndSize();
  }

  private void calculateScaledPositionAndSize() {
    this.scaledX = Math.round(this.x / scaling);
    this.scaledY = Math.round(this.y / scaling);
    this.scaledWidth = Math.round(this.width / scaling);
    this.scaledHeight = Math.round(this.height / scaling);
  }

  @Override
  public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
    scaledX = Math.round(this.x / scaling);
    scaledY = Math.round(this.y / scaling);
    scaledWidth = Math.round(this.width / scaling);
    scaledHeight = Math.round(this.height / scaling);

    Minecraft minecraft = Minecraft.getInstance();
    Font font = minecraft.font;
    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
    int i = this.getYImage(this.isHoveredOrFocused());
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    RenderSystem.enableDepthTest();

    // Scaling down the button image
    int buttonPosTop = (46 + i * 20) / 2;
    blit(poseStack, this.x, this.y, 0, buttonPosTop, this.width / 2, this.height, 256, 128);
    blit(poseStack, this.x + this.width / 2, this.y, 200 - this.width / 2, buttonPosTop,
        this.width / 2, this.height, 256, 128);
    this.renderBg(poseStack, minecraft, mouseX, mouseY);
    int j = getFGColor();

    // Scaling Button Text
    poseStack.pushPose();
    poseStack.scale(scaling, scaling, scaling);
    drawCenteredString(poseStack, font, this.getMessage(), this.scaledX + this.scaledWidth / 2,
        this.scaledY + (this.scaledHeight - 8) / 2, j | Mth.ceil(this.alpha * 255.0F) << 24);
    poseStack.popPose();

    if (this.isHoveredOrFocused()) {
      this.renderToolTip(poseStack, mouseX, mouseY);
    }

  }

}
