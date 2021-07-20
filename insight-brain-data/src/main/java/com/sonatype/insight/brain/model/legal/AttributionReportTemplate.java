/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.legal;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

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

  @Column(name = "last_updated_at")
  private Date lastUpdatedAt;

  public AttributionReportTemplate() {
  }

  public AttributionReportTemplate(
      String id,
      String documentTitle,
      String documentHeader,
      String documentFooter,
      boolean includeTableOfContents,
      boolean includeAppendix)
  {
    this.id = id;
    this.documentTitle = documentTitle;
    this.documentHeader = documentHeader;
    this.documentFooter = documentFooter;
    this.includeTableOfContents = includeTableOfContents;
    this.includeAppendix = includeAppendix;
  }

  public AttributionReportTemplate(
      String documentTitle,
      String documentHeader,
      String documentFooter,
      boolean includeTableOfContents,
      boolean includeAppendix)
  {
    this.id = null;
    this.documentTitle = documentTitle;
    this.documentHeader = documentHeader;
    this.documentFooter = documentFooter;
    this.includeTableOfContents = includeTableOfContents;
    this.includeAppendix = includeAppendix;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
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

  public Date getLastUpdatedAt() {
    return lastUpdatedAt;
  }

  public void setLastUpdatedAt(Date lastUpdatedAt) {
    this.lastUpdatedAt = lastUpdatedAt;
  }
}
