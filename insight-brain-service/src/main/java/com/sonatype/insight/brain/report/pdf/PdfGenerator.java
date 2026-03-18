/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report.pdf;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.report.ReportPdfEntity;
import com.sonatype.insight.brain.report.pdf.PdfData.PdfComponent;
import com.sonatype.insight.brain.report.pdf.PdfData.PdfComponent.PdfComponentLicense;
import com.sonatype.insight.brain.report.pdf.PdfData.PdfComponent.PdfComponentLicenseThreat;
import com.sonatype.insight.brain.report.pdf.PdfData.PdfComponent.PdfComponentPolicyViolation;
import com.sonatype.insight.brain.report.pdf.PdfData.PdfComponent.PdfComponentSecurityIssue;
import com.sonatype.insight.brain.sbom.SbomSpecification;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vandeseer.easytable.RepeatedHeaderTableDrawer;
import org.vandeseer.easytable.TableDrawer;
import org.vandeseer.easytable.drawing.DrawingContext;
import org.vandeseer.easytable.drawing.PositionedStyledText;
import org.vandeseer.easytable.drawing.cell.TextCellDrawer;
import org.vandeseer.easytable.settings.HorizontalAlignment;
import org.vandeseer.easytable.settings.Settings;
import org.vandeseer.easytable.settings.VerticalAlignment;
import org.vandeseer.easytable.structure.Row;
import org.vandeseer.easytable.structure.Row.RowBuilder;
import org.vandeseer.easytable.structure.Table;
import org.vandeseer.easytable.structure.Table.TableBuilder;
import org.vandeseer.easytable.structure.cell.AbstractCell.AbstractCellBuilder;
import org.vandeseer.easytable.structure.cell.TextCell;
import org.vandeseer.easytable.structure.cell.paragraph.ParagraphCell;
import org.vandeseer.easytable.structure.cell.paragraph.ParagraphCell.Paragraph;
import org.vandeseer.easytable.structure.cell.paragraph.ParagraphCell.Paragraph.ParagraphBuilder;
import org.vandeseer.easytable.structure.cell.paragraph.ParagraphCell.ParagraphCellBuilder;
import org.vandeseer.easytable.structure.cell.paragraph.StyledText;
import rst.pdfbox.layout.text.FontDescriptor;
import rst.pdfbox.layout.text.TextSequenceUtil;
import rst.pdfbox.layout.text.annotations.AnnotatedStyledText;
import rst.pdfbox.layout.text.annotations.Annotations;
import rst.pdfbox.layout.text.annotations.Annotations.HyperlinkAnnotation.LinkStyle;
import rst.pdfbox.layout.util.Pair;
import rst.pdfbox.layout.util.WordBreakerFactory;
import rst.pdfbox.layout.util.WordBreakers;

import static com.sonatype.insight.brain.report.pdf.PdfGeneratorUtils.addImage;
import static com.sonatype.insight.brain.report.pdf.PdfGeneratorUtils.addText;
import static com.sonatype.insight.brain.report.pdf.PdfGeneratorUtils.addTextWithWordWrapAtChar;
import static com.sonatype.insight.brain.report.pdf.PdfGeneratorUtils.drawChart;
import static com.sonatype.insight.brain.report.pdf.PdfGeneratorUtils.drawRectangleWithText;
import static com.sonatype.insight.brain.report.pdf.PdfGeneratorUtils.loadPDType0Font;

public class PdfGenerator
{
  private static final Logger log = LoggerFactory.getLogger(PdfGenerator.class);

  public static final String REPORT_FILE_NAME = "report.pdf";

  // User space units per inch from org.apache.pdfbox.pdmodel.common.PDRectangle
  // Visible for testing
  static final int USER_SPACE_UNITS_PER_INCH = 72;

  private static final int MARGIN = 10;

  // Note the MediaBox defines, in user space units, the physical medium boundaries to display/print the page on
  // Use minimum of A4 and Letter dimensions to ensure the pdf can fit on both when printing without needing scaling
  // Visible for testing
  static final PDRectangle MEDIA_BOX_SIZE = new PDRectangle(
      Math.min(PDRectangle.A4.getWidth(), PDRectangle.LETTER.getWidth()),
      Math.min(PDRectangle.A4.getHeight(), PDRectangle.LETTER.getHeight()));

  // Note the CropBox defines, in user space units, the visible region the page should be cropped to for display/print
  // Take off at least 1/2 an inch from all sides as a safe bet for printer margins
  // Visible for testing
  static final PDRectangle CROP_BOX_SIZE = new PDRectangle(
      MEDIA_BOX_SIZE.getWidth() - USER_SPACE_UNITS_PER_INCH,
      MEDIA_BOX_SIZE.getHeight() - USER_SPACE_UNITS_PER_INCH);

  static final String DATE_FORMAT_STRING = "EEE MMM dd yyyy 'at' HH:mm:ss 'UTC' Z";

  private static final int PADDING = 3;

  private static final int CELL_BORDER_WIDTH = 1;

  private static final int CELL_PADDING = 4;

  private static final int DONUT_CHART_SIZE = 20;

  private static final int SUMMARY_IMAGE_SIZE = 20;

  private static final int DEFAULT_FONT_SIZE = 8;

  private static final int HEADER_FONT_SIZE = 20;

  private static final int TITLE_FONT_SIZE = 16;

  private static final int LEGACY_VIOLATIONS_SYMBOL_FONT_SIZE = 25;

  private static final int THREAT_LEVEL_FONT_SIZE = 10;

  private static final int SUMMARY_PADDING = 4;

  private static final Color DEFAULT_FONT_COLOR = Color.BLACK;

  private static final Color APPLICATION_COMPOSITION_REPORT_COLOR = new Color(119, 130, 251);

  private static final Color BILL_OF_MATERIALS_REPORT_COLOR = new Color(13, 13, 12, 1);

  private static final Color DATE_DESCRIPTOR_COLOR = Color.GRAY;

  private static final Color HEADER_FILL_COLOR = new Color(245, 245, 245);

  private static final Color CELL_BORDER_COLOR = new Color(224, 224, 224);

  private static final char LEGACY_VIOLATIONS_SYMBOL = '\uf1da';

  private static final int MAX_CELL_CHARACTERS = 500;

  private static final String ORIGINAL_FILE_LABEL = "Original File:";

  private static final List<String> SBOM_METADATA_CDX_LABELS =
      List.of("Author:", "Manufacturer:", "Supplier:", "Specification:", "Spec Version:",
          "File Format:", ORIGINAL_FILE_LABEL);

  private static final List<String> SBOM_METADATA_SPDX_LABELS =
      List.of("Person:", "Organization:", "Specification:", "Spec Version:", "File Format:");

  private static final String SBOM_METADATA_EMPTY_DEFAULT = "NONE";

  private static final int SBOM_METADATA_TITLE_TOP_MARGIN = 5;

  private static final int SBOM_METADATA_LABELS_Y_MARGIN = 65;

  private final Context productContext;

  private final PdfData pdfData;

  private final Predicate<PdfComponentPolicyViolation> isNotLegacyViolation =
      violation -> !violation.legacyViolation;

  private ReportPdfEntity reportPdf;

  private PDDocument pdf;

  private FontStyle sonatypeFontStyle;

  private FontStyle applicationCompositionReportFontStyle;

  private FontStyle billOfMaterialsReportFontStyle;

  private FontStyle titleFontStyle;

  private FontStyle dateDescriptorFontStyle;

  private FontStyle dateFontStyle;

  private FontStyle summaryHeaderFontStyle;

  private FontStyle summaryFontStyle;

  private FontStyle legacyViolationsFontStyle;

  private FontStyle tableRowHeaderFontStyle;

  private FontStyle tableRowFontStyle;

  private FontStyle rectangleFontStyle;

  private FontStyle threatLevelFontStyle;

  private FontStyle sbomMetadataTitleFontStyle;

  private FontStyle sbomMetadataFontStyle;

  private String createdOnDateTime;

  private String analyzedOnDateTime;

  enum Context
  {
    LIFECYCLE,
    SBOM
  }

  static {
    System.setProperty(WordBreakerFactory.WORD_BREAKER_CLASS_PROPERTY, WordBreaker.class.getName());
  }

  // Visible for testing
  PdfGenerator(ReportPdfEntity reportPdf, PdfData pdfData, Context productContext) {
    this.reportPdf = reportPdf;
    this.pdfData = pdfData;
    this.productContext = productContext;
  }

  PdfGenerator(ReportPdfEntity reportPdf, PdfData pdfData) {
    this(reportPdf, pdfData, Context.LIFECYCLE);
  }

  private void generate() throws IOException {
    long freeMemory = Runtime.getRuntime().freeMemory();
    long maxUseMemory = (freeMemory * 50) / 100; // use up to 50% of available memory
    try (PDDocument pdDocument = new PDDocument(MemoryUsageSetting.setupMixed(maxUseMemory))) {
      doGenerate(pdDocument);
    }
  }

  PDDocument getPdf() {
    return pdf;
  }

  // Visible for testing.
  void doGenerate(PDDocument pdDocument) throws IOException {
    this.pdf = pdDocument;
    initFontStyles(pdf);
    setDocumentMetadata();
    DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_STRING, Locale.ENGLISH);
    createdOnDateTime = dateFormat.format(pdfData.createdDate);
    analyzedOnDateTime = dateFormat.format(pdfData.analyzedDate);
    addPolicyViolationsSection();
    addVulnerabilitiesSection();
    addLicensesSection();
    addBomSection();
    addPageNumbers();
    try (OutputStream outputStream = reportPdf.getOutputStream()) {
      pdf.save(outputStream);
    }
  }

  // Visible for testing
  void initFontStyles(PDDocument pdf) throws IOException {
    PDFontList regularFont = new PDFontList(
        Arrays.asList(loadPDType0Font(pdf, "OpenSans-Regular.ttf"), loadPDType0Font(pdf, "NotoSansCJKsc-Regular.ttf")));
    PDFontList semiBoldFont = new PDFontList(Collections.singletonList(loadPDType0Font(pdf, "OpenSans-SemiBold.ttf")));
    PDFontList boldFont = new PDFontList(
        Arrays.asList(loadPDType0Font(pdf, "OpenSans-Bold.ttf"), loadPDType0Font(pdf, "NotoSansCJKsc-Bold.ttf")));
    PDType0Font fontawesome = loadPDType0Font(pdf, "fontawesome-webfont.ttf");

    sonatypeFontStyle = new FontStyle(regularFont, HEADER_FONT_SIZE, DEFAULT_FONT_COLOR);
    applicationCompositionReportFontStyle = new FontStyle(regularFont, HEADER_FONT_SIZE,
        APPLICATION_COMPOSITION_REPORT_COLOR);
    billOfMaterialsReportFontStyle = new FontStyle(regularFont, HEADER_FONT_SIZE, BILL_OF_MATERIALS_REPORT_COLOR);
    titleFontStyle = new FontStyle(regularFont, TITLE_FONT_SIZE, DEFAULT_FONT_COLOR);
    dateDescriptorFontStyle = new FontStyle(regularFont, DEFAULT_FONT_SIZE, DATE_DESCRIPTOR_COLOR);
    dateFontStyle = new FontStyle(regularFont, DEFAULT_FONT_SIZE, DEFAULT_FONT_COLOR);
    summaryHeaderFontStyle = new FontStyle(boldFont, DEFAULT_FONT_SIZE, DEFAULT_FONT_COLOR);
    summaryFontStyle = new FontStyle(regularFont, DEFAULT_FONT_SIZE, DEFAULT_FONT_COLOR);
    legacyViolationsFontStyle = new FontStyle(fontawesome, LEGACY_VIOLATIONS_SYMBOL_FONT_SIZE, DEFAULT_FONT_COLOR);
    tableRowHeaderFontStyle = new FontStyle(semiBoldFont, DEFAULT_FONT_SIZE, DEFAULT_FONT_COLOR);
    tableRowFontStyle = new FontStyle(regularFont, DEFAULT_FONT_SIZE, DEFAULT_FONT_COLOR);
    rectangleFontStyle = new FontStyle(summaryFontStyle.getFont(), 14f, Color.WHITE);
    threatLevelFontStyle = new FontStyle(semiBoldFont, THREAT_LEVEL_FONT_SIZE, DEFAULT_FONT_COLOR);
    sbomMetadataTitleFontStyle = new FontStyle(boldFont, DEFAULT_FONT_SIZE, DEFAULT_FONT_COLOR);
    sbomMetadataFontStyle = new FontStyle(regularFont, DEFAULT_FONT_SIZE, DEFAULT_FONT_COLOR);
  }

  private void setDocumentMetadata() {
    PDDocumentInformation docInfo = new PDDocumentInformation();
    docInfo.setTitle(pdfData.title);
    docInfo.setCreator("Nexus IQ Server release " + pdfData.productVersion);
    docInfo.setProducer(docInfo.getCreator());
    docInfo.setCreationDate(new GregorianCalendar());
    pdf.setDocumentInformation(docInfo);
  }

  private void addPolicyViolationsSection() throws IOException {
    // Start policy violations section
    PDPage page = newPage();
    PDRectangle pageRec = page.getCropBox();
    try (PDPageContentStream contentStream = new PDPageContentStream(pdf, page)) {
      // Add header
      float headerLeftStartX = MARGIN;
      float headerLeftStartY = pageRec.getHeight() - MARGIN - sonatypeFontStyle.getFontAscent();
      float headerRightStartY = headerLeftStartY;
      float logoLeftStartY = headerLeftStartY - 7;
      if (this.productContext.equals(Context.SBOM)) {
        addImage(pdf, contentStream,
            headerLeftStartX, logoLeftStartY, "assets/sbomManager/assets/sonatype-sbom-manager-logo-nav.png");
        String billOfMaterialsReport = "Bill of Materials Report";
        float headerRightStartX = pageRec.getWidth() - MARGIN -
            billOfMaterialsReportFontStyle.getStringWidth(billOfMaterialsReport);
        addText(contentStream, headerRightStartX, headerRightStartY, billOfMaterialsReportFontStyle,
            billOfMaterialsReport);
      }
      else {
        addText(contentStream, headerLeftStartX, headerLeftStartY, sonatypeFontStyle, "Sonatype");
        String applicationCompositionReport = "Application Composition Report";
        float headerRightStartX = pageRec.getWidth() - MARGIN -
            applicationCompositionReportFontStyle.getStringWidth(applicationCompositionReport);
        addText(contentStream, headerRightStartX, headerRightStartY, applicationCompositionReportFontStyle,
            applicationCompositionReport);
      }

      // Add title and dates
      float titleAndDatesStartY = addTitleAndDates(contentStream, pageRec, "Policy Violations", MARGIN,
          headerLeftStartY - sonatypeFontStyle.getFontDescent() - titleFontStyle.getFontAscent());

      // Add SBOM metadata
      float sbomMetadataStartY = addSbomMetadataSection(contentStream, pageRec, MARGIN,
          titleAndDatesStartY - dateDescriptorFontStyle.getFontHeight() - SBOM_METADATA_TITLE_TOP_MARGIN);

      // Add violations summary
      long critical = countPolicyViolations(8, 10);
      long severe = countPolicyViolations(4, 7);
      long moderate = countPolicyViolations(2, 3);
      long total = critical + severe + moderate;
      float criticalStartX = MARGIN;
      float criticalStartY = sbomMetadataStartY - dateDescriptorFontStyle.getFontDescent() - SUMMARY_IMAGE_SIZE
          - SUMMARY_PADDING;
      float criticalWidth = drawRectangleWithText(contentStream, criticalStartX, criticalStartY, SUMMARY_IMAGE_SIZE,
          SUMMARY_IMAGE_SIZE, ThreatLevelColor.get(8), rectangleFontStyle, String.valueOf(critical));
      float severeStartX = criticalStartX + criticalWidth + PADDING;
      float severeStartY = criticalStartY;
      float severeWidth = drawRectangleWithText(contentStream, severeStartX, severeStartY, SUMMARY_IMAGE_SIZE,
          SUMMARY_IMAGE_SIZE, ThreatLevelColor.get(4), rectangleFontStyle, String.valueOf(severe));
      float moderateStartX = severeStartX + severeWidth + PADDING;
      float moderateStartY = criticalStartY;
      float moderateWidth = drawRectangleWithText(contentStream, moderateStartX, moderateStartY, SUMMARY_IMAGE_SIZE,
          SUMMARY_IMAGE_SIZE, ThreatLevelColor.get(2), rectangleFontStyle, String.valueOf(moderate));
      float violationsTextStartX = moderateStartX + moderateWidth + PADDING;
      float violationsTextStartY = criticalStartY + SUMMARY_IMAGE_SIZE / 2f;
      String violationsText = total + " VIOLATIONS";
      addText(contentStream, violationsTextStartX, violationsTextStartY, summaryHeaderFontStyle, violationsText);
      float affectedComponentsStartX = violationsTextStartX;
      float affectedComponentsStartY = criticalStartY;
      long affectedComponents = countAffectedComponents();
      String affectingText = "Affecting " + affectedComponents + " components";
      addText(contentStream, affectedComponentsStartX, affectedComponentsStartY, summaryFontStyle, affectingText);

      // Add legacy violation summary
      if (!this.productContext.equals(Context.SBOM)) {
        float maxViolationsAffectedWidth =
            Math.max(summaryHeaderFontStyle.getStringWidth(violationsText),
                summaryFontStyle.getStringWidth(affectingText));
        float legacyViolationsSymbolStartX = affectedComponentsStartX + maxViolationsAffectedWidth + 4 * PADDING;
        float legacyViolationsSymbolStartY = affectedComponentsStartY;
        addText(contentStream, legacyViolationsSymbolStartX, legacyViolationsSymbolStartY, legacyViolationsFontStyle,
            String.valueOf(LEGACY_VIOLATIONS_SYMBOL));
        float legacyViolationsCountStartX = legacyViolationsSymbolStartX +
            legacyViolationsFontStyle.getStringWidth(String.valueOf(LEGACY_VIOLATIONS_SYMBOL)) + PADDING;
        float legacyViolationsCountStartY = violationsTextStartY - 4;
        long legacyViolations = pdfData.components.stream()
            .flatMap(component -> component.policyViolations.stream())
            .filter(violation -> violation.legacyViolation)
            .count();

        String legacyViolationsText = legacyViolations + " LEGACY VIOLATIONS";
        if (legacyViolations == 1) {
          legacyViolationsText = legacyViolations + " LEGACY VIOLATION";
        }
        addText(contentStream, legacyViolationsCountStartX, legacyViolationsCountStartY, summaryHeaderFontStyle,
            legacyViolationsText);
      }

      // Add policy violations table
      Table table = createPolicyViolationsTable(page);
      TableDrawer tableDrawer =
          createTableDrawer(contentStream, criticalStartY - summaryFontStyle.getFontDescent()
              - SUMMARY_PADDING, table);

      // End policy violations section
      pdf.addPage(page);
      draw(tableDrawer);
    }
  }

  // Visible for testing
  Table createPolicyViolationsTable(PDPage page) {
    float threatLevelColorWidthPercent = 1;
    float threatLevelWidthPercent = 9;
    float policyNameWidthPercent = 18;
    float policyTypeWidthPercent = 14;
    float waivedWidthPercent = 8;
    float componentWidthPercent = 50;

    float tableWidthOnePercent = (page.getCropBox().getWidth() - 2 * MARGIN) / 100;
    TableBuilder tableBuilder = Table.builder()
        .addColumnsOfWidth(
            tableWidthOnePercent * threatLevelColorWidthPercent,
            tableWidthOnePercent * threatLevelWidthPercent,
            tableWidthOnePercent * policyNameWidthPercent,
            tableWidthOnePercent * policyTypeWidthPercent,
            tableWidthOnePercent * waivedWidthPercent,
            tableWidthOnePercent * componentWidthPercent);

    // Add policy violations table headers
    tableBuilder.addRow(Row.builder()
        .add(headerCellBuilder().text("THREAT").colSpan(2).build())
        .add(headerCellBuilder().text("POLICY NAME").build())
        .add(headerCellBuilder().text("POLICY TYPE").build())
        .add(headerCellBuilder().text("WAIVED").build())
        .add(headerCellBuilder().text("COMPONENT").build())
        .build());

    // Add policy violations table data
    List<PolicyViolationsTableRow> policyViolationsTableRows = createPolicyViolationsTableData();
    policyViolationsTableRows.sort(null);
    for (PolicyViolationsTableRow policyViolationsTableRow : policyViolationsTableRows) {
      List<Row> rows = buildTableRowAndSplitIfNeeded(
          new TextCellBuilder("",
              t -> cellBuilder(t).backgroundColor(ThreatLevelColor.get(policyViolationsTableRow.threatLevel))),
          new TextCellBuilder(String.valueOf(policyViolationsTableRow.threatLevel),
              t -> cellBuilder(t)
                  .font(threatLevelFontStyle.getFont())
                  .fontSize((int) threatLevelFontStyle.getFontSize())
                  .textColor(threatLevelFontStyle.getFontColor())),
          new TextCellBuilder(policyViolationsTableRow.policyName, this::cellBuilder),
          new TextCellBuilder(policyViolationsTableRow.policyType, this::cellBuilder),
          new TextCellBuilder(policyViolationsTableRow.waived ? "Yes" : "No", this::cellBuilder),
          new TextCellBuilder(policyViolationsTableRow.componentName, this::cellBuilder));
      rows.forEach(tableBuilder::addRow);
    }

    return tableBuilder.build();
  }

  // Visible for testing
  List<PolicyViolationsTableRow> createPolicyViolationsTableData() {
    List<PolicyViolationsTableRow> policyViolationsTableRows = new ArrayList<>();
    for (PdfComponent component : pdfData.components) {
      for (PdfComponentPolicyViolation violation : component.policyViolations) {
        if (!isNotLegacyViolation.test(violation)) {
          continue;
        }
        PolicyViolationsTableRow policyViolationsTableRow = new PolicyViolationsTableRow();
        policyViolationsTableRow.threatLevel = violation.policyThreatLevel;
        policyViolationsTableRow.policyName = violation.policyName;
        policyViolationsTableRow.policyType = violation.policyThreatCategory == null
            ? ""
            : StringUtils
                .capitalize(violation.policyThreatCategory.toLowerCase(Locale.ROOT));
        policyViolationsTableRow.waived = violation.waived;
        policyViolationsTableRow.componentName = component.displayName;
        policyViolationsTableRows.add(policyViolationsTableRow);
      }
    }
    return policyViolationsTableRows;
  }

  private void addVulnerabilitiesSection() throws IOException {
    // Start security issues section
    PDPage page = newPage();
    PDRectangle pageRec = page.getCropBox();
    try (PDPageContentStream contentStream = new PDPageContentStream(pdf, page)) {
      // Add title and dates
      float titleAndDatesStartY = addTitleAndDates(contentStream, pageRec, "Vulnerabilities", MARGIN,
          pageRec.getHeight() - MARGIN - titleFontStyle.getFontHeight());

      // Add security issues table
      Table table = createSecurityIssuesTable(page);
      TableDrawer tableDrawer =
          createTableDrawer(contentStream, titleAndDatesStartY - dateDescriptorFontStyle.getFontDescent()
              - SUMMARY_PADDING, table);

      // End security issues section
      pdf.addPage(page);
      draw(tableDrawer);
    }
  }

  // Visible for testing
  Table createSecurityIssuesTable(PDPage page) throws IOException {
    boolean includeAnalysisState = productContext.equals(Context.SBOM);
    float vulnIdWidth = tableRowFontStyle.getStringWidth("sonatype-0000-000000") + 2 * CELL_PADDING;
    float vulnScoreWidth = tableRowHeaderFontStyle.getStringWidth("CVSS SCORE") + 2 * CELL_PADDING;
    float analysisStateWidth =
        includeAnalysisState ? tableRowHeaderFontStyle.getStringWidth("ANALYSIS STATE") + 2 * CELL_PADDING : 0;
    float componentWidth =
        page.getCropBox().getWidth() - 2 * MARGIN - vulnIdWidth - vulnScoreWidth - analysisStateWidth;
    TableBuilder tableBuilder = Table.builder()
        .addColumnsOfWidth(vulnIdWidth, vulnScoreWidth, componentWidth);
    if (includeAnalysisState) {
      tableBuilder.addColumnsOfWidth(analysisStateWidth);
    }

    // Add security issues table headers
    Row.RowBuilder headerRowBuilder = Row.builder()
        .add(headerCellBuilder().text("VULNERABILITY").build())
        .add(headerCellBuilder().text("CVSS SCORE").build())
        .add(headerCellBuilder().text("COMPONENT").build());
    if (includeAnalysisState) {
      headerRowBuilder.add(headerCellBuilder().text("ANALYSIS STATE").build());
    }
    tableBuilder.addRow(headerRowBuilder.build());

    // Add security issues table data
    List<SecurityIssuesTableRow> securityIssuesTableRows = createSecurityIssuesTableData();
    securityIssuesTableRows.sort(null);
    for (SecurityIssuesTableRow securityIssuesTableRow : securityIssuesTableRows) {
      List<Row> rows;
      if (includeAnalysisState) {
        rows = buildTableRowAndSplitIfNeeded(
            new TextCellBuilder(securityIssuesTableRow.reference, this::buildVulnerabilityIdCell),
            new TextCellBuilder(
                securityIssuesTableRow.severity == null ? "" : securityIssuesTableRow.severity.toString(),
                this::cellBuilder),
            new TextCellBuilder(securityIssuesTableRow.componentName, this::cellBuilder),
            new TextCellBuilder(securityIssuesTableRow.analysisState, this::cellBuilder));
      }
      else {
        rows = buildTableRowAndSplitIfNeeded(
            new TextCellBuilder(securityIssuesTableRow.reference, this::buildVulnerabilityIdCell),
            new TextCellBuilder(
                securityIssuesTableRow.severity == null ? "" : securityIssuesTableRow.severity.toString(),
                this::cellBuilder),
            new TextCellBuilder(securityIssuesTableRow.componentName, this::cellBuilder));
      }

      rows.forEach(tableBuilder::addRow);
    }

    return tableBuilder.build();
  }

  private AbstractCellBuilder<?, ?> buildVulnerabilityIdCell(String vulnerabilityId) {
    if (pdfData.baseUrl == null) {
      return cellBuilder(vulnerabilityId);
    }

    String url = pdfData.baseUrl + UserInterfaceLinksHelper.getVulnerabilityDetailsUrl(vulnerabilityId);
    Annotations.HyperlinkAnnotation hyperlink = new Annotations.HyperlinkAnnotation(url, LinkStyle.none);
    AnnotatedStyledText annotatedStyledText =
        new AnnotatedStyledText(vulnerabilityId, tableRowFontStyle.getFontSize(), tableRowFontStyle.getFont(),
            Color.BLUE, 0f, Collections.singleton(hyperlink));
    Paragraph paragraph = Paragraph.builder().build();
    paragraph.getWrappedParagraph().add(annotatedStyledText);
    return paragraphCellBuilder().paragraph(paragraph);
  }

  private List<SecurityIssuesTableRow> createSecurityIssuesTableData() {
    List<SecurityIssuesTableRow> securityIssuesTableRows = new ArrayList<>();
    for (PdfComponent component : pdfData.components) {
      for (PdfComponentSecurityIssue securityIssue : component.securityIssues) {
        SecurityIssuesTableRow securityIssuesTableRow = new SecurityIssuesTableRow();
        securityIssuesTableRow.reference = securityIssue.reference;
        securityIssuesTableRow.severity = securityIssue.severity;
        securityIssuesTableRow.componentName = component.displayName;
        if (productContext.equals(Context.SBOM)) {
          securityIssuesTableRow.analysisState = securityIssue.analysisState;
        }
        securityIssuesTableRows.add(securityIssuesTableRow);
      }
    }
    return securityIssuesTableRows;
  }

  private void addLicensesSection() throws IOException {
    // Start license section
    PDPage page = newPage();
    PDRectangle pageRec = page.getCropBox();
    try (PDPageContentStream contentStream = new PDPageContentStream(pdf, page)) {
      // Add title and dates
      float titleAndDatesStartY = addTitleAndDates(contentStream, pageRec, "Licenses", MARGIN,
          pageRec.getHeight() - MARGIN - titleFontStyle.getFontHeight());

      // Add licenses table
      Table table = createLicensesTable(page);
      TableDrawer tableDrawer =
          createTableDrawer(contentStream, titleAndDatesStartY - dateDescriptorFontStyle.getFontDescent()
              - SUMMARY_PADDING, table);

      // End licenses section
      pdf.addPage(page);
      draw(tableDrawer);
    }
  }

  // Visible for testing
  Table createLicensesTable(PDPage page) {
    if (productContext.equals(Context.SBOM)) {
      return createLicensesTableForSbom(page);
    }

    // for Lifecycle
    float threatLevelColorWidthPercent = 1;
    float threatLevelWidthPercent = 9;
    float effectiveLicenseWidthPercent = 16;
    float declaredLicenseWidthPercent = 16;
    float observedLicenseWidthPercent = 16;
    float componentWidthPercent = 42;

    float tableWidthOnePercent = (page.getCropBox().getWidth() - 2 * MARGIN) / 100;
    TableBuilder tableBuilder =
        Table.builder()
            .addColumnsOfWidth(
                tableWidthOnePercent * threatLevelColorWidthPercent,
                tableWidthOnePercent * threatLevelWidthPercent,
                tableWidthOnePercent * effectiveLicenseWidthPercent,
                tableWidthOnePercent * declaredLicenseWidthPercent,
                tableWidthOnePercent * observedLicenseWidthPercent,
                tableWidthOnePercent * componentWidthPercent);

    // Add licenses table headers
    tableBuilder.addRow(Row.builder()
        .add(headerCellBuilder().text("THREAT").colSpan(2).build())
        .add(headerCellBuilder().text("EFFECTIVE").build())
        .add(headerCellBuilder().text("DECLARED").build())
        .add(headerCellBuilder().text("OBSERVED").build())
        .add(headerCellBuilder().text("COMPONENT").build())
        .build());

    // Add licenses table data
    List<LicensesTableRow> licensesTableRows = createLicensesTableData();
    licensesTableRows.sort(null);
    for (LicensesTableRow licensesTableRow : licensesTableRows) {
      List<Row> rows = buildTableRowAndSplitIfNeeded(
          new TextCellBuilder("",
              t -> cellBuilder(t).backgroundColor(ThreatLevelColor.get(licensesTableRow.threatLevel))),
          new TextCellBuilder(String.valueOf(licensesTableRow.threatLevel),
              t -> cellBuilder(t)
                  .font(threatLevelFontStyle.getFont())
                  .fontSize((int) threatLevelFontStyle.getFontSize())
                  .textColor(threatLevelFontStyle.getFontColor())),
          new TextCellBuilder("",
              t -> buildLicensesCell(licensesTableRow.effectiveLicenses, licensesTableRow.overridden)),
          new TextCellBuilder("", t -> buildLicensesCell(licensesTableRow.declaredLicenses, false)),
          new TextCellBuilder("", t -> buildLicensesCell(licensesTableRow.observedLicenses, false)),
          new TextCellBuilder(licensesTableRow.componentName, this::cellBuilder));
      rows.forEach(tableBuilder::addRow);
    }
    return tableBuilder.build();
  }

  private Table createLicensesTableForSbom(final PDPage page) {
    float threatLevelColorWidthPercent = 1;
    float threatLevelWidthPercent = 14;
    float licensesWidthPercent = 30;
    float componentWidthPercent = 55;

    float tableWidthOnePercent = (page.getCropBox().getWidth() - 2 * MARGIN) / 100;
    TableBuilder tableBuilder =
        Table.builder()
            .addColumnsOfWidth(
                tableWidthOnePercent * threatLevelColorWidthPercent,
                tableWidthOnePercent * threatLevelWidthPercent,
                tableWidthOnePercent * licensesWidthPercent,
                tableWidthOnePercent * componentWidthPercent);

    // Add licenses table headers
    tableBuilder.addRow(Row.builder()
        .add(headerCellBuilder().text("THREAT").colSpan(2).build())
        .add(headerCellBuilder().text("LICENSE TYPE").build())
        .add(headerCellBuilder().text("COMPONENT").build())
        .build());

    // Add licenses table data
    List<LicensesTableRow> licensesTableRows = createLicensesTableData();
    licensesTableRows.sort(null);
    for (LicensesTableRow licensesTableRow : licensesTableRows) {
      List<Row> rows = buildTableRowAndSplitIfNeeded(
          new TextCellBuilder("",
              t -> cellBuilder(t).backgroundColor(ThreatLevelColor.get(licensesTableRow.threatLevel))),
          new TextCellBuilder(String.valueOf(licensesTableRow.threatLevel),
              t -> cellBuilder(t)
                  .font(threatLevelFontStyle.getFont())
                  .fontSize((int) threatLevelFontStyle.getFontSize())
                  .textColor(threatLevelFontStyle.getFontColor())),
          new TextCellBuilder("",
              t -> buildLicensesCell(licensesTableRow.effectiveLicenses, licensesTableRow.overridden)),
          new TextCellBuilder(licensesTableRow.componentName, this::cellBuilder));
      rows.forEach(tableBuilder::addRow);
    }
    return tableBuilder.build();
  }

  // Visible for testing
  AbstractCellBuilder<?, ?> buildLicensesCell(String licenses, boolean overridden) {
    ParagraphBuilder paragraphBuilder = Paragraph.builder();
    if (!licenses.isEmpty()) {
      paragraphBuilder.append(StyledText.builder()
          .font(tableRowFontStyle.getFont())
          .fontSize(tableRowFontStyle.getFontSize())
          .color(tableRowFontStyle.getFontColor())
          .text(licenses)
          .build());
      if (overridden) {
        paragraphBuilder.append(StyledText.builder()
            .font(summaryHeaderFontStyle.getFont())
            .fontSize(summaryHeaderFontStyle.getFontSize())
            .color(summaryHeaderFontStyle.getFontColor())
            .text(" (Overridden)")
            .build());
      }
    }
    return paragraphCellBuilder().paragraph(paragraphBuilder.build());
  }

  private List<LicensesTableRow> createLicensesTableData() {
    if (productContext.equals(Context.SBOM)) {
      return createLicensesTableDataForSbom();
    }

    List<LicensesTableRow> licensesTableRows = new ArrayList<>();
    for (PdfComponent component : pdfData.components) {
      if (CollectionUtils.isEmpty(component.effectiveLicenses) && CollectionUtils.isEmpty(component.declaredLicenses) &&
          CollectionUtils.isEmpty(component.observedLicenses))
      {
        continue;
      }
      LicensesTableRow licensesTableRow = new LicensesTableRow();
      licensesTableRow.overridden = CollectionUtils.isNotEmpty(component.overriddenLicenses);
      licensesTableRow.threatLevel = getMaxLicenseThreatLevel(component.effectiveLicenseThreats);
      licensesTableRow.effectiveLicenses = licensesToString(component.effectiveLicenses);
      licensesTableRow.declaredLicenses = licensesToString(component.declaredLicenses);
      licensesTableRow.observedLicenses = licensesToString(component.observedLicenses);
      licensesTableRow.componentName = component.displayName;
      licensesTableRows.add(licensesTableRow);
    }
    return licensesTableRows;
  }

  private List<LicensesTableRow> createLicensesTableDataForSbom() {
    List<LicensesTableRow> licensesTableRows = new ArrayList<>();
    for (PdfComponent component : pdfData.components) {
      if (CollectionUtils.isEmpty(component.effectiveLicenses)) {
        continue;
      }
      LicensesTableRow licensesTableRow = new LicensesTableRow();
      licensesTableRow.overridden = CollectionUtils.isNotEmpty(component.overriddenLicenses);
      licensesTableRow.threatLevel = getMaxLicenseThreatLevel(component.effectiveLicenseThreats);
      licensesTableRow.effectiveLicenses = licensesToString(component.effectiveLicenses);
      licensesTableRow.componentName = component.displayName;
      licensesTableRows.add(licensesTableRow);
    }
    return licensesTableRows;
  }

  private void addBomSection() throws IOException {
    // Start bill-of-materials section
    PDPage page = newPage();
    PDRectangle pageRec = page.getCropBox();
    try (PDPageContentStream contentStream = new PDPageContentStream(pdf, page)) {
      // Add title and dates
      float titleAndDatesStartY = addTitleAndDates(contentStream, pageRec, "Component BOM", MARGIN,
          pageRec.getHeight() - MARGIN - titleFontStyle.getFontHeight());

      // Add components summary
      int totalComponents = pdfData.components.size();
      int totalMatched = (int) pdfData.components.stream()
          .filter(
              component -> MatchState.EXACT.getName().equalsIgnoreCase(component.matchState) ||
                  MatchState.SIMILAR.getName().equalsIgnoreCase(component.matchState))
          .count();
      long componentPercentIdentified = Math.round(100.0d * totalMatched / totalComponents);
      float donutChartStartX = MARGIN;
      float donutChartStartY = titleAndDatesStartY - DONUT_CHART_SIZE - dateDescriptorFontStyle.getFontDescent()
          - SUMMARY_PADDING;
      IdentifiedPercentDonutChart identifiedPercentDonutChart =
          new IdentifiedPercentDonutChart(componentPercentIdentified);
      drawChart(pdf, contentStream, donutChartStartX, donutChartStartY, DONUT_CHART_SIZE, DONUT_CHART_SIZE,
          identifiedPercentDonutChart);
      float totalComponentsStartX = donutChartStartX + DONUT_CHART_SIZE + PADDING;
      float totalComponentsStartY = donutChartStartY + SUMMARY_IMAGE_SIZE / 2f;
      addText(contentStream, totalComponentsStartX, totalComponentsStartY, summaryHeaderFontStyle,
          totalComponents + " COMPONENTS");
      float componentPercentIdentifiedStartX = totalComponentsStartX;
      float componentPercentIdentifiedStartY = donutChartStartY;
      addText(contentStream, componentPercentIdentifiedStartX, componentPercentIdentifiedStartY, summaryFontStyle,
          componentPercentIdentified + "% of all components identified");

      // Add bom table
      Table table = createBomTable(page);
      TableDrawer tableDrawer =
          createTableDrawer(contentStream, donutChartStartY - summaryFontStyle.getFontDescent()
              - SUMMARY_PADDING, table);

      // End bom section
      pdf.addPage(page);
      draw(tableDrawer);
    }
  }

  private float addSbomMetadataSection(
      final PDPageContentStream contentStream,
      final PDRectangle pageRec,
      final int startX,
      final float startY) throws IOException
  {
    float yPosition = startY;
    if (this.productContext.equals(Context.SBOM)) {
      addText(contentStream, startX, yPosition, sbomMetadataTitleFontStyle, "SBOM Metadata");
      yPosition -= sbomMetadataFontStyle.getFontHeight();
      yPosition -= SBOM_METADATA_TITLE_TOP_MARGIN;

      if (pdfData.sbomMetadata.specification.equals(SbomSpecification.CYCLONEDX.toString())) {
        yPosition = addSbomMetadataForCDX(contentStream, pageRec, startX, yPosition);
      }
      else {
        yPosition = addSbomMetadataForSPDX(contentStream, pageRec, startX, yPosition);
      }
    }

    return yPosition;
  }

  private float addSbomMetadataForCDX(
      final PDPageContentStream contentStream,
      final PDRectangle pageRec,
      final int startX,
      final float startY) throws IOException
  {
    List<String> values;
    List<String> labelsToUse;
    values = Arrays.asList(
        getSbomMetadataListValuesJoinedOrDefault(pdfData.sbomMetadata.author),
        getSbomMetadataListValuesJoinedOrDefault(pdfData.sbomMetadata.manufacturer),
        getSbomMetadataListValuesJoinedOrDefault(pdfData.sbomMetadata.supplier),
        pdfData.sbomMetadata.specification,
        pdfData.sbomMetadata.specVersion,
        pdfData.sbomMetadata.fileFormat,
        pdfData.sbomMetadata.originalFile);
    labelsToUse = SBOM_METADATA_CDX_LABELS;
    return addSbomMetadataSectionToPdf(labelsToUse, contentStream, pageRec, startX, startY, values);
  }

  private float addSbomMetadataForSPDX(
      final PDPageContentStream contentStream,
      final PDRectangle pageRec,
      final int startX,
      final float startY) throws IOException
  {
    List<String> values;
    List<String> labelsToUse;
    values = List.of(
        getSbomMetadataListValuesJoinedOrDefault(pdfData.sbomMetadata.person),
        getSbomMetadataListValuesJoinedOrDefault(pdfData.sbomMetadata.organization),
        pdfData.sbomMetadata.specification,
        pdfData.sbomMetadata.specVersion,
        pdfData.sbomMetadata.fileFormat);
    labelsToUse = SBOM_METADATA_SPDX_LABELS;
    return addSbomMetadataSectionToPdf(labelsToUse, contentStream, pageRec, startX, startY, values);
  }

  private float addSbomMetadataSectionToPdf(
      List<String> labelsToUse,
      final PDPageContentStream contentStream,
      final PDRectangle pageRec,
      final int startX,
      float startY,
      List<String> values) throws IOException
  {
    for (int i = 0; i < labelsToUse.size(); i++) {
      if (labelsToUse.get(i).equals(ORIGINAL_FILE_LABEL) && values.get(i) == null) {
        continue;
      }
      addText(contentStream, startX, startY, sbomMetadataFontStyle, labelsToUse.get(i));
      startY = addTextWithWordWrapAtChar(contentStream, pageRec, startX + SBOM_METADATA_LABELS_Y_MARGIN,
          startY, MARGIN, sbomMetadataFontStyle, values.get(i), ",");
    }

    return startY;
  }

  private String getSbomMetadataListValuesJoinedOrDefault(List<String> listValues) {
    return listValues.isEmpty() ? SBOM_METADATA_EMPTY_DEFAULT : StringUtils.join(listValues, ", ");
  }

  // Visible for testing
  Table createBomTable(PDPage page) {
    float componentWidthPercent = 100;

    float tableWidthOnePercent = (page.getCropBox().getWidth() - 2 * MARGIN) / 100;
    TableBuilder tableBuilder = Table.builder().addColumnsOfWidth(tableWidthOnePercent * componentWidthPercent);

    // Add bom table headers
    tableBuilder.addRow(Row.builder()
        .add(headerCellBuilder().text("COMPONENT").build())
        .build());

    // Add bom table data
    List<BomTableRow> bomTableRows = createBomTableData();
    bomTableRows.sort(null);
    for (BomTableRow bomTableRow : bomTableRows) {
      List<Row> rows = buildTableRowAndSplitIfNeeded(new TextCellBuilder(bomTableRow.componentName, this::cellBuilder));
      rows.forEach(tableBuilder::addRow);
    }

    return tableBuilder.build();
  }

  private List<BomTableRow> createBomTableData() {
    List<BomTableRow> bomTableRows = new ArrayList<>();
    for (PdfComponent component : pdfData.components) {
      BomTableRow bomTableRow = new BomTableRow();
      bomTableRow.componentName = component.displayName;
      bomTableRows.add(bomTableRow);
    }
    return bomTableRows;
  }

  private float addTitleAndDates(
      PDPageContentStream contentStream,
      PDRectangle pageRec,
      String sectionName,
      float startX,
      float startY) throws IOException
  {
    // Add title
    addText(contentStream, startX, startY, titleFontStyle, getTitle(sectionName));

    // Add dates
    String createdOn = "Created on:";
    String analyzedOn = "Analyzed on: ";

    float createdOnDescriptorStartX = MARGIN;
    float createdOnDescriptorStartY =
        startY - titleFontStyle.getFontDescent() - dateDescriptorFontStyle.getFontAscent();
    addText(contentStream, createdOnDescriptorStartX, createdOnDescriptorStartY, dateDescriptorFontStyle, createdOn);
    float createdOnDateStartX = createdOnDescriptorStartX + dateDescriptorFontStyle.getStringWidth(analyzedOn);
    float createdOnDateStartY = createdOnDescriptorStartY;
    addText(contentStream, createdOnDateStartX, createdOnDateStartY, dateFontStyle, createdOnDateTime);

    float analyzedOnDescriptorStartX = createdOnDescriptorStartX;
    float analyzedOnDescriptorStartY =
        createdOnDescriptorStartY - dateDescriptorFontStyle.getFontHeight();
    addText(contentStream, analyzedOnDescriptorStartX, analyzedOnDescriptorStartY, dateDescriptorFontStyle, analyzedOn);
    float analyzedOnDateStartX = analyzedOnDescriptorStartX + dateDescriptorFontStyle.getStringWidth(analyzedOn);
    float analyzedOnDateStartY = analyzedOnDescriptorStartY;
    addText(contentStream, analyzedOnDateStartX, analyzedOnDateStartY, dateFontStyle, analyzedOnDateTime);

    addCommitHash(contentStream, pageRec, createdOnDescriptorStartY);
    if (!this.productContext.equals(Context.SBOM)) {
      addProductVersion(contentStream, pageRec, analyzedOnDateStartY);
    }
    return analyzedOnDateStartY;
  }

  private void addCommitHash(PDPageContentStream contentStream, PDRectangle pageRec, float startY) throws IOException {
    if (pdfData.commitHash != null && !this.productContext.equals(Context.SBOM)) {
      String commitLabel = "Commit: ";
      float commitHashStartY = startY;
      float commitHashStartX =
          pageRec.getUpperRightX() - MARGIN - dateFontStyle.getStringWidth(pdfData.commitHash);
      addText(contentStream, commitHashStartX, commitHashStartY, dateFontStyle, pdfData.commitHash);
      commitHashStartX -= dateDescriptorFontStyle.getStringWidth(commitLabel);
      addText(contentStream, commitHashStartX, commitHashStartY, dateDescriptorFontStyle, commitLabel);
    }
  }

  private void addProductVersion(
      PDPageContentStream contentStream,
      PDRectangle pageRec,
      float startY) throws IOException
  {
    String versionLabel = "IQ Server release: ";
    float versionStartX = pageRec.getUpperRightX() - MARGIN - dateFontStyle.getStringWidth(pdfData.productVersion);
    float versionStartY = startY;
    addText(contentStream, versionStartX, versionStartY, dateFontStyle, pdfData.productVersion);
    versionStartX -= dateDescriptorFontStyle.getStringWidth(versionLabel);
    addText(contentStream, versionStartX, versionStartY, dateDescriptorFontStyle, versionLabel);
  }

  private void addPageNumbers() throws IOException {
    int pageNumber = 1;
    PDPageTree pdPageTree = pdf.getPages();
    for (PDPage page : pdPageTree) {
      String pageText = "Page " + pageNumber + " of " + pdPageTree.getCount();
      try (PDPageContentStream contentStream = new PDPageContentStream(pdf, page, AppendMode.APPEND, true, true)) {
        addText(contentStream,
            page.getCropBox().getLowerLeftX() + page.getCropBox().getWidth() - MARGIN
                - summaryFontStyle.getStringWidth(pageText),
            summaryFontStyle.getFontDescent() + MARGIN,
            summaryFontStyle,
            pageText);
      }
      pageNumber++;
    }
  }

  // Visible for testing
  String getTitle(String sectionName) {
    return sectionName + (StringUtils.isBlank(pdfData.title) ? "" : " for " + pdfData.title);
  }

  // Visible for testing
  long countPolicyViolations(int minThreatLevel, int maxThreatLevel) {
    return pdfData.components.stream()
        .flatMap(component -> component.policyViolations.stream())
        .filter(isNotLegacyViolation)
        .filter(violation -> violation.policyThreatLevel >= minThreatLevel
            && violation.policyThreatLevel <= maxThreatLevel)
        .count();
  }

  // Visible for testing
  long countAffectedComponents() {
    return pdfData.components.stream()
        .filter(component -> component.policyViolations.stream()
            .anyMatch(violation -> isNotLegacyViolation.test(violation) && violation.policyThreatLevel >= 2))
        .count();
  }

  private TableDrawer createTableDrawer(PDPageContentStream contentStream, float tableStartY, Table table) {
    return RepeatedHeaderTableDrawer.builder()
        .contentStream(contentStream)
        .startX(MARGIN)
        .startY(tableStartY)
        .endY(MARGIN + summaryFontStyle.getFontHeight() * 2)
        .table(table)
        .build();
  }

  private TextCell.TextCellBuilder<?, ?> headerCellBuilder() {
    return TextCell.builder()
        .settings(Settings.builder().build())
        .borderWidth(CELL_BORDER_WIDTH)
        .font(tableRowHeaderFontStyle.getFont())
        .fontSize((int) tableRowHeaderFontStyle.getFontSize())
        .textColor(tableRowHeaderFontStyle.getFontColor())
        .horizontalAlignment(HorizontalAlignment.CENTER)
        .verticalAlignment(VerticalAlignment.MIDDLE)
        .backgroundColor(HEADER_FILL_COLOR)
        .borderColor(CELL_BORDER_COLOR)
        .drawer(new TextCellDrawer<TextCell>()
        {
          @Override
          protected void drawText(
              DrawingContext drawingContext,
              PositionedStyledText positionedStyledText) throws IOException
        {
            addText(drawingContext.getContentStream(), positionedStyledText.getX(), positionedStyledText.getY(),
                tableRowHeaderFontStyle, positionedStyledText.getText());
          }
        });
  }

  private TextCell.TextCellBuilder<?, ?> cellBuilder(String text) {
    return TextCell.builder()
        .settings(Settings.builder().build())
        .borderWidthBottom(CELL_BORDER_WIDTH)
        .font(tableRowFontStyle.getFont())
        .fontSize((int) tableRowFontStyle.getFontSize())
        .textColor(tableRowFontStyle.getFontColor())
        .text(text == null ? "" : text)
        .horizontalAlignment(HorizontalAlignment.LEFT)
        .verticalAlignment(VerticalAlignment.TOP)
        .borderColor(CELL_BORDER_COLOR)
        .paddingTop(PADDING)
        .drawer(new TextCellDrawer<TextCell>()
        {
          @Override
          protected void drawText(
              DrawingContext drawingContext,
              PositionedStyledText positionedStyledText) throws IOException
        {
            addText(drawingContext.getContentStream(), positionedStyledText.getX(), positionedStyledText.getY(),
                tableRowFontStyle, positionedStyledText.getText());
          }
        });
  }

  private ParagraphCellBuilder<?, ?> paragraphCellBuilder() {
    return ParagraphCell.builder()
        .settings(Settings.builder()
            .font(tableRowFontStyle.getFont())
            .fontSize((int) tableRowFontStyle.getFontSize())
            .textColor(tableRowFontStyle.getFontColor())
            .build())
        .borderWidthBottom(CELL_BORDER_WIDTH)
        .horizontalAlignment(HorizontalAlignment.LEFT)
        .verticalAlignment(VerticalAlignment.TOP)
        .paddingTop(0)
        .borderColor(CELL_BORDER_COLOR);
  }

  private static class TextCellBuilder
  {
    private final List<String> textParts;

    private final Function<String, AbstractCellBuilder<?, ?>> textToCellBuilder;

    public TextCellBuilder(
        final String text,
        final Function<String, AbstractCellBuilder<?, ?>> textToCellBuilder)
    {
      this.textParts =
          Lists.newArrayList(Splitter.fixedLength(MAX_CELL_CHARACTERS).split(text == null ? "" : text).iterator());
      this.textToCellBuilder = textToCellBuilder;
    }

    private String getTextPartOrEmpty(int index) {
      if (textParts.size() > index) {
        return textParts.get(index);
      }
      return "";
    }
  }

  private List<Row> buildTableRowAndSplitIfNeeded(TextCellBuilder... textCellBuilders) {
    List<Row> result = new ArrayList<>();
    int rows = Arrays.stream(textCellBuilders)
        .filter(Objects::nonNull)
        .mapToInt(textCellBuilder -> textCellBuilder.textParts.size())
        .max()
        .orElse(0);
    for (int row = 0; row < rows; row++) {
      RowBuilder rowBuilder = Row.builder();
      for (TextCellBuilder textCellBuilder : textCellBuilders) {
        if (textCellBuilder == null) {
          continue;
        }
        AbstractCellBuilder<?, ?> abstractCellBuilder =
            textCellBuilder.textToCellBuilder.apply(textCellBuilder.getTextPartOrEmpty(row));
        if (rows > 1) {
          if (row == 0) {
            abstractCellBuilder.borderWidthBottom(0);
          }
          else if (row == rows - 1) {
            abstractCellBuilder.borderWidthTop(0);
          }
          else {
            abstractCellBuilder.borderWidthTop(0).borderWidthBottom(0);
          }
        }
        rowBuilder.add(abstractCellBuilder.build());
      }
      result.add(rowBuilder.build());
    }
    return result;
  }

  // Visible for testing
  static String licensesToString(List<PdfComponentLicense> licenses) {
    if (CollectionUtils.isEmpty(licenses)) {
      return "";
    }

    return licenses.stream().map(license -> license.name).collect(Collectors.joining(", "));
  }

  static Integer getMaxLicenseThreatLevel(List<PdfComponentLicenseThreat> licenseThreats) {
    if (CollectionUtils.isEmpty(licenseThreats)) {
      return 0;
    }
    return licenseThreats.stream()
        .map(licenseThreat -> licenseThreat.licenseThreatGroupLevel)
        .max(Integer::compareTo)
        .orElse(0);
  }

  /**
   * This is a slight modification of rst.pdfbox.layout.util.WordBreakers.DefaultWordBreaker to not break a word if a
   * dash or period is found after a non-digit letter unless it has no other choice i.e.
   * <p>
   * Breaks a word if one of the following characters is found after a non-digit letter:
   * <ul>
   * <li>,</li>
   * <li>/</li>
   * </ul>
   * <p>
   * This also provides a fix for rst.pdfbox.layout.util.WordBreakers.AbstractWordBreaker.breakWordHard potentially
   * causing a StringIndexOutOfBoundsException, see CLM-15256
   */
  public static class WordBreaker
      extends WordBreakers.AbstractWordBreaker
  {
    /**
     * A letter followed by either <code>,</code> or <code>/</code>.
     */
    private final Pattern breakPattern = Pattern.compile("[A-Za-z\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u00FF]([\\,/])");

    @Override
    protected Pair<String> breakWordSoft(
        String word,
        FontDescriptor fontDescriptor,
        float maxWidth) throws IOException
    {
      Matcher matcher = breakPattern.matcher(word);
      int breakIndex = -1;
      boolean maxWidthExceeded = false;
      while (!maxWidthExceeded && matcher.find()) {
        int currentIndex = matcher.end();
        if (currentIndex < word.length() - 1) {
          if (TextSequenceUtil.getStringWidth(word.substring(0, currentIndex),
              fontDescriptor) < maxWidth)
          {
            breakIndex = currentIndex;
          }
          else {
            maxWidthExceeded = true;
          }
        }
      }

      if (breakIndex < 0) {
        return null;
      }
      return new Pair<>(word.substring(0, breakIndex), word.substring(breakIndex));
    }

    @Override
    protected Pair<String> breakWordHard(
        String word,
        FontDescriptor fontDescriptor,
        float maxWidth) throws IOException
    {
      int cutIndex = (int) (maxWidth / TextSequenceUtil.getEmWidth(fontDescriptor));
      float currentWidth = TextSequenceUtil.getStringWidth(word.substring(0, cutIndex), fontDescriptor);
      if (currentWidth > maxWidth) {
        while (currentWidth > maxWidth) {
          --cutIndex;
          currentWidth = TextSequenceUtil.getStringWidth(word.substring(0, cutIndex), fontDescriptor);
        }
        ++cutIndex;
      }
      else if (currentWidth < maxWidth) {
        while (currentWidth < maxWidth) {
          ++cutIndex;
          if (cutIndex > word.length()) {
            break;
          }
          currentWidth = TextSequenceUtil.getStringWidth(word.substring(0, cutIndex), fontDescriptor);
        }
        --cutIndex;
      }
      return new Pair<>(word.substring(0, cutIndex), word.substring(cutIndex));
    }
  }

  // Visible for testing
  static PDPage newPage() {
    PDPage page = new PDPage();
    page.setMediaBox(MEDIA_BOX_SIZE);
    page.setCropBox(CROP_BOX_SIZE);
    return page;
  }

  private void draw(TableDrawer tableDrawer) throws IOException {
    tableDrawer
        .draw(() -> pdf, PdfGenerator::newPage, MEDIA_BOX_SIZE.getHeight() - CROP_BOX_SIZE.getHeight() + MARGIN);
  }

  public static void generate(ReportPdfEntity reportPdf, PdfData pdfData) throws IOException {
    generate(reportPdf, pdfData, Context.LIFECYCLE);
  }

  public static void generate(ReportPdfEntity reportPdf, PdfData pdfData, Context productContext) throws IOException {
    if (reportPdf.canCreate()) {
      try {
        log.debug("Generating report PDF {}", reportPdf);
        long millis = System.currentTimeMillis();

        new PdfGenerator(reportPdf, pdfData, productContext).generate();
        if (reportPdf.length() <= 0) {
          throw new IOException("Could not generate report " + reportPdf);
        }

        millis = System.currentTimeMillis() - millis;
        log.debug("Generated report PDF {} in {} ms", reportPdf, millis);
      }
      catch (Exception e) {
        try {
          reportPdf.deleteIfExists();
        }
        catch (Exception suppressed) {
          e.addSuppressed(suppressed);
        }
        if (reportPdf.exists()) {
          log.error("Could not delete broken PDF {}", reportPdf);
        }
        throw e;
      }
    }
  }
}
