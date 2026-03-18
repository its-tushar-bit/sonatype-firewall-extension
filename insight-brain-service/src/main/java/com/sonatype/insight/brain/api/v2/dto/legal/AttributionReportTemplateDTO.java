/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.legal;

import java.util.Date;
import java.util.Objects;

import com.sonatype.insight.brain.model.legal.AttributionReportTemplate;

/**
 * @since 1.120
 */
public class AttributionReportTemplateDTO
{
  private String id;

  private String templateName;

  private String documentTitle;

  private String header;

  private String footer;

  private boolean includeTableOfContents;

  private boolean includeAppendix;

  private boolean includeStandardLicenseTexts;

  private boolean includeSonatypeSpecialLicenses;

  private Date lastUpdatedAt;

  private boolean includeInnerSource;

  public AttributionReportTemplateDTO() {
  }

  public AttributionReportTemplateDTO(
      final String id,
      final String templateName,
      final String documentTitle,
      final String header,
      final String footer,
      final boolean contents,
      final boolean appendix,
      final boolean includeStandardLicenseTexts,
      final Date lastUpdatedAt,
      final boolean includeInnerSource,
      final boolean includeSonatypeSpecialLicenses)
  {
    this.id = id;
    this.templateName = templateName;
    this.documentTitle = documentTitle;
    this.header = header;
    this.footer = footer;
    this.includeTableOfContents = contents;
    this.includeAppendix = appendix;
    this.includeStandardLicenseTexts = includeStandardLicenseTexts;
    this.lastUpdatedAt = lastUpdatedAt;
    this.includeInnerSource = includeInnerSource;
    this.includeSonatypeSpecialLicenses = includeSonatypeSpecialLicenses;
  }

  public AttributionReportTemplateDTO(
      final String templateName,
      final String documentTitle,
      final String header,
      final String footer,
      final boolean contents,
      final boolean appendix,
      final boolean includeStandardLicenseTexts,
      final boolean includeInnerSource,
      final boolean includeSonatypeSpecialLicenses)
  {
    this.templateName = templateName;
    this.documentTitle = documentTitle;
    this.header = header;
    this.footer = footer;
    this.includeTableOfContents = contents;
    this.includeAppendix = appendix;
    this.includeStandardLicenseTexts = includeStandardLicenseTexts;
    this.includeInnerSource = includeInnerSource;
    this.includeSonatypeSpecialLicenses = includeSonatypeSpecialLicenses;
  }

  public static AttributionReportTemplateDTO fromReportTemplate(
      final AttributionReportTemplate attributionReportTemplate)
  {
    if (attributionReportTemplate == null) {
      return null;
    }
    return new AttributionReportTemplateDTO(
        attributionReportTemplate.getId(),
        attributionReportTemplate.getTemplateName(),
        attributionReportTemplate.getDocumentTitle(),
        attributionReportTemplate.getDocumentHeader(),
        attributionReportTemplate.getDocumentFooter(),
        attributionReportTemplate.isIncludeTableOfContents(),
        attributionReportTemplate.isIncludeAppendix(),
        attributionReportTemplate.isIncludeStandardLicenseTexts(),
        attributionReportTemplate.getLastUpdatedAt(),
        attributionReportTemplate.isIncludeInnerSource(),
        attributionReportTemplate.isIncludeSonatypeSpecialLicenses());
  }

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public String getTemplateName() {
    return templateName;
  }

  public void setTemplateName(final String templateName) {
    this.templateName = templateName;
  }

  public String getDocumentTitle() {
    return documentTitle;
  }

  public void setDocumentTitle(final String documentTitle) {
    this.documentTitle = documentTitle;
  }

  public String getHeader() {
    return header;
  }

  public void setHeader(final String header) {
    this.header = header;
  }

  public String getFooter() {
    return footer;
  }

  public void setFooter(final String footer) {
    this.footer = footer;
  }

  public boolean isIncludeTableOfContents() {
    return includeTableOfContents;
  }

  public void setIncludeTableOfContents(final boolean includeTOC) {
    this.includeTableOfContents = includeTOC;
  }

  public boolean isIncludeAppendix() {
    return includeAppendix;
  }

  public void setIncludeAppendix(final boolean includeAppendix) {
    this.includeAppendix = includeAppendix;
  }

  public boolean isIncludeStandardLicenseTexts() {
    return includeStandardLicenseTexts;
  }

  public void setIncludeStandardLicenseTexts(final boolean includeStandardLicenseTexts) {
    this.includeStandardLicenseTexts = includeStandardLicenseTexts;
  }

  public Date getLastUpdatedAt() {
    return lastUpdatedAt;
  }

  public void setLastUpdatedAt(final Date lastUpdatedAt) {
    this.lastUpdatedAt = lastUpdatedAt;
  }

  public boolean isIncludeSonatypeSpecialLicenses() {
    return includeSonatypeSpecialLicenses;
  }

  public void setIncludeSonatypeSpecialLicenses(boolean includeSonatypeSpecialLicenses) {
    this.includeSonatypeSpecialLicenses = includeSonatypeSpecialLicenses;
  }

  public boolean isIncludeInnerSource() {
    return includeInnerSource;
  }

  public void setIncludeInnerSource(boolean includeInnerSource) {
    this.includeInnerSource = includeInnerSource;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AttributionReportTemplateDTO that = (AttributionReportTemplateDTO) o;
    return includeTableOfContents == that.includeTableOfContents && includeAppendix == that.includeAppendix &&
        includeStandardLicenseTexts == that.includeStandardLicenseTexts && Objects.equals(id, that.id) &&
        Objects.equals(templateName, that.templateName) &&
        Objects.equals(documentTitle, that.documentTitle) && Objects.equals(header, that.header) &&
        Objects.equals(footer, that.footer) && includeInnerSource == that.includeInnerSource &&
        Objects.equals(includeSonatypeSpecialLicenses, that.includeSonatypeSpecialLicenses);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, templateName, documentTitle, header, footer, includeTableOfContents, includeAppendix,
        includeStandardLicenseTexts, includeInnerSource, includeSonatypeSpecialLicenses);
  }
}
