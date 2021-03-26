package de.markusbordihn.advancementstracker.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;

import de.markusbordihn.advancementstracker.Constants;

@EventBusSubscriber(value = Dist.DEDICATED_SERVER)
public class ServerHandler {

  public static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  protected ServerHandler() {
  }

  @SubscribeEvent
  public static void handleServerStartingEvent(FMLServerStartingEvent event) {
    log.info("This mod doesn't need to be run on the server. ^_-");
  }
}
