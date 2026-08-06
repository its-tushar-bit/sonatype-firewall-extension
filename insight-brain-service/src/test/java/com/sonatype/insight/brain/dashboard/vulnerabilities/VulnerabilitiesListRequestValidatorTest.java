/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.vulnerabilities;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VulnerabilitiesListRequestValidatorTest
{
  @Mock
  private Configuration configuration;

  private VulnerabilitiesListRequestValidator validator;

  @Before
  public void setUp() {
    lenient().when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(2048);
    validator = new VulnerabilitiesListRequestValidator(configuration);
  }

  @Test
  public void validate_catalogEpssRange_isAccepted() {
    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.tab = VulnerabilitiesListRequestValidator.TAB_CATALOG;
    request.minEpssScore = 0.25f;
    request.maxEpssScore = 0.75f;

    assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
  }

  @Test
  public void validate_catalogEpssMinOnly_isAccepted() {
    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.tab = VulnerabilitiesListRequestValidator.TAB_CATALOG;
    request.minEpssScore = 0.5f;

    assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
  }

  @Test
  public void validate_catalogEpssMaxOnly_isAccepted() {
    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.tab = VulnerabilitiesListRequestValidator.TAB_CATALOG;
    request.maxEpssScore = 0.1f;

    assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
  }

  @Test
  public void validate_catalogEpssMinGreaterThanMax_returns400() {
    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.tab = VulnerabilitiesListRequestValidator.TAB_CATALOG;
    request.minEpssScore = 0.9f;
    request.maxEpssScore = 0.1f;

    assertThatThrownBy(() -> validator.validate(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("EPSS range");
  }

  @Test
  public void validate_catalogEpssOutsideDomain_returns400() {
    VulnerabilitiesListRequestDTO below = new VulnerabilitiesListRequestDTO();
    below.tab = VulnerabilitiesListRequestValidator.TAB_CATALOG;
    below.minEpssScore = -0.1f;

    VulnerabilitiesListRequestDTO above = new VulnerabilitiesListRequestDTO();
    above.tab = VulnerabilitiesListRequestValidator.TAB_CATALOG;
    above.maxEpssScore = 1.1f;

    assertThatThrownBy(() -> validator.validate(below))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("minEpssScore");
    assertThatThrownBy(() -> validator.validate(above))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("maxEpssScore");
  }

  @Test
  public void validate_rejectsTooManyCwes() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(2);
    validator = new VulnerabilitiesListRequestValidator(configuration);

    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.tab = VulnerabilitiesListRequestValidator.TAB_CATALOG;
    request.cwes = Set.of("CWE-79", "CWE-89", "CWE-22");

    assertThatThrownBy(() -> validator.validate(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("cwes contains too many ids");
  }

  @Test
  public void validate_acceptsCwesAtClauseBudget() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(3);
    validator = new VulnerabilitiesListRequestValidator(configuration);

    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.tab = VulnerabilitiesListRequestValidator.TAB_CATALOG;
    request.cwes = IntStream.rangeClosed(1, 3)
        .mapToObj(i -> "CWE-" + i)
        .collect(Collectors.toSet());

    assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
  }
}
