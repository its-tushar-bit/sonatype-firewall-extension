/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.roi;

import java.math.BigDecimal;

import com.sonatype.insight.brain.model.roi.CurrencyTypes;

public class RoiConfigurationCurrentAndMinimumValuesDTO
{
  public CurrencyTypes currency;

  public BigDecimal malwareAttacksPrevented;

  public BigDecimal malwareAttacksPreventedMinimum;

  public BigDecimal namespaceAttacksPrevented;

  public BigDecimal namespaceAttacksPreventedMinimum;

  public BigDecimal safeComponentsAutoSelected;

  public BigDecimal safeComponentsAutoSelectedMinimum;

  public Integer baselineDaysToResolveViolation;

  public Integer baselineDaysToResolveViolationMinimum;

  public BigDecimal dailyRiskCostOfUnfixedViolation;

  public BigDecimal dailyRiskCostOfUnfixedViolationMinimum;

  public RoiConfigurationCurrentAndMinimumValuesDTO() {
    // for Jackson
  }

  public RoiConfigurationCurrentAndMinimumValuesDTO(
      final CurrencyTypes currency,
      final BigDecimal malwareAttacksPrevented,
      final BigDecimal malwareAttacksPreventedMinimum,
      final BigDecimal namespaceAttacksPrevented,
      final BigDecimal namespaceAttacksPreventedMinimum,
      final BigDecimal safeComponentsAutoSelected,
      final BigDecimal safeComponentsAutoSelectedMinimum,
      final Integer baselineDaysToResolveViolation,
      final Integer baselineDaysToResolveViolationMinimum,
      final BigDecimal dailyRiskCostOfUnfixedViolation,
      final BigDecimal dailyRiskCostOfUnfixedViolationMinimum)
  {
    this.currency = currency;
    this.malwareAttacksPrevented = malwareAttacksPrevented;
    this.malwareAttacksPreventedMinimum = malwareAttacksPreventedMinimum;
    this.namespaceAttacksPrevented = namespaceAttacksPrevented;
    this.namespaceAttacksPreventedMinimum = namespaceAttacksPreventedMinimum;
    this.safeComponentsAutoSelected = safeComponentsAutoSelected;
    this.safeComponentsAutoSelectedMinimum = safeComponentsAutoSelectedMinimum;
    this.baselineDaysToResolveViolation = baselineDaysToResolveViolation;
    this.baselineDaysToResolveViolationMinimum = baselineDaysToResolveViolationMinimum;
    this.dailyRiskCostOfUnfixedViolation = dailyRiskCostOfUnfixedViolation;
    this.dailyRiskCostOfUnfixedViolationMinimum = dailyRiskCostOfUnfixedViolationMinimum;
  }
}
