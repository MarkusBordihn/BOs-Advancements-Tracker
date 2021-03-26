package de.markusbordihn.advancementstracker.client.screen;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import de.markusbordihn.advancementstracker.Constants;
import de.markusbordihn.advancementstracker.client.gui.config.ConfigScreen;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ScreenManager {

  private static final Logger log = LogManager.getLogger(Constants.LOG_NAME);
  private static Timer timer = new Timer("Timer");

  protected ScreenManager() {
  }

  @SubscribeEvent
  public static void handleFMLClientSetupEvent(FMLClientSetupEvent event) {
    ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY,
        () -> (mc, screen) -> new ConfigScreen(screen));
  }

  public static void saveScreenshot(File folder, String name, Long delay) {
    if (!folder.exists()) {
      folder.mkdirs();
    }
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft == null || minecraft.getMainRenderTarget() == null) {
      return;
    }
    Framebuffer framebuffer = minecraft.getMainRenderTarget();
    String screenshotName = String.format("%s-%s.png", name,
        new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date()));
    Consumer<ITextComponent> messageConsumer = unused -> new StringTextComponent("");
    TimerTask task = new TimerTask() {
      public void run() {
        log.info("Saving screenshot {} under {}", screenshotName, folder);
        ScreenShotHelper.grab(folder, screenshotName, framebuffer.width, framebuffer.height, framebuffer,
            messageConsumer);
        cancel();
      }
    };
    timer.schedule(task, delay);
  }
}
