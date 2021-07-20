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

  private String documentTitle;

  private String header;

  private String footer;

  private boolean includeTableOfContents;

  private boolean includeAppendix;

  private Date lastUpdatedAt;

  public AttributionReportTemplateDTO() {
  }

  public AttributionReportTemplateDTO(
      final String id,
      final String documentTitle,
      final String header,
      final String footer,
      final boolean contents,
      final boolean appendix,
      final Date lastUpdatedAt)
  {
    this.id = id;
    this.documentTitle = documentTitle;
    this.header = header;
    this.footer = footer;
    this.includeTableOfContents = contents;
    this.includeAppendix = appendix;
    this.lastUpdatedAt = lastUpdatedAt;
  }

  public static AttributionReportTemplateDTO fromReportTemplate(
      final AttributionReportTemplate attributionReportTemplate)
  {
    return new AttributionReportTemplateDTO(
        attributionReportTemplate.getId(),
        attributionReportTemplate.getDocumentTitle(),
        attributionReportTemplate.getDocumentHeader(),
        attributionReportTemplate.getDocumentFooter(),
        attributionReportTemplate.isIncludeTableOfContents(),
        attributionReportTemplate.isIncludeAppendix(),
        attributionReportTemplate.getLastUpdatedAt()
    );
  }

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
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

  public Date getLastUpdatedAt() {
    return lastUpdatedAt;
  }

  public void setLastUpdatedAt(final Date lastUpdatedAt) {
    this.lastUpdatedAt = lastUpdatedAt;
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
    return Objects.equals(getId(), that.getId()) &&
        Objects.equals(getDocumentTitle(), that.getDocumentTitle()) &&
        Objects.equals(getHeader(), that.getHeader()) &&
        Objects.equals(getFooter(), that.getFooter()) &&
        Objects.equals(isIncludeTableOfContents(), that.isIncludeTableOfContents()) &&
        Objects.equals(isIncludeAppendix(), that.isIncludeAppendix()) &&
        Objects.equals(getLastUpdatedAt(), that.getLastUpdatedAt());
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, documentTitle, header, footer, includeTableOfContents, includeAppendix);
  }
}
