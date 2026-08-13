/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.violations;

import java.util.Set;

import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * API-boundary coverage for {@link ViolationsListRequestValidator} — componentHash length,
 * accepted category ids, and kitchen-sink age rejection.
 */
public class ViolationsListRequestValidatorTest
{
  private ViolationsListRequestValidator validator;

  @BeforeEach
  public void setUp() {
    validator = new ViolationsListRequestValidator();
  }

  @Test
  public void validate_nullRequest_isNoOp() {
    assertThatCode(() -> validator.validate(null)).doesNotThrowAnyException();
  }

  @Test
  public void validate_nullOrBlankComponentHash_isAccepted() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();

    request.componentHash = "   ";
    assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
  }

  @Test
  public void validate_componentHashAtMaxLength_isAccepted() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.componentHash = "a".repeat(ViolationsListRequestValidator.MAX_COMPONENT_HASH_LENGTH);

    assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
  }

  @Test
  public void validate_componentHashExceedsMaxLength_rejects() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.componentHash = "a".repeat(ViolationsListRequestValidator.MAX_COMPONENT_HASH_LENGTH + 1);

    assertThatThrownBy(() -> validator.validate(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("componentHash exceeds maximum length of 40");
  }

  @Test
  public void validate_componentHashTrimmedLengthExceedsMax_rejects() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.componentHash = "  " + "a".repeat(ViolationsListRequestValidator.MAX_COMPONENT_HASH_LENGTH + 1) + "  ";

    assertThatThrownBy(() -> validator.validate(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("componentHash exceeds maximum length of 40");
  }

  @Test
  public void validate_acceptsNonEmptyApplicationCategoryIds() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.applicationCategoryIds = Set.of("cat-1", "cat-2");

    assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
  }

  @Test
  public void validate_acceptsEmptyApplicationCategoryIds() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.applicationCategoryIds = Set.of();

    assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
  }

  @Test
  public void validate_rejectsAgeInDays() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.ageInDays = 30;

    assertThatThrownBy(() -> validator.validate(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("ageInDays");
  }

  @Test
  public void validate_rejectsUnsupportedOrderBy() {
    ViolationsListRequestDTO request = new ViolationsListRequestDTO();
    request.orderBy = "firstOccurredTime";

    assertThatThrownBy(() -> validator.validate(request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("orderBy");
  }
}
