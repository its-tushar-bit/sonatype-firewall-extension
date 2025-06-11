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
import com.sonatype.insight.brain.api.experimental.PurlIdentifiersWithVulnerabilities;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.license.LicenseNameProvider;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.telemetry.PolicyViolationTelemetryBuilder;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;

import static com.sonatype.insight.brain.callflow.PolicyViolationReachabilityHelper.hasPolicyViolationByComponentIdentifier;

public class PolicyViolationTelemetryCollector
{
  @VisibleForTesting
  static final String APPLICATION_ID = "application_id";

  static final String COMPONENT_DOWNGRADE = "downgrade";

  static final String COMPONENT_IDENTIFIER = "component_identifier";

  static final String COMPONENT_NAMESPACE = "component_namespace";

  static final String COMPONENT_NAME = "component_name";

  static final String COMPONENT_UPGRADE = "upgrade";

  static final String COMPONENT_VERSION = "component_version";

  static final String CONDITION_TYPE = "condition_type";

  static final String COUNT = "count";

  static final String ECOSYSTEM = "ecosystem";

  static final String FIX_TIME = "fix_time";

  static final String IS_SCM_ENABLED = "is_scm_enabled";

  static final String NEW_POLICY_VIOLATION_ID = "new_policy_violation_id";

  static final String OPEN_TIME = "open_time";

  static final String POLICY_VIOLATION_ID = "policy_violation_id";

  static final String STAGE = "stage_id";

  static final String THREAT_CATEGORY = "threat_category";

  static final String THREAT_LEVEL = "threat_level";

  static final String REACHABILITY_STATUS = "reachability_status";

  static final String TIME = "time";

  static final String UNWAIVE_TIME = "unwaive_time";

  static final String WAIVE_TIME = "waive_time";

  static final String LEGACY_VIOLATION_TIME = "legacy_violation_time";

  static final String INNERSOURCE_DEPENDENCY = "innersource_dependency";

  static final String DIRECT_DEPENDENCY = "direct_dependency";

  static final String WAIVER_EXPIRATION = "waiver_expiration";

  static final String WAIVER_EXPIRATION_NEVER = "never";

  static final String POLICY_WAIVER_ID = "policy_waiver_id";

  static final String AUTO_POLICY_WAIVER_ID = "auto_policy_waiver_id";

  static final String FIX_BY_VERSION_CHANGE = "fix_by_version_change";

  static final String CALL_FLOW_EVALUATION_SUCCESSFUL = "call_flow_evaluation_successful";

  static final String CALL_FLOW_HAS_REACHABLE_INFORMATION_FOR_COMPONENT =
      "call_flow_has_reachable_information_for_component";

  private final PolicyWaiverDAO policyWaiverDAO;

  private final List<TelemetryData> telemetryDataList = new ArrayList<>();

  private final TelemetryUtils telemetryUtils;

  private final LicenseNameProvider licenseNameProvider;

  private boolean isScmEnabled;

  private Date timeOfPolicyEvaluation;

  public PolicyViolationTelemetryCollector(
      final PolicyWaiverDAO policyWaiverDAO,
      TelemetryUtils telemetryUtils,
      LicenseNameProvider licenseNameProvider,
      boolean isScmEnabled)
  {
    this.policyWaiverDAO = policyWaiverDAO;
    this.telemetryUtils = telemetryUtils;
    this.licenseNameProvider = licenseNameProvider;
    this.isScmEnabled = isScmEnabled;
    timeOfPolicyEvaluation = new Date();
  }

  public List<TelemetryData> getTelemetryData() {
    return Collections.unmodifiableList(telemetryDataList);
  }

  public void addTelemetryForFixedViolation(
      PolicyViolation fixedPolicyViolation,
      List<Component> components)
  {
    if (fixedPolicyViolation != null) {
      TelemetryData telemetryData =
          createTelemetry(TelemetryPurpose.TIME_TO_REMEDIATE_POLICY_VIOLATION, fixedPolicyViolation, components)
              .put(FIX_TIME, timeOfPolicyEvaluation.getTime());

      telemetryDataList.add(telemetryData);
      possiblyAddTelemetryForVersionChange(fixedPolicyViolation, components);
    }
  }

  private void possiblyAddTelemetryForVersionChange(
      PolicyViolation fixedPolicyViolation,
      List<Component> components)
  {
    if (components.size() == 1) {
      String fixByVersionChange = calculateFixByVersionChange(components, fixedPolicyViolation);
      if (fixByVersionChange != null) {
        TelemetryData telemetryData =
            createTelemetry(TelemetryPurpose.TIME_TO_CHANGE_VERSION_POLICY_VIOLATION, fixedPolicyViolation, components)
                .put(FIX_BY_VERSION_CHANGE, fixByVersionChange)
                .put(FIX_TIME, timeOfPolicyEvaluation.getTime());

        telemetryDataList.add(telemetryData);
      }
    }
  }

  public void addTelemetryForUnwaivedViolation(
      final PolicyViolation unwaivedPolicyViolation,
      final PolicyViolation newPolicyViolation,
      final Component component
  )
  {
    if (unwaivedPolicyViolation != null) {
      TelemetryData telemetryData =
          createTelemetry(TelemetryPurpose.TIME_TO_WAIVE_POLICY_VIOLATION, unwaivedPolicyViolation, component)
              .put(WAIVE_TIME, unwaivedPolicyViolation.getWaiveTime().getTime())
              .put(UNWAIVE_TIME, timeOfPolicyEvaluation.getTime())
              .put(COUNT, -1)
              .put(NEW_POLICY_VIOLATION_ID, newPolicyViolation.getId());

      if (null != unwaivedPolicyViolation.getPolicyWaiverId()) {
        var policyWaiverId = unwaivedPolicyViolation.getPolicyWaiverId();
        var waiverExpirationInDays = getPolicyWaiverExpirationDays(policyWaiverId);
        telemetryData
            .put(POLICY_WAIVER_ID, unwaivedPolicyViolation.getPolicyWaiverId())
            .put(WAIVER_EXPIRATION, waiverExpirationInDays);
      }
      else { // was auto-waived
        telemetryData
            .put(AUTO_POLICY_WAIVER_ID, unwaivedPolicyViolation.getAutoPolicyWaiverId())
            .put(WAIVER_EXPIRATION, WAIVER_EXPIRATION_NEVER);
      }

      telemetryDataList.add(telemetryData);
    }
  }

  public void addTelemetryForWaivedViolation(PolicyViolation waivedPolicyViolation, Component component) {
    if (waivedPolicyViolation != null) {
      String policyWaiverId = waivedPolicyViolation.getPolicyWaiverId();
      String waiverExpirationInDays = getPolicyWaiverExpirationDays(policyWaiverId);

      TelemetryData telemetryData =
          createTelemetry(TelemetryPurpose.TIME_TO_WAIVE_POLICY_VIOLATION, waivedPolicyViolation, component)
              .put(WAIVER_EXPIRATION, waiverExpirationInDays)
              .put(WAIVE_TIME, timeOfPolicyEvaluation.getTime())
              .put(POLICY_WAIVER_ID, policyWaiverId);

      telemetryDataList.add(telemetryData);
    }
  }

  public void addTelemetryForAutoWaivedViolation(PolicyViolation waivedPolicyViolation, Component component) {
    if (waivedPolicyViolation != null) {
      String autoPolicyWaiverId = waivedPolicyViolation.getAutoPolicyWaiverId();

      TelemetryData telemetryData =
          createTelemetry(TelemetryPurpose.TIME_TO_WAIVE_POLICY_VIOLATION, waivedPolicyViolation, component);
      addComponentMetadata(telemetryData, waivedPolicyViolation);
      telemetryData.put(WAIVE_TIME, timeOfPolicyEvaluation.getTime());
      telemetryData.put(WAIVER_EXPIRATION, WAIVER_EXPIRATION_NEVER);
      telemetryData.put(AUTO_POLICY_WAIVER_ID, autoPolicyWaiverId);
      telemetryDataList.add(telemetryData);
    }
  }

  public void addTelemetryForConditionTypeViolation(
      PolicyViolation policyViolation,
      String conditionType,
      List<Component> components)
  {
    if (policyViolation != null) {
      TelemetryData telemetryData =
          createTelemetry(TelemetryPurpose.CONDITION_TYPE_VIOLATION, policyViolation, components)
              .put(CONDITION_TYPE, conditionType);

      telemetryDataList.add(telemetryData);
    }
  }

  public void addTelemetryForLegacyViolation(PolicyViolation legacyViolation, Component component) {
    if (legacyViolation != null) {
      TelemetryData telemetryData =
          createTelemetry(TelemetryPurpose.TIME_TO_LEGACY_VIOLATION, legacyViolation, component)
              .put(LEGACY_VIOLATION_TIME, timeOfPolicyEvaluation.getTime());

      telemetryDataList.add(telemetryData);
    }
  }

  public void addTelemetryForReachableViolation(
      final PolicyViolation policyViolation,
      final Component component,
      final PurlIdentifiersWithVulnerabilities reachablePurlIdentifiersWithVulnerabilities)
  {
    TelemetryData telemetryData = createTelemetry(
        TelemetryPurpose.CALLFLOW_EVALUATION_COMPONENT_COUNTS,
        policyViolation,
        component
    );

    // important to note that we don't add specific component/policy violation
    // reachability status as that is being added through the createTelemetry method.
    telemetryData.put(CALL_FLOW_EVALUATION_SUCCESSFUL, reachablePurlIdentifiersWithVulnerabilities != null);
    telemetryData.put(CALL_FLOW_HAS_REACHABLE_INFORMATION_FOR_COMPONENT,
        hasPolicyViolationByComponentIdentifier(policyViolation, reachablePurlIdentifiersWithVulnerabilities));

    telemetryDataList.add(telemetryData);
  }

  public void setTimeOfPolicyEvaluation(final Date timeOfPolicyEvaluation) {
    if (null == timeOfPolicyEvaluation) {
      throw new IllegalArgumentException("time of evaluation is required");
    }
    this.timeOfPolicyEvaluation = timeOfPolicyEvaluation;
  }

  private void addComponentMetadata(TelemetryData telemetryData, PolicyViolation policyViolation) {
    ComponentIdentifier componentIdentifier = policyViolation.getComponentIdentifier();
    if (null != componentIdentifier) {
      telemetryData.put(ECOSYSTEM, componentIdentifier.getFormat());
      telemetryData.put(COMPONENT_IDENTIFIER, componentIdentifier.toString());

      PackageUrlIdentifier packageUrlIdentifier =
          PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);

      telemetryData.put(COMPONENT_NAMESPACE, packageUrlIdentifier.getNamespace());
      telemetryData.put(COMPONENT_NAME, packageUrlIdentifier.getName());
      telemetryData.put(COMPONENT_VERSION, packageUrlIdentifier.getVersion());
    }
  }

  private void addDependencyInfo(TelemetryData telemetryData, Component component) {
    if (component != null) {
      telemetryData.put(INNERSOURCE_DEPENDENCY, component.getInnerSourceData() != null);

      if (component.getDirectDependency() != null) {
        telemetryData.put(DIRECT_DEPENDENCY, component.getDirectDependency());
      }
    }
  }

  private TelemetryData createTelemetry(
      TelemetryPurpose telemetryPurpose,
      PolicyViolation policyViolation,
      List<Component> components)
  {
    Component firstComponent = CollectionUtils.isNotEmpty(components) ? components.get(0) : null;
    return createTelemetry(telemetryPurpose, policyViolation, firstComponent);
  }

  private TelemetryData createTelemetry(
      TelemetryPurpose telemetryPurpose,
      PolicyViolation policyViolation,
      Component component)
  {
    TelemetryData telemetryData =
        new PolicyViolationTelemetryBuilder(policyViolation, telemetryPurpose, telemetryUtils, licenseNameProvider)
            .forComponent(component)
            .withScmEnabled(isScmEnabled)
            .withTime(computeTimeBetween(policyViolation.getOpenTime(), timeOfPolicyEvaluation))
            .build();

    addComponentMetadata(telemetryData, policyViolation);
    addDependencyInfo(telemetryData, component);

    return telemetryData;
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
        return COMPONENT_DOWNGRADE;
      }
      if (comparisonResult < 0) {
        return COMPONENT_UPGRADE;
      }
    }
    return null;
  }

  private String getPolicyWaiverExpirationDays(String policyWaiverId) {
    if (policyWaiverId != null) {
      PolicyWaiver policyWaiver = policyWaiverDAO.getById(policyWaiverId);
      if (policyWaiver != null && policyWaiver.getExpiryTime() != null) {
        return String.valueOf(TimeUnit.MILLISECONDS
            .toDays(policyWaiver.getExpiryTime().getTime() - policyWaiver.getCreateTime().getTime()));
      }
    }
    return WAIVER_EXPIRATION_NEVER;
  }
}
