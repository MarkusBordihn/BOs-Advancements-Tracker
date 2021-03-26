package de.markusbordihn.advancementstracker.client.keybinds;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import de.markusbordihn.advancementstracker.Constants;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KeyBindings {

  protected KeyBindings() {
  }

  private static final int KEY_L = 76;

  public static final KeyBinding SHOW_OVERVIEW = new KeyBinding(
      new TranslationTextComponent(Constants.MOD_PREFIX + "keys.showOverview").getString(), KeyConflictContext.IN_GAME,
      KeyModifier.CONTROL, InputMappings.Type.KEYSYM, KEY_L,
      new TranslationTextComponent(Constants.MOD_PREFIX + "keys.category").getString());

  public static final KeyBinding SHOW_GUI = new KeyBinding(
      new TranslationTextComponent(Constants.MOD_PREFIX + "keys.showGui").getString(), KeyConflictContext.IN_GAME,
      KeyModifier.ALT, InputMappings.Type.KEYSYM, KEY_L,
      new TranslationTextComponent(Constants.MOD_PREFIX + "keys.category").getString());

  @SubscribeEvent
  public static void setupClient(FMLClientSetupEvent event) {
    ClientRegistry.registerKeyBinding(KeyBindings.SHOW_OVERVIEW);
    ClientRegistry.registerKeyBinding(KeyBindings.SHOW_GUI);
  }
}
