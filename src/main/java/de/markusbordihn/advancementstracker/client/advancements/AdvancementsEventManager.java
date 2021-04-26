package de.markusbordihn.advancementstracker.client.advancements;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.advancements.Advancement;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import de.markusbordihn.advancementstracker.Constants;

@EventBusSubscriber(Dist.CLIENT)
public class AdvancementsEventManager {

  private static final Logger log = LogManager.getLogger(Constants.LOG_NAME);
  private static int numberOfAdvancements = 0;

  protected AdvancementsEventManager() {}

  @SubscribeEvent
  public static void handleWorldEventLoad(WorldEvent.Load event) {
    if (!event.getWorld().isClientSide()) {
      return;
    }
    reset();
  }

  @SubscribeEvent
  public static void handleAdvancementEvent(AdvancementEvent advancementEvent) {
    Advancement advancement = advancementEvent.getAdvancement();
    if (ClientAdvancementManager.isValidAdvancement(advancement)) {
      log.debug("[Advancement Event] {}", advancement);
      String advancementId = advancement.getId().toString();
      Advancement rootAdvancement = advancement.getParent();
      if (rootAdvancement == null) {
        if (advancementId.contains("/root") || advancementId.contains(":root")) {
          ClientAdvancementManager.reset();
        }
        AdvancementsManager.addAdvancementRoot(advancement);
      } else {
        while (rootAdvancement.getParent() != null) {
          rootAdvancement = rootAdvancement.getParent();
        }
        AdvancementsManager.addAdvancementRoot(rootAdvancement);
        AdvancementsManager.addAdvancementTask(advancement);
      }

      // Make sure that we are covering changes which are not catch by the advancements events.
      Minecraft minecraft = Minecraft.getInstance();
      int possibleNumberOfAdvancements = minecraft.player.connection.getAdvancements()
          .getAdvancements().getAllAdvancements().size();
      if (possibleNumberOfAdvancements > numberOfAdvancements) {
        log.debug("Force sync of advancements because it seems we are missing some {} > {}",
            possibleNumberOfAdvancements, numberOfAdvancements);
        ClientAdvancementManager.reset();
        numberOfAdvancements = possibleNumberOfAdvancements;
      }
    }
  }

  public static void reset() {
    numberOfAdvancements = 0;
  }

}
