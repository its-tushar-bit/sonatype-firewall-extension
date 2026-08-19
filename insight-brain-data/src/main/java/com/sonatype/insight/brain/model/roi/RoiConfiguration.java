/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.roi;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "roi_configuration")
public class RoiConfiguration
    implements HasStringId
{
  @Id
  @Column(name = "roi_configuration_id")
  private String id;

  @Column(name = "currency")
  @Enumerated(EnumType.STRING)
  private CurrencyTypes currency;

  @Column(name = "malware_attacks_prevented")
  private BigDecimal malwareAttacksPrevented;

  @Column(name = "namespace_attacks_prevented")
  private BigDecimal namespaceAttacksPrevented;

  @Column(name = "safe_components_auto_selected")
  private BigDecimal safeComponentsAutoSelected;

  @Column(name = "baseline_days_to_resolve_violation")
  private Integer baselineDaysToResolveViolation;

  @Column(name = "daily_risk_cost_of_unfixed_violation")
  private BigDecimal dailyRiskCostOfUnfixedViolation;

  public RoiConfiguration() {
  }

  public RoiConfiguration(
      final CurrencyTypes currency,
      final BigDecimal malwareAttacksPrevented,
      final BigDecimal namespaceAttacksPrevented,
      final BigDecimal safeComponentsAutoSelected,
      final Integer baselineDaysToResolveViolation,
      final BigDecimal dailyRiskCostOfUnfixedViolation)
  {
    this.currency = currency;
    this.malwareAttacksPrevented = malwareAttacksPrevented;
    this.namespaceAttacksPrevented = namespaceAttacksPrevented;
    this.safeComponentsAutoSelected = safeComponentsAutoSelected;
    this.baselineDaysToResolveViolation = baselineDaysToResolveViolation;
    this.dailyRiskCostOfUnfixedViolation = dailyRiskCostOfUnfixedViolation;
  }

  public Integer getBaselineDaysToResolveViolation() {
    return baselineDaysToResolveViolation;
  }

  public void setBaselineDaysToResolveViolation(final Integer baselineDaysToResolveViolation) {
    this.baselineDaysToResolveViolation = baselineDaysToResolveViolation;
  }

  public BigDecimal getDailyRiskCostOfUnfixedViolation() {
    return dailyRiskCostOfUnfixedViolation;
  }

  public void setDailyRiskCostOfUnfixedViolation(final BigDecimal dailyRiskCostOfUnfixedViolation) {
    this.dailyRiskCostOfUnfixedViolation = dailyRiskCostOfUnfixedViolation;
  }

  public CurrencyTypes getCurrency() {
    return currency;
  }

  public BigDecimal getMalwareAttacksPrevented() {
    return malwareAttacksPrevented;
  }

  public BigDecimal getNamespaceAttacksPrevented() {
    return namespaceAttacksPrevented;
  }

  public BigDecimal getSafeComponentsAutoSelected() {
    return safeComponentsAutoSelected;
  }

  public void setCurrency(final CurrencyTypes currency) {
    this.currency = currency;
  }

  public void setMalwareAttacksPrevented(final BigDecimal supplyChainAttacksBlocked) {
    this.malwareAttacksPrevented = supplyChainAttacksBlocked;
  }

  public void setNamespaceAttacksPrevented(final BigDecimal namespaceAttacksBlocked) {
    this.namespaceAttacksPrevented = namespaceAttacksBlocked;
  }

  public void setSafeComponentsAutoSelected(final BigDecimal safeComponentsAutoSelected) {
    this.safeComponentsAutoSelected = safeComponentsAutoSelected;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public RoiConfiguration(
      final CurrencyTypes currency,
      final BigDecimal malwareAttacksPrevented,
      final BigDecimal namespaceAttacksPrevented,
      final BigDecimal safeComponentsAutoSelected)
  {
    this.currency = currency;
    this.malwareAttacksPrevented = malwareAttacksPrevented;
    this.namespaceAttacksPrevented = namespaceAttacksPrevented;
    this.safeComponentsAutoSelected = safeComponentsAutoSelected;
    this.baselineDaysToResolveViolation = 0;
    this.dailyRiskCostOfUnfixedViolation = BigDecimal.ZERO;
  }

  public RoiConfiguration(
      final CurrencyTypes currency,
      final Integer baselineDaysToResolveViolation,
      final BigDecimal dailyRiskCostOfUnfixedViolation)
  {
    this.currency = currency;
    safeComponentsAutoSelected = BigDecimal.ZERO;
    namespaceAttacksPrevented = BigDecimal.ZERO;
    malwareAttacksPrevented = BigDecimal.ZERO;
    this.dailyRiskCostOfUnfixedViolation = dailyRiskCostOfUnfixedViolation;
    this.baselineDaysToResolveViolation = baselineDaysToResolveViolation;
  }
}
