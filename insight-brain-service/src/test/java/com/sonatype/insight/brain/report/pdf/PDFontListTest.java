/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report.pdf;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.Arrays;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.Test;

import static com.sonatype.insight.brain.report.pdf.PdfGeneratorUtils.loadPDType0Font;
import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class PDFontListTest
{
  @Test
  public void testInstantiation() throws Exception {
    List<PDFont> pdFonts = Arrays.asList(PDType1Font.HELVETICA, PDType1Font.COURIER);
    double[] ascents = pdFonts.stream().mapToDouble(pdFont -> pdFont.getFontDescriptor().getAscent()).toArray();
    double[] descents = pdFonts.stream().mapToDouble(pdFont -> pdFont.getFontDescriptor().getDescent()).toArray();
    double[] capHeights = pdFonts.stream().mapToDouble(pdFont -> pdFont.getFontDescriptor().getCapHeight()).toArray();
    assertThat(Arrays.stream(ascents).distinct().count()).isEqualTo(pdFonts.size());
    assertThat(Arrays.stream(descents).distinct().count()).isEqualTo(pdFonts.size());
    assertThat(Arrays.stream(capHeights).distinct().count()).isEqualTo(pdFonts.size());

    PDFontList pdFontList = new PDFontList(pdFonts);

    assertThat(pdFontList.getPDFonts()).isEqualTo(pdFonts);
    PDFontDescriptor pdFontDescriptor = pdFontList.getFontDescriptor();
    assertThat(pdFontDescriptor).isNotNull();
    assertThat(pdFontDescriptor.getAscent()).isGreaterThan(0).isEqualTo((float) Arrays.stream(ascents).max().orElse(0));
    assertThat(pdFontDescriptor.getDescent()).isLessThan(0).isEqualTo((float) Arrays.stream(descents).min().orElse(0));
    assertThat(pdFontDescriptor.getCapHeight()).isGreaterThan(0)
        .isEqualTo((float) Arrays.stream(capHeights).max().orElse(0));
  }

  @Test
  public void testGetStringWidth() throws Exception {
    try (PDDocument pdDocument = new PDDocument()) {
      PDFont notoSansCJKRegular = loadPDType0Font(pdDocument, "NotoSansCJKsc-Regular.ttf");
      List<PDFont> pdFonts = Arrays.asList(PDType1Font.HELVETICA, notoSansCJKRegular);
      String text = "a義b";
      float expectedStringWidth = PDType1Font.HELVETICA.getStringWidth("a") + notoSansCJKRegular.getStringWidth("義") +
          PDType1Font.HELVETICA.getStringWidth("b");

      assertThat(new PDFontList(pdFonts).getStringWidth(text)).isEqualTo(expectedStringWidth);
    }
  }
}
