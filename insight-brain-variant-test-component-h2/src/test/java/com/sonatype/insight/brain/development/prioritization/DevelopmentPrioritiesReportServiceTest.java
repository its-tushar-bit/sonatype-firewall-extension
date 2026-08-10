/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.development.prioritization;

import java.io.IOException;

import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentH2Test
public class DevelopmentPrioritiesReportServiceTest
    extends AbstractComponentH2Test
{
  private static final String GIVEN_SOME_PUBLIC_APP_ID = "any-app-id";

  private static final String GIVEN_SOME_SCAN_ID = "any-scan-id";

  @Mock
  private ApiReportDataServiceV2 apiReportDataServiceV2;

  private DevelopmentPrioritiesReportService developmentPrioritiesReportService;

  @BeforeEach
  public void setup() {
    developmentPrioritiesReportService = new DevelopmentPrioritiesReportService(apiReportDataServiceV2);
  }

  @Test
  public void testGetDependencyInformation_shouldThrowNotFoundExceptionGivenIOException() throws IOException {
    when(apiReportDataServiceV2.getDataForPrioritization(anyString(), anyString())).thenThrow(new IOException());

    final String expectedErrorMessage = "Could not find the requested report for prioritization.";
    assertThatThrownBy(
        () -> developmentPrioritiesReportService.getDependencyInformation(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID))
            .withFailMessage(expectedErrorMessage)
            .isInstanceOf(NotFoundException.class);

    verify(apiReportDataServiceV2).getDataForPrioritization(GIVEN_SOME_PUBLIC_APP_ID, GIVEN_SOME_SCAN_ID);
  }
}
