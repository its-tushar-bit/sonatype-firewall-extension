/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics;

import static com.sonatype.clm.dto.model.policy.Stage.ID_BUILD;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.sonatype.insight.brain.model.Organization;

import jakarta.ws.rs.BadRequestException;
import org.junit.Test;

/**
 * Input validation for dashboard metrics filter ids (CLM-40927).
 */
public class MetricFilterValidatorTest
{
  private final MetricFilterValidator validator = new MetricFilterValidator();

  @Test
  public void testValidateAcceptsRealisticInternalIds() {
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.organizationIds = Set.of(
        Organization.ROOT_ORGANIZATION_ID,
        "2FAB4462f587401299AC3728ee21ADDc");
    request.applicationIds = Set.of("9CDe1234F567890123ABcdef45678901");
    request.stageIds = Set.of(ID_BUILD, "stage-release");
    request.tagIds = Set.of("a1b2c3d4e5f6789012345678901234ab");

    assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
  }

  @Test
  public void testValidateNullAndEmptySetsAreNoOp() {
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.organizationIds = null;
    request.applicationIds = Collections.emptySet();
    request.stageIds = null;
    request.tagIds = Collections.emptySet();

    assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
  }

  @Test
  public void testValidateRejectsQuerySyntaxInIds() {
    assertInvalidId("*:* OR x");
    assertInvalidId("a:b");
    assertInvalidId("a b");
    assertInvalidId("field[value]");
    assertInvalidId("a\"b");
  }

  @Test
  public void testValidateRejectsOverLengthId() {
    String overLengthId = IntStream.range(0, 65).mapToObj(i -> "a").collect(Collectors.joining());
    assertInvalidId(overLengthId);
  }

  @Test
  public void testValidateRejectsOversizedFilterSet() {
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.organizationIds = IntStream.range(0, MetricFilterValidator.MAX_FILTER_IDS + 1)
        .mapToObj(i -> String.format("%032d", i))
        .collect(Collectors.toSet());

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> validator.validate(request))
        .withMessage("Too many organizationIds filter ids (max 1000).");
  }

  @Test
  public void testRejectUnsupportedFiltersRejectsNonEmptySets() {
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.organizationIds = Set.of(Organization.ROOT_ORGANIZATION_ID);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> validator.rejectUnsupportedFilters(request))
        .withMessage("Request filters are not supported yet.");
  }

  @Test
  public void testRejectUnsupportedFiltersAllowsEmptyRequest() {
    assertThatCode(() -> validator.rejectUnsupportedFilters(new DashboardMetricsRequestDTO()))
        .doesNotThrowAnyException();
  }

  private void assertInvalidId(String invalidId) {
    DashboardMetricsRequestDTO request = new DashboardMetricsRequestDTO();
    request.organizationIds = Set.of(invalidId);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> validator.validate(request))
        .withMessage("Invalid organizationIds filter id.");
  }
}
