package de.markusbordihn.advancementstracker.client.gui.widgets;

import java.util.HashSet;
import java.util.Set;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import de.markusbordihn.advancementstracker.client.advancements.AdvancementEntry;
import de.markusbordihn.advancementstracker.client.gui.WidgetBuilder;

@EventBusSubscriber(Dist.CLIENT)
public class TrackerWidget extends WidgetBuilder {

  public static Set<AdvancementEntry> trackedAdvancements = new HashSet<>();

  protected static TrackerWidget trackerWidget;

  TrackerWidget() {
    super(Minecraft.getInstance());
  }

  @SubscribeEvent
  public static void handleRenderGameOverlayEventPre(RenderGameOverlayEvent.Post event) {
    if (trackerWidget == null) {
      trackerWidget = new TrackerWidget();
    }
    MatrixStack matrixStack = event.getMatrixStack();
    int width = 200;
    int height = 200;
    trackerWidget.renderTracker(matrixStack, width, height);
  }

  public void renderTracker(MatrixStack matrixStack, int width, int height) {
    int yPos = 100;
    for (AdvancementEntry advancement : trackedAdvancements) {
      fontRenderer.draw(matrixStack, advancement.title, 400, yPos, 0xFF000000);
      yPos += fontRenderer.lineHeight;
    }
  }

}
