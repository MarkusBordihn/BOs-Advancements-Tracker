package de.markusbordihn.advancementstracker.client.advancements;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientAdvancementManager.IListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import de.markusbordihn.advancementstracker.Constants;

@EventBusSubscriber(Dist.CLIENT)
public class AdvancementProgressManager implements IListener {

  private static final Logger log = LogManager.getLogger(Constants.LOG_NAME);
  private static int backgroundAddListenerCheck = 0;
  private static Timer rateControlTimer;
  private static boolean hasAdvancementProgressListener = false;

  protected AdvancementProgressManager() {
  }

  @SubscribeEvent
  public static void handlePlayerLoggedInEvent(PlayerEvent.PlayerLoggedInEvent event) {
    log.info("Try to get advancements progress ...");
    rateControlAddListener();
  }

  public static void rateControlAddListener() {
    if (rateControlTimer != null) {
      rateControlTimer.cancel();
    }
    TimerTask task = new TimerTask() {
      public void run() {
        addListener();
        cancel();
      }
    };
    rateControlTimer = new Timer("Timer");
    rateControlTimer.schedule(task, 250L);
  }

  public static void addListener() {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft == null || minecraft.player == null || minecraft.player.connection == null) {
      if (backgroundAddListenerCheck++ < 250) {
        log.debug("Listener not ready. Will try it later ({})...", backgroundAddListenerCheck);
        rateControlAddListener();
      } else {
        log.error("Unable to add listener after {} tries! Giving up ...", backgroundAddListenerCheck);
      }
      return;
    }
    minecraft.player.connection.getAdvancements().setListener(new AdvancementProgressManager());
    if (!hasAdvancementProgressListener) {
      hasAdvancementProgressListener = true;
    }
  }

  public void onUpdateAdvancementProgress(Advancement advancement, AdvancementProgress advancementProgress) {
    AdvancementsManager.setAdvancementProgress(advancement, advancementProgress);
  }

  @Override
  public void onAddAdvancementRoot(Advancement advancement) {
    // Not used.
  }

  @Override
  public void onRemoveAdvancementRoot(Advancement advancement) {
    // Not used.
  }

  @Override
  public void onAddAdvancementTask(Advancement advancement) {
    // Not used.
  }

  @Override
  public void onRemoveAdvancementTask(Advancement advancement) {
    // Not used.
  }

  @Override
  public void onAdvancementsCleared() {
    // Not used.
  }

  @Override
  public void onSelectedTabChanged(Advancement advancement) {
    // Not used.
  }

  public static boolean hasAdvancementProgressListener() {
    return hasAdvancementProgressListener;
  }

}
