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

  @Column(name = "developer_hourly_rate_default_value")
  private BigDecimal developerHourlyRateDefault;

  @Column(name = "developer_hourly_rate_minimum_value")
  private BigDecimal developerHourlyRateMinimum;

  @Column(name = "fix_rate_hours_default_value")
  private Long fixRateHoursDefault;

  @Column(name = "fix_rate_hours_minimum_value")
  private Long fixRateHoursMinimum;

  @Column(name = "security_violation_critical_default_value")
  private BigDecimal securityViolationCriticalDefault;

  @Column(name = "security_violation_critical_minimum_value")
  private BigDecimal securityViolationCriticalMinimum;

  @Column(name = "security_violation_critical_enabled")
  private Boolean securityViolationCriticalEnabled;

  @Column(name = "security_violation_high_default_value")
  private BigDecimal securityViolationHighDefault;

  @Column(name = "security_violation_high_minimum_value")
  private BigDecimal securityViolationHighMinimum;

  @Column(name = "security_violation_high_enabled")
  private Boolean securityViolationHighEnabled;

  @Column(name = "security_violation_medium_default_value")
  private BigDecimal securityViolationMediumDefault;

  @Column(name = "security_violation_medium_minimum_value")
  private BigDecimal securityViolationMediumMinimum;

  @Column(name = "security_violation_medium_enabled")
  private Boolean securityViolationMediumEnabled;

  @Column(name = "security_violation_low_default_value")
  private BigDecimal securityViolationLowDefault;

  @Column(name = "security_violation_low_minimum_value")
  private BigDecimal securityViolationLowMinimum;

  @Column(name = "security_violation_low_enabled")
  private Boolean securityViolationLowEnabled;

  @Column(name = "supply_chain_attacks_blocked_default_value")
  private BigDecimal supplyChainAttacksBlockedDefault;

  @Column(name = "supply_chain_attacks_blocked_minimum_value")
  private BigDecimal supplyChainAttacksBlockedMinimum;

  @Column(name = "namespace_attacks_blocked_default_value")
  private BigDecimal namespaceAttacksBlockedDefault;

  @Column(name = "namespace_attacks_blocked_minimum_value")
  private BigDecimal namespaceAttacksBlockedMinimum;

  @Column(name = "safe_components_auto_selected_default_value")
  private BigDecimal safeComponentsAutoSelectedDefault;

  @Column(name = "safe_components_auto_selected_minimum_value")
  private BigDecimal safeComponentsAutoSelectedMinimum;

  @Column(name = "waived_policies_counted")
  private Boolean waivedPoliciesCounted;

  public RoiConfigurationDefaultValues() {
  }

  public RoiConfigurationDefaultValues(
      final CurrencyTypes currency,
      final BigDecimal developerHourlyRateDefault,
      final BigDecimal developerHourlyRateMinimum,
      final Long fixRateHoursDefault,
      final Long fixRateHoursMinimum,
      final BigDecimal securityViolationCriticalDefault,
      final BigDecimal securityViolationCriticalMinimum,
      final Boolean securityViolationCriticalEnabled,
      final BigDecimal securityViolationHighDefault,
      final BigDecimal securityViolationHighMinimum,
      final Boolean securityViolationHighEnabled,
      final BigDecimal securityViolationMediumDefault,
      final BigDecimal securityViolationMediumMinimum,
      final Boolean securityViolationMediumEnabled,
      final BigDecimal securityViolationLowDefault,
      final BigDecimal securityViolationLowMinimum,
      final Boolean securityViolationLowEnabled,
      final BigDecimal supplyChainAttacksBlockedDefault,
      final BigDecimal supplyChainAttacksBlockedMinimum,
      final BigDecimal namespaceAttacksBlockedDefault,
      final BigDecimal namespaceAttacksBlockedMinimum,
      final BigDecimal safeComponentsAutoSelectedDefault,
      final BigDecimal safeComponentsAutoSelectedMinimum,
      final Boolean waivedPoliciesCounted)
  {
    this.currency = currency;
    this.developerHourlyRateDefault = developerHourlyRateDefault;
    this.developerHourlyRateMinimum = developerHourlyRateMinimum;
    this.fixRateHoursDefault = fixRateHoursDefault;
    this.fixRateHoursMinimum = fixRateHoursMinimum;
    this.securityViolationCriticalDefault = securityViolationCriticalDefault;
    this.securityViolationCriticalMinimum = securityViolationCriticalMinimum;
    this.securityViolationCriticalEnabled = securityViolationCriticalEnabled;
    this.securityViolationHighDefault = securityViolationHighDefault;
    this.securityViolationHighMinimum = securityViolationHighMinimum;
    this.securityViolationHighEnabled = securityViolationHighEnabled;
    this.securityViolationMediumDefault = securityViolationMediumDefault;
    this.securityViolationMediumMinimum = securityViolationMediumMinimum;
    this.securityViolationMediumEnabled = securityViolationMediumEnabled;
    this.securityViolationLowDefault = securityViolationLowDefault;
    this.securityViolationLowMinimum = securityViolationLowMinimum;
    this.securityViolationLowEnabled = securityViolationLowEnabled;
    this.supplyChainAttacksBlockedDefault = supplyChainAttacksBlockedDefault;
    this.supplyChainAttacksBlockedMinimum = supplyChainAttacksBlockedMinimum;
    this.namespaceAttacksBlockedDefault = namespaceAttacksBlockedDefault;
    this.namespaceAttacksBlockedMinimum = namespaceAttacksBlockedMinimum;
    this.safeComponentsAutoSelectedDefault = safeComponentsAutoSelectedDefault;
    this.safeComponentsAutoSelectedMinimum = safeComponentsAutoSelectedMinimum;
    this.waivedPoliciesCounted = waivedPoliciesCounted;
  }

  public CurrencyTypes getCurrency() {
    return currency;
  }

  public BigDecimal getDeveloperHourlyRateDefault() {
    return developerHourlyRateDefault;
  }

  public BigDecimal getDeveloperHourlyRateMinimum() {
    return developerHourlyRateMinimum;
  }

  public Long getFixRateHoursDefault() {
    return fixRateHoursDefault;
  }

  public Long getFixRateHoursMinimum() {
    return fixRateHoursMinimum;
  }

  public BigDecimal getSecurityViolationCriticalDefault() {
    return securityViolationCriticalDefault;
  }

  public BigDecimal getSecurityViolationCriticalMinimum() {
    return securityViolationCriticalMinimum;
  }

  public BigDecimal getSecurityViolationHighDefault() {
    return securityViolationHighDefault;
  }

  public BigDecimal getSecurityViolationHighMinimum() {
    return securityViolationHighMinimum;
  }

  public BigDecimal getSecurityViolationMediumDefault() {
    return securityViolationMediumDefault;
  }

  public BigDecimal getSecurityViolationMediumMinimum() {
    return securityViolationMediumMinimum;
  }

  public BigDecimal getSecurityViolationLowEnabled() {
    return securityViolationLowDefault;
  }

  public BigDecimal getSecurityViolationLowMinimum() {
    return securityViolationLowMinimum;
  }

  public BigDecimal getSupplyChainAttacksBlockedDefault() {
    return supplyChainAttacksBlockedDefault;
  }

  public BigDecimal getSupplyChainAttacksBlockedMinimum() {
    return supplyChainAttacksBlockedMinimum;
  }

  public BigDecimal getNamespaceAttacksBlockedDefault() {
    return namespaceAttacksBlockedDefault;
  }

  public BigDecimal getNamespaceAttacksBlockedMinimum() {
    return namespaceAttacksBlockedMinimum;
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

  public void setDeveloperHourlyRateDefault(final BigDecimal developerHourlyRateDefault) {
    this.developerHourlyRateDefault = developerHourlyRateDefault;
  }

  public void setDeveloperHourlyRateMinimum(final BigDecimal developerHourlyRateMinimum) {
    this.developerHourlyRateMinimum = developerHourlyRateMinimum;
  }

  public void setFixRateHoursDefault(final Long fixRateHoursDefault) {
    this.fixRateHoursDefault = fixRateHoursDefault;
  }

  public void setFixRateHoursMinimum(final Long fixRateHoursMinimum) {
    this.fixRateHoursMinimum = fixRateHoursMinimum;
  }

  public void setSecurityViolationCriticalDefault(final BigDecimal securityViolationCriticalDefault) {
    this.securityViolationCriticalDefault = securityViolationCriticalDefault;
  }

  public void setSecurityViolationCriticalMinimum(final BigDecimal securityViolationCriticalMinimum) {
    this.securityViolationCriticalMinimum = securityViolationCriticalMinimum;
  }

  public void setSecurityViolationHighDefault(final BigDecimal securityViolationHighDefault) {
    this.securityViolationHighDefault = securityViolationHighDefault;
  }

  public void setSecurityViolationHighMinimum(final BigDecimal securityViolationHighMinimum) {
    this.securityViolationHighMinimum = securityViolationHighMinimum;
  }

  public void setSecurityViolationMediumDefault(final BigDecimal securityViolationMediumMinimum) {
    this.securityViolationMediumDefault = securityViolationMediumMinimum;
  }

  public void setSecurityViolationMediumMinimum(final BigDecimal securityViolationMediumValue) {
    this.securityViolationMediumMinimum = securityViolationMediumValue;
  }

  public void setSecurityViolationLowDefault(final BigDecimal securityViolationLowEnabled) {
    this.securityViolationLowDefault = securityViolationLowEnabled;
  }

  public void setSecurityViolationLowMinimum(final BigDecimal securityViolationLowValue) {
    this.securityViolationLowMinimum = securityViolationLowValue;
  }

  public void setSupplyChainAttacksBlockedDefault(final BigDecimal supplyChainAttacksBlockedDefault) {
    this.supplyChainAttacksBlockedDefault = supplyChainAttacksBlockedDefault;
  }

  public void setSupplyChainAttacksBlockedMinimum(final BigDecimal supplyChainAttacksBlockedMinimum) {
    this.supplyChainAttacksBlockedMinimum = supplyChainAttacksBlockedMinimum;
  }

  public void setNamespaceAttacksBlockedDefault(final BigDecimal namespaceAttacksBlocked) {
    this.namespaceAttacksBlockedDefault = namespaceAttacksBlocked;
  }

  public void setNamespaceAttacksBlockedMinimum(final BigDecimal namespaceAttacksBlockedMinimum) {
    this.namespaceAttacksBlockedMinimum = namespaceAttacksBlockedMinimum;
  }

  public void setSafeComponentsAutoSelectedDefault(final BigDecimal safeComponentsAutoSelectedDefault) {
    this.safeComponentsAutoSelectedDefault = safeComponentsAutoSelectedDefault;
  }

  public void setSafeComponentsAutoSelectedMinimum(final BigDecimal safeComponentsAutoSelectedMinimum) {
    this.safeComponentsAutoSelectedMinimum = safeComponentsAutoSelectedMinimum;
  }

  public Boolean isSecurityViolationCriticalEnabled() {
    return securityViolationCriticalEnabled;
  }

  public Boolean isSecurityViolationHighEnabled() {
    return securityViolationHighEnabled;
  }

  public Boolean isSecurityViolationMediumEnabled() {
    return securityViolationMediumEnabled;
  }

  public BigDecimal getSecurityViolationLowDefault() {
    return securityViolationLowDefault;
  }

  public Boolean isSecurityViolationLowEnabled() {
    return securityViolationLowEnabled;
  }

  public void setSecurityViolationCriticalEnabled(final Boolean securityViolationCriticalEnabled) {
    this.securityViolationCriticalEnabled = securityViolationCriticalEnabled;
  }

  public void setSecurityViolationHighEnabled(final Boolean securityViolationHighEnabled) {
    this.securityViolationHighEnabled = securityViolationHighEnabled;
  }

  public void setSecurityViolationMediumEnabled(final Boolean securityViolationMediumEnabled) {
    this.securityViolationMediumEnabled = securityViolationMediumEnabled;
  }

  public void setSecurityViolationLowEnabled(final Boolean securityViolationLowEnabled) {
    this.securityViolationLowEnabled = securityViolationLowEnabled;
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
}
