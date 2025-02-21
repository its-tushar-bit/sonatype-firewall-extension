/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.roi;

import java.math.BigDecimal;

import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "roi_configuration_default_values")
public class RoiConfigurationDefaultValues
    implements HasStringId
{
  @Id
  @Column(name = "roi_configuration_default_values_id")
  private String id;

  @Column(name = "currency")
  @Enumerated(EnumType.STRING)
  private CurrencyTypes currency;

  @Column(name = "malware_attacks_prevented_default")
  private BigDecimal malwareAttacksPreventedDefault;

  @Column(name = "malware_attacks_prevented_minimum")
  private BigDecimal malwareAttacksPreventedMinimum;

  @Column(name = "namespace_attacks_prevented_default")
  private BigDecimal namespaceAttacksPreventedDefault;

  @Column(name = "namespace_attacks_prevented_minimum")
  private BigDecimal namespaceAttacksPreventedMinimum;

  @Column(name = "safe_components_auto_selected_default")
  private BigDecimal safeComponentsAutoSelectedDefault;

  @Column(name = "safe_components_auto_selected_minimum")
  private BigDecimal safeComponentsAutoSelectedMinimum;

  @Column(name = "baseline_days_to_resolve_violation_default")
  private Integer baselineDaysToResolveViolationDefault;

  @Column(name = "daily_risk_cost_of_unfixed_violation_default")
  private BigDecimal dailyRiskCostOfUnfixedViolationDefault;

  @Column(name = "baseline_days_to_resolve_violation_minimum")
  private Integer baselineDaysToResolveViolationMinimum;

  @Column(name = "daily_risk_cost_of_unfixed_violation_minimum")
  private BigDecimal dailyRiskCostOfUnfixedViolationMinimum;

  public RoiConfigurationDefaultValues() {
  }

  public RoiConfigurationDefaultValues(
      final CurrencyTypes currency,
      final BigDecimal malwareAttacksPreventedDefault,
      final BigDecimal malwareAttacksPreventedMinimum,
      final BigDecimal namespaceAttacksPreventedDefault,
      final BigDecimal namespaceAttacksPreventedMinimum,
      final BigDecimal safeComponentsAutoSelectedDefault,
      final BigDecimal safeComponentsAutoSelectedMinimum,
      final Integer baselineDaysToResolveViolationDefault,
      final Integer baselineDaysToResolveViolationMinimum,
      final BigDecimal dailyRiskCostOfUnfixedViolationDefault,
      final BigDecimal dailyRiskCostOfUnfixedViolationMinimum)
  {
    this.currency = currency;
    this.malwareAttacksPreventedDefault = malwareAttacksPreventedDefault;
    this.malwareAttacksPreventedMinimum = malwareAttacksPreventedMinimum;
    this.namespaceAttacksPreventedDefault = namespaceAttacksPreventedDefault;
    this.namespaceAttacksPreventedMinimum = namespaceAttacksPreventedMinimum;
    this.safeComponentsAutoSelectedDefault = safeComponentsAutoSelectedDefault;
    this.safeComponentsAutoSelectedMinimum = safeComponentsAutoSelectedMinimum;
    this.baselineDaysToResolveViolationDefault = baselineDaysToResolveViolationDefault;
    this.baselineDaysToResolveViolationMinimum = baselineDaysToResolveViolationMinimum;
    this.dailyRiskCostOfUnfixedViolationDefault = dailyRiskCostOfUnfixedViolationDefault;
    this.dailyRiskCostOfUnfixedViolationMinimum = dailyRiskCostOfUnfixedViolationMinimum;
  }

  public CurrencyTypes getCurrency() {
    return currency;
  }

  public BigDecimal getMalwareAttacksPreventedDefault() {
    return malwareAttacksPreventedDefault;
  }

  public BigDecimal getMalwareAttacksPreventedMinimum() {
    return malwareAttacksPreventedMinimum;
  }

  public BigDecimal getNamespaceAttacksPreventedDefault() {
    return namespaceAttacksPreventedDefault;
  }

  public BigDecimal getNamespaceAttacksPreventedMinimum() {
    return namespaceAttacksPreventedMinimum;
  }

  public BigDecimal getSafeComponentsAutoSelectedDefault() {
    return safeComponentsAutoSelectedDefault;
  }

  public BigDecimal getSafeComponentsAutoSelectedMinimum() {
    return safeComponentsAutoSelectedMinimum;
  }

  public void setCurrency(final CurrencyTypes currency) {
    this.currency = currency;
  }

  public void setMalwareAttacksPreventedDefault(final BigDecimal supplyChainAttacksBlockedDefault) {
    this.malwareAttacksPreventedDefault = supplyChainAttacksBlockedDefault;
  }

  public void setMalwareAttacksPreventedMinimum(final BigDecimal supplyChainAttacksBlockedMinimum) {
    this.malwareAttacksPreventedMinimum = supplyChainAttacksBlockedMinimum;
  }

  public void setNamespaceAttacksPreventedDefault(final BigDecimal namespaceAttacksBlocked) {
    this.namespaceAttacksPreventedDefault = namespaceAttacksBlocked;
  }

  public void setNamespaceAttacksPreventedMinimum(final BigDecimal namespaceAttacksBlockedMinimum) {
    this.namespaceAttacksPreventedMinimum = namespaceAttacksBlockedMinimum;
  }

  public void setSafeComponentsAutoSelectedDefault(final BigDecimal safeComponentsAutoSelectedDefault) {
    this.safeComponentsAutoSelectedDefault = safeComponentsAutoSelectedDefault;
  }

  public void setSafeComponentsAutoSelectedMinimum(final BigDecimal safeComponentsAutoSelectedMinimum) {
    this.safeComponentsAutoSelectedMinimum = safeComponentsAutoSelectedMinimum;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public Integer getBaselineDaysToResolveViolationDefault() {
    return baselineDaysToResolveViolationDefault;
  }

  public void setBaselineDaysToResolveViolationDefault(final Integer baselineDaysToResolveViolationDefault) {
    this.baselineDaysToResolveViolationDefault = baselineDaysToResolveViolationDefault;
  }

  public BigDecimal getDailyRiskCostOfUnfixedViolationDefault() {
    return dailyRiskCostOfUnfixedViolationDefault;
  }

  public void setDailyRiskCostOfUnfixedViolationDefault(final BigDecimal dailyRiskCostOfUnfixedViolationDefault) {
    this.dailyRiskCostOfUnfixedViolationDefault = dailyRiskCostOfUnfixedViolationDefault;
  }

  public Integer getBaselineDaysToResolveViolationMinimum() {
    return baselineDaysToResolveViolationMinimum;
  }

  public void setBaselineDaysToResolveViolationMinimum(final Integer baselineDaysToResolveViolationMinimum) {
    this.baselineDaysToResolveViolationMinimum = baselineDaysToResolveViolationMinimum;
  }

  public BigDecimal getDailyRiskCostOfUnfixedViolationMinimum() {
    return dailyRiskCostOfUnfixedViolationMinimum;
  }

  public void setDailyRiskCostOfUnfixedViolationMinimum(final BigDecimal dailyRiskCostOfUnfixedViolationMinumum) {
    this.dailyRiskCostOfUnfixedViolationMinimum = dailyRiskCostOfUnfixedViolationMinumum;
  }
}
