/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report.pdf;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentPolicyViolationsDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportPolicyDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportPolicyViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiSecurityIssueDTO;
import com.sonatype.insight.brain.component.ComponentDisplayFilename;
import com.sonatype.insight.brain.model.component.MatchState;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vandeseer.easytable.RepeatedHeaderTableDrawer;
import org.vandeseer.easytable.TableDrawer;
import org.vandeseer.easytable.settings.HorizontalAlignment;
import org.vandeseer.easytable.settings.Settings;
import org.vandeseer.easytable.settings.VerticalAlignment;
import org.vandeseer.easytable.structure.Row;
import org.vandeseer.easytable.structure.Table;
import org.vandeseer.easytable.structure.Table.TableBuilder;
import org.vandeseer.easytable.structure.cell.TextCell;
import org.vandeseer.easytable.structure.cell.TextCell.TextCellBuilder;
import org.vandeseer.easytable.structure.cell.paragraph.ParagraphCell;
import org.vandeseer.easytable.structure.cell.paragraph.ParagraphCell.Paragraph;
import org.vandeseer.easytable.structure.cell.paragraph.ParagraphCell.Paragraph.ParagraphBuilder;
import org.vandeseer.easytable.structure.cell.paragraph.ParagraphCell.ParagraphCellBuilder;
import org.vandeseer.easytable.structure.cell.paragraph.StyledText;
import rst.pdfbox.layout.text.FontDescriptor;
import rst.pdfbox.layout.text.TextSequenceUtil;
import rst.pdfbox.layout.util.Pair;
import rst.pdfbox.layout.util.WordBreakerFactory;
import rst.pdfbox.layout.util.WordBreakers;

import static com.sonatype.insight.brain.report.pdf.PdfGeneratorUtils.addText;
import static com.sonatype.insight.brain.report.pdf.PdfGeneratorUtils.drawChart;
import static com.sonatype.insight.brain.report.pdf.PdfGeneratorUtils.drawRectangleWithText;
import static com.sonatype.insight.brain.report.pdf.PdfGeneratorUtils.loadFont;

public class PdfGenerator
{
  private static final Logger log = LoggerFactory.getLogger(PdfGenerator.class);

  private static final String REPORT_FILE_NAME = "report.pdf";

  // Use minimum of A4 and Letter dimensions to ensure the pdf can fit on both when printing without needing scaling
  // Visible for testing
  static final PDRectangle PAGE_SIZE = new PDRectangle(
      Math.min(PDRectangle.A4.getWidth(), PDRectangle.LETTER.getWidth()),
      Math.min(PDRectangle.A4.getHeight(), PDRectangle.LETTER.getHeight())
  );

  private static final String DATE_FORMAT_STRING = "EEE MMM dd yyyy 'at' HH:mm:ss";

  private static final float MARGIN = 10;

  private static final float PADDING = 3;

  private static final float SUMMARY_SPACING = 4 * PADDING;

  private static final float CELL_BORDER_WIDTH = 1;

  private static final int DONUT_CHART_SIZE = 20;

  private static final float SUMMARY_IMAGE_SIZE = 20;

  private static final float DEFAULT_FONT_SIZE = 10;

  private static final float HEADER_FONT_SIZE = 20;

  private static final float TITLE_FONT_SIZE = 16;

  private static final float GRANDFATHERED_SYMBOL_FONT_SIZE = 25;

  private static final float THREAT_LEVEL_FONT_SIZE = 12;

  private static final Color DEFAULT_FONT_COLOR = Color.BLACK;

  private static final Color APPLICATION_COMPOSITION_REPORT_COLOR = new Color(119, 130, 251);

  private static final Color DATE_DESCRIPTOR_COLOR = Color.GRAY;

  private static final Color HEADER_FILL_COLOR = new Color(245, 245, 245);

  private static final Color CELL_BORDER_COLOR = new Color(224, 224, 224);

  private static final char GRANDFATHERED_SYMBOL = '\uf1da';

  private ApiReportPolicyDataDTOV2 policyData;

  private ApiReportRawDataDTOV2 rawData;

  private File pdfFile;

  private PDDocument pdf;

  private FontStyle sonatypeFontStyle;

  private FontStyle applicationCompositionReportFontStyle;

  private FontStyle titleFontStyle;

  private FontStyle dateDescriptorFontStyle;

  private FontStyle dateFontStyle;

  private FontStyle summaryHeaderFontStyle;

  private FontStyle summaryFontStyle;

  private FontStyle grandfatheredFontStyle;

  private FontStyle tableRowHeaderFontStyle;

  private FontStyle tableRowFontStyle;

  private FontStyle rectangleFontStyle;

  private FontStyle threatLevelFontStyle;

  private FontStyle declaredLicensesFontStyle;

  private FontStyle observedLicensesFontStyle;

  private String createdOnDateTime;

  private String analyzedOnDateTime;

  static {
    System.setProperty(WordBreakerFactory.WORD_BREAKER_CLASS_PROPERTY, WordBreaker.class.getName());
  }

  // Visible for testing
  PdfGenerator(File pdfFile, ApiReportPolicyDataDTOV2 policyData, ApiReportRawDataDTOV2 rawData) {
    this.pdfFile = pdfFile;
    this.policyData = policyData;
    this.rawData = rawData;
  }

  private void generate() throws IOException {
    try (PDDocument pdf = new PDDocument()) {
      this.pdf = pdf;
      initFontStyles(pdf);
      setDocumentMetadata();
      DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_STRING);
      createdOnDateTime = dateFormat.format(new Date());
      analyzedOnDateTime = dateFormat.format(policyData.reportTime);
      addPolicyViolationsSection();
      addVulnerabilitiesSection();
      addLicensesSection();
      addBomSection();
      addPageNumbers();
      pdf.save(pdfFile);
    }
  }

  // Visible for testing
  void initFontStyles(PDDocument pdf) {
    PDFont proximanova = loadFont(pdf, "proximanova-reg-webfont.ttf");
    PDFont proximanovaSemibold = loadFont(pdf, "proximanova-sbold-webfont.ttf");
    PDFont proximanovaBold = loadFont(pdf, "proximanova-bold-webfont.ttf");
    PDFont fontawesome = loadFont(pdf, "fontawesome-webfont.ttf");

    sonatypeFontStyle = new FontStyle(proximanova, HEADER_FONT_SIZE, DEFAULT_FONT_COLOR);
    applicationCompositionReportFontStyle = new FontStyle(proximanova, HEADER_FONT_SIZE,
        APPLICATION_COMPOSITION_REPORT_COLOR);
    titleFontStyle = new FontStyle(proximanova, TITLE_FONT_SIZE, DEFAULT_FONT_COLOR);
    dateDescriptorFontStyle = new FontStyle(proximanova, DEFAULT_FONT_SIZE, DATE_DESCRIPTOR_COLOR);
    dateFontStyle = new FontStyle(proximanova, DEFAULT_FONT_SIZE, DEFAULT_FONT_COLOR);
    summaryHeaderFontStyle = new FontStyle(proximanovaBold, DEFAULT_FONT_SIZE, DEFAULT_FONT_COLOR);
    summaryFontStyle = new FontStyle(proximanova, DEFAULT_FONT_SIZE, DEFAULT_FONT_COLOR);
    grandfatheredFontStyle = new FontStyle(fontawesome, GRANDFATHERED_SYMBOL_FONT_SIZE, DEFAULT_FONT_COLOR);
    tableRowHeaderFontStyle = new FontStyle(proximanovaSemibold, DEFAULT_FONT_SIZE, DEFAULT_FONT_COLOR);
    tableRowFontStyle = new FontStyle(proximanova, DEFAULT_FONT_SIZE, DEFAULT_FONT_COLOR);
    rectangleFontStyle = new FontStyle(summaryFontStyle.getFont(), 14f, Color.WHITE);
    threatLevelFontStyle = new FontStyle(proximanovaSemibold, THREAT_LEVEL_FONT_SIZE, DEFAULT_FONT_COLOR);
    declaredLicensesFontStyle = new FontStyle(proximanovaBold, DEFAULT_FONT_SIZE, DEFAULT_FONT_COLOR);
    observedLicensesFontStyle = new FontStyle(proximanova, DEFAULT_FONT_SIZE, DEFAULT_FONT_COLOR);
  }

  private void setDocumentMetadata() {
    PDDocumentInformation docInfo = new PDDocumentInformation();
    docInfo.setTitle(policyData.application.name + " " + policyData.reportTitle);
    docInfo.setCreator("Nexus IQ Server");
    docInfo.setProducer(docInfo.getCreator());
    docInfo.setCreationDate(new GregorianCalendar());
    pdf.setDocumentInformation(docInfo);
  }

  private void addPolicyViolationsSection() throws IOException {
    // Start policy violations section
    PDPage page = newPage();
    PDRectangle pageRec = page.getMediaBox();
    try (PDPageContentStream contentStream = new PDPageContentStream(pdf, page)) {
      // Add header
      float headerLeftStartX = MARGIN;
      float headerLeftStartY = pageRec.getHeight() - MARGIN - sonatypeFontStyle.getFontHeight();
      addText(contentStream, headerLeftStartX, headerLeftStartY, sonatypeFontStyle, "Sonatype");
      String applicationCompositionReport = "Application Composition Report";
      float headerRightStartX = pageRec.getWidth() - MARGIN -
          applicationCompositionReportFontStyle.getStringWidth(applicationCompositionReport);
      float headerRightStartY = headerLeftStartY;
      addText(contentStream, headerRightStartX, headerRightStartY, applicationCompositionReportFontStyle,
          applicationCompositionReport);

      // Add title and dates
      float titleAndDatesStartY = addTitleAndDates(contentStream, "Policy Violations", MARGIN,
          headerLeftStartY - 2 * titleFontStyle.getFontHeight());

      // Add violations summary
      long critical = countPolicyViolations(8, 10);
      long severe = countPolicyViolations(4, 7);
      long moderate = countPolicyViolations(2, 3);
      long total = critical + severe + moderate;
      float criticalStartX = MARGIN;
      float criticalStartY = titleAndDatesStartY - dateDescriptorFontStyle.getFontHeight() - SUMMARY_IMAGE_SIZE;
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
      float violationsTextStartY = criticalStartY + SUMMARY_IMAGE_SIZE / 2;
      String violationsText = total + " VIOLATIONS";
      addText(contentStream, violationsTextStartX, violationsTextStartY, summaryHeaderFontStyle, violationsText);
      float affectedComponentsStartX = violationsTextStartX;
      float affectedComponentsStartY = criticalStartY;
      long affectedComponents = policyData.components.stream().filter(component -> component.violations.stream()
          .anyMatch(violation -> violation.policyThreatLevel >= 2)).count();
      String affectingText = "Affecting " + affectedComponents + " components";
      addText(contentStream, affectedComponentsStartX, affectedComponentsStartY, summaryFontStyle, affectingText);

      // Add grandfathered summary
      float maxViolationsAffectedWidth =
          Math.max(summaryHeaderFontStyle.getStringWidth(violationsText),
              summaryFontStyle.getStringWidth(affectingText));
      float grandfatheredSymbolStartX = affectedComponentsStartX + maxViolationsAffectedWidth + SUMMARY_SPACING;
      float grandfatheredSymbolStartY = affectedComponentsStartY;
      addText(contentStream, grandfatheredSymbolStartX, grandfatheredSymbolStartY, grandfatheredFontStyle,
          String.valueOf(GRANDFATHERED_SYMBOL));
      float grandfatheredCountStartX = grandfatheredSymbolStartX +
          grandfatheredFontStyle.getStringWidth(String.valueOf(GRANDFATHERED_SYMBOL)) + PADDING;
      float grandfatheredCountStartY = violationsTextStartY;
      long grandfathered = policyData.components.stream().flatMap(component -> component.violations.stream())
          .filter(violation -> violation.grandfathered).count();
      addText(contentStream, grandfatheredCountStartX, grandfatheredCountStartY, summaryHeaderFontStyle,
          grandfathered + " GRANDFATHERED");
      float grandfatheredViolationsStartX = grandfatheredCountStartX;
      float grandfatheredViolationsStartY = criticalStartY;
      addText(contentStream, grandfatheredViolationsStartX, grandfatheredViolationsStartY, summaryFontStyle,
          "violations");

      // Add policy violations table
      Table table = createPolicyViolationsTable(page);
      TableDrawer tableDrawer = createTableDrawer(contentStream, criticalStartY - MARGIN, table);

      // End policy violations section
      pdf.addPage(page);
      draw(tableDrawer);
    }
  }

  // Visible for testing
  Table createPolicyViolationsTable(PDPage page) {
    float threatLevelColorWidthPercent = 1;
    float threatLevelWidthPercent = 9;
    float policyNameWidthPercent = 20;
    float policyTypeWidthPercent = 15;
    float componentWidthPercent = 55;

    float tableWidthOnePercent = (page.getMediaBox().getWidth() - 2 * MARGIN) / 100;
    TableBuilder tableBuilder = Table.builder()
        .addColumnsOfWidth(
            tableWidthOnePercent * threatLevelColorWidthPercent,
            tableWidthOnePercent * threatLevelWidthPercent,
            tableWidthOnePercent * policyNameWidthPercent,
            tableWidthOnePercent * policyTypeWidthPercent,
            tableWidthOnePercent * componentWidthPercent);

    // Add policy violations table headers
    tableBuilder.addRow(Row.builder()
        .add(headerCellBuilder().text("THREAT").colSpan(2).build())
        .add(headerCellBuilder().text("POLICY NAME").build())
        .add(headerCellBuilder().text("POLICY TYPE").build())
        .add(headerCellBuilder().text("COMPONENT").build())
        .build());

    // Add policy violations table data
    List<PolicyViolationsTableRow> policyViolationsTableRows = createPolicyViolationsTableData();
    policyViolationsTableRows.sort(null);
    for (PolicyViolationsTableRow policyViolationsTableRow : policyViolationsTableRows) {
      tableBuilder.addRow(Row.builder()
          .add(cellBuilder("")
              .backgroundColor(ThreatLevelColor.get(policyViolationsTableRow.threatLevel)).build())
          .add(cellBuilder(String.valueOf(policyViolationsTableRow.threatLevel))
              .font(threatLevelFontStyle.getFont())
              .fontSize((int) threatLevelFontStyle.getFontSize()).textColor(threatLevelFontStyle.getFontColor())
              .build())
          .add(cellBuilder(policyViolationsTableRow.policyName).build())
          .add(cellBuilder(policyViolationsTableRow.policyType).build())
          .add(cellBuilder(policyViolationsTableRow.componentName).build())
          .build());
    }

    return tableBuilder.build();
  }

  private List<PolicyViolationsTableRow> createPolicyViolationsTableData() {
    List<PolicyViolationsTableRow> policyViolationsTableRows = new ArrayList<>();
    for (ApiReportComponentPolicyViolationsDTOV2 component : policyData.components) {
      for (ApiReportPolicyViolationDTOV2 violation : component.violations) {
        PolicyViolationsTableRow policyViolationsTableRow = new PolicyViolationsTableRow();
        policyViolationsTableRow.threatLevel = violation.policyThreatLevel;
        policyViolationsTableRow.policyName = violation.policyName;
        policyViolationsTableRow.policyType = violation.policyThreatCategory == null ? "" : StringUtils
            .capitalise(violation.policyThreatCategory.toLowerCase(Locale.ROOT));
        policyViolationsTableRow.componentName = getComponentName(component.componentIdentifier, component.pathnames);
        policyViolationsTableRows.add(policyViolationsTableRow);
      }
    }
    return policyViolationsTableRows;
  }

  private void addVulnerabilitiesSection() throws IOException {
    // Start security issues section
    PDPage page = newPage();
    PDRectangle pageRec = page.getMediaBox();
    try (PDPageContentStream contentStream = new PDPageContentStream(pdf, page)) {
      // Add title and dates
      float titleAndDatesStartY = addTitleAndDates(contentStream, "Vulnerabilities", MARGIN,
          pageRec.getHeight() - MARGIN - titleFontStyle.getFontHeight());

      // Add security issues table
      Table table = createSecurityIssuesTable(page);
      TableDrawer tableDrawer = createTableDrawer(contentStream, titleAndDatesStartY - MARGIN, table);

      // End security issues section
      pdf.addPage(page);
      draw(tableDrawer);
    }
  }

  // Visible for testing
  Table createSecurityIssuesTable(PDPage page) {
    float vulnerabilityWidthPercent = 20;
    float cvssScoreWidthPercent = 15;
    float componentWidthPercent = 65;

    float tableWidthOnePercent = (page.getMediaBox().getWidth() - 2 * MARGIN) / 100;
    TableBuilder tableBuilder = Table.builder()
        .addColumnsOfWidth(
            tableWidthOnePercent * vulnerabilityWidthPercent,
            tableWidthOnePercent * cvssScoreWidthPercent,
            tableWidthOnePercent * componentWidthPercent);

    // Add security issues table headers
    tableBuilder.addRow(Row.builder()
        .add(headerCellBuilder().text("VULNERABILITY").build())
        .add(headerCellBuilder().text("CVSS SCORE").build())
        .add(headerCellBuilder().text("COMPONENT").build())
        .build());

    // Add security issues table data
    List<SecurityIssuesTableRow> securityIssuesTableRows = createSecurityIssuesTableData();
    securityIssuesTableRows.sort(null);
    for (SecurityIssuesTableRow securityIssuesTableRow : securityIssuesTableRows) {
      tableBuilder.addRow(Row.builder()
          .add(cellBuilder(securityIssuesTableRow.reference).build())
          .add(cellBuilder(securityIssuesTableRow.severity == null ? "" : securityIssuesTableRow.severity.toString())
              .build())
          .add(cellBuilder(securityIssuesTableRow.componentName).build())
          .build());
    }

    return tableBuilder.build();
  }

  private List<SecurityIssuesTableRow> createSecurityIssuesTableData() {
    List<SecurityIssuesTableRow> securityIssuesTableRows = new ArrayList<>();
    for (ApiReportComponentDTOV2 component : rawData.components) {
      if (component.securityData == null) {
        continue;
      }
      for (ApiSecurityIssueDTO securityIssue : component.securityData.securityIssues) {
        SecurityIssuesTableRow securityIssuesTableRow = new SecurityIssuesTableRow();
        securityIssuesTableRow.reference = securityIssue.reference;
        securityIssuesTableRow.severity = securityIssue.severity;
        securityIssuesTableRow.componentName = getComponentName(component.componentIdentifier, component.pathnames);
        securityIssuesTableRows.add(securityIssuesTableRow);
      }
    }
    return securityIssuesTableRows;
  }

  private void addLicensesSection() throws IOException {
    // Start license section
    PDPage page = newPage();
    PDRectangle pageRec = page.getMediaBox();
    try (PDPageContentStream contentStream = new PDPageContentStream(pdf, page)) {
      // Add title and dates
      float titleAndDatesStartY = addTitleAndDates(contentStream, "Licenses", MARGIN,
          pageRec.getHeight() - MARGIN - titleFontStyle.getFontHeight());

      // Add licenses table
      Table table = createLicensesTable(page);
      TableDrawer tableDrawer = createTableDrawer(contentStream, titleAndDatesStartY - MARGIN, table);

      // End licenses section
      pdf.addPage(page);
      draw(tableDrawer);
    }
  }

  // Visible for testing
  Table createLicensesTable(PDPage page) {
    float licenseWidthPercent = 20;
    float componentWidthPercent = 80;

    float tableWidthOnePercent = (page.getMediaBox().getWidth() - 2 * MARGIN) / 100;
    TableBuilder tableBuilder =
        Table.builder().addColumnsOfWidth(
            tableWidthOnePercent * licenseWidthPercent,
            tableWidthOnePercent * componentWidthPercent);

    // Add licenses table headers
    tableBuilder.addRow(Row.builder()
        .add(headerCellBuilder().text("LICENSE").build())
        .add(headerCellBuilder().text("COMPONENT").build())
        .build());

    // Add licenses table data
    List<LicensesTableRow> licensesTableRows = createLicensesTableData();
    licensesTableRows.sort(null);
    for (LicensesTableRow licensesTableRow : licensesTableRows) {
      tableBuilder.addRow(Row.builder()
          .add(licensesCellBuilder(licensesTableRow.declaredLicenses, licensesTableRow.observedLicenses).build())
          .add(cellBuilder(licensesTableRow.componentName).build())
          .build());
    }
    return tableBuilder.build();
  }

  private List<LicensesTableRow> createLicensesTableData() {
    List<LicensesTableRow> licensesTableRows = new ArrayList<>();
    for (ApiReportComponentDTOV2 component : rawData.components) {
      if (component.licenseData == null) {
        continue;
      }
      LicensesTableRow licensesTableRow = new LicensesTableRow();
      licensesTableRow.declaredLicenses = licensesToString(component.licenseData.declaredLicenses);
      licensesTableRow.observedLicenses = licensesToString(component.licenseData.observedLicenses);
      licensesTableRow.componentName = getComponentName(component.componentIdentifier, component.pathnames);
      licensesTableRows.add(licensesTableRow);
    }
    return licensesTableRows;
  }

  private void addBomSection() throws IOException {
    // Start bill-of-materials section
    PDPage page = newPage();
    PDRectangle pageRec = page.getMediaBox();
    try (PDPageContentStream contentStream = new PDPageContentStream(pdf, page)) {
      // Add title and dates
      float titleAndDatesStartY = addTitleAndDates(contentStream, "Component BOM", MARGIN,
          pageRec.getHeight() - MARGIN - titleFontStyle.getFontHeight());

      // Add components summary
      int totalComponents = policyData.components.size();
      int totalMatched = (int) policyData.components.stream().filter(
          component -> MatchState.EXACT.getName().equalsIgnoreCase(component.matchState) ||
              MatchState.SIMILAR.getName().equalsIgnoreCase(component.matchState)).count();
      long componentPercentIdentified = Math.round(100.0d * totalMatched / totalComponents);
      float donutChartStartX = MARGIN;
      float donutChartStartY = titleAndDatesStartY - DONUT_CHART_SIZE - dateDescriptorFontStyle.getFontHeight();
      IdentifiedPercentDonutChart identifiedPercentDonutChart =
          new IdentifiedPercentDonutChart(componentPercentIdentified);
      drawChart(pdf, contentStream, donutChartStartX, donutChartStartY, DONUT_CHART_SIZE, DONUT_CHART_SIZE,
          identifiedPercentDonutChart);
      float totalComponentsStartX = donutChartStartX + DONUT_CHART_SIZE + PADDING;
      float totalComponentsStartY = donutChartStartY + SUMMARY_IMAGE_SIZE / 2;
      addText(contentStream, totalComponentsStartX, totalComponentsStartY, summaryHeaderFontStyle,
          totalComponents + " COMPONENTS");
      float componentPercentIdentifiedStartX = totalComponentsStartX;
      float componentPercentIdentifiedStartY = donutChartStartY;
      addText(contentStream, componentPercentIdentifiedStartX, componentPercentIdentifiedStartY, summaryFontStyle,
          componentPercentIdentified + "% of all components identified");

      // Add bom table
      Table table = createBomTable(page);
      TableDrawer tableDrawer = createTableDrawer(contentStream, donutChartStartY - MARGIN, table);

      // End bom section
      pdf.addPage(page);
      draw(tableDrawer);
    }
  }

  // Visible for testing
  Table createBomTable(PDPage page) {
    float componentWidthPercent = 100;

    float tableWidthOnePercent = (page.getMediaBox().getWidth() - 2 * MARGIN) / 100;
    TableBuilder tableBuilder = Table.builder().addColumnsOfWidth(tableWidthOnePercent * componentWidthPercent);

    // Add bom table headers
    tableBuilder.addRow(Row.builder()
        .add(headerCellBuilder().text("COMPONENT").build())
        .build());

    // Add bom table data
    List<BomTableRow> bomTableRows = createBomTableData();
    bomTableRows.sort(null);
    for (BomTableRow bomTableRow : bomTableRows) {
      tableBuilder.addRow(Row.builder().add(cellBuilder(bomTableRow.componentName).build()).build());
    }

    return tableBuilder.build();
  }

  private List<BomTableRow> createBomTableData() {
    List<BomTableRow> bomTableRows = new ArrayList<>();
    for (ApiReportComponentDTOV2 component : rawData.components) {
      BomTableRow bomTableRow = new BomTableRow();
      bomTableRow.componentName = getComponentName(component.componentIdentifier, component.pathnames);
      bomTableRows.add(bomTableRow);
    }
    return bomTableRows;
  }

  private float addTitleAndDates(
      PDPageContentStream contentStream,
      String sectionName,
      float startX,
      float startY) throws IOException
  {
    // Add title
    addText(contentStream, startX, startY, titleFontStyle, getTitle(sectionName));

    // Add dates
    String createdOn = "Created on:";
    float createdOnDescriptorStartX = MARGIN;
    float createdOnDescriptorStartY = startY - 2 * dateDescriptorFontStyle.getFontHeight();
    addText(contentStream, createdOnDescriptorStartX, createdOnDescriptorStartY, dateDescriptorFontStyle, createdOn);
    float createdOnDateStartX = 2 * createdOnDescriptorStartX + dateDescriptorFontStyle.getStringWidth(createdOn);
    float createdOnDateStartY = createdOnDescriptorStartY;
    addText(contentStream, createdOnDateStartX, createdOnDateStartY, dateFontStyle, createdOnDateTime);
    String analyzedOn = "Analyzed on:";
    float analyzedOnDescriptorStartX = createdOnDescriptorStartX;
    float analyzedOnDescriptorStartY = createdOnDescriptorStartY - 2 * dateDescriptorFontStyle.getFontHeight();
    addText(contentStream, analyzedOnDescriptorStartX, analyzedOnDescriptorStartY, dateDescriptorFontStyle, analyzedOn);
    float analyzedOnDateStartX = createdOnDateStartX;
    float analyzedOnDateStartY = analyzedOnDescriptorStartY;
    addText(contentStream, analyzedOnDateStartX, analyzedOnDateStartY, dateFontStyle, analyzedOnDateTime);
    return analyzedOnDateStartY;
  }

  private void addPageNumbers() throws IOException {
    int pageNumber = 1;
    PDPageTree pdPageTree = pdf.getPages();
    for (PDPage page : pdPageTree) {
      String pageText = "Page " + pageNumber + " of " + pdPageTree.getCount();
      try (PDPageContentStream contentStream = new PDPageContentStream(pdf, page, AppendMode.APPEND, true, true)) {
        addText(contentStream, page.getMediaBox().getLowerLeftX() + page.getMediaBox().getWidth() - MARGIN -
            summaryFontStyle.getStringWidth(pageText), MARGIN, summaryFontStyle, pageText);
      }
      pageNumber++;
    }
  }

  // Visible for testing
  String getTitle(String sectionName) {
    String title = sectionName;
    String suffix = "";
    if (policyData.application != null && policyData.application.name != null) {
      suffix += " " + policyData.application.name;
    }
    if (policyData.reportTitle != null) {
      suffix += " " + policyData.reportTitle;
    }
    title += suffix.isEmpty() ? "" : " for" + suffix;
    return title;
  }

  // Visible for testing
  long countPolicyViolations(int minThreatLevel, int maxThreatLevel) {
    return policyData.components.stream().flatMap(component -> component.violations.stream()).filter(
        violation -> violation.policyThreatLevel >= minThreatLevel && violation.policyThreatLevel <= maxThreatLevel)
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

  private TextCellBuilder<?, ?> headerCellBuilder() {
    return TextCell.builder()
        .settings(Settings.builder().build())
        .borderWidth(CELL_BORDER_WIDTH)
        .font(tableRowHeaderFontStyle.getFont())
        .fontSize((int) tableRowHeaderFontStyle.getFontSize())
        .textColor(tableRowHeaderFontStyle.getFontColor())
        .horizontalAlignment(HorizontalAlignment.CENTER)
        .verticalAlignment(VerticalAlignment.MIDDLE)
        .backgroundColor(HEADER_FILL_COLOR)
        .borderColor(CELL_BORDER_COLOR);
  }

  private TextCellBuilder<?, ?> cellBuilder(String text) {
    return TextCell.builder()
        .settings(Settings.builder().build())
        .borderWidthBottom(CELL_BORDER_WIDTH)
        .font(tableRowFontStyle.getFont())
        .fontSize((int) tableRowFontStyle.getFontSize())
        .textColor(tableRowFontStyle.getFontColor())
        .text(text == null ? "" : text)
        .horizontalAlignment(HorizontalAlignment.LEFT)
        .verticalAlignment(VerticalAlignment.TOP)
        .borderColor(CELL_BORDER_COLOR);
  }

  // Visible for testing
  ParagraphCellBuilder<?, ?> licensesCellBuilder(String declaredLicenses, String observedLicenses) {
    ParagraphBuilder paragraphBuilder = Paragraph.builder();
    if (!declaredLicenses.isEmpty()) {
      paragraphBuilder.append(StyledText.builder()
          .font(declaredLicensesFontStyle.getFont())
          .fontSize(declaredLicensesFontStyle.getFontSize())
          .color(declaredLicensesFontStyle.getFontColor())
          .text(declaredLicenses)
          .build());
    }
    if (!observedLicenses.isEmpty()) {
      paragraphBuilder.append(StyledText.builder()
          .font(observedLicensesFontStyle.getFont())
          .fontSize(observedLicensesFontStyle.getFontSize())
          .color(observedLicensesFontStyle.getFontColor())
          .text((declaredLicenses.isEmpty() ? "" : ", ") + observedLicenses)
          .build());
    }
    return ParagraphCell.builder().settings(Settings.builder().build())
        .paragraph(paragraphBuilder.build())
        .borderWidthBottom(CELL_BORDER_WIDTH)
        .horizontalAlignment(HorizontalAlignment.LEFT)
        .verticalAlignment(VerticalAlignment.TOP)
        .borderColor(CELL_BORDER_COLOR);
  }

  // Visible for testing
  static String licensesToString(List<ApiLicenseDTO> licenses) {
    return licenses.stream().map(license -> license.licenseName).collect(Collectors.joining(", "));
  }

  // Visible for testing
  static String getComponentName(ApiComponentIdentifierDTOV2 componentIdentifier, List<String> pathnames) {
    if (componentIdentifier != null) {
      try {
        return ComponentDisplayNameUtil.fromIdentifier(componentIdentifier.toComponentIdentifier()).toString();
      }
      catch (InvalidComponentIdentifierException e) {
        log.error(e.getMessage(), e);
      }
    }
    return new ComponentDisplayFilename().addPathnames(pathnames).getFilename().orElse(null);
  }

  public static File getPdfFile(File reportFile) {
    return new File(reportFile.getParentFile(), REPORT_FILE_NAME);
  }

  /**
   * This is a slight modification of rst.pdfbox.layout.util.WordBreakers.DefaultWordBreaker to not break a word if a
   * dash or period is found after a non-digit letter unless it has no other choice i.e.
   * Breaks a word if one of the following characters is found after a
   * non-digit letter:
   * <ul>
   * <li>,</li>
   * <li>/</li>
   * </ul>
   */
  protected static class WordBreaker
      extends WordBreakers.AbstractWordBreaker
  {
    /**
     * A letter followed by either <code>,</code> or <code>/</code>.
     */
    private final Pattern breakPattern = Pattern.compile("[A-Za-z\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u00FF]([\\,/])");

    public WordBreaker() {
    }

    @Override
    protected Pair<String> breakWordSoft(String word, FontDescriptor fontDescriptor, float maxWidth)
        throws IOException
    {
      Matcher matcher = breakPattern.matcher(word);
      int breakIndex = -1;
      boolean maxWidthExceeded = false;
      while (!maxWidthExceeded && matcher.find()) {
        int currentIndex = matcher.end();
        if (currentIndex < word.length() - 1) {
          if (TextSequenceUtil.getStringWidth(word.substring(0, currentIndex),
              fontDescriptor) < maxWidth) {
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
  }

  private PDPage newPage() {
    return new PDPage(PAGE_SIZE);
  }

  private void draw(TableDrawer tableDrawer) throws IOException {
    tableDrawer.draw(() -> pdf, this::newPage, MARGIN);
  }

  public static void generate(
      File pdfFile,
      ApiReportPolicyDataDTOV2 apiReportPolicyDataDTOV2,
      ApiReportRawDataDTOV2 apiReportRawDataDTOV2) throws IOException
  {
    if (!pdfFile.isFile() || pdfFile.length() == 0) {
      try {
        generatePdfFile(pdfFile, apiReportPolicyDataDTOV2, apiReportRawDataDTOV2);
      }
      catch (Exception e) {
        boolean deleted = false;
        try {
          deleted = pdfFile.delete() && !pdfFile.exists();
        }
        catch (Exception suppressed) {
          e.addSuppressed(suppressed);
        }
        if (!deleted) {
          log.error("Could not delete broken PDF {}", pdfFile);
        }
        throw e;
      }
    }
  }

  private static void generatePdfFile(
      File pdfFile,
      ApiReportPolicyDataDTOV2 policyData,
      ApiReportRawDataDTOV2 rawData)
      throws IOException
  {
    log.debug("Generating report PDF {}", pdfFile);
    long millis = System.currentTimeMillis();

    new PdfGenerator(pdfFile, policyData, rawData).generate();

    if (pdfFile.length() <= 0) {
      throw new IOException("Could not generate report " + pdfFile);
    }

    millis = System.currentTimeMillis() - millis;
    log.debug("Generated report PDF {} in {} ms", pdfFile, millis);
  }
}
