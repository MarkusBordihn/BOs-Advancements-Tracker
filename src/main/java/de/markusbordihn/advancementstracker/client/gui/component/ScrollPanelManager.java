package de.markusbordihn.advancementstracker.client.gui.component;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.matrix.MatrixStack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.gui.ScrollPanel;

import de.markusbordihn.advancementstracker.Constants;

public class ScrollPanelManager extends ScrollPanel {

  FontRenderer fontRenderer;
  Logger log = LogManager.getLogger(Constants.LOG_NAME);
  Map<String, ScrollPanelContent> contentMap = new HashMap<>();
  private int baseContentX;
  private int baseContentY;
  protected Minecraft minecraft;
  protected ResourceLocation background;
  protected TextureManager textureManager;
  protected int contentHeight = 0;
  protected int contentX = 0;
  protected int contentY = 0;
  static final float TEXTURE_SCALE = 32.0F;
  static final int BORDER_COLOR = 0xFFAAAAAA;

  protected ScrollPanelManager(Minecraft minecraft, int width, int height, int top, int left) {
    super(minecraft, width, height, top, left);
    log.debug("[Scroll Panel Manager] width:{} height:{} at top:{} left:{}", width, height, top, left);
    this.baseContentX = this.contentX = left;
    this.baseContentY = this.contentY = top;
    this.fontRenderer = minecraft.font;
    this.minecraft = minecraft;
    this.textureManager = minecraft.getTextureManager();
  }

  public void addContent(String contentName, ScrollPanelContent content, boolean active) {
    log.debug("[Scroll Content] {} to scroll panel manager with {}x{} at x:{} y:{}", contentName, content.width,
        content.height, this.contentX, this.contentY);
    content.setMinecraftInstance(this.minecraft);
    content.hasScrollBar(hasScrollBar());
    content.setPosition(this.contentX, this.contentY);
    content.isActive = active;
    this.contentMap.put(contentName, content);

    // Adjust content position and content height for next added content.
    this.contentY += content.height;
    this.contentHeight += content.height;
  }

  public void clearContent() {
    if (this.contentMap.size() <= 0) {
      return;
    }
    log.debug("Clearing scroll content for {} entries...", this.contentMap.size());
    for (ScrollPanelContent scrollPanelContent : this.contentMap.values()) {
      scrollPanelContent.isActive = false;
    }
    this.contentMap = new HashMap<>();
    this.contentX = this.baseContentX;
    this.contentY = this.baseContentY;
    this.contentHeight = 0;
    this.scrollDistance = 0;
  }

  public void handleClick(ScrollPanelContent scrollPanelContent, int button) {
    //
  }

  public boolean hasScrollBar() {
    return (this.getContentHeight() + border) - height > 0;
  }

  public void preRender() {
    boolean hasScrollBar = hasScrollBar();
    for (ScrollPanelContent scrollPanelContent : this.contentMap.values()) {
      scrollPanelContent.hasScrollBar(hasScrollBar);
      scrollPanelContent.scrollDistance = this.scrollDistance;
    }
  }

  @Override
  public int getContentHeight() {
    return this.contentHeight;
  }

  @Override
  protected void drawBackground() {
    //
  }

  @Override
  protected int getScrollAmount() {
    return this.fontRenderer.lineHeight * 3;
  }

  @Override
  public boolean clickPanel(double mouseX, double mouseY, int button) {
    if (mouseY + border >= 0 && mouseY <= this.height + this.scrollDistance - this.border) {
      boolean foundClickTarget = false;
      int baseX = (int) mouseX + this.baseContentX;
      int baseY = (int) mouseY + this.baseContentY + this.border;
      for (ScrollPanelContent scrollPanelContent : this.contentMap.values()) {
        if (scrollPanelContent.isInsideEventAreaY(baseY + scrollPanelContent.relativeY)) {
          log.debug("Detected click for {} at x:{} y:{}", scrollPanelContent.getContentName(), baseX, baseY);
          handleClick(scrollPanelContent, button);
          scrollPanelContent.handleClick(baseX, baseY, button);
          foundClickTarget = true;
          scrollPanelContent.isActive = true;
        } else {
          scrollPanelContent.isActive = false;
        }
      }
      if (!foundClickTarget) {
        log.warn("Detected unmatched click on ScrollPanelManager at x:{} y:{}({}) button:{}", mouseX, mouseY, baseY,
            button);
      }
    }
    return false;
  }

  @Override
  protected void drawPanel(MatrixStack matrixStack, int entryRight, int relativeY, Tessellator tessellator, int mouseX,
      int mouseY) {
    if (this.background != null && this.background != TextureManager.INTENTIONAL_MISSING_TEXTURE) {
      this.textureManager.bind(this.background);
      BufferBuilder buffer = tessellator.getBuilder();
      buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
      buffer.vertex(this.left, this.bottom, 0.0D).color(0x50, 0x50, 0x50, 0xFF)
          .uv(this.left / TEXTURE_SCALE, (this.bottom + (int) this.scrollDistance) / TEXTURE_SCALE).endVertex();
      buffer.vertex(this.right, this.bottom, 0.0D).color(0x50, 0x50, 0x50, 0xFF)
          .uv(this.right / TEXTURE_SCALE, (this.bottom + (int) this.scrollDistance) / TEXTURE_SCALE).endVertex();
      buffer.vertex(this.right, this.top, 0.0D).color(0x50, 0x50, 0x50, 0xFF)
          .uv(this.right / TEXTURE_SCALE, (this.top + (int) this.scrollDistance) / TEXTURE_SCALE).endVertex();
      buffer.vertex(this.left, this.top, 0.0D).color(0x50, 0x50, 0x50, 0xFF)
          .uv(this.left / TEXTURE_SCALE, (this.top + (int) this.scrollDistance) / TEXTURE_SCALE).endVertex();
      tessellator.end();
    }
    int baseY = (int) this.scrollDistance * -1;
    for (ScrollPanelContent scrollPanelContent : this.contentMap.values()) {
      if (hasScrollBar()) {
        scrollPanelContent.scrollDistance = this.scrollDistance;
        scrollPanelContent.setRelativeY(baseY);
      }
      scrollPanelContent.drawBackground(matrixStack, tessellator);
      if (scrollPanelContent.isActive) {
        fill(matrixStack, scrollPanelContent.x, scrollPanelContent.y, scrollPanelContent.xMax, scrollPanelContent.yMax,
            0x40000000);
        this.hLine(matrixStack, scrollPanelContent.x, scrollPanelContent.xMax, scrollPanelContent.y, BORDER_COLOR);
        this.hLine(matrixStack, scrollPanelContent.x, scrollPanelContent.xMax, scrollPanelContent.yMax - 1,
            BORDER_COLOR);
        this.vLine(matrixStack, scrollPanelContent.x, scrollPanelContent.y, scrollPanelContent.yMax - 1, BORDER_COLOR);
        this.vLine(matrixStack, scrollPanelContent.xMax - 1, scrollPanelContent.y, scrollPanelContent.yMax - 1,
            BORDER_COLOR);
      }
      scrollPanelContent.drawContent(matrixStack, entryRight, relativeY, tessellator, mouseX, mouseY);
    }
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
    if (!hasScrollBar() || scroll == 0) {
      return false;
    }
    return super.mouseScrolled(mouseX, mouseY, scroll);
  }

}
