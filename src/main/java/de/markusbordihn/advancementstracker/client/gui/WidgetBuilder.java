package de.markusbordihn.advancementstracker.client.gui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.markusbordihn.advancementstracker.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.util.text.TranslationTextComponent;

public class WidgetBuilder extends IngameGui {

  public static final Logger log = LogManager.getLogger(Constants.LOG_NAME);
  public FontRenderer fontRenderer;

  public WidgetBuilder() {
    super(Minecraft.getInstance());
    fontRenderer = minecraft.font;
  }

  public WidgetBuilder(Minecraft minecraft) {
    super(minecraft);
    fontRenderer = minecraft.font;
  }

  public static TranslationTextComponent getText(String translationKey) {
    return new TranslationTextComponent(Constants.MOD_PREFIX + translationKey);
  }

  public static TranslationTextComponent getText(String translationKey, Object object) {
    return new TranslationTextComponent(Constants.MOD_PREFIX + translationKey, object);
  }

}
