/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.legal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.v2.service.legal.report.ApplicationAttributionReportBuilder;
import com.sonatype.insight.brain.api.v2.service.legal.report.LegalCustomReportParameters;
import com.sonatype.insight.error.exception.BadRequestException;

import com.codahale.metrics.annotation.Timed;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.message.internal.MediaTypes;

@Named
@Timed
@Path(LegalReportResource.MULTI_APPLICATION_REPORT_FROM_FILTER)
public class LegalReportResource
{
  public static final String REPORT = "/attribution/multiApplication/activeUserFilter/report";

  public static final String MULTI_APPLICATION_REPORT_FROM_FILTER = "/rest/" + "legal";

  static final String REPORT_FORM_TITLE = "title";

  static final String REPORT_FORM_HEADER = "header";

  static final String REPORT_FORM_FOOTER = "footer";

  static final String REPORT_FORM_TOC = "includeToc";

  static final String REPORT_FORM_STANDARD_LICENSE = "includeStandardLicenseTexts";

  static final String REPORT_FORM_SONATYPE_SPECIAL_LICENSES = "includeSonatypeSpecialLicenses";

  static final String REPORT_FORM_APPENDIX = "includeAppendix";

  static final String REPORT_FORM_NOTICE_FILES = "noticeFiles";

  static final String REPORT_FORM_INNER_SOURCE = "includeInnerSource";

  static final String DEFAULT_VALUE_FALSE = "false";

  private final ApplicationAttributionReportBuilder applicationAttributionReportBuilder;

  @Inject
  public LegalReportResource(
      final ApplicationAttributionReportBuilder applicationAttributionReportBuilder)
  {
    this.applicationAttributionReportBuilder = applicationAttributionReportBuilder;
  }

  @POST
  @Path(REPORT)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.TEXT_HTML)
  public String getLicenseLegalMultiApplicationReportFromActiveUserFilter(FormDataMultiPart formData) {
    LegalCustomReportParameters.Builder reportParametersBuilder = LegalCustomReportParameters.builder();
    reportParametersBuilder.withTitle(requireMultiPartValue(formData, REPORT_FORM_TITLE))
        .withHeader(getMultiPartValue(formData, REPORT_FORM_HEADER, ""))
        .withFooter(getMultiPartValue(formData, REPORT_FORM_FOOTER, ""))
        .withIncludeToc(Boolean.parseBoolean(getMultiPartValue(formData, REPORT_FORM_TOC, "true")))
        .withIncludeStandardLicenseTexts(
            Boolean.parseBoolean(getMultiPartValue(formData, REPORT_FORM_STANDARD_LICENSE, "true")))
        .withIncludeIncludeSonatypeSpecialLicenses(Boolean
            .parseBoolean(getMultiPartValue(formData, REPORT_FORM_SONATYPE_SPECIAL_LICENSES, DEFAULT_VALUE_FALSE)))
        .withIncludeAppendix(Boolean.parseBoolean(getMultiPartValue(formData, REPORT_FORM_APPENDIX, "true")))
        .withNoticeFiles(getNoticeFilesFromFormData(formData))
        .withIncludeInnerSource(
            Boolean.parseBoolean(getMultiPartValue(formData, REPORT_FORM_INNER_SOURCE, DEFAULT_VALUE_FALSE)));
    return applicationAttributionReportBuilder
        .generateLegalMultiApplicationAttributionReportFromActiveUserFilter(reportParametersBuilder.build());
  }

  private List<String> getNoticeFilesFromFormData(final FormDataMultiPart formData) {
    final List<FormDataBodyPart> parts = Optional.ofNullable(formData.getFields(REPORT_FORM_NOTICE_FILES))
        .orElse(Collections.emptyList());
    if (parts.isEmpty()) {
      return Collections.emptyList();
    }
    final List<String> invalidMime = new ArrayList<>();
    final List<String> files = parts.stream()
        .filter(part -> {
          if (MediaTypes.typeEqual(MediaType.TEXT_PLAIN_TYPE, part.getMediaType())) {
            return true;
          }
          invalidMime.add(part.getContentDisposition().getFileName());
          return false;
        })
        .map(FormDataBodyPart::getValue)
        .collect(Collectors.toList());
    if (!invalidMime.isEmpty()) {
      throw new BadRequestException("Following notice files must be plain text files: "
          + String.join(", ", invalidMime));
    }
    return files;
  }

  private String requireMultiPartValue(final FormDataMultiPart formData, final String paramName) {
    return Optional.ofNullable(formData.getField(paramName))
        .map(FormDataBodyPart::getValue)
        .orElseThrow(() -> new BadRequestException("Missing required parameter: " + paramName));
  }

  private String getMultiPartValue(
      final FormDataMultiPart formData,
      final String paramName,
      final String defaultValue)
  {
    return Optional.ofNullable(formData.getField(paramName))
        .map(FormDataBodyPart::getValue)
        .orElse(defaultValue);
  }
}
