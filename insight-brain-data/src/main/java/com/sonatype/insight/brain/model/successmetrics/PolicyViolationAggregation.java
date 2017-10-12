/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.successmetrics;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.utils.ThreatLevel;
import com.sonatype.insight.model.HasStringId;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * @since 1.33
 */
@Entity
@Table(name = "policy_violation_aggregation")
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

  @Column(name = "resolved_count_low_threat")
  private int resolvedCountLowThreat;

  @Column(name = "resolved_count_moderate_threat")
  private int resolvedCountModerateThreat;

  @Column(name = "resolved_count_severe_threat")
  private int resolvedCountSevereThreat;

  @Column(name = "resolved_count_critical_threat")
  private int resolvedCountCriticalThreat;

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

  @Column(name = "evaluation_count")
  private int evaluationCount;

  public PolicyViolationAggregation() {
  }

  public PolicyViolationAggregation(String applicationId,
                                    Date timePeriodStart,
                                    DescriptiveStatistics mttrLowThreatStats,
                                    DescriptiveStatistics mttrModerateThreatStats,
                                    DescriptiveStatistics mttrSevereThreatStats,
                                    DescriptiveStatistics mttrCriticalThreatStats)
  {
    this(applicationId, timePeriodStart, null, mttrLowThreatStats, mttrModerateThreatStats, mttrSevereThreatStats,
        mttrCriticalThreatStats, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
  }

  public PolicyViolationAggregation(String applicationId,
                                    Date timePeriodStart,
                                    Date timePeriodEnd,
                                    DescriptiveStatistics mttrLowThreatStats,
                                    DescriptiveStatistics mttrModerateThreatStats,
                                    DescriptiveStatistics mttrSevereThreatStats,
                                    DescriptiveStatistics mttrCriticalThreatStats,
                                    int discoveredCountSecurityLowThreat,
                                    int discoveredCountSecurityModerateThreat,
                                    int discoveredCountSecuritySevereThreat,
                                    int discoveredCountSecurityCriticalThreat,
                                    int discoveredCountLicenseLowThreat,
                                    int discoveredCountLicenseModerateThreat,
                                    int discoveredCountLicenseSevereThreat,
                                    int discoveredCountLicenseCriticalThreat,
                                    int discoveredCountQualityLowThreat,
                                    int discoveredCountQualityModerateThreat,
                                    int discoveredCountQualitySevereThreat,
                                    int discoveredCountQualityCriticalThreat,
                                    int discoveredCountOtherLowThreat,
                                    int discoveredCountOtherModerateThreat,
                                    int discoveredCountOtherSevereThreat,
                                    int discoveredCountOtherCriticalThreat,
                                    int evaluationCount)
  {
    this.applicationId = applicationId;
    this.timePeriodStart = timePeriodStart;
    this.timePeriodEnd = timePeriodEnd;

    this.mttrLowThreat = mttrLowThreatStats.getN() != 0 ? (long) mttrLowThreatStats.getMean() : null;
    this.mttrModerateThreat = mttrModerateThreatStats.getN() != 0 ? (long) mttrModerateThreatStats.getMean() : null;
    this.mttrSevereThreat = mttrSevereThreatStats.getN() != 0 ? (long) mttrSevereThreatStats.getMean() : null;
    this.mttrCriticalThreat = mttrCriticalThreatStats.getN() != 0 ? (long) mttrCriticalThreatStats.getMean() : null;

    this.resolvedCountLowThreat = (int) mttrLowThreatStats.getN();
    this.resolvedCountModerateThreat = (int) mttrModerateThreatStats.getN();
    this.resolvedCountSevereThreat = (int) mttrSevereThreatStats.getN();
    this.resolvedCountCriticalThreat = (int) mttrCriticalThreatStats.getN();

    this.discoveredCountSecurityLowThreat = discoveredCountSecurityLowThreat;
    this.discoveredCountSecurityModerateThreat = discoveredCountSecurityModerateThreat;
    this.discoveredCountSecuritySevereThreat = discoveredCountSecuritySevereThreat;
    this.discoveredCountSecurityCriticalThreat = discoveredCountSecurityCriticalThreat;
    this.discoveredCountLicenseLowThreat = discoveredCountLicenseLowThreat;
    this.discoveredCountLicenseModerateThreat = discoveredCountLicenseModerateThreat;
    this.discoveredCountLicenseSevereThreat = discoveredCountLicenseSevereThreat;
    this.discoveredCountLicenseCriticalThreat = discoveredCountLicenseCriticalThreat;
    this.discoveredCountQualityLowThreat = discoveredCountQualityLowThreat;
    this.discoveredCountQualityModerateThreat = discoveredCountQualityModerateThreat;
    this.discoveredCountQualitySevereThreat = discoveredCountQualitySevereThreat;
    this.discoveredCountQualityCriticalThreat = discoveredCountQualityCriticalThreat;
    this.discoveredCountOtherLowThreat = discoveredCountOtherLowThreat;
    this.discoveredCountOtherModerateThreat = discoveredCountOtherModerateThreat;
    this.discoveredCountOtherSevereThreat = discoveredCountOtherSevereThreat;
    this.discoveredCountOtherCriticalThreat = discoveredCountOtherCriticalThreat;

    this.evaluationCount = evaluationCount;
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
    return resolvedCountLowThreat;
  }

  public int getResolvedCountModerateThreat() {
    return resolvedCountModerateThreat;
  }

  public int getResolvedCountSevereThreat() {
    return resolvedCountSevereThreat;
  }

  public int getResolvedCountCriticalThreat() {
    return resolvedCountCriticalThreat;
  }

  public void setDiscoveredCount(PolicyThreatCategory threatCategory, ThreatLevel threatLevel, int count) {
    switch (threatCategory) {
      case SECURITY:
        setDiscoveredCountSecurity(threatLevel, count);
        break;
      case LICENSE:
        setDiscoveredCountLicense(threatLevel, count);
        break;
      case QUALITY:
        setDiscoveredCountQuality(threatLevel, count);
        break;
      case OTHER:
        setDiscoveredCountOther(threatLevel, count);
        break;
      default:
        throw new IllegalArgumentException("Unsupported Threat Category: " + threatCategory);
    }
  }

  public void setDiscoveredCountSecurity(ThreatLevel threatLevel, int count) {
    switch (threatLevel) {
      case LOW:
        discoveredCountSecurityLowThreat = count;
        break;
      case MODERATE:
        discoveredCountSecurityModerateThreat = count;
        break;
      case SEVERE:
        discoveredCountSecuritySevereThreat = count;
        break;
      case CRITICAL:
        discoveredCountSecurityCriticalThreat = count;
        break;
      default:
        throw new IllegalArgumentException("Unsupported Threat Level: " + threatLevel);
    }
  }

  public void setDiscoveredCountLicense(ThreatLevel threatLevel, int count) {
    switch (threatLevel) {
      case LOW:
        discoveredCountLicenseLowThreat = count;
        break;
      case MODERATE:
        discoveredCountLicenseModerateThreat = count;
        break;
      case SEVERE:
        discoveredCountLicenseSevereThreat = count;
        break;
      case CRITICAL:
        discoveredCountLicenseCriticalThreat = count;
        break;
      default:
        throw new IllegalArgumentException("Unsupported Threat Level: " + threatLevel);
    }
  }

  public void setDiscoveredCountQuality(ThreatLevel threatLevel, int count) {
    switch (threatLevel) {
      case LOW:
        discoveredCountQualityLowThreat = count;
        break;
      case MODERATE:
        discoveredCountQualityModerateThreat = count;
        break;
      case SEVERE:
        discoveredCountQualitySevereThreat = count;
        break;
      case CRITICAL:
        discoveredCountQualityCriticalThreat = count;
        break;
      default:
        throw new IllegalArgumentException("Unsupported Threat Level: " + threatLevel);
    }
  }

  public void setDiscoveredCountOther(ThreatLevel threatLevel, int count) {
    switch (threatLevel) {
      case LOW:
        discoveredCountOtherLowThreat = count;
        break;
      case MODERATE:
        discoveredCountOtherModerateThreat = count;
        break;
      case SEVERE:
        discoveredCountOtherSevereThreat = count;
        break;
      case CRITICAL:
        discoveredCountOtherCriticalThreat = count;
        break;
      default:
        throw new IllegalArgumentException("Unsupported Threat Level: " + threatLevel);
    }
  }

  public void setEvaluationCount(int evaluationCount) {
    this.evaluationCount = evaluationCount;
  }
  
  public int getDiscoveredCount(PolicyThreatCategory threatCategory, ThreatLevel threatLevel) {
    switch (threatCategory) {
      case SECURITY:
        return getDiscoveredCountSecurity(threatLevel);
      case LICENSE:
        return getDiscoveredCountLicense(threatLevel);
      case QUALITY:
        return getDiscoveredCountQuality(threatLevel);
      case OTHER:
        return getDiscoveredCountOther(threatLevel);
      default:
        throw new IllegalArgumentException("Unsupported Threat Category: " + threatCategory);
    }
  }

  private int getDiscoveredCountSecurity(ThreatLevel threatLevel) {
    switch (threatLevel) {
      case LOW:
        return discoveredCountSecurityLowThreat;
      case MODERATE:
        return discoveredCountSecurityModerateThreat;
      case SEVERE:
        return discoveredCountSecuritySevereThreat;
      case CRITICAL:
        return discoveredCountSecurityCriticalThreat;
      default:
        throw new IllegalArgumentException("Unsupported Threat Level: " + threatLevel);
    }
  }

  public int getDiscoveredCountLicense(ThreatLevel threatLevel) {
    switch (threatLevel) {
      case LOW:
        return discoveredCountLicenseLowThreat;
      case MODERATE:
        return discoveredCountLicenseModerateThreat;
      case SEVERE:
        return discoveredCountLicenseSevereThreat;
      case CRITICAL:
        return discoveredCountLicenseCriticalThreat;
      default:
        throw new IllegalArgumentException("Unsupported Threat Level: " + threatLevel);
    }
  }

  public int getDiscoveredCountQuality(ThreatLevel threatLevel) {
    switch (threatLevel) {
      case LOW:
        return discoveredCountQualityLowThreat;
      case MODERATE:
        return discoveredCountQualityModerateThreat;
      case SEVERE:
        return discoveredCountQualitySevereThreat;
      case CRITICAL:
        return discoveredCountQualityCriticalThreat;
      default:
        throw new IllegalArgumentException("Unsupported Threat Level: " + threatLevel);
    }
  }

  public int getDiscoveredCountOther(ThreatLevel threatLevel) {
    switch (threatLevel) {
      case LOW:
        return discoveredCountOtherLowThreat;
      case MODERATE:
        return discoveredCountOtherModerateThreat;
      case SEVERE:
        return discoveredCountOtherSevereThreat;
      case CRITICAL:
        return discoveredCountOtherCriticalThreat;
      default:
        throw new IllegalArgumentException("Unsupported Threat Level: " + threatLevel);
    }
  }
  
  public int getEvaluationCount() {
    return evaluationCount;
  }
}
