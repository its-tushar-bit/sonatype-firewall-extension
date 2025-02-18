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
    BigDecimal developerHourlyRate,
    Long fixRateHours,
    Boolean securityViolationCriticalEnabled,
    BigDecimal securityViolationCriticalValue,
    Boolean securityViolationHighEnabled,
    BigDecimal securityViolationHighValue,
    Boolean securityViolationMediumEnabled,
    BigDecimal securityViolationMediumValue,
    Boolean securityViolationLowEnabled,
    BigDecimal securityViolationLowValue,
    BigDecimal supplyChainAttacksBlocked,
    BigDecimal namespaceAttacksBlocked,
    BigDecimal safeComponentsAutoSelected,
    Boolean waivedPoliciesCounted
) {
  public RoiConfigurationDTO(
      String id,
      CurrencyTypes currency,
      BigDecimal developerHourlyRate,
      Long fixRateHours,
      Boolean securityViolationCriticalEnabled,
      BigDecimal securityViolationCriticalValue,
      Boolean securityViolationHighEnabled,
      BigDecimal securityViolationHighValue,
      Boolean securityViolationMediumEnabled,
      BigDecimal securityViolationMediumValue,
      Boolean securityViolationLowEnabled,
      BigDecimal securityViolationLowValue,
      Boolean waivedPoliciesCounted)
  {
    this(id, currency, developerHourlyRate, fixRateHours, securityViolationCriticalEnabled,
        securityViolationCriticalValue, securityViolationHighEnabled, securityViolationHighValue,
        securityViolationMediumEnabled, securityViolationMediumValue, securityViolationLowEnabled,
        securityViolationLowValue, null, null, null, waivedPoliciesCounted);
  }

  public RoiConfigurationDTO(
      String id,
      CurrencyTypes currency,
      BigDecimal supplyChainAttacksBlocked,
      BigDecimal namespaceAttacksBlocked,
      BigDecimal safeComponentsAutoSelected)
  {
    this(id, currency, null, null, null, null, null, null, null, null, null, null,
        supplyChainAttacksBlocked, namespaceAttacksBlocked, safeComponentsAutoSelected, null);
  }
}
