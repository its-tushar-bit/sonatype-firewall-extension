/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.legal;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.120
 */
@Entity
@Table(name = "attribution_report_template")
public class AttributionReportTemplate
    implements HasStringId
{
  @Id
  @Column(name = "attribution_report_template_id")
  private String id;

  @Column(name = "template_name")
  private String templateName;

  @Column(name = "document_title")
  private String documentTitle;

  @Column(name = "document_header")
  private String documentHeader;

  @Column(name = "document_footer")
  private String documentFooter;

  @Column(name = "include_table_of_contents")
  private boolean includeTableOfContents;

  @Column(name = "include_appendix")
  private boolean includeAppendix;

  @Column(name = "include_standard_license_texts")
  private boolean includeStandardLicenseTexts;

  @Column(name = "last_updated_at")
  private Date lastUpdatedAt;

  @Column(name = "include_sonatype_special_licenses")
  private boolean includeSonatypeSpecialLicenses;

  @Column(name = "include_inner_source")
  private boolean includeInnerSource;

  public AttributionReportTemplate() {
  }

  public AttributionReportTemplate(
      String id,
      String templateName,
      String documentTitle,
      String documentHeader,
      String documentFooter,
      boolean includeTableOfContents,
      boolean includeAppendix,
      boolean includeStandardLicenseTexts,
      boolean includeInnerSource,
      boolean includeSonatypeSpecialLicenses)
  {
    this.id = id;
    this.templateName = templateName;
    this.documentTitle = documentTitle;
    this.documentHeader = documentHeader;
    this.documentFooter = documentFooter;
    this.includeTableOfContents = includeTableOfContents;
    this.includeAppendix = includeAppendix;
    this.includeStandardLicenseTexts = includeStandardLicenseTexts;
    this.includeInnerSource = includeInnerSource;
    this.includeSonatypeSpecialLicenses = includeSonatypeSpecialLicenses;
  }

  public AttributionReportTemplate(
      String templateName,
      String documentTitle,
      String documentHeader,
      String documentFooter,
      boolean includeTableOfContents,
      boolean includeAppendix,
      boolean includeStandardLicenseTexts,
      boolean includeInnerSource,
      boolean includeSonatypeSpecialLicenses)
  {
    this.templateName = templateName;
    this.documentTitle = documentTitle;
    this.documentHeader = documentHeader;
    this.documentFooter = documentFooter;
    this.includeTableOfContents = includeTableOfContents;
    this.includeAppendix = includeAppendix;
    this.includeStandardLicenseTexts = includeStandardLicenseTexts;
    this.includeInnerSource = includeInnerSource;
    this.includeSonatypeSpecialLicenses = includeSonatypeSpecialLicenses;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
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

  public void setDocumentTitle(String documentTitle) {
    this.documentTitle = documentTitle;
  }

  public String getDocumentHeader() {
    return documentHeader;
  }

  public void setDocumentHeader(String documentHeader) {
    this.documentHeader = documentHeader;
  }

  public String getDocumentFooter() {
    return documentFooter;
  }

  public void setDocumentFooter(String documentFooter) {
    this.documentFooter = documentFooter;
  }

  public boolean isIncludeTableOfContents() {
    return includeTableOfContents;
  }

  public void setIncludeTableOfContents(boolean includeTableOfContents) {
    this.includeTableOfContents = includeTableOfContents;
  }

  public boolean isIncludeAppendix() {
    return includeAppendix;
  }

  public void setIncludeAppendix(boolean includeAppendix) {
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

  public void setLastUpdatedAt(Date lastUpdatedAt) {
    this.lastUpdatedAt = lastUpdatedAt;
  }

  public boolean isIncludeSonatypeSpecialLicenses() {
    return includeSonatypeSpecialLicenses;
  }

  public void setIncludeSonatypeSpecialLicenses(final boolean includeSonatypeSpecialLicenses) {
    this.includeSonatypeSpecialLicenses = includeSonatypeSpecialLicenses;
  }

  public boolean isIncludeInnerSource() {
    return includeInnerSource;
  }

  public void setIncludeInnerSource(boolean includeInnerSource) {
    this.includeInnerSource = includeInnerSource;
  }
}
