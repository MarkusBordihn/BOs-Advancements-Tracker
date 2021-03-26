package de.markusbordihn.advancementstracker.config;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.config.ModConfig;

import de.markusbordihn.advancementstracker.Constants;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class ClientConfig {

  private static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  private ClientConfig() {
  }

  static final ForgeConfigSpec clientSpec;
  public static final Config CLIENT;
  static {
    final Pair<Config, ForgeConfigSpec> specPair =
        new ForgeConfigSpec.Builder().configure(Config::new);
    clientSpec = specPair.getRight();
    CLIENT = specPair.getLeft();
    log.info("Registering client config ...");
    ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, clientSpec);
  }

  public static class Config {
    public final ForgeConfigSpec.IntValue maxNumberOfTrackedAdvancements;

    Config(ForgeConfigSpec.Builder builder) {
      builder.comment("Advancements Tracker (Client configuration)");

      builder.push("general");
      maxNumberOfTrackedAdvancements = builder.defineInRange("maxNumberOfTrackedAdvancements", 5, 1, 10);
      builder.pop();

      builder.push("gui");
      builder.pop();
    }
  }

  @SubscribeEvent
  public static void handleModConfigLoadEvent(ModConfig.Loading event) {
    log.info("Loading client config file {}", event.getConfig().getFileName());
  }

  @SubscribeEvent
  public static void handleModConfigReloadEvent(ModConfig.Reloading event) {
    log.info("Reloading client config file {}", event.getConfig().getFileName());
  }

}
