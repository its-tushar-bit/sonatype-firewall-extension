/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service.legal.report;

import java.util.Arrays;

import com.sonatype.insight.brain.api.v2.dto.legal.AttributionReportTemplateDTO;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LegalCustomReportParametersTest
{
  @Test
  public void testSanitizeTitle() {
    final LegalCustomReportParameters reportParameters =
        LegalCustomReportParameters.builder().withTitle("<html>title</html>").build();

    assertThat(reportParameters.getTitle()).isEqualTo("&lt;html&gt;title&lt;/html&gt;");
  }

  @Test
  public void testSanitizeHeader() {
    final LegalCustomReportParameters reportParameters =
        LegalCustomReportParameters.builder().withHeader("<html>header</html>").build();

    assertThat(reportParameters.getHeader()).isEqualTo("&lt;html&gt;header&lt;/html&gt;");
  }

  @Test
  public void testSanitizeFooter() {
    final LegalCustomReportParameters reportParameters =
        LegalCustomReportParameters.builder().withFooter("<html>footer</html>").build();

    assertThat(reportParameters.getFooter()).isEqualTo("&lt;html&gt;footer&lt;/html&gt;");
  }

  @Test
  public void testSanitizeFiles() {
    final LegalCustomReportParameters reportParameters =
        LegalCustomReportParameters.builder().withNoticeFiles(
            Arrays.asList("<html>File 1</html>", "<script>File 2</script>")
        ).build();

    assertThat(reportParameters.getNoticeFiles()).containsExactly(
        "&lt;html&gt;File 1&lt;/html&gt;", "&lt;script&gt;File 2&lt;/script&gt;"
    );
  }

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
  }

  @Test
  public void testFromTemplateDTO() {
    final AttributionReportTemplateDTO templateDTO = new AttributionReportTemplateDTO(
        "template name", "title", "header", "footer", true, true, true);

    final LegalCustomReportParameters parameters =
        LegalCustomReportParameters.builder().fromAttributionReportTemplateDTO(templateDTO)
            .withNoticeFiles(Arrays.asList("one", "two"))
            .build();

    assertThat(parameters.getTitle()).isEqualTo("title");
    assertThat(parameters.getHeader()).isEqualTo("header");
    assertThat(parameters.getFooter()).isEqualTo("footer");
    assertThat(parameters.isIncludeToc()).isTrue();
    assertThat(parameters.isIncludeStandardLicenseTexts()).isTrue();
    assertThat(parameters.isIncludeAppendix()).isTrue();
    assertThat(parameters.getNoticeFiles()).isEqualTo(Arrays.asList("one", "two"));
  }
}
