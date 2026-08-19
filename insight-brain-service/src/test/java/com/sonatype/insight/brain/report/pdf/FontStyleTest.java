/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report.pdf;

import java.awt.*;

import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FontStyleTest
{
  @Test
  public void testGetFontAscent() {
    FontStyle fontStyle = new FontStyle(PDType1Font.HELVETICA, 12, Color.BLACK);
    assertThat(fontStyle.getFontAscent()).isEqualTo(8.616f);
  }

  @Test
  public void testGetFontDescent() {
    FontStyle fontStyle = new FontStyle(PDType1Font.HELVETICA, 12, Color.BLACK);
    assertThat(fontStyle.getFontDescent()).isEqualTo(2.484f);
  }

  @Test
  public void testGetFontHeight() {
    FontStyle fontStyle = new FontStyle(PDType1Font.HELVETICA, 12, Color.BLACK);
    assertThat(fontStyle.getFontHeight()).isEqualTo(11.1f);
  }

  @Test
  public void testGetStringWidth() throws Exception {
    FontStyle fontStyle = new FontStyle(PDType1Font.HELVETICA, 12, Color.BLACK);
    assertThat(fontStyle.getStringWidth("this is a small text")).isEqualTo(94.692F);
  }
}
