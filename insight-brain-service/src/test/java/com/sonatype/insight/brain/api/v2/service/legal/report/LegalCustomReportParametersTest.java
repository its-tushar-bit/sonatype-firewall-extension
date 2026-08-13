/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service.legal.report;

import java.util.Arrays;

import com.sonatype.insight.brain.api.v2.dto.legal.AttributionReportTemplateDTO;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LegalCustomReportParametersTest
{
  @Test
  public void testGenerateDefaultParameters() {
    final LegalCustomReportParameters parameters = LegalCustomReportParameters.builder().buildWithDefaults("appId");

    assertThat(parameters.getTitle()).isEqualTo("Attribution Report for appId");
    assertThat(parameters.getHeader()).isEmpty();
    assertThat(parameters.getFooter()).isEmpty();
    assertThat(parameters.isIncludeToc()).isTrue();
    assertThat(parameters.isIncludeStandardLicenseTexts()).isTrue();
    assertThat(parameters.isIncludeAppendix()).isTrue();
    assertThat(parameters.getNoticeFiles()).isEmpty();
    assertThat(parameters.isIncludeInnerSource()).isFalse();
    assertThat(parameters.isIncludeSonatypeSpecialLicenses()).isFalse();
  }

  @Test
  public void testFromTemplateDTO() {
    final AttributionReportTemplateDTO templateDTO = new AttributionReportTemplateDTO(
        "template name", "title", "header", "footer", true,
        true, true, true, true);

    final LegalCustomReportParameters parameters =
        LegalCustomReportParameters.builder()
            .fromAttributionReportTemplateDTO(templateDTO)
            .withNoticeFiles(Arrays.asList("one", "two"))
            .build();

    assertThat(parameters.getTitle()).isEqualTo("title");
    assertThat(parameters.getHeader()).isEqualTo("header");
    assertThat(parameters.getFooter()).isEqualTo("footer");
    assertThat(parameters.isIncludeToc()).isTrue();
    assertThat(parameters.isIncludeStandardLicenseTexts()).isTrue();
    assertThat(parameters.isIncludeAppendix()).isTrue();
    assertThat(parameters.getNoticeFiles()).isEqualTo(Arrays.asList("one", "two"));
    assertThat(parameters.isIncludeInnerSource()).isTrue();
    assertThat(parameters.isIncludeSonatypeSpecialLicenses()).isTrue();
  }
}
