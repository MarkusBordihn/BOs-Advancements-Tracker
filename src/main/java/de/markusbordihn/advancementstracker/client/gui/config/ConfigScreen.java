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

package de.markusbordihn.advancementstracker.client.gui.config;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.TranslationTextComponent;
import com.mojang.blaze3d.matrix.MatrixStack;

import de.markusbordihn.advancementstracker.Constants;
import de.markusbordihn.advancementstracker.client.gui.ScreenBuilder;

public final class ConfigScreen extends ScreenBuilder {
  private final Screen parentScreen;

  public ConfigScreen(Screen parentScreen) {
    super("configScreen.title", Constants.MOD_NAME);
    this.parentScreen = parentScreen;
  }

  @Override
  protected void init() {
    this.addButton(new Button((this.width - 200) / 2, this.height - 26, 200, 20,
        new TranslationTextComponent("gui.done"), button -> this.onClose()));
  }

  @Override
  public void onClose() {
    this.minecraft.setScreen(this.parentScreen);
  }

  @Override
  public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    this.renderBackground(matrixStack);
    net.minecraft.client.gui.AbstractGui.drawCenteredString(matrixStack, this.font, this.title,
        width / 2, 8, 0xFFF);
    super.render(matrixStack, mouseX, mouseY, partialTicks);
  }
}
