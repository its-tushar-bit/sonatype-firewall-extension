/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.api.v2.service.legal.report;

import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.api.v2.dto.legal.AttributionReportTemplateDTO;

public final class LegalCustomReportParameters
{
  private final String title;

  private final String header;

  private final String footer;

  private final boolean includeToc;

  private final boolean includeStandardLicenseTexts;

  private final boolean includeAppendix;

  private final List<String> noticeFiles;

  public static Builder builder() {
    return new Builder();
  }

  private LegalCustomReportParameters(
      final String title,
      final String header,
      final String footer,
      final boolean includeToc,
      final boolean includeStandardLicenseTexts, final boolean includeAppendix, final List<String> noticeFiles)
  {
    this.title = title;
    this.header = header;
    this.footer = footer;
    this.includeToc = includeToc;
    this.includeStandardLicenseTexts = includeStandardLicenseTexts;
    this.includeAppendix = includeAppendix;
    this.noticeFiles = noticeFiles;
  }

  public String getTitle() {
    return title;
  }

  public String getHeader() {
    return header;
  }

  public String getFooter() {
    return footer;
  }

  public boolean isIncludeToc() {
    return includeToc;
  }

  public boolean isIncludeStandardLicenseTexts() {
    return includeStandardLicenseTexts;
  }

  public boolean isIncludeAppendix() {
    return includeAppendix;
  }

  public List<String> getNoticeFiles() {
    if (noticeFiles == null) {
      return Collections.emptyList();
    }
    return noticeFiles;
  }

  public static final class Builder
  {
    private String title = "";

    private String header = "";

    private String footer = "";

    private boolean includeToc = true;

    private boolean includeStandardLicenseTexts = true;

    private boolean includeAppendix = true;

    private List<String> noticeFiles = Collections.emptyList();

    private Builder() { }

    public Builder withTitle(final String title) {
      this.title = title;
      return this;
    }

    public Builder withHeader(final String header) {
      this.header = header == null ? "" : header;
      return this;
    }

    public Builder withFooter(final String footer) {
      this.footer = footer == null ? "" : footer;
      return this;
    }

    public Builder withIncludeToc(final boolean includeToc) {
      this.includeToc = includeToc;
      return this;
    }

    public Builder withIncludeStandardLicenseTexts(final boolean includeStandardLicenseTexts) {
      this.includeStandardLicenseTexts = includeStandardLicenseTexts;
      return this;
    }

    public Builder withIncludeAppendix(final boolean includeAppendix) {
      this.includeAppendix = includeAppendix;
      return this;
    }

    public Builder withNoticeFiles(final List<String> noticeFiles) {
      this.noticeFiles = noticeFiles;
      return this;
    }

    public Builder fromAttributionReportTemplateDTO(final AttributionReportTemplateDTO templateDTO) {
      return this
          .withHeader(templateDTO.getHeader())
          .withFooter(templateDTO.getFooter())
          .withTitle(templateDTO.getDocumentTitle())
          .withIncludeStandardLicenseTexts(templateDTO.isIncludeAppendix())
          .withIncludeToc(templateDTO.isIncludeTableOfContents())
          .withIncludeAppendix(templateDTO.isIncludeAppendix());
    }

    public LegalCustomReportParameters build() {
      return new LegalCustomReportParameters(
          this.title,
          this.header,
          this.footer,
          this.includeToc,
          this.includeStandardLicenseTexts,
          this.includeAppendix,
          this.noticeFiles);
    }

    public LegalCustomReportParameters buildWithDefaults(final String applicationId) {
      return new LegalCustomReportParameters(
          "Attribution Report for " + applicationId,
          "", "",
          true, true, true, Collections.emptyList()
      );
    }
  }
}
