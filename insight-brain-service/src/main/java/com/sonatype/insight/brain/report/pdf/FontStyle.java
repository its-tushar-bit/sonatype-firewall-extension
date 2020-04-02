/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report.pdf;

import java.awt.*;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.font.PDFont;

public class FontStyle
{
  private final PDFont font;

  private final float fontSize;

  private final Color fontColor;

  FontStyle(PDFont font, float fontSize, Color fontColor) {
    this.font = font;
    this.fontSize = fontSize;
    this.fontColor = fontColor;
  }

  public PDFont getFont() {
    return font;
  }

  public float getFontSize() {
    return fontSize;
  }

  public Color getFontColor() {
    return fontColor;
  }

  public float getFontAscent() {
    return font.getFontDescriptor().getAscent() / 1000 * fontSize;
  }

  public float getFontDescent() {
    return -1 * font.getFontDescriptor().getDescent() / 1000 * fontSize;
  }

  public float getFontHeight() {
    return getFontAscent() + getFontDescent();
  }

  public float getStringWidth(String string) throws IOException {
    return font.getStringWidth(string) / 1000 * fontSize;
  }
}
