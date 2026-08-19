/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.successmetrics;

import java.util.Date;
import java.util.Optional;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.stream.Stream;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

import com.sonatype.insight.brain.model.EnumIntegerTable;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.utils.ThreatLevel;
import com.sonatype.insight.model.HasStringId;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.LICENSE;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.OTHER;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.QUALITY;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.SECURITY;
import static com.sonatype.insight.brain.utils.ThreatLevel.CRITICAL;
import static com.sonatype.insight.brain.utils.ThreatLevel.LOW;
import static com.sonatype.insight.brain.utils.ThreatLevel.MODERATE;
import static com.sonatype.insight.brain.utils.ThreatLevel.SEVERE;

/**
 * @since 1.33
 */
@Entity
@jakarta.persistence.Table(name = "policy_violation_aggregation")
public class PolicyViolationAggregation
    implements HasStringId
{
  @Id
  @Column(name = "policy_violation_aggregation_id")
  private String id;

  @Column(name = "application_id")
  private String applicationId;

  @Column(name = "time_period_start")
  private Date timePeriodStart;

  @Column(name = "time_period_end")
  private Date timePeriodEnd;

  /*
   * Average numbers of milliseconds that it took to resolve violations at each threat level that were resolved within
   * this time period. Null indicates that there were no resolved violations within this period.
   */

  @Column(name = "mttr_low_threat")
  private Long mttrLowThreat;

  @Column(name = "mttr_moderate_threat")
  private Long mttrModerateThreat;

  @Column(name = "mttr_severe_threat")
  private Long mttrSevereThreat;

  @Column(name = "mttr_critical_threat")
  private Long mttrCriticalThreat;

  @Column(name = "fixed_count_security_low_threat")
  private int fixedCountSecurityLowThreat;

  @Column(name = "fixed_count_security_moderate_threat")
  private int fixedCountSecurityModerateThreat;

  @Column(name = "fixed_count_security_severe_threat")
  private int fixedCountSecuritySevereThreat;

  @Column(name = "fixed_count_security_critical_threat")
  private int fixedCountSecurityCriticalThreat;

  @Column(name = "fixed_count_license_low_threat")
  private int fixedCountLicenseLowThreat;

  @Column(name = "fixed_count_license_moderate_threat")
  private int fixedCountLicenseModerateThreat;

  @Column(name = "fixed_count_license_severe_threat")
  private int fixedCountLicenseSevereThreat;

  @Column(name = "fixed_count_license_critical_threat")
  private int fixedCountLicenseCriticalThreat;

  @Column(name = "fixed_count_quality_low_threat")
  private int fixedCountQualityLowThreat;

  @Column(name = "fixed_count_quality_moderate_threat")
  private int fixedCountQualityModerateThreat;

  @Column(name = "fixed_count_quality_severe_threat")
  private int fixedCountQualitySevereThreat;

  @Column(name = "fixed_count_quality_critical_threat")
  private int fixedCountQualityCriticalThreat;

  @Column(name = "fixed_count_other_low_threat")
  private int fixedCountOtherLowThreat;

  @Column(name = "fixed_count_other_moderate_threat")
  private int fixedCountOtherModerateThreat;

  @Column(name = "fixed_count_other_severe_threat")
  private int fixedCountOtherSevereThreat;

  @Column(name = "fixed_count_other_critical_threat")
  private int fixedCountOtherCriticalThreat;

  @Column(name = "waived_count_security_low_threat")
  private int waivedCountSecurityLowThreat;

  @Column(name = "waived_count_security_moderate_threat")
  private int waivedCountSecurityModerateThreat;

  @Column(name = "waived_count_security_severe_threat")
  private int waivedCountSecuritySevereThreat;

  @Column(name = "waived_count_security_critical_threat")
  private int waivedCountSecurityCriticalThreat;

  @Column(name = "waived_count_license_low_threat")
  private int waivedCountLicenseLowThreat;

  @Column(name = "waived_count_license_moderate_threat")
  private int waivedCountLicenseModerateThreat;

  @Column(name = "waived_count_license_severe_threat")
  private int waivedCountLicenseSevereThreat;

  @Column(name = "waived_count_license_critical_threat")
  private int waivedCountLicenseCriticalThreat;

  @Column(name = "waived_count_quality_low_threat")
  private int waivedCountQualityLowThreat;

  @Column(name = "waived_count_quality_moderate_threat")
  private int waivedCountQualityModerateThreat;

  @Column(name = "waived_count_quality_severe_threat")
  private int waivedCountQualitySevereThreat;

  @Column(name = "waived_count_quality_critical_threat")
  private int waivedCountQualityCriticalThreat;

  @Column(name = "waived_count_other_low_threat")
  private int waivedCountOtherLowThreat;

  @Column(name = "waived_count_other_moderate_threat")
  private int waivedCountOtherModerateThreat;

  @Column(name = "waived_count_other_severe_threat")
  private int waivedCountOtherSevereThreat;

  @Column(name = "waived_count_other_critical_threat")
  private int waivedCountOtherCriticalThreat;

  @Column(name = "discovered_count_security_low_threat")
  private int discoveredCountSecurityLowThreat;

  @Column(name = "discovered_count_security_moderate_threat")
  private int discoveredCountSecurityModerateThreat;

  @Column(name = "discovered_count_security_severe_threat")
  private int discoveredCountSecuritySevereThreat;

  @Column(name = "discovered_count_security_critical_threat")
  private int discoveredCountSecurityCriticalThreat;

  @Column(name = "discovered_count_license_low_threat")
  private int discoveredCountLicenseLowThreat;

  @Column(name = "discovered_count_license_moderate_threat")
  private int discoveredCountLicenseModerateThreat;

  @Column(name = "discovered_count_license_severe_threat")
  private int discoveredCountLicenseSevereThreat;

  @Column(name = "discovered_count_license_critical_threat")
  private int discoveredCountLicenseCriticalThreat;

  @Column(name = "discovered_count_quality_low_threat")
  private int discoveredCountQualityLowThreat;

  @Column(name = "discovered_count_quality_moderate_threat")
  private int discoveredCountQualityModerateThreat;

  @Column(name = "discovered_count_quality_severe_threat")
  private int discoveredCountQualitySevereThreat;

  @Column(name = "discovered_count_quality_critical_threat")
  private int discoveredCountQualityCriticalThreat;

  @Column(name = "discovered_count_other_low_threat")
  private int discoveredCountOtherLowThreat;

  @Column(name = "discovered_count_other_moderate_threat")
  private int discoveredCountOtherModerateThreat;

  @Column(name = "discovered_count_other_severe_threat")
  private int discoveredCountOtherSevereThreat;

  @Column(name = "discovered_count_other_critical_threat")
  private int discoveredCountOtherCriticalThreat;

  @Column(name = "open_count_security_low_threat")
  private int openCountSecurityLowThreat;

  @Column(name = "open_count_security_moderate_threat")
  private int openCountSecurityModerateThreat;

  @Column(name = "open_count_security_severe_threat")
  private int openCountSecuritySevereThreat;

  @Column(name = "open_count_security_critical_threat")
  private int openCountSecurityCriticalThreat;

  @Column(name = "open_count_license_low_threat")
  private int openCountLicenseLowThreat;

  @Column(name = "open_count_license_moderate_threat")
  private int openCountLicenseModerateThreat;

  @Column(name = "open_count_license_severe_threat")
  private int openCountLicenseSevereThreat;

  @Column(name = "open_count_license_critical_threat")
  private int openCountLicenseCriticalThreat;

  @Column(name = "open_count_quality_low_threat")
  private int openCountQualityLowThreat;

  @Column(name = "open_count_quality_moderate_threat")
  private int openCountQualityModerateThreat;

  @Column(name = "open_count_quality_severe_threat")
  private int openCountQualitySevereThreat;

  @Column(name = "open_count_quality_critical_threat")
  private int openCountQualityCriticalThreat;

  @Column(name = "open_count_other_low_threat")
  private int openCountOtherLowThreat;

  @Column(name = "open_count_other_moderate_threat")
  private int openCountOtherModerateThreat;

  @Column(name = "open_count_other_severe_threat")
  private int openCountOtherSevereThreat;

  @Column(name = "open_count_other_critical_threat")
  private int openCountOtherCriticalThreat;

  @Column(name = "evaluation_count")
  private int evaluationCount;

  @Column(name = "time_period")
  @Enumerated(EnumType.STRING)
  private TimePeriod timePeriod;

  // A few maps to help deal with the large number of count fields.
  @Transient
  private Table<PolicyThreatCategory, ThreatLevel, IntSupplier> discoveredGettersMap = HashBasedTable.create();

  @Transient
  private Table<PolicyThreatCategory, ThreatLevel, IntConsumer> discoveredSettersMap = HashBasedTable.create();

  @Transient
  private Table<PolicyThreatCategory, ThreatLevel, IntSupplier> fixedGettersMap = HashBasedTable.create();

  @Transient
  private Table<PolicyThreatCategory, ThreatLevel, IntConsumer> fixedSettersMap = HashBasedTable.create();

  @Transient
  private Table<PolicyThreatCategory, ThreatLevel, IntSupplier> waivedGettersMap = HashBasedTable.create();

  @Transient
  private Table<PolicyThreatCategory, ThreatLevel, IntConsumer> waivedSettersMap = HashBasedTable.create();

  @Transient
  private Table<PolicyThreatCategory, ThreatLevel, IntSupplier> openGettersMap = HashBasedTable.create();

  @Transient
  private Table<PolicyThreatCategory, ThreatLevel, IntConsumer> openSettersMap = HashBasedTable.create();

  {
    discoveredGettersMap.put(SECURITY, LOW, () -> discoveredCountSecurityLowThreat);
    discoveredGettersMap.put(SECURITY, MODERATE, () -> discoveredCountSecurityModerateThreat);
    discoveredGettersMap.put(SECURITY, SEVERE, () -> discoveredCountSecuritySevereThreat);
    discoveredGettersMap.put(SECURITY, CRITICAL, () -> discoveredCountSecurityCriticalThreat);
    discoveredGettersMap.put(LICENSE, LOW, () -> discoveredCountLicenseLowThreat);
    discoveredGettersMap.put(LICENSE, MODERATE, () -> discoveredCountLicenseModerateThreat);
    discoveredGettersMap.put(LICENSE, SEVERE, () -> discoveredCountLicenseSevereThreat);
    discoveredGettersMap.put(LICENSE, CRITICAL, () -> discoveredCountLicenseCriticalThreat);
    discoveredGettersMap.put(QUALITY, LOW, () -> discoveredCountQualityLowThreat);
    discoveredGettersMap.put(QUALITY, MODERATE, () -> discoveredCountQualityModerateThreat);
    discoveredGettersMap.put(QUALITY, SEVERE, () -> discoveredCountQualitySevereThreat);
    discoveredGettersMap.put(QUALITY, CRITICAL, () -> discoveredCountQualityCriticalThreat);
    discoveredGettersMap.put(OTHER, LOW, () -> discoveredCountOtherLowThreat);
    discoveredGettersMap.put(OTHER, MODERATE, () -> discoveredCountOtherModerateThreat);
    discoveredGettersMap.put(OTHER, SEVERE, () -> discoveredCountOtherSevereThreat);
    discoveredGettersMap.put(OTHER, CRITICAL, () -> discoveredCountOtherCriticalThreat);

    discoveredSettersMap.put(SECURITY, LOW, count -> discoveredCountSecurityLowThreat = count);
    discoveredSettersMap.put(SECURITY, MODERATE, count -> discoveredCountSecurityModerateThreat = count);
    discoveredSettersMap.put(SECURITY, SEVERE, count -> discoveredCountSecuritySevereThreat = count);
    discoveredSettersMap.put(SECURITY, CRITICAL, count -> discoveredCountSecurityCriticalThreat = count);
    discoveredSettersMap.put(LICENSE, LOW, count -> discoveredCountLicenseLowThreat = count);
    discoveredSettersMap.put(LICENSE, MODERATE, count -> discoveredCountLicenseModerateThreat = count);
    discoveredSettersMap.put(LICENSE, SEVERE, count -> discoveredCountLicenseSevereThreat = count);
    discoveredSettersMap.put(LICENSE, CRITICAL, count -> discoveredCountLicenseCriticalThreat = count);
    discoveredSettersMap.put(QUALITY, LOW, count -> discoveredCountQualityLowThreat = count);
    discoveredSettersMap.put(QUALITY, MODERATE, count -> discoveredCountQualityModerateThreat = count);
    discoveredSettersMap.put(QUALITY, SEVERE, count -> discoveredCountQualitySevereThreat = count);
    discoveredSettersMap.put(QUALITY, CRITICAL, count -> discoveredCountQualityCriticalThreat = count);
    discoveredSettersMap.put(OTHER, LOW, count -> discoveredCountOtherLowThreat = count);
    discoveredSettersMap.put(OTHER, MODERATE, count -> discoveredCountOtherModerateThreat = count);
    discoveredSettersMap.put(OTHER, SEVERE, count -> discoveredCountOtherSevereThreat = count);
    discoveredSettersMap.put(OTHER, CRITICAL, count -> discoveredCountOtherCriticalThreat = count);

    fixedGettersMap.put(SECURITY, LOW, () -> fixedCountSecurityLowThreat);
    fixedGettersMap.put(SECURITY, MODERATE, () -> fixedCountSecurityModerateThreat);
    fixedGettersMap.put(SECURITY, SEVERE, () -> fixedCountSecuritySevereThreat);
    fixedGettersMap.put(SECURITY, CRITICAL, () -> fixedCountSecurityCriticalThreat);
    fixedGettersMap.put(LICENSE, LOW, () -> fixedCountLicenseLowThreat);
    fixedGettersMap.put(LICENSE, MODERATE, () -> fixedCountLicenseModerateThreat);
    fixedGettersMap.put(LICENSE, SEVERE, () -> fixedCountLicenseSevereThreat);
    fixedGettersMap.put(LICENSE, CRITICAL, () -> fixedCountLicenseCriticalThreat);
    fixedGettersMap.put(QUALITY, LOW, () -> fixedCountQualityLowThreat);
    fixedGettersMap.put(QUALITY, MODERATE, () -> fixedCountQualityModerateThreat);
    fixedGettersMap.put(QUALITY, SEVERE, () -> fixedCountQualitySevereThreat);
    fixedGettersMap.put(QUALITY, CRITICAL, () -> fixedCountQualityCriticalThreat);
    fixedGettersMap.put(OTHER, LOW, () -> fixedCountOtherLowThreat);
    fixedGettersMap.put(OTHER, MODERATE, () -> fixedCountOtherModerateThreat);
    fixedGettersMap.put(OTHER, SEVERE, () -> fixedCountOtherSevereThreat);
    fixedGettersMap.put(OTHER, CRITICAL, () -> fixedCountOtherCriticalThreat);

    fixedSettersMap.put(SECURITY, LOW, count -> fixedCountSecurityLowThreat = count);
    fixedSettersMap.put(SECURITY, MODERATE, count -> fixedCountSecurityModerateThreat = count);
    fixedSettersMap.put(SECURITY, SEVERE, count -> fixedCountSecuritySevereThreat = count);
    fixedSettersMap.put(SECURITY, CRITICAL, count -> fixedCountSecurityCriticalThreat = count);
    fixedSettersMap.put(LICENSE, LOW, count -> fixedCountLicenseLowThreat = count);
    fixedSettersMap.put(LICENSE, MODERATE, count -> fixedCountLicenseModerateThreat = count);
    fixedSettersMap.put(LICENSE, SEVERE, count -> fixedCountLicenseSevereThreat = count);
    fixedSettersMap.put(LICENSE, CRITICAL, count -> fixedCountLicenseCriticalThreat = count);
    fixedSettersMap.put(QUALITY, LOW, count -> fixedCountQualityLowThreat = count);
    fixedSettersMap.put(QUALITY, MODERATE, count -> fixedCountQualityModerateThreat = count);
    fixedSettersMap.put(QUALITY, SEVERE, count -> fixedCountQualitySevereThreat = count);
    fixedSettersMap.put(QUALITY, CRITICAL, count -> fixedCountQualityCriticalThreat = count);
    fixedSettersMap.put(OTHER, LOW, count -> fixedCountOtherLowThreat = count);
    fixedSettersMap.put(OTHER, MODERATE, count -> fixedCountOtherModerateThreat = count);
    fixedSettersMap.put(OTHER, SEVERE, count -> fixedCountOtherSevereThreat = count);
    fixedSettersMap.put(OTHER, CRITICAL, count -> fixedCountOtherCriticalThreat = count);

    waivedGettersMap.put(SECURITY, LOW, () -> waivedCountSecurityLowThreat);
    waivedGettersMap.put(SECURITY, MODERATE, () -> waivedCountSecurityModerateThreat);
    waivedGettersMap.put(SECURITY, SEVERE, () -> waivedCountSecuritySevereThreat);
    waivedGettersMap.put(SECURITY, CRITICAL, () -> waivedCountSecurityCriticalThreat);
    waivedGettersMap.put(LICENSE, LOW, () -> waivedCountLicenseLowThreat);
    waivedGettersMap.put(LICENSE, MODERATE, () -> waivedCountLicenseModerateThreat);
    waivedGettersMap.put(LICENSE, SEVERE, () -> waivedCountLicenseSevereThreat);
    waivedGettersMap.put(LICENSE, CRITICAL, () -> waivedCountLicenseCriticalThreat);
    waivedGettersMap.put(QUALITY, LOW, () -> waivedCountQualityLowThreat);
    waivedGettersMap.put(QUALITY, MODERATE, () -> waivedCountQualityModerateThreat);
    waivedGettersMap.put(QUALITY, SEVERE, () -> waivedCountQualitySevereThreat);
    waivedGettersMap.put(QUALITY, CRITICAL, () -> waivedCountQualityCriticalThreat);
    waivedGettersMap.put(OTHER, LOW, () -> waivedCountOtherLowThreat);
    waivedGettersMap.put(OTHER, MODERATE, () -> waivedCountOtherModerateThreat);
    waivedGettersMap.put(OTHER, SEVERE, () -> waivedCountOtherSevereThreat);
    waivedGettersMap.put(OTHER, CRITICAL, () -> waivedCountOtherCriticalThreat);

    waivedSettersMap.put(SECURITY, LOW, count -> waivedCountSecurityLowThreat = count);
    waivedSettersMap.put(SECURITY, MODERATE, count -> waivedCountSecurityModerateThreat = count);
    waivedSettersMap.put(SECURITY, SEVERE, count -> waivedCountSecuritySevereThreat = count);
    waivedSettersMap.put(SECURITY, CRITICAL, count -> waivedCountSecurityCriticalThreat = count);
    waivedSettersMap.put(LICENSE, LOW, count -> waivedCountLicenseLowThreat = count);
    waivedSettersMap.put(LICENSE, MODERATE, count -> waivedCountLicenseModerateThreat = count);
    waivedSettersMap.put(LICENSE, SEVERE, count -> waivedCountLicenseSevereThreat = count);
    waivedSettersMap.put(LICENSE, CRITICAL, count -> waivedCountLicenseCriticalThreat = count);
    waivedSettersMap.put(QUALITY, LOW, count -> waivedCountQualityLowThreat = count);
    waivedSettersMap.put(QUALITY, MODERATE, count -> waivedCountQualityModerateThreat = count);
    waivedSettersMap.put(QUALITY, SEVERE, count -> waivedCountQualitySevereThreat = count);
    waivedSettersMap.put(QUALITY, CRITICAL, count -> waivedCountQualityCriticalThreat = count);
    waivedSettersMap.put(OTHER, LOW, count -> waivedCountOtherLowThreat = count);
    waivedSettersMap.put(OTHER, MODERATE, count -> waivedCountOtherModerateThreat = count);
    waivedSettersMap.put(OTHER, SEVERE, count -> waivedCountOtherSevereThreat = count);
    waivedSettersMap.put(OTHER, CRITICAL, count -> waivedCountOtherCriticalThreat = count);

    openGettersMap.put(SECURITY, LOW, () -> openCountSecurityLowThreat);
    openGettersMap.put(SECURITY, MODERATE, () -> openCountSecurityModerateThreat);
    openGettersMap.put(SECURITY, SEVERE, () -> openCountSecuritySevereThreat);
    openGettersMap.put(SECURITY, CRITICAL, () -> openCountSecurityCriticalThreat);
    openGettersMap.put(LICENSE, LOW, () -> openCountLicenseLowThreat);
    openGettersMap.put(LICENSE, MODERATE, () -> openCountLicenseModerateThreat);
    openGettersMap.put(LICENSE, SEVERE, () -> openCountLicenseSevereThreat);
    openGettersMap.put(LICENSE, CRITICAL, () -> openCountLicenseCriticalThreat);
    openGettersMap.put(QUALITY, LOW, () -> openCountQualityLowThreat);
    openGettersMap.put(QUALITY, MODERATE, () -> openCountQualityModerateThreat);
    openGettersMap.put(QUALITY, SEVERE, () -> openCountQualitySevereThreat);
    openGettersMap.put(QUALITY, CRITICAL, () -> openCountQualityCriticalThreat);
    openGettersMap.put(OTHER, LOW, () -> openCountOtherLowThreat);
    openGettersMap.put(OTHER, MODERATE, () -> openCountOtherModerateThreat);
    openGettersMap.put(OTHER, SEVERE, () -> openCountOtherSevereThreat);
    openGettersMap.put(OTHER, CRITICAL, () -> openCountOtherCriticalThreat);

    openSettersMap.put(SECURITY, LOW, count -> openCountSecurityLowThreat = count);
    openSettersMap.put(SECURITY, MODERATE, count -> openCountSecurityModerateThreat = count);
    openSettersMap.put(SECURITY, SEVERE, count -> openCountSecuritySevereThreat = count);
    openSettersMap.put(SECURITY, CRITICAL, count -> openCountSecurityCriticalThreat = count);
    openSettersMap.put(LICENSE, LOW, count -> openCountLicenseLowThreat = count);
    openSettersMap.put(LICENSE, MODERATE, count -> openCountLicenseModerateThreat = count);
    openSettersMap.put(LICENSE, SEVERE, count -> openCountLicenseSevereThreat = count);
    openSettersMap.put(LICENSE, CRITICAL, count -> openCountLicenseCriticalThreat = count);
    openSettersMap.put(QUALITY, LOW, count -> openCountQualityLowThreat = count);
    openSettersMap.put(QUALITY, MODERATE, count -> openCountQualityModerateThreat = count);
    openSettersMap.put(QUALITY, SEVERE, count -> openCountQualitySevereThreat = count);
    openSettersMap.put(QUALITY, CRITICAL, count -> openCountQualityCriticalThreat = count);
    openSettersMap.put(OTHER, LOW, count -> openCountOtherLowThreat = count);
    openSettersMap.put(OTHER, MODERATE, count -> openCountOtherModerateThreat = count);
    openSettersMap.put(OTHER, SEVERE, count -> openCountOtherSevereThreat = count);
    openSettersMap.put(OTHER, CRITICAL, count -> openCountOtherCriticalThreat = count);
  }

  public PolicyViolationAggregation() {
  }

  public PolicyViolationAggregation(
      String applicationId,
      Date timePeriodStart,
      Date timePeriodEnd,
      TimePeriod timePeriod,
      DescriptiveStatistics mttrLowThreatStats,
      DescriptiveStatistics mttrModerateThreatStats,
      DescriptiveStatistics mttrSevereThreatStats,
      DescriptiveStatistics mttrCriticalThreatStats,
      Table<PolicyThreatCategory, ThreatLevel, Integer> discoveredCounts,
      Table<PolicyThreatCategory, ThreatLevel, Integer> fixedCounts,
      Table<PolicyThreatCategory, ThreatLevel, Integer> waivedCounts,
      Table<PolicyThreatCategory, ThreatLevel, Integer> openCounts,
      Integer evaluationCount)
  {
    this.applicationId = applicationId;
    this.timePeriodStart = timePeriodStart;
    this.timePeriodEnd = timePeriodEnd;
    this.timePeriod = timePeriod;

    this.mttrLowThreat = mttrLowThreatStats.getN() != 0 ? (long) mttrLowThreatStats.getMean() : null;
    this.mttrModerateThreat = mttrModerateThreatStats.getN() != 0 ? (long) mttrModerateThreatStats.getMean() : null;
    this.mttrSevereThreat = mttrSevereThreatStats.getN() != 0 ? (long) mttrSevereThreatStats.getMean() : null;
    this.mttrCriticalThreat = mttrCriticalThreatStats.getN() != 0 ? (long) mttrCriticalThreatStats.getMean() : null;

    this.setCounts(discoveredCounts, discoveredSettersMap);
    this.setCounts(fixedCounts, fixedSettersMap);
    this.setCounts(waivedCounts, waivedSettersMap);
    this.setCounts(openCounts, openSettersMap);

    this.evaluationCount = evaluationCount;
  }

  private void setCounts(
      Table<PolicyThreatCategory, ThreatLevel, Integer> counts,
      Table<PolicyThreatCategory, ThreatLevel, IntConsumer> settersMap)
  {
    for (PolicyThreatCategory category : PolicyThreatCategory.values()) {
      for (ThreatLevel threatLevel : ThreatLevel.values()) {
        settersMap.get(category, threatLevel).accept(Optional.ofNullable(counts.get(category, threatLevel)).orElse(0));
      }
    }
  }

  public Table<PolicyThreatCategory, ThreatLevel, Integer> getDiscoveredAsTable() {
    return countsToTable(discoveredGettersMap);
  }

  public Table<PolicyThreatCategory, ThreatLevel, Integer> getFixedAsTable() {
    return countsToTable(fixedGettersMap);
  }

  public Table<PolicyThreatCategory, ThreatLevel, Integer> getWaivedAsTable() {
    return countsToTable(waivedGettersMap);
  }

  public Table<PolicyThreatCategory, ThreatLevel, Integer> getOpenAsTable() {
    return countsToTable(openGettersMap);
  }

  private Table<PolicyThreatCategory, ThreatLevel, Integer> countsToTable(
      Table<PolicyThreatCategory, ThreatLevel, IntSupplier> getterMap)
  {
    Table<PolicyThreatCategory, ThreatLevel, Integer> result =
        new EnumIntegerTable<>(PolicyThreatCategory.class, ThreatLevel.class);
    for (PolicyThreatCategory category : PolicyThreatCategory.values()) {
      for (ThreatLevel level : ThreatLevel.values()) {
        result.put(category, level, getterMap.get(category, level).getAsInt());
      }
    }
    return result;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(String applicationId) {
    this.applicationId = applicationId;
  }

  public Date getTimePeriodStart() {
    return timePeriodStart;
  }

  public void setTimePeriodStart(Date timePeriodStart) {
    this.timePeriodStart = timePeriodStart;
  }

  public Date getTimePeriodEnd() {
    return timePeriodEnd;
  }

  public Long getMttrLowThreat() {
    return mttrLowThreat;
  }

  public Long getMttrModerateThreat() {
    return mttrModerateThreat;
  }

  public Long getMttrSevereThreat() {
    return mttrSevereThreat;
  }

  public Long getMttrCriticalThreat() {
    return mttrCriticalThreat;
  }

  public int getResolvedCountLowThreat() {
    return getResolvedForThreatLevel(LOW);
  }

  public int getResolvedCountModerateThreat() {
    return getResolvedForThreatLevel(MODERATE);
  }

  public int getResolvedCountSevereThreat() {
    return getResolvedForThreatLevel(SEVERE);
  }

  public int getResolvedCountCriticalThreat() {
    return getResolvedForThreatLevel(CRITICAL);
  }

  private int getResolvedForThreatLevel(ThreatLevel level) {
    return Stream
        .concat(fixedGettersMap.column(level).values().stream(), waivedGettersMap.column(level).values().stream())
        .mapToInt(getter -> getter.getAsInt())
        .sum();
  }

  public int getEvaluationCount() {
    return evaluationCount;
  }

  public void setEvaluationCount(int evaluationCount) {
    this.evaluationCount = evaluationCount;
  }

  public int getDiscoveredCount(PolicyThreatCategory category, ThreatLevel level) {
    return discoveredGettersMap.get(category, level).getAsInt();
  }

  public void setDiscoveredCount(PolicyThreatCategory category, ThreatLevel level, int count) {
    discoveredSettersMap.get(category, level).accept(count);
  }

  public int getFixedCount(PolicyThreatCategory category, ThreatLevel level) {
    return fixedGettersMap.get(category, level).getAsInt();
  }

  public int getWaivedCount(PolicyThreatCategory category, ThreatLevel level) {
    return waivedGettersMap.get(category, level).getAsInt();
  }

  public int getOpenCount(PolicyThreatCategory category, ThreatLevel level) {
    return openGettersMap.get(category, level).getAsInt();
  }

  public TimePeriod getTimePeriod() {
    return timePeriod;
  }

  public void setTimePeriod(final TimePeriod timePeriod) {
    this.timePeriod = timePeriod;
  }
}
