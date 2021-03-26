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
    // ModSettings.save();
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
