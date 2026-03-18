/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.roi.dtos;

import java.math.BigDecimal;

import com.sonatype.insight.brain.model.roi.CurrencyTypes;

public record RoiConfigurationDTO(
    String id,
    CurrencyTypes currency,
    BigDecimal malwareAttacksPrevented,
    BigDecimal namespaceAttacksPrevented,
    BigDecimal safeComponentsAutoSelected,
    Integer baselineDaysToResolveViolation,
    BigDecimal dailyRiskCostOfUnfixedViolation)
{
  public RoiConfigurationDTO(
      String id,
      CurrencyTypes currency,
      Integer baselineDaysToResolveViolation,
      BigDecimal dailyRiskCostOfUnfixedViolation)
  {
    this(id, currency, null, null, null, baselineDaysToResolveViolation,
        dailyRiskCostOfUnfixedViolation);
  }

  public RoiConfigurationDTO(
      String id,
      CurrencyTypes currency,
      BigDecimal supplyChainAttacksBlocked,
      BigDecimal namespaceAttacksBlocked,
      BigDecimal safeComponentsAutoSelected)
  {
    this(id, currency,
        supplyChainAttacksBlocked, namespaceAttacksBlocked, safeComponentsAutoSelected, null, null);
  }
}
