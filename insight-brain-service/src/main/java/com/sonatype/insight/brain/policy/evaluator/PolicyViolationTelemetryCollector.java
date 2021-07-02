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
import java.util.concurrent.TimeUnit;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.common.annotations.VisibleForTesting;
import org.apache.maven.artifact.versioning.ComparableVersion;

public class PolicyViolationTelemetryCollector
{
  @VisibleForTesting
  static final String APPLICATION_ID = "application_id";

  static final String CONDITION_TYPE = "condition_type";

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

  static final String GRANDFATHER_TIME = "grandfather_time";

  static final String INNERSOURCE_DEPENDENCY = "innersource_dependency";

  static final String DIRECT_DEPENDENCY = "direct_dependency";

  static final String WAIVER_EXPIRATION = "waiver_expiration";

  static final String FIX_BY_VERSION_CHANGE = "fix_by_version_change";

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

  public void addTelemetryForFixedViolation(PolicyViolation fixedPolicyViolation, List<Component> components) {
    if (fixedPolicyViolation != null) {
      TelemetryData telemetryData =
          createTelemetry(TelemetryPurpose.TIME_TO_REMEDIATE_POLICY_VIOLATION, fixedPolicyViolation);
      if (components.size() == 1) {
        addTelemetryDependencyInfo(components.get(0), telemetryData);
      }
      else if (components.size() > 1 && components.get(0).getInnerSourceData() != null) {
        telemetryData.put(INNERSOURCE_DEPENDENCY, true);
      }
      telemetryData.put(FIX_TIME, timeOfPolicyEvaluation.getTime());
      telemetryDataList.add(telemetryData);
      addTelemetryForVersionChange(fixedPolicyViolation, components);
    }
  }

  private void addTelemetryForVersionChange(PolicyViolation fixedPolicyViolation, List<Component> components) {
    if (components.size() == 1) {
      String fixByVersionChange = calculateFixByVersionChange(components, fixedPolicyViolation);
      if (fixByVersionChange != null) {
        TelemetryData telemetryData =
            createTelemetry(TelemetryPurpose.TIME_TO_CHANGE_VERSION_POLICY_VIOLATION, fixedPolicyViolation);
        addTelemetryDependencyInfo(components.get(0), telemetryData);
        telemetryData.put(FIX_BY_VERSION_CHANGE, fixByVersionChange);

        telemetryData.put(FIX_TIME, timeOfPolicyEvaluation.getTime());
        telemetryDataList.add(telemetryData);
      }
    }
  }

  public void addTelemetryForUnwaivedViolation(PolicyViolation unwaivedPolicyViolation, Component component) {
    if (unwaivedPolicyViolation != null) {
      TelemetryData telemetryData =
          createTelemetry(TelemetryPurpose.TIME_TO_WAIVE_POLICY_VIOLATION, unwaivedPolicyViolation);
      if (component != null) {
        addTelemetryDependencyInfo(component, telemetryData);
      }
      telemetryData.put(UNWAIVE_TIME, timeOfPolicyEvaluation.getTime());
      telemetryData.put(COUNT, -1);
      telemetryDataList.add(telemetryData);
    }
  }

  public void addTelemetryForWaivedViolation(PolicyViolation waivedPolicyViolation, Component component) {
    if (waivedPolicyViolation != null) {
      String policyWaiverId = waivedPolicyViolation.getPolicyWaiverId();
      String waiverExpirationInDays = getPolicyWaiverExpirationDays(policyWaiverId);

      TelemetryData telemetryData =
          createTelemetry(TelemetryPurpose.TIME_TO_WAIVE_POLICY_VIOLATION, waivedPolicyViolation);
      if (component != null) {
        addTelemetryDependencyInfo(component, telemetryData);
      }
      telemetryData.put(WAIVER_EXPIRATION, waiverExpirationInDays);
      telemetryData.put(WAIVE_TIME, timeOfPolicyEvaluation.getTime());
      telemetryDataList.add(telemetryData);
    }
  }

  public void addTelemetryForConditionTypeViolation(PolicyViolation policyViolation, String conditionType) {
    if (policyViolation != null) {
      telemetryDataList.add(createTelemetry(TelemetryPurpose.CONDITION_TYPE_VIOLATION, policyViolation)
          .put(CONDITION_TYPE, conditionType));
    }
  }

  public void addTelemetryForGrandfatheredViolation(PolicyViolation grandfatheredPolicyViolation, Component component) {
    if (grandfatheredPolicyViolation != null) {
      TelemetryData telemetryData =
          createTelemetry(TelemetryPurpose.TIME_TO_GRANDFATHER_POLICY_VIOLATION, grandfatheredPolicyViolation);
      telemetryData.put(GRANDFATHER_TIME, timeOfPolicyEvaluation.getTime());
      if (component != null) {
        addTelemetryDependencyInfo(component, telemetryData);
      }

      telemetryDataList.add(telemetryData);
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

  private String calculateFixByVersionChange(List<Component> components, PolicyViolation oldPolicyViolation) {
    String newVersion = components.get(0).getVersion();
    String oldVersion = null;
    if (oldPolicyViolation.getComponentIdentifier() != null) {
      oldVersion = oldPolicyViolation.getComponentIdentifier().get(ComponentIdentifier.VERSION);
    }
    if (oldVersion != null && newVersion != null) {
      ComparableVersion oldComparableVersion = new ComparableVersion(oldVersion);
      ComparableVersion newComparableVersion = new ComparableVersion(newVersion);
      int comparisonResult = oldComparableVersion.compareTo(newComparableVersion);
      if (comparisonResult > 0) {
        return "downgrade";
      }
      if (comparisonResult < 0) {
        return "upgrade";
      }
    }
    return null;
  }

  private String getPolicyWaiverExpirationDays(String policyWaiverId) {
    if (policyWaiverId != null) {
      PolicyWaiver policyWaiver = new PolicyWaiverDAO().getById(policyWaiverId);
      if (policyWaiver != null && policyWaiver.getExpiryTime() != null) {
        return String.valueOf(TimeUnit.MILLISECONDS
            .toDays(policyWaiver.getExpiryTime().getTime() - policyWaiver.getCreateTime().getTime()));
      }
    }
    return "never";
  }

  private void addTelemetryDependencyInfo(Component component, TelemetryData telemetryData) {
    telemetryData.put(INNERSOURCE_DEPENDENCY, component.getInnerSourceData() != null);

    if (component.getDirectDependency() != null) {
      telemetryData.put(DIRECT_DEPENDENCY, component.getDirectDependency());
    }
  }
}
