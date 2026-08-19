/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report.pdf;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2D;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.knowm.xchart.internal.chartpart.Chart;

public final class PdfGeneratorUtils
{
  private static final ConcurrentMap<String, File> FONT_FILE_CACHE = new ConcurrentHashMap<>();

  private PdfGeneratorUtils() {
    throw new UnsupportedOperationException();
  }

  static PDType0Font loadPDType0Font(PDDocument pdDocument, String fontFileName) {
    File fontFile = FONT_FILE_CACHE.compute(fontFileName, (name, existing) -> {
      if (existing != null && existing.exists()) {
        return existing;
      }
      try (InputStream is = PdfGeneratorUtils.class.getClassLoader()
          .getResourceAsStream("assets/fonts/" + name))
      {
        if (is == null) {
          throw new RuntimeException("Font resource not found on classpath: assets/fonts/" + name);
        }
        File tempFile = Files.createTempFile("font-", "-" + name).toFile();
        tempFile.deleteOnExit();
        Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return tempFile;
      }
      catch (RuntimeException e) {
        throw e;
      }
      catch (Exception e) {
        throw new RuntimeException("Failed to load font " + name, e);
      }
    });
    try (InputStream fis = new FileInputStream(fontFile)) {
      return PDType0Font.load(pdDocument, fis);
    }
    catch (IOException e) {
      throw new RuntimeException("Failed to load font " + fontFileName, e);
    }
  }

  static float addTextWithWordWrapAtChar(
      PDPageContentStream pdPageContentStream,
      PDRectangle pageRec,
      float x,
      float y,
      float margin,
      FontStyle fontStyle,
      String text,
      String charToWrap) throws IOException
  {
    float availableSpace = pageRec.getWidth() - (margin * 2) - x;
    String textToAdd = "";
    if (fontStyle.getStringWidth(text) > availableSpace) {
      if (StringUtils.isNotEmpty(charToWrap)) {
        // If text is longer than available space and we have a character to wrap on
        String[] textParts = text.split(charToWrap);
        StringBuilder wrappedText = new StringBuilder();
        for (String textPart : textParts) {
          if (fontStyle.getStringWidth(wrappedText + charToWrap + " " + textPart) <= availableSpace) {
            if (wrappedText.isEmpty()) {
              wrappedText.append(textPart.trim());
            }
            else {
              wrappedText.append(charToWrap).append(" ").append(textPart.trim());
            }
          }
          else {
            addText(pdPageContentStream, x, y, fontStyle, wrappedText + charToWrap);
            y -= fontStyle.getFontHeight();
            wrappedText = new StringBuilder(textPart.trim());
          }
        }

        // Add whatever text was left as the last line
        addText(pdPageContentStream, x, y, fontStyle, wrappedText.toString());
        y -= fontStyle.getFontHeight();
      }
      else {
        // Otherwise if there's no char to wrap on, we just wrap the text to the max possible
        StringBuilder wrappedText = new StringBuilder();
        for (char c : text.toCharArray()) {
          if (fontStyle.getStringWidth(wrappedText + String.valueOf(c)) <= availableSpace) {
            wrappedText.append(c);
          }
          else {
            textToAdd = wrappedText.toString();
            break;
          }
        }

        addText(pdPageContentStream, x, y, fontStyle, textToAdd);
        y -= fontStyle.getFontHeight();
      }
    }
    else {
      // If text is smaller than available space, we add it as is.
      addText(pdPageContentStream, x, y, fontStyle, text);
      y -= fontStyle.getFontHeight();
    }

    return y;
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

  public static void addText(
      PDPageContentStream pdPageContentStream,
      PDFont font,
      float fontSize,
      String text) throws IOException
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

  public static void addImage(
      PDDocument pdDocument,
      PDPageContentStream pdPageContentStream,
      float x,
      float y,
      String resourcePath)
  {
    try (InputStream inputStream = PdfGeneratorUtils.class.getClassLoader()
        .getResourceAsStream(resourcePath))
    {
      byte[] data = IOUtils.toByteArray(inputStream);
      PDImageXObject pdImageXObject =
          PDImageXObject.createFromByteArray(pdDocument, data, null);
      pdPageContentStream.drawImage(pdImageXObject, x, y, 250, 25);
    }
    catch (Exception e) {
      throw new RuntimeException("Failed to load image from " + resourcePath, e);
    }
  }
}
