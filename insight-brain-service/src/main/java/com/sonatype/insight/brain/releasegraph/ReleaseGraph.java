/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.releasegraph;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageOutputStream;

public class ReleaseGraph
{
  private BufferedImage image;

  private ReleaseGraphModel model;

  private static final int WIDTH = 200;

  private static final int HEIGHT = 25;

  private static final double MIN_HEIGHT = 2.0;

  private static final int SPACER = 1;

  private static final Color POPULAR_VER_COLOR = new Color(145, 196, 74, 255);

  private static final Color OTHER_VER_COLOR = new Color(189, 189, 189, 255);

  private static final Color RECENT_VER_COLOR = new Color(110, 156, 206, 255);

  private static final Color BG_COLOR = new Color(255, 255, 255, 0);

  private static final Color CURRENT_VER_COLOR = Color.BLACK;

  private static final IndexColorModel COLOR_MODEL;

  static {
    byte[] r = new byte[]{0, (byte) 255, (byte) 145, (byte) 189, (byte) 110};
    byte[] g = new byte[]{0, (byte) 255, (byte) 196, (byte) 189, (byte) 156};
    byte[] b = new byte[]{0, (byte) 255, (byte) 74, (byte) 189, (byte) 206};
    byte[] a = new byte[]{(byte) 255, (byte) 0.0, (byte) 255, (byte) 255, (byte) 255};
    COLOR_MODEL = new IndexColorModel( /* r.length < 2^3 */3, r.length, r, g, b, a);
  }

  // TODO This is missing an offset at the start, and possibly some extra width information for the bars
  public ReleaseGraph(ReleaseGraphModel model, int slots) {
    this.model = model;
    image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_INDEXED, COLOR_MODEL);
    create(image.createGraphics(), slots);
  }

  private void create(Graphics2D g, int slots) {
    g.setPaint(BG_COLOR);
    g.setBackground(BG_COLOR);
    g.clearRect(0, 0, WIDTH, HEIGHT);

    final int barWidth = WIDTH / slots - SPACER;
    int[] slotIndices = model.getSlotIndices();
    int[] popularityData = model.getPopularity();
    for (int i = 0; i < slotIndices.length; i++) {
      int width = barWidth;
      if (slotIndices[i] != -1) {
        double height = (HEIGHT - MIN_HEIGHT) * popularityData[slotIndices[i]] / 100.0 + MIN_HEIGHT;
        if (slotIndices[i] == model.getCurrentVersionIndex()) {
          g.setColor(CURRENT_VER_COLOR);
          g.setPaint(CURRENT_VER_COLOR);
        }
        else if (slotIndices[i] == model.getMostPopularVersionIndex()) {
          g.setColor(POPULAR_VER_COLOR);
          g.setPaint(POPULAR_VER_COLOR);
        }
        else if (slotIndices[i] == model.getMostRecentVersionIndex()) {
          g.setColor(RECENT_VER_COLOR);
          g.setPaint(RECENT_VER_COLOR);
        }
        else {
          g.setColor(OTHER_VER_COLOR);
          g.setPaint(OTHER_VER_COLOR);
        }
        g.fill(new Rectangle(i * (barWidth + SPACER), (int) Math.round(HEIGHT - height), Math.round(width), (int) Math
            .round(height)));
      }
    }

    g.dispose();
  }

  public byte[] getBytes() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
    ImageIO.write(image, "png", new MemoryCacheImageOutputStream(out));
    return out.toByteArray();
  }
}
