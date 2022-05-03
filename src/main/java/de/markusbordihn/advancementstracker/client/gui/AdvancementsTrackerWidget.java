/**
 * Copyright 2022 Markus Bordihn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.markusbordihn.advancementstracker.client.gui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Set;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.entity.ItemRenderer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import de.markusbordihn.advancementstracker.Constants;
import de.markusbordihn.advancementstracker.client.advancements.AdvancementEntry;
import de.markusbordihn.advancementstracker.client.advancements.AdvancementsManager;
import de.markusbordihn.advancementstracker.config.ClientConfig;
import de.markusbordihn.advancementstracker.utils.gui.PositionManager;

@EventBusSubscriber(value = Dist.CLIENT)
public class AdvancementsTrackerWidget extends GuiComponent {

  protected static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  private static final ClientConfig.Config CLIENT = ClientConfig.CLIENT;

  private static final int TEXT_COLOR_WHITE = ChatFormatting.WHITE.getColor();

  private final PositionManager positionManager;
  private final Font fontRender;
  private final ItemRenderer itemRenderer;
  private final Minecraft minecraft;

  private static boolean hudVisible = true;

  private int x;
  private int y;

  public AdvancementsTrackerWidget(Minecraft minecraft) {
    this.minecraft = minecraft;
    this.fontRender = minecraft.font;
    this.itemRenderer = minecraft.getItemRenderer();
    positionManager = new PositionManager(minecraft);
    positionManager.setInstance(minecraft);
    positionManager.setWidth(81);
    positionManager.setHeight(22);
  }

  @SubscribeEvent
  public static void handleWorldEventLoad(WorldEvent.Load event) {}

  @SubscribeEvent()
  public void renderOverlay(RenderGameOverlayEvent.Pre event) {
    if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
      return;
    }
    if (!hudVisible) {
      return;
    }


    // Position Manager Updates
    positionManager.updateWindow();
    positionManager.setPosition(positionManager.getMiddleRight());
    positionManager.setWidth(140);
    positionManager.setHeight(160);
    x = positionManager.getPositionX();
    y = positionManager.getPositionY();

    PoseStack poseStack = event.getMatrixStack();

    poseStack.pushPose();
    renderBackground(poseStack);
    renderTitle(poseStack);
    poseStack.popPose();

    // List tracked advancement
    poseStack.pushPose();
    renderAdvancements(poseStack, x, y + this.fontRender.lineHeight + 4);
    poseStack.popPose();
  }

  public void renderBackground(PoseStack poseStack) {
    fill(poseStack, x, y, positionManager.getPositionXWidth(), positionManager.getPositionYHeight(),
        1325400064);
  }

  public void renderTitle(PoseStack poseStack) {
    fill(poseStack, x, y, positionManager.getPositionXWidth(), y + this.fontRender.lineHeight + 2,
        1325400064);
    fontRender.draw(poseStack, "Advancements Tracker", x + 2.0f, y + 2.0f, TEXT_COLOR_WHITE);
  }

  public void renderAdvancements(PoseStack poseStack, int x, int y) {
    int topPos = y;
    Set<AdvancementEntry> rootAdvancements = AdvancementsManager.getRootAdvancements();
    for (AdvancementEntry rootAdvancement : rootAdvancements) {
      fontRender.draw(poseStack, rootAdvancement.title, x + 10.0f, topPos, TEXT_COLOR_WHITE);
      topPos += fontRender.lineHeight + 2;
    }
  }

  public static void toggleVisibility() {
    hudVisible = !hudVisible;
  }

}
