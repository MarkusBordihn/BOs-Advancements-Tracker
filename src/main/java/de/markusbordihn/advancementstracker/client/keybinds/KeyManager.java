package de.markusbordihn.advancementstracker.client.keybinds;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import de.markusbordihn.advancementstracker.client.gui.overview.OverviewScreen;
import de.markusbordihn.advancementstracker.client.gui.widgets.TrackerWidget;

@EventBusSubscriber(value = Dist.CLIENT)
public class KeyManager {

  protected KeyManager() {
  }

  @SubscribeEvent
  public static void handleKeyInputEvent(InputEvent.KeyInputEvent event) {
      if (KeyBindings.SHOW_OVERVIEW.isDown()) {
        Minecraft.getInstance().setScreen(new OverviewScreen());
      } else if (KeyBindings.SHOW_GUI.isDown()) {
        // Minecraft.getInstance().setScreen(new TrackerWidget());
      }
  }

}
