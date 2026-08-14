/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ComponentsListRequestValidatorTest
{
  @Mock
  private Configuration configuration;

  private ComponentsListRequestValidator validator;

  @BeforeEach
  public void setUp() {
    lenient().when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(2048);
    validator = new ComponentsListRequestValidator(configuration);
  }

  @Test
  public void validate_acceptsClassicComponentRiskOrderByTokens() {
    ComponentsListRequestDTO request = new ComponentsListRequestDTO();
    request.orderBy = "-TOTAL_RISK";

    assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
  }

  @Test
  public void validate_acceptsNameAndAffectedAppsOrderBy() {
    ComponentsListRequestDTO asc = new ComponentsListRequestDTO();
    asc.orderBy = "NAME";
    ComponentsListRequestDTO descApps = new ComponentsListRequestDTO();
    descApps.orderBy = "-NUMBER_OF_AFFECTED_APPS";

    assertThatCode(() -> validator.validate(asc)).doesNotThrowAnyException();
    assertThatCode(() -> validator.validate(descApps)).doesNotThrowAnyException();
  }

  @Test
  public void validate_rejectsMarthaOnlyCamelCaseOrderBy() {
    ComponentsListRequestDTO request = new ComponentsListRequestDTO();
    request.orderBy = "-policyThreatLevel";

    assertThatThrownBy(() -> validator.validate(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("orderBy");
  }

  @Test
  public void validate_acceptsSupportedThreatLevelRanges() {
    ComponentsListRequestDTO request = new ComponentsListRequestDTO();
    request.policyThreatLevelRanges = List.of(
        new PolicyThreatLevelFilter(8, 10),
        new PolicyThreatLevelFilter(1, 1));

    assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
  }

  @Test
  public void validate_rejectsTooManyThreatLevelRanges() {
    ComponentsListRequestDTO request = new ComponentsListRequestDTO();
    List<PolicyThreatLevelFilter> ranges = new ArrayList<>();
    for (int i = 0; i < ComponentsListRequestValidator.MAX_POLICY_THREAT_LEVEL_RANGES + 1; i++) {
      ranges.add(new PolicyThreatLevelFilter(1, 1));
    }
    request.policyThreatLevelRanges = ranges;

    assertThatThrownBy(() -> validator.validate(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("too many entries");
  }

  @Test
  public void validate_rejectsTooManyComponentHashes() {
    validator = new ComponentsListRequestValidator(configuration);
    ComponentsListRequestDTO request = new ComponentsListRequestDTO();
    // Soft UX ceiling is MAX_SCOPED_COMPONENT_HASH_FILTER_CLAUSES (512), not clause-budget.
    java.util.LinkedHashSet<String> hashes = new java.util.LinkedHashSet<>();
    for (int i = 0; i < ComponentsListIndexQueryBuilder.MAX_SCOPED_COMPONENT_HASH_FILTER_CLAUSES + 1; i++) {
      hashes.add("h" + i);
    }
    request.componentHashes = hashes;

    assertThatThrownBy(() -> validator.validate(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("componentHashes");
  }

  @Test
  public void validate_rejectsTooManyStageIds() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(2);
    validator = new ComponentsListRequestValidator(configuration);
    ComponentsListRequestDTO request = new ComponentsListRequestDTO();
    request.stageIds = Set.of("build", "develop", "release");

    assertThatThrownBy(() -> validator.validate(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("stageIds");
  }
}
