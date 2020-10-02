/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report.pdf;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.fontbox.util.BoundingBox;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;

public class PDFontList
    extends PDFont
{
  private final List<PDFont> pdFonts;

  public PDFontList(List<PDFont> pdFonts) throws IOException {
    super(new COSDictionary());
    this.pdFonts = pdFonts;
    setFontDescriptor();
  }

  public List<PDFont> getPDFonts() {
    return pdFonts;
  }

  private void setFontDescriptor() {
    COSDictionary cosDictionary = new COSDictionary();
    cosDictionary.setFloat(COSName.ASCENT,
        (float) pdFonts.stream().mapToDouble(pdFont -> pdFont.getFontDescriptor().getAscent()).max().orElse(0));
    cosDictionary.setFloat(COSName.DESCENT,
        (float) pdFonts.stream().mapToDouble(pdFont -> pdFont.getFontDescriptor().getDescent()).min().orElse(0));
    cosDictionary.setFloat(COSName.CAP_HEIGHT,
        (float) pdFonts.stream().mapToDouble(pdFont -> pdFont.getFontDescriptor().getCapHeight()).max().orElse(0));
    PDFontDescriptor pdFontDescriptor = new PDFontDescriptor(cosDictionary);
    setFontDescriptor(pdFontDescriptor);
  }

  @Override
  public float getStringWidth(String text) throws IOException {
    float stringWidth = 0;
    for (int i = 0; i < text.length(); i++) {
      String character = String.valueOf(text.charAt(i));
      for (int j = 0; j < pdFonts.size(); j++) {
        PDFont pdFont = pdFonts.get(j);
        try {
          stringWidth += pdFont.getStringWidth(character);
          break; // This font worked for this character so no need to try the others
        }
        catch (IllegalArgumentException e) {
          if (j == pdFonts.size() - 1) {
            // No fonts worked so raise an error
            throw new IllegalArgumentException("No glyph found for " + character);
          }
        }
      }
    }
    return stringWidth;
  }

  @Override
  protected float getStandard14Width(int code) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected byte[] encode(int unicode) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int readCode(InputStream in) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isVertical() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addToSubset(int codePoint) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void subset() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean willBeSubset() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public BoundingBox getBoundingBox() {
    throw new UnsupportedOperationException();
  }

  @Override
  public float getHeight(int code) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasExplicitWidth(int code) {
    throw new UnsupportedOperationException();
  }

  @Override
  public float getWidthFromFont(int code) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isEmbedded() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isDamaged() {
    throw new UnsupportedOperationException();
  }
}
