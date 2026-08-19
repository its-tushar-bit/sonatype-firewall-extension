/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.roi.dto;

import java.math.BigDecimal;

import com.sonatype.insight.brain.model.roi.CurrencyTypes;

public class RoiFirewallMetricsDTO
{
  private CurrencyTypes currency;

  private BigDecimal malwareAttacksPrevented;

  private BigDecimal namespaceAttacksPrevented;

  private BigDecimal safeComponentsAutoSelected;

  private BigDecimal totalSaved;

  public CurrencyTypes getCurrency() {
    return currency;
  }

  public void setCurrency(final CurrencyTypes currency) {
    this.currency = currency;
  }

  public BigDecimal getMalwareAttacksPrevented() {
    return malwareAttacksPrevented;
  }

  public void setMalwareAttacksPrevented(final BigDecimal malwareAttacksPrevented) {
    this.malwareAttacksPrevented = malwareAttacksPrevented;
  }

  public BigDecimal getNamespaceAttacksPrevented() {
    return namespaceAttacksPrevented;
  }

  public void setNamespaceAttacksPrevented(final BigDecimal namespaceAttacksPrevented) {
    this.namespaceAttacksPrevented = namespaceAttacksPrevented;
  }

  public BigDecimal getSafeComponentsAutoSelected() {
    return safeComponentsAutoSelected;
  }

  public void setSafeComponentsAutoSelected(final BigDecimal safeComponentsAutoSelected) {
    this.safeComponentsAutoSelected = safeComponentsAutoSelected;
  }

  public BigDecimal getTotalSaved() {
    return totalSaved;
  }

  public void setTotalSaved(final BigDecimal totalSaved) {
    this.totalSaved = totalSaved;
  }
}
