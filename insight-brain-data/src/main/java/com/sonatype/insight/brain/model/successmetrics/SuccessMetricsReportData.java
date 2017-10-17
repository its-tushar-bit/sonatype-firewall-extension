/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.successmetrics;

import java.io.IOException;
import java.util.Date;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;
import com.sonatype.insight.json.store.JsonUtils;

/**
 * @since 1.39
 */
@Entity
@Table(name = "success_metrics_report_data")
public class SuccessMetricsReportData
    implements HasStringId
{
  @Id
  @Column(name = "success_metrics_report_data_id")
  private String id;

  @Column(name = "last_updated")
  private Date lastUpdated;

  @Column(name = "included_application_ids_json")
  private String includedApplicationIdsJson;

  @Column(name = "month_count")
  private int monthCount;

  @Column(name = "active_application_count")
  private int activeApplicationCount;

  @Column(name = "evaluations_per_month")
  private double evaluationsPerMonth;

  @Column(name = "total_policy_violations_per_application")
  private double totalPolicyViolationsPerApplication;

  @Column(name = "total_critical_policy_violations_per_application")
  private double totalCriticalPolicyViolationsPerApplication;

  @Column(name = "security_policy_violations_per_application")
  private double securityPolicyViolationsPerApplication;

  @Column(name = "security_critical_policy_violations_per_application")
  private double securityCriticalPolicyViolationsPerApplication;

  @Column(name = "license_policy_violations_per_application")
  private double licensePolicyViolationsPerApplication;

  @Column(name = "license_critical_policy_violations_per_application")
  private double licenseCriticalPolicyViolationsPerApplication;

  @Column(name = "quality_policy_violations_per_application")
  private double qualityPolicyViolationsPerApplication;

  @Column(name = "quality_critical_policy_violations_per_application")
  private double qualityCriticalPolicyViolationsPerApplication;

  @Column(name = "other_policy_violations_per_application")
  private double otherPolicyViolationsPerApplication;

  @Column(name = "other_critical_policy_violations_per_application")
  private double otherCriticalPolicyViolationsPerApplication;

  @Column(name = "mttr_month_1_time_period_start")
  private Date mttrMonth1TimePeriodStart;

  @Column(name = "mttr_month_1_all")
  private Integer mttrMonth1All;

  @Column(name = "mttr_month_1_critical")
  private Integer mttrMonth1Critical;

  @Column(name = "mttr_month_2_time_period_start")
  private Date mttrMonth2TimePeriodStart;

  @Column(name = "mttr_month_2_all")
  private Integer mttrMonth2All;

  @Column(name = "mttr_month_2_critical")
  private Integer mttrMonth2Critical;

  @Column(name = "mttr_month_3_time_period_start")
  private Date mttrMonth3TimePeriodStart;

  @Column(name = "mttr_month_3_all")
  private Integer mttrMonth3All;

  @Column(name = "mttr_month_3_critical")
  private Integer mttrMonth3Critical;

  @Column(name = "mttr_month_4_time_period_start")
  private Date mttrMonth4TimePeriodStart;

  @Column(name = "mttr_month_4_all")
  private Integer mttrMonth4All;

  @Column(name = "mttr_month_4_critical")
  private Integer mttrMonth4Critical;

  @Column(name = "mttr_month_5_time_period_start")
  private Date mttrMonth5TimePeriodStart;

  @Column(name = "mttr_month_5_all")
  private Integer mttrMonth5All;

  @Column(name = "mttr_month_5_critical")
  private Integer mttrMonth5Critical;

  @Column(name = "mttr_month_6_time_period_start")
  private Date mttrMonth6TimePeriodStart;

  @Column(name = "mttr_month_6_all")
  private Integer mttrMonth6All;

  @Column(name = "mttr_month_6_critical")
  private Integer mttrMonth6Critical;

  @Column(name = "mttr_month_7_time_period_start")
  private Date mttrMonth7TimePeriodStart;

  @Column(name = "mttr_month_7_all")
  private Integer mttrMonth7All;

  @Column(name = "mttr_month_7_critical")
  private Integer mttrMonth7Critical;

  @Column(name = "mttr_month_8_time_period_start")
  private Date mttrMonth8TimePeriodStart;

  @Column(name = "mttr_month_8_all")
  private Integer mttrMonth8All;

  @Column(name = "mttr_month_8_critical")
  private Integer mttrMonth8Critical;

  @Column(name = "mttr_month_9_time_period_start")
  private Date mttrMonth9TimePeriodStart;

  @Column(name = "mttr_month_9_all")
  private Integer mttrMonth9All;

  @Column(name = "mttr_month_9_critical")
  private Integer mttrMonth9Critical;

  @Column(name = "mttr_month_10_time_period_start")
  private Date mttrMonth10TimePeriodStart;

  @Column(name = "mttr_month_10_all")
  private Integer mttrMonth10All;

  @Column(name = "mttr_month_10_critical")
  private Integer mttrMonth10Critical;

  @Column(name = "mttr_month_11_time_period_start")
  private Date mttrMonth11TimePeriodStart;

  @Column(name = "mttr_month_11_all")
  private Integer mttrMonth11All;

  @Column(name = "mttr_month_11_critical")
  private Integer mttrMonth11Critical;

  @Column(name = "mttr_month_12_time_period_start")
  private Date mttrMonth12TimePeriodStart;

  @Column(name = "mttr_month_12_all")
  private Integer mttrMonth12All;

  @Column(name = "mttr_month_12_critical")
  private Integer mttrMonth12Critical;

  @Column(name = "applications_with_violations_total")
  private int applicationsWithViolationsTotal;

  @Column(name = "applications_with_critical_violations_total")
  private int applicationsWithCriticalViolationsTotal;

  @Column(name = "applications_with_violations_security")
  private int applicationsWithViolationsSecurity;

  @Column(name = "applications_with_critical_violations_security")
  private int applicationsWithCriticalViolationsSecurity;

  @Column(name = "applications_with_violations_license")
  private int applicationsWithViolationsLicense;

  @Column(name = "applications_with_critical_violations_license")
  private int applicationsWithCriticalViolationsLicense;

  @Column(name = "applications_with_violations_quality")
  private int applicationsWithViolationsQuality;

  @Column(name = "applications_with_critical_violations_quality")
  private int applicationsWithCriticalViolationsQuality;

  @Column(name = "applications_with_violations_other")
  private int applicationsWithViolationsOther;

  @Column(name = "applications_with_critical_violations_other")
  private int applicationsWithCriticalViolationsOther;

  public SuccessMetricsReportData() {
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public int getMonthCount() {
    return monthCount;
  }

  public void setMonthCount(int monthCount) {
    this.monthCount = monthCount;
  }

  public int getActiveApplicationCount() {
    return activeApplicationCount;
  }

  public void setActiveApplicationCount(int activeApplicationCount) {
    this.activeApplicationCount = activeApplicationCount;
  }

  public double getEvaluationsPerMonth() {
    return evaluationsPerMonth;
  }

  public void setEvaluationsPerMonth(double evaluationsPerMonth) {
    this.evaluationsPerMonth = evaluationsPerMonth;
  }

  public double getTotalPolicyViolationsPerApplication() {
    return totalPolicyViolationsPerApplication;
  }

  public void setTotalPolicyViolationsPerApplication(double totalPolicyViolationsPerApplication) {
    this.totalPolicyViolationsPerApplication = totalPolicyViolationsPerApplication;
  }

  public double getTotalCriticalPolicyViolationsPerApplication() {
    return totalCriticalPolicyViolationsPerApplication;
  }

  public void setTotalCriticalPolicyViolationsPerApplication(double totalCriticalPolicyViolationsPerApplication) {
    this.totalCriticalPolicyViolationsPerApplication = totalCriticalPolicyViolationsPerApplication;
  }

  public double getSecurityPolicyViolationsPerApplication() {
    return securityPolicyViolationsPerApplication;
  }

  public void setSecurityPolicyViolationsPerApplication(double securityPolicyViolationsPerApplication) {
    this.securityPolicyViolationsPerApplication = securityPolicyViolationsPerApplication;
  }

  public double getSecurityCriticalPolicyViolationsPerApplication() {
    return securityCriticalPolicyViolationsPerApplication;
  }

  public void setSecurityCriticalPolicyViolationsPerApplication(double securityCriticalPolicyViolationsPerApplication) {
    this.securityCriticalPolicyViolationsPerApplication = securityCriticalPolicyViolationsPerApplication;
  }

  public double getLicensePolicyViolationsPerApplication() {
    return licensePolicyViolationsPerApplication;
  }

  public void setLicensePolicyViolationsPerApplication(double licensePolicyViolationsPerApplication) {
    this.licensePolicyViolationsPerApplication = licensePolicyViolationsPerApplication;
  }

  public double getLicenseCriticalPolicyViolationsPerApplication() {
    return licenseCriticalPolicyViolationsPerApplication;
  }

  public void setLicenseCriticalPolicyViolationsPerApplication(double licenseCriticalPolicyViolationsPerApplication) {
    this.licenseCriticalPolicyViolationsPerApplication = licenseCriticalPolicyViolationsPerApplication;
  }

  public double getQualityPolicyViolationsPerApplication() {
    return qualityPolicyViolationsPerApplication;
  }

  public void setQualityPolicyViolationsPerApplication(double qualityPolicyViolationsPerApplication) {
    this.qualityPolicyViolationsPerApplication = qualityPolicyViolationsPerApplication;
  }

  public double getQualityCriticalPolicyViolationsPerApplication() {
    return qualityCriticalPolicyViolationsPerApplication;
  }

  public void setQualityCriticalPolicyViolationsPerApplication(double qualityCriticalPolicyViolationsPerApplication) {
    this.qualityCriticalPolicyViolationsPerApplication = qualityCriticalPolicyViolationsPerApplication;
  }

  public double getOtherPolicyViolationsPerApplication() {
    return otherPolicyViolationsPerApplication;
  }

  public void setOtherPolicyViolationsPerApplication(double otherPolicyViolationsPerApplication) {
    this.otherPolicyViolationsPerApplication = otherPolicyViolationsPerApplication;
  }

  public double getOtherCriticalPolicyViolationsPerApplication() {
    return otherCriticalPolicyViolationsPerApplication;
  }

  public void setOtherCriticalPolicyViolationsPerApplication(double otherCriticalPolicyViolationsPerApplication) {
    this.otherCriticalPolicyViolationsPerApplication = otherCriticalPolicyViolationsPerApplication;
  }

  public int getApplicationsWithViolationsTotal() {
    return applicationsWithViolationsTotal;
  }

  public void setApplicationsWithViolationsTotal(int applicationsWithViolationsTotal) {
    this.applicationsWithViolationsTotal = applicationsWithViolationsTotal;
  }

  public int getApplicationsWithCriticalViolationsTotal() {
    return applicationsWithCriticalViolationsTotal;
  }

  public void setApplicationsWithCriticalViolationsTotal(int applicationsWithCriticalViolationsTotal) {
    this.applicationsWithCriticalViolationsTotal = applicationsWithCriticalViolationsTotal;
  }

  public int getApplicationsWithViolationsSecurity() {
    return applicationsWithViolationsSecurity;
  }

  public void setApplicationsWithViolationsSecurity(int applicationsWithViolationsSecurity) {
    this.applicationsWithViolationsSecurity = applicationsWithViolationsSecurity;
  }

  public int getApplicationsWithCriticalViolationsSecurity() {
    return applicationsWithCriticalViolationsSecurity;
  }

  public void setApplicationsWithCriticalViolationsSecurity(int applicationsWithCriticalViolationsSecurity) {
    this.applicationsWithCriticalViolationsSecurity = applicationsWithCriticalViolationsSecurity;
  }

  public int getApplicationsWithViolationsLicense() {
    return applicationsWithViolationsLicense;
  }

  public void setApplicationsWithViolationsLicense(int applicationsWithViolationsLicense) {
    this.applicationsWithViolationsLicense = applicationsWithViolationsLicense;
  }

  public int getApplicationsWithCriticalViolationsLicense() {
    return applicationsWithCriticalViolationsLicense;
  }

  public void setApplicationsWithCriticalViolationsLicense(int applicationsWithCriticalViolationsLicense) {
    this.applicationsWithCriticalViolationsLicense = applicationsWithCriticalViolationsLicense;
  }

  public int getApplicationsWithViolationsQuality() {
    return applicationsWithViolationsQuality;
  }

  public void setApplicationsWithViolationsQuality(int applicationsWithViolationsQuality) {
    this.applicationsWithViolationsQuality = applicationsWithViolationsQuality;
  }

  public int getApplicationsWithCriticalViolationsQuality() {
    return applicationsWithCriticalViolationsQuality;
  }

  public void setApplicationsWithCriticalViolationsQuality(int applicationsWithCriticalViolationsQuality) {
    this.applicationsWithCriticalViolationsQuality = applicationsWithCriticalViolationsQuality;
  }

  public int getApplicationsWithViolationsOther() {
    return applicationsWithViolationsOther;
  }

  public void setApplicationsWithViolationsOther(int applicationsWithViolationsOther) {
    this.applicationsWithViolationsOther = applicationsWithViolationsOther;
  }

  public int getApplicationsWithCriticalViolationsOther() {
    return applicationsWithCriticalViolationsOther;
  }

  public void setApplicationsWithCriticalViolationsOther(int applicationsWithCriticalViolationsOther) {
    this.applicationsWithCriticalViolationsOther = applicationsWithCriticalViolationsOther;
  }

  public void setMttrMonthAll(int monthNum, Integer value) {
    switch (monthNum) {
      case 1:
        mttrMonth1All = value;
        break;
      case 2:
        mttrMonth2All = value;
        break;
      case 3:
        mttrMonth3All = value;
        break;
      case 4:
        mttrMonth4All = value;
        break;
      case 5:
        mttrMonth5All = value;
        break;
      case 6:
        mttrMonth6All = value;
        break;
      case 7:
        mttrMonth7All = value;
        break;
      case 8:
        mttrMonth8All = value;
        break;
      case 9:
        mttrMonth9All = value;
        break;
      case 10:
        mttrMonth10All = value;
        break;
      case 11:
        mttrMonth11All = value;
        break;
      case 12:
        mttrMonth12All = value;
        break;
      default:
        throw new IllegalArgumentException("monthNum must be between 1 and 12 inclusive, but was " + monthNum);
    }
  }

  public void setMttrMonthCritical(int monthNum, Integer value) {
    switch (monthNum) {
      case 1:
        mttrMonth1Critical = value;
        break;
      case 2:
        mttrMonth2Critical = value;
        break;
      case 3:
        mttrMonth3Critical = value;
        break;
      case 4:
        mttrMonth4Critical = value;
        break;
      case 5:
        mttrMonth5Critical = value;
        break;
      case 6:
        mttrMonth6Critical = value;
        break;
      case 7:
        mttrMonth7Critical = value;
        break;
      case 8:
        mttrMonth8Critical = value;
        break;
      case 9:
        mttrMonth9Critical = value;
        break;
      case 10:
        mttrMonth10Critical = value;
        break;
      case 11:
        mttrMonth11Critical = value;
        break;
      case 12:
        mttrMonth12Critical = value;
        break;
      default:
        throw new IllegalArgumentException("monthNum must be between 1 and 12 inclusive, but was " + monthNum);
    }
  }

  public void setMttrMonthTimePeriodStart(int monthNum, Date value) {
    switch (monthNum) {
      case 1:
        mttrMonth1TimePeriodStart = value;
        break;
      case 2:
        mttrMonth2TimePeriodStart = value;
        break;
      case 3:
        mttrMonth3TimePeriodStart = value;
        break;
      case 4:
        mttrMonth4TimePeriodStart = value;
        break;
      case 5:
        mttrMonth5TimePeriodStart = value;
        break;
      case 6:
        mttrMonth6TimePeriodStart = value;
        break;
      case 7:
        mttrMonth7TimePeriodStart = value;
        break;
      case 8:
        mttrMonth8TimePeriodStart = value;
        break;
      case 9:
        mttrMonth9TimePeriodStart = value;
        break;
      case 10:
        mttrMonth10TimePeriodStart = value;
        break;
      case 11:
        mttrMonth11TimePeriodStart = value;
        break;
      case 12:
        mttrMonth12TimePeriodStart = value;
        break;
      default:
        throw new IllegalArgumentException("monthNum must be between 1 and 12 inclusive, but was " + monthNum);
    }
  }

  public Integer getMttrMonthAll(int monthNum) {
    switch (monthNum) {
      case 1:
        return mttrMonth1All;
      case 2:
        return mttrMonth2All;
      case 3:
        return mttrMonth3All;
      case 4:
        return mttrMonth4All;
      case 5:
        return mttrMonth5All;
      case 6:
        return mttrMonth6All;
      case 7:
        return mttrMonth7All;
      case 8:
        return mttrMonth8All;
      case 9:
        return mttrMonth9All;
      case 10:
        return mttrMonth10All;
      case 11:
        return mttrMonth11All;
      case 12:
        return mttrMonth12All;
      default:
        throw new IllegalArgumentException("monthNum must be between 1 and 12 inclusive, but was " + monthNum);
    }
  }

  public Integer getMttrMonthCritical(int monthNum) {
    switch (monthNum) {
      case 1:
        return mttrMonth1Critical;
      case 2:
        return mttrMonth2Critical;
      case 3:
        return mttrMonth3Critical;
      case 4:
        return mttrMonth4Critical;
      case 5:
        return mttrMonth5Critical;
      case 6:
        return mttrMonth6Critical;
      case 7:
        return mttrMonth7Critical;
      case 8:
        return mttrMonth8Critical;
      case 9:
        return mttrMonth9Critical;
      case 10:
        return mttrMonth10Critical;
      case 11:
        return mttrMonth11Critical;
      case 12:
        return mttrMonth12Critical;
      default:
        throw new IllegalArgumentException("monthNum must be between 1 and 12 inclusive, but was " + monthNum);
    }
  }

  public Date getMttrMonthTimePeriodStart(int monthNum) {
    switch (monthNum) {
      case 1:
        return mttrMonth1TimePeriodStart;
      case 2:
        return mttrMonth2TimePeriodStart;
      case 3:
        return mttrMonth3TimePeriodStart;
      case 4:
        return mttrMonth4TimePeriodStart;
      case 5:
        return mttrMonth5TimePeriodStart;
      case 6:
        return mttrMonth6TimePeriodStart;
      case 7:
        return mttrMonth7TimePeriodStart;
      case 8:
        return mttrMonth8TimePeriodStart;
      case 9:
        return mttrMonth9TimePeriodStart;
      case 10:
        return mttrMonth10TimePeriodStart;
      case 11:
        return mttrMonth11TimePeriodStart;
      case 12:
        return mttrMonth12TimePeriodStart;
      default:
        throw new IllegalArgumentException("monthNum must be between 1 and 12 inclusive, but was " + monthNum);
    }
  }

  public Date getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(Date lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  public Set<String> getIncludedApplicationIds() {
    try {
      @SuppressWarnings("unchecked")
      Set<String> retval = JsonUtils.parse(includedApplicationIdsJson, Set.class);

      return retval;
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void setIncludedApplicationIds(Set<String> includedApplicationIds) {
    this.includedApplicationIdsJson = JsonUtils.format(includedApplicationIds);
  }
}
