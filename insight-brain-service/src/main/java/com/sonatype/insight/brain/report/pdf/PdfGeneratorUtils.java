/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report.pdf;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.InputStream;

import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2D;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.knowm.xchart.internal.chartpart.Chart;

public final class PdfGeneratorUtils
{
  private PdfGeneratorUtils() {
    throw new UnsupportedOperationException();
  }

  static PDType0Font loadPDType0Font(PDDocument pdDocument, String fontFileName) {
    try (InputStream inputStream = PdfGeneratorUtils.class.getClassLoader()
        .getResourceAsStream("assets/fonts/" + fontFileName)) {
      return PDType0Font.load(pdDocument, inputStream);
    }
    catch (Exception e) {
      throw new RuntimeException("Failed to load font " + fontFileName, e);
    }
  }

  static void addText(
      PDPageContentStream pdPageContentStream,
      float x,
      float y,
      FontStyle fontStyle,
      String text) throws IOException
  {
    pdPageContentStream.beginText();
    pdPageContentStream.setNonStrokingColor(fontStyle.getFontColor());
    pdPageContentStream.newLineAtOffset(x, y);
    addText(pdPageContentStream, fontStyle.getFont(), fontStyle.getFontSize(), text);
    pdPageContentStream.endText();
  }

  public static void addText(PDPageContentStream pdPageContentStream, PDFont font, float fontSize, String text)
      throws IOException
  {
    if (font instanceof PDFontList) {
      PDFontList pdFontList = (PDFontList) font;
      for (int i = 0; i < text.length(); i++) {
        String character = String.valueOf(text.charAt(i));
        for (int j = 0; j < pdFontList.getPDFonts().size(); j++) {
          PDFont pdFont = pdFontList.getPDFonts().get(j);
          try {
            pdFont.encode(character);
            // Only set the font and try showing if we know the font can encode it
            pdPageContentStream.setFont(pdFont, fontSize);
            pdPageContentStream.showText(character);
            break; // This font worked for this character so no need to try the others
          }
          catch (IllegalArgumentException e) {
            if (j == pdFontList.getPDFonts().size() - 1) {
              // No fonts worked so raise an error
              throw new IllegalArgumentException("No glyph found for " + character);
            }
          }
        }
      }
    }
    else {
      pdPageContentStream.setFont(font, fontSize);
      pdPageContentStream.showText(text);
    }
  }

  private static void drawRectangle(
      PDPageContentStream pdPageContentStream,
      float x,
      float y,
      float width,
      float height,
      Color color) throws IOException
  {
    pdPageContentStream.addRect(x, y, width, height);
    pdPageContentStream.setNonStrokingColor(color);
    pdPageContentStream.fill();
  }

  static float drawRectangleWithText(
      PDPageContentStream pdPageContentStream,
      float x,
      float y,
      float rectangleWidth,
      float rectangleHeight,
      Color rectangleColor,
      FontStyle fontStyle,
      String text) throws IOException
  {
    float textWidth = fontStyle.getStringWidth(text);
    if (textWidth > rectangleWidth) {
      rectangleWidth = textWidth;
    }
    float textHeight = fontStyle.getFontAscent();
    if (textHeight > rectangleHeight) {
      rectangleHeight = textHeight;
    }

    drawRectangle(pdPageContentStream, x, y, rectangleWidth, rectangleHeight, rectangleColor);
    addText(pdPageContentStream, x + (rectangleWidth / 2) - (textWidth / 2),
        y + (rectangleHeight - textHeight),
        fontStyle, text);
    return rectangleWidth;
  }

  static void drawChart(
      PDDocument pdDocument,
      PDPageContentStream pdPageContentStream,
      float x,
      float y,
      int width,
      int height,
      Chart<?, ?> chart) throws IOException
  {
    PdfBoxGraphics2D pdfBoxGraphics2D = new PdfBoxGraphics2D(pdDocument, chart.getWidth(), chart.getHeight());
    chart.paint(pdfBoxGraphics2D, chart.getWidth(), chart.getHeight());
    pdfBoxGraphics2D.dispose();
    PDFormXObject pdFormXObject = pdfBoxGraphics2D.getXFormObject();
    AffineTransform affineTransform = new AffineTransform();
    affineTransform.translate(x, y);
    affineTransform.scale((double) width / chart.getWidth(), (double) height / chart.getHeight());
    pdFormXObject.setMatrix(affineTransform);
    pdPageContentStream.drawForm(pdFormXObject);
  }
}
