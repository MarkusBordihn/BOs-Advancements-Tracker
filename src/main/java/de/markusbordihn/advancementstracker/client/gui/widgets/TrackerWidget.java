/**
 * Copyright 2021 Markus Bordihn
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

package de.markusbordihn.advancementstracker.client.gui.widgets;

import java.util.HashSet;
import java.util.Set;

import com.mojang.blaze3d.matrix.MatrixStack;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import de.markusbordihn.advancementstracker.Constants;
import de.markusbordihn.advancementstracker.client.advancements.AdvancementEntry;
import de.markusbordihn.advancementstracker.client.gui.WidgetBuilder;
import de.markusbordihn.advancementstracker.client.gui.utils.TextUtils;
import de.markusbordihn.advancementstracker.config.ClientConfig;

@EventBusSubscriber(Dist.CLIENT)
public class TrackerWidget extends WidgetBuilder {

  private MainWindow mainWindow;
  private TextUtils textUtils;
  private int backgroundMax;
  private int height;
  private int left;
  private int leftMax;
  private int scaledHeight;
  private int scaledWidth;
  private int top;
  private int topMax;
  private int width;
  private static Set<AdvancementEntry> trackedAdvancements = new HashSet<>();
  private static TranslationTextComponent widgetTitle;
  private static boolean active = true;
  private static double configHeight = ClientConfig.CLIENT.widgetHeight.get();
  private static double configLeft = ClientConfig.CLIENT.widgetLeft.get();
  private static double configTop = ClientConfig.CLIENT.widgetTop.get();
  private static double configWidth = ClientConfig.CLIENT.widgetWidth.get();

  protected static TrackerWidget trackerWidget;

  TrackerWidget() {
    super(Minecraft.getInstance());
    this.mainWindow = Minecraft.getInstance().getWindow();
    this.textUtils = new TextUtils(Minecraft.getInstance());
  }

  @SubscribeEvent
  public static void handlePlayerLoggedInEvent(PlayerEvent.PlayerLoggedInEvent event) {
    configHeight = ClientConfig.CLIENT.widgetHeight.get();
    configLeft = ClientConfig.CLIENT.widgetLeft.get();
    configTop = ClientConfig.CLIENT.widgetTop.get();
    configWidth = ClientConfig.CLIENT.widgetWidth.get();
  }

  @SubscribeEvent
  public static void handleRenderGameOverlayEventPost(RenderGameOverlayEvent.Post event) {
    if (event.getType() != ElementType.ALL) {
      return;
    }
    if (trackerWidget == null) {
      trackerWidget = new TrackerWidget();
      widgetTitle = new TranslationTextComponent(Constants.MOD_PREFIX + "advancementWidget.title");
    }
    MatrixStack matrixStack = event.getMatrixStack();
    trackerWidget.renderTracker(matrixStack);
  }

  public void renderTracker(MatrixStack matrixStack) {
    if (!active) {
      return;
    }
    // Calculate position and height, but only if width and height changed.
    int scaledWidth = mainWindow.getGuiScaledWidth();
    int scaledHeight = mainWindow.getGuiScaledHeight();
    if (this.scaledWidth != scaledWidth || this.scaledHeight != scaledHeight) {
      this.scaledWidth = scaledWidth;
      this.scaledHeight = scaledHeight;
      this.height = (int) (this.scaledHeight * this.configHeight);
      this.width = (int) (this.scaledWidth * this.configWidth);
      this.top = (int) ((this.scaledHeight - this.height) * this.configHeight);
      this.topMax = this.top + this.height;
      this.left = (int) ((this.scaledWidth - this.width) * this.configLeft);
      this.leftMax = this.left + this.width;
      this.backgroundMax = this.topMax;
    }
    int topPos = this.top;
    boolean hasTrackedAdvancements = !trackedAdvancements.isEmpty();

    // Draw background
    fill(matrixStack, this.left, this.top, this.leftMax, this.backgroundMax, 0x10000000);

    // Draw title
    fill(matrixStack, this.left, this.top, this.leftMax, this.top + fontRenderer.lineHeight + 2, 0x10000000);
    topPos += 2;

    topPos = textUtils.drawTextWithShadow(matrixStack, widgetTitle.getString(), (this.left + 2), topPos, width, height,
        0xFF00FF00);
    topPos += 3;

    // List tracked advancement
    if (hasTrackedAdvancements) {
      for (AdvancementEntry advancement : trackedAdvancements) {
        if (advancement.icon != null) {
          GL11.glPushMatrix();
          GL11.glScalef(0.5F, 0.5F, 0.5F);
          this.minecraft.getItemRenderer().renderGuiItem(advancement.icon, (this.left + 2) * 2, topPos * 2);
          GL11.glPopMatrix();
        }
        if (advancement.criteriaNumber > 1) {
          textUtils.drawTextWithShadow(matrixStack,
              advancement.completedCriteriaNumber + "/" + advancement.criteriaNumber, this.left + width - 24, topPos,
              24, height, 0xFFFFFF00);
          topPos = textUtils.drawTrimmedTextWithShadow(matrixStack, advancement.title, this.left + 12, topPos,
              width - 40, 0xFFFFFF00);
        } else {
          topPos = textUtils.drawTrimmedTextWithShadow(matrixStack, advancement.title, this.left + 12, topPos, width,
              0xFFFFFF00);
        }
        topPos = textUtils.drawTextWithShadow(matrixStack, advancement.description, this.left + 2, topPos, width,
            height, 0xFFFFFFFF);
        topPos += 5;
      }
    } else {
      topPos = textUtils.drawTextWithShadow(matrixStack, "No tracked advancements.", this.left + 2, topPos, width,
          height, 0xFFFFFFFF);
    }

    this.backgroundMax = topPos;
  }

  public static void setActive(boolean active) {
    TrackerWidget.active = active;
  }

  public static void toggleActive() {
    setActive(!TrackerWidget.active);
  }

  public static void setTrackedAdvancements(Set<AdvancementEntry> advancementEntries) {
    trackedAdvancements = advancementEntries;
  }

}
