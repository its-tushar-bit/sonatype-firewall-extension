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

  public BigDecimal developerHourlyRateValue;

  public BigDecimal developerHourlyRateMinimum;

  public Long fixRateHoursValue;

  public Long fixRateHoursMinimum;

  public Boolean securityViolationCriticalEnabled;

  public BigDecimal securityViolationCriticalValue;

  public BigDecimal securityViolationCriticalValueMinimum;

  public Boolean securityViolationHighEnabled;

  public BigDecimal securityViolationHighValue;

  public BigDecimal securityViolationHighValueMinimum;

  public Boolean securityViolationMediumEnabled;

  public BigDecimal securityViolationMediumValue;

  public BigDecimal securityViolationMediumValueMinimum;

  public Boolean securityViolationLowEnabled;

  public BigDecimal securityViolationLowValue;

  public BigDecimal securityViolationLowValueMinimum;

  public BigDecimal supplyChainAttacksBlockedValue;

  public BigDecimal supplyChainAttacksBlockedValueMinimum;

  public BigDecimal namespaceAttacksBlockedValue;

  public BigDecimal namespaceAttacksBlockedValueMinimum;

  public BigDecimal safeComponentsAutoSelectedValue;

  public BigDecimal safeComponentsAutoSelectedValueMinimum;

  public Boolean waivedPoliciesCounted;

  public RoiConfigurationCurrentAndMinimumValuesDTO() {
    // for Jackson
  }
}
