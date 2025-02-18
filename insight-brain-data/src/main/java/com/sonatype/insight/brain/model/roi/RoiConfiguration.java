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

  @Column(name = "developer_hourly_rate")
  private BigDecimal developerHourlyRate;

  @Column(name = "fix_rate_hours")
  private Long fixRateHours;

  @Column(name = "security_violation_critical_enabled")
  private Boolean securityViolationCriticalEnabled;

  @Column(name = "security_violation_critical_value")
  private BigDecimal securityViolationCriticalValue;

  @Column(name = "security_violation_high_enabled")
  private Boolean securityViolationHighEnabled;

  @Column(name = "security_violation_high_value")
  private BigDecimal securityViolationHighValue;

  @Column(name = "security_violation_medium_enabled")
  private Boolean securityViolationMediumEnabled;

  @Column(name = "security_violation_medium_value")
  private BigDecimal securityViolationMediumValue;

  @Column(name = "security_violation_low_enabled")
  private Boolean securityViolationLowEnabled;

  @Column(name = "security_violation_low_value")
  private BigDecimal securityViolationLowValue;

  @Column(name = "supply_chain_attacks_blocked_value")
  private BigDecimal supplyChainAttacksBlocked;

  @Column(name = "namespace_attacks_blocked_value")
  private BigDecimal namespaceAttacksBlocked;

  @Column(name = "safe_components_auto_selected_value")
  private BigDecimal safeComponentsAutoSelected;

  @Column(name = "waived_policies_counted")
  private Boolean waivedPoliciesCounted;

  public RoiConfiguration() {
  }

  public RoiConfiguration(
      final CurrencyTypes currency,
      final BigDecimal developerHourlyRate,
      final Long fixRateHours,
      final Boolean securityViolationCriticalEnabled,
      final BigDecimal securityViolationCriticalValue,
      final Boolean securityViolationHighEnabled,
      final BigDecimal securityViolationHighValue,
      final Boolean securityViolationMediumEnabled,
      final BigDecimal securityViolationMediumValue,
      final Boolean securityViolationLowEnabled,
      final BigDecimal securityViolationLowValue,
      final BigDecimal supplyChainAttacksBlocked,
      final BigDecimal namespaceAttacksBlocked,
      final BigDecimal safeComponentsAutoSelected,
      final Boolean waivedPoliciesCounted)
  {
    this.currency = currency;
    this.developerHourlyRate = developerHourlyRate;
    this.fixRateHours = fixRateHours;
    this.securityViolationCriticalEnabled = securityViolationCriticalEnabled;
    this.securityViolationCriticalValue = securityViolationCriticalValue;
    this.securityViolationHighEnabled = securityViolationHighEnabled;
    this.securityViolationHighValue = securityViolationHighValue;
    this.securityViolationMediumEnabled = securityViolationMediumEnabled;
    this.securityViolationMediumValue = securityViolationMediumValue;
    this.securityViolationLowEnabled = securityViolationLowEnabled;
    this.securityViolationLowValue = securityViolationLowValue;
    this.supplyChainAttacksBlocked = supplyChainAttacksBlocked;
    this.namespaceAttacksBlocked = namespaceAttacksBlocked;
    this.safeComponentsAutoSelected = safeComponentsAutoSelected;
    this.waivedPoliciesCounted = waivedPoliciesCounted;
  }

  public CurrencyTypes getCurrency() {
    return currency;
  }

  public BigDecimal getDeveloperHourlyRate() {
    return developerHourlyRate;
  }

  public Long getFixRateHours() {
    return fixRateHours;
  }

  public Boolean isSecurityViolationCriticalEnabled() {
    return securityViolationCriticalEnabled;
  }

  public BigDecimal getSecurityViolationCriticalValue() {
    return securityViolationCriticalValue;
  }

  public Boolean isSecurityViolationHighEnabled() {
    return securityViolationHighEnabled;
  }

  public BigDecimal getSecurityViolationHighValue() {
    return securityViolationHighValue;
  }

  public Boolean isSecurityViolationMediumEnabled() {
    return securityViolationMediumEnabled;
  }

  public BigDecimal getSecurityViolationMediumValue() {
    return securityViolationMediumValue;
  }

  public Boolean isSecurityViolationLowEnabled() {
    return securityViolationLowEnabled;
  }

  public BigDecimal getSecurityViolationLowValue() {
    return securityViolationLowValue;
  }

  public BigDecimal getSupplyChainAttacksBlocked() {
    return supplyChainAttacksBlocked;
  }

  public BigDecimal getNamespaceAttacksBlocked() {
    return namespaceAttacksBlocked;
  }

  public BigDecimal getSafeComponentsAutoSelected() {
    return safeComponentsAutoSelected;
  }

  public void setCurrency(final CurrencyTypes currency) {
    this.currency = currency;
  }

  public void setDeveloperHourlyRate(final BigDecimal developerHourlyRate) {
    this.developerHourlyRate = developerHourlyRate;
  }

  public void setFixRateHours(final Long fixRateHours) {
    this.fixRateHours = fixRateHours;
  }

  public void setSecurityViolationCriticalEnabled(final Boolean securityViolationCriticalEnabled) {
    this.securityViolationCriticalEnabled = securityViolationCriticalEnabled;
  }

  public void setSecurityViolationCriticalValue(final BigDecimal securityViolationCriticalValue) {
    this.securityViolationCriticalValue = securityViolationCriticalValue;
  }

  public void setSecurityViolationHighEnabled(final Boolean securityViolationHighEnabled) {
    this.securityViolationHighEnabled = securityViolationHighEnabled;
  }

  public void setSecurityViolationHighValue(final BigDecimal securityViolationHighValue) {
    this.securityViolationHighValue = securityViolationHighValue;
  }

  public void setSecurityViolationMediumEnabled(final Boolean securityViolationMediumEnabled) {
    this.securityViolationMediumEnabled = securityViolationMediumEnabled;
  }

  public void setSecurityViolationMediumValue(final BigDecimal securityViolationMediumValue) {
    this.securityViolationMediumValue = securityViolationMediumValue;
  }

  public void setSecurityViolationLowEnabled(final Boolean securityViolationLowEnabled) {
    this.securityViolationLowEnabled = securityViolationLowEnabled;
  }

  public void setSecurityViolationLowValue(final BigDecimal securityViolationLowValue) {
    this.securityViolationLowValue = securityViolationLowValue;
  }

  public void setSupplyChainAttacksBlocked(final BigDecimal supplyChainAttacksBlocked) {
    this.supplyChainAttacksBlocked = supplyChainAttacksBlocked;
  }

  public void setNamespaceAttacksBlocked(final BigDecimal namespaceAttacksBlocked) {
    this.namespaceAttacksBlocked = namespaceAttacksBlocked;
  }

  public void setSafeComponentsAutoSelected(final BigDecimal safeComponentsAutoSelected) {
    this.safeComponentsAutoSelected = safeComponentsAutoSelected;
  }

  public Boolean isWaivedPoliciesCounted() {
    return waivedPoliciesCounted;
  }

  public void setWaivedPoliciesCounted(final Boolean waivedPoliciesCounted) {
    this.waivedPoliciesCounted = waivedPoliciesCounted;
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
      final BigDecimal supplyChainAttacksBlocked,
      final BigDecimal namespaceAttacksBlocked,
      final BigDecimal safeComponentsAutoSelected)
  {
    this.currency = currency;
    this.supplyChainAttacksBlocked = supplyChainAttacksBlocked;
    this.namespaceAttacksBlocked = namespaceAttacksBlocked;
    this.safeComponentsAutoSelected = safeComponentsAutoSelected;
    developerHourlyRate = BigDecimal.ZERO;
    this.fixRateHours = 0L;
    this.securityViolationCriticalValue = BigDecimal.ZERO;
    this.securityViolationHighValue = BigDecimal.ZERO;
    this.securityViolationMediumValue = BigDecimal.ZERO;
    this.securityViolationLowValue = BigDecimal.ZERO;
  }

  public RoiConfiguration(
      final CurrencyTypes currency,
      final BigDecimal developerHourlyRate,
      final Long fixRateHours,
      final Boolean securityViolationCriticalEnabled,
      final BigDecimal securityViolationCriticalValue,
      final Boolean securityViolationHighEnabled,
      final BigDecimal securityViolationHighValue,
      final Boolean securityViolationMediumEnabled,
      final BigDecimal securityViolationMediumValue,
      final Boolean securityViolationLowEnabled,
      final BigDecimal securityViolationLowValue,
      final Boolean waivedPoliciesCounted)
  {
    this.currency = currency;
    this.developerHourlyRate = developerHourlyRate;
    this.fixRateHours = fixRateHours;
    this.securityViolationCriticalEnabled = securityViolationCriticalEnabled;
    this.securityViolationCriticalValue = securityViolationCriticalValue;
    this.securityViolationHighEnabled = securityViolationHighEnabled;
    this.securityViolationHighValue = securityViolationHighValue;
    this.securityViolationMediumEnabled = securityViolationMediumEnabled;
    this.securityViolationMediumValue = securityViolationMediumValue;
    this.securityViolationLowEnabled = securityViolationLowEnabled;
    this.securityViolationLowValue = securityViolationLowValue;
    this.waivedPoliciesCounted = waivedPoliciesCounted;
    safeComponentsAutoSelected = BigDecimal.ZERO;
    namespaceAttacksBlocked = BigDecimal.ZERO;
    supplyChainAttacksBlocked = BigDecimal.ZERO;
  }
}
