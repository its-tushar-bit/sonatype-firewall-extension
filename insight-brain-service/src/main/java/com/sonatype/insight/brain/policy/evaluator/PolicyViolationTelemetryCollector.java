/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.common.annotations.VisibleForTesting;

import static com.sonatype.insight.telemetry.model.TelemetryPurpose.TIME_TO_REMEDIATE_POLICY_VIOLATION;
import static com.sonatype.insight.telemetry.model.TelemetryPurpose.TIME_TO_WAIVE_POLICY_VIOLATION;

public class PolicyViolationTelemetryCollector
{
  @VisibleForTesting
  static final String APPLICATION_ID = "application_id";

  static final String COUNT = "count";

  static final String FIX_TIME = "fix_time";

  static final String IS_SCM_ENABLED = "is_scm_enabled";

  static final String OPEN_TIME = "open_time";

  static final String STAGE = "stage_id";

  static final String THREAT_CATEGORY = "threat_category";

  static final String THREAT_LEVEL = "threat_level";

  static final String TIME = "time";

  static final String UNWAIVE_TIME = "waive_time"; // yes, same as WAIVE_TIME for now

  static final String WAIVE_TIME = "waive_time";

  private List<TelemetryData> telemetryDataList = new ArrayList<>();

  private boolean isScmEnabled;

  private Date timeOfPolicyEvaluation;

  public PolicyViolationTelemetryCollector(boolean isScmEnabled) {
    this.isScmEnabled = isScmEnabled;
    timeOfPolicyEvaluation = new Date();
  }

  public List<TelemetryData> getTelemetryData() {
    return Collections.unmodifiableList(telemetryDataList);
  }

  public void addTelemetryForFixedViolation(PolicyViolation fixedPolicyViolation) {
    if (fixedPolicyViolation != null) {
      telemetryDataList.add(createTelemetry(TIME_TO_REMEDIATE_POLICY_VIOLATION, fixedPolicyViolation)
          .put(FIX_TIME, timeOfPolicyEvaluation.getTime())
      );
    }
  }

  public void addTelemetryForUnwaivedViolation(PolicyViolation unwaivedPolicyViolation) {
    if (unwaivedPolicyViolation != null) {
      telemetryDataList.add(createTelemetry(TIME_TO_WAIVE_POLICY_VIOLATION, unwaivedPolicyViolation)
          .put(UNWAIVE_TIME, timeOfPolicyEvaluation.getTime())
          .put(COUNT, -1)
      );
    }
  }

  public void addTelemetryForWaivedViolation(PolicyViolation waivedPolicyViolation) {
    if (waivedPolicyViolation != null) {
      telemetryDataList.add(createTelemetry(TIME_TO_WAIVE_POLICY_VIOLATION, waivedPolicyViolation)
          .put(WAIVE_TIME, timeOfPolicyEvaluation.getTime())
      );
    }
  }

  public void setTimeOfPolicyEvaluation(final Date timeOfPolicyEvaluation) {
    if (null == timeOfPolicyEvaluation) {
      throw new IllegalArgumentException("time of evaluation is required");
    }
    this.timeOfPolicyEvaluation = timeOfPolicyEvaluation;
  }

  private TelemetryData createTelemetry(TelemetryPurpose telemetryPurpose, PolicyViolation policyViolation) {
    return new TelemetryData(telemetryPurpose)
        .put(APPLICATION_ID, HdsClientAnalytics.obfuscate(policyViolation.getApplicationId()))
        .put(STAGE, policyViolation.getStageTypeId())
        .put(IS_SCM_ENABLED, isScmEnabled)
        .put(COUNT, 1)
        .put(OPEN_TIME, policyViolation.getOpenTime().getTime())
        .put(TIME, computeTimeBetween(policyViolation.getOpenTime(), timeOfPolicyEvaluation))
        .put(THREAT_CATEGORY, policyViolation.getThreatCategory().getName())
        .put(THREAT_LEVEL, policyViolation.getThreatLevel());
  }

  private long computeTimeBetween(Date date1, Date date2) {
    return Math.abs(date1.getTime() - date2.getTime());
  }
}
