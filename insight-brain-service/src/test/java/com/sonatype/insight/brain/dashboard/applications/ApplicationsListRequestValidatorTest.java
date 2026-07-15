/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.applications;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
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
public class ApplicationsListRequestValidatorTest
{
  @Mock
  private Configuration configuration;

  private ApplicationsListRequestValidator validator;

  @Before
  public void setUp() {
    lenient().when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(2048);
    validator = new ApplicationsListRequestValidator(configuration);
  }

  @Test
  public void validate_acceptsSupportedThreatLevelRanges() {
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.policyThreatLevelRanges = List.of(
        new PolicyThreatLevelFilter(8, 10),
        new PolicyThreatLevelFilter(1, 1));

    assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
  }

  @Test
  public void validate_rejectsTooManyThreatLevelRanges() {
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    List<PolicyThreatLevelFilter> ranges = new ArrayList<>();
    for (int i = 0; i < ApplicationsListRequestValidator.MAX_POLICY_THREAT_LEVEL_RANGES + 1; i++) {
      ranges.add(new PolicyThreatLevelFilter(1, 1));
    }
    request.policyThreatLevelRanges = ranges;

    assertThatThrownBy(() -> validator.validate(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("too many entries");
  }

  @Test
  public void validate_rejectsOutOfDomainThreatLevelBounds() {
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.policyThreatLevelRanges = List.of(new PolicyThreatLevelFilter(-1, 5));

    assertThatThrownBy(() -> validator.validate(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("minPolicyThreatLevel");
  }

  @Test
  public void validate_rejectsTooManyStageIds() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(2);
    validator = new ApplicationsListRequestValidator(configuration);
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.stageIds = Set.of("build", "develop", "release");

    assertThatThrownBy(() -> validator.validate(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("stageIds contains too many ids");
  }

  @Test
  public void validate_rejectsNullThreatLevelRangeElement() {
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.policyThreatLevelRanges = new ArrayList<>();
    request.policyThreatLevelRanges.add(null);
    request.policyThreatLevelRanges.add(new PolicyThreatLevelFilter(8, 10));

    assertThatThrownBy(() -> validator.validate(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("must not contain null elements");
  }
}
