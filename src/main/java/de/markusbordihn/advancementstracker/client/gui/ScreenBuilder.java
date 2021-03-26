package de.markusbordihn.advancementstracker.client.gui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.markusbordihn.advancementstracker.Constants;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.TranslationTextComponent;

public abstract class ScreenBuilder extends Screen {

  protected ScreenBuilder(String title) {
    super(getText(title));
  }

  protected ScreenBuilder(String title, Object object) {
    super(getText(title, object));
  }

  public static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  public static TranslationTextComponent getText(String translationKey) {
    return new TranslationTextComponent(Constants.MOD_PREFIX + translationKey);
  }

  public static TranslationTextComponent getText(String translationKey, Object object) {
    return new TranslationTextComponent(Constants.MOD_PREFIX + translationKey, object);
  }

}
