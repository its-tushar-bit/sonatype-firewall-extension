/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report.pdf;

import java.awt.*;
import java.io.IOException;
import java.util.Arrays;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static com.sonatype.insight.brain.report.pdf.PdfGeneratorUtils.loadPDType0Font;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PdfGeneratorUtilsTest
{
  @Test
  public void testAddText_PDFontList() throws Exception {
    try (PDDocument pdDocument = new PDDocument()) {
      PDPage pdPage = new PDPage();
      try (PDPageContentStream pdPageContentStreamSpy = spy(new PDPageContentStream(pdDocument, pdPage))) {
        PDFont notoSansCJKRegular = loadPDType0Font(pdDocument, "NotoSansCJKsc-Regular.ttf");
        reset(pdPageContentStreamSpy);
        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PDFont> fontArgumentCaptor = ArgumentCaptor.forClass(PDFont.class);

        PdfGeneratorUtils.addText(pdPageContentStreamSpy, 0, 0, new FontStyle(notoSansCJKRegular, 16, Color.BLUE),
            "a星");

        verify(pdPageContentStreamSpy, times(1)).showText(stringArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getAllValues()).containsExactly("a星");
        verify(pdPageContentStreamSpy, times(1)).setFont(fontArgumentCaptor.capture(), eq(16f));
        assertThat(fontArgumentCaptor.getAllValues()).containsExactly(notoSansCJKRegular);
      }
    }
  }

  @Test
  public void testAddText_PDFontList_NoFontForFirstCharacter() throws Exception {
    try (PDDocument pdDocument = new PDDocument()) {
      PDPage pdPage = new PDPage();
      try (PDPageContentStream pdPageContentStreamSpy = spy(new PDPageContentStream(pdDocument, pdPage))) {
        pdPageContentStreamSpy.beginText();
        PDFont openSansRegular = loadPDType0Font(pdDocument, "OpenSans-Regular.ttf");
        PDFontList pdFontList =
            new PDFontList(Arrays.asList(PDType1Font.HELVETICA, openSansRegular));

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> PdfGeneratorUtils.addText(pdPageContentStreamSpy, pdFontList, 10, "星a"))
            .withMessageContaining("No glyph found for 星");

        verify(pdPageContentStreamSpy, never()).showText(any());
        verify(pdPageContentStreamSpy, never()).setFont(any(), anyFloat());
        pdPageContentStreamSpy.endText();
      }
    }
  }

  @Test
  public void testAddText_PDFontList_NoFontForLastCharacter() throws Exception {
    try (PDDocument pdDocument = new PDDocument()) {
      PDPage pdPage = new PDPage();
      try (PDPageContentStream pdPageContentStreamSpy = spy(new PDPageContentStream(pdDocument, pdPage))) {
        pdPageContentStreamSpy.beginText();
        PDFont openSansRegular = loadPDType0Font(pdDocument, "OpenSans-Regular.ttf");
        PDFontList pdFontList =
            new PDFontList(Arrays.asList(PDType1Font.HELVETICA, openSansRegular));
        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PDFont> fontArgumentCaptor = ArgumentCaptor.forClass(PDFont.class);

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> PdfGeneratorUtils.addText(pdPageContentStreamSpy, pdFontList, 10, "a星"))
            .withMessageContaining("No glyph found for 星");

        verify(pdPageContentStreamSpy).showText(stringArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getAllValues()).containsExactly("a");
        verify(pdPageContentStreamSpy).setFont(fontArgumentCaptor.capture(), eq(10f));
        assertThat(fontArgumentCaptor.getAllValues()).containsExactly(PDType1Font.HELVETICA);
        pdPageContentStreamSpy.endText();
      }
    }
  }

  @Test
  public void testAddTextWithWordWrapAtChar_smallText_noWrapChar() throws IOException {
    try (PDDocument pdDocument = new PDDocument()) {
      PDPage pdPage = new PDPage();
      try (PDPageContentStream pdPageContentStreamSpy = spy(new PDPageContentStream(pdDocument, pdPage))) {
        PDFont notoSansCJKRegular = loadPDType0Font(pdDocument, "OpenSans-Regular.ttf");
        reset(pdPageContentStreamSpy);
        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

        PdfGeneratorUtils.addTextWithWordWrapAtChar(pdPageContentStreamSpy, pdPage.getCropBox(), 0, 0, 10,
            new FontStyle(notoSansCJKRegular, 16, Color.BLUE), "This is a small text", null);

        verify(pdPageContentStreamSpy, times(1)).showText(stringArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getAllValues()).containsExactly("This is a small text");
      }
    }
  }

  @Test
  public void testAddTextWithWordWrapAtChar_largeText_noWrapChar() throws IOException {
    try (PDDocument pdDocument = new PDDocument()) {
      PDPage pdPage = new PDPage();
      try (PDPageContentStream pdPageContentStreamSpy = spy(new PDPageContentStream(pdDocument, pdPage))) {
        PDFont notoSansCJKRegular = loadPDType0Font(pdDocument, "OpenSans-Regular.ttf");
        reset(pdPageContentStreamSpy);
        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

        String textToAdd = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer convallis neque a " +
            "mollis pretium. Duis pharetra ex quis urna consequat, nec sagittis quam rhoncus. Ut tincidunt posuere " +
            "purus non commodo. Vestibulum at varius ex, quis cursus lacus. Vivamus eget hendrerit leo. Integer " +
            "ultrices massa vel eros vestibulum convallis. Nulla facilisi. In consequat turpis nec nisi tincidunt, " +
            "non pellentesque enim ornare. Morbi eleifend";

        PdfGeneratorUtils.addTextWithWordWrapAtChar(pdPageContentStreamSpy, pdPage.getCropBox(), 0, 0, 10,
            new FontStyle(notoSansCJKRegular, 16, Color.BLUE), textToAdd, null);

        verify(pdPageContentStreamSpy, times(1)).showText(stringArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getAllValues()).containsExactly(
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer convallis neque");
      }
    }
  }

  @Test
  public void testAddTextWithWordWrapAtChar_withWrapChar() throws IOException {
    try (PDDocument pdDocument = new PDDocument()) {
      PDPage pdPage = new PDPage();
      try (PDPageContentStream pdPageContentStreamSpy = spy(new PDPageContentStream(pdDocument, pdPage))) {
        PDFont notoSansCJKRegular = loadPDType0Font(pdDocument, "OpenSans-Regular.ttf");
        reset(pdPageContentStreamSpy);
        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

        String textToAdd =
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit, Integer convallis neque a mollis";

        PdfGeneratorUtils.addTextWithWordWrapAtChar(pdPageContentStreamSpy, pdPage.getCropBox(), 0, 0, 10,
            new FontStyle(notoSansCJKRegular, 16, Color.BLUE), textToAdd, ",");

        verify(pdPageContentStreamSpy, times(2)).showText(stringArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getAllValues()).containsExactly(
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit,",
            "Integer convallis neque a mollis");
      }
    }
  }
}
