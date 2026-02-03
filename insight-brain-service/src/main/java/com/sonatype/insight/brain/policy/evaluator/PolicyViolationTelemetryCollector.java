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
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.api.experimental.PurlIdentifiersWithVulnerabilities;
import com.sonatype.insight.brain.component.ComponentHelper;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.license.LicenseNameProvider;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.callflow.PolicyViolationReachabilityHelper.hasPolicyViolationByComponentIdentifier;

public class PolicyViolationTelemetryCollector
{
  private static final Logger log = LoggerFactory.getLogger(PolicyViolationTelemetryCollector.class);

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

  static final String IS_LEGACY_VIOLATION = "is_legacy_violation";

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

  static final String PULL_REQUEST_IS_GOLDEN = "pull_request_is_golden";

  static final String PULL_REQUEST_NUMBER = "pull_request_number";

  static final String PULL_REQUEST_REMEDIATION_VERSION = "pull_request_remediation_version";

  static final String POLICY_CONSTRAINTS = "policy_constraints";

  // arbitrary look-back window for associating remediation PRs to a policy violation's open time
  static final int REMEDIATION_PR_LOOKBACK_DAYS = 7;

  static final String REMEDIATION_VERSION = "remediation_version";

  private final PolicyWaiverDAO policyWaiverDAO;

  private final SourceControlEventDAO sourceControlEventDAO;

  private final List<TelemetryData> telemetryDataList = new ArrayList<>();

  private final TelemetryUtils telemetryUtils;

  private final LicenseNameProvider licenseNameProvider;

  private boolean isScmEnabled;

  private Date timeOfPolicyEvaluation;

  private final ComponentHelper componentHelper;

  public PolicyViolationTelemetryCollector(
      final PolicyWaiverDAO policyWaiverDAO,
      SourceControlEventDAO sourceControlEventDAO,
      TelemetryUtils telemetryUtils,
      LicenseNameProvider licenseNameProvider,
      boolean isScmEnabled,
      ComponentHelper componentHelper)
  {
    this.policyWaiverDAO = policyWaiverDAO;
    this.sourceControlEventDAO = sourceControlEventDAO;
    this.telemetryUtils = telemetryUtils;
    this.licenseNameProvider = licenseNameProvider;
    this.isScmEnabled = isScmEnabled;
    this.componentHelper = componentHelper;
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
              .put(FIX_TIME, timeOfPolicyEvaluation.getTime())
              .put(IS_LEGACY_VIOLATION, fixedPolicyViolation.isLegacyViolation());

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
        var newComponent = components.get(0);

        TelemetryData telemetryData =
            createTelemetry(TelemetryPurpose.TIME_TO_CHANGE_VERSION_POLICY_VIOLATION, fixedPolicyViolation, components)
                .put(FIX_BY_VERSION_CHANGE, fixByVersionChange)
                .put(REMEDIATION_VERSION, newComponent.getVersion())
                .put(FIX_TIME, timeOfPolicyEvaluation.getTime());

        // need to account for the fact that the remediation PR could have been triggered by an evaluation at a
        // different (earlier) Lifecycle stage (i.e. it could have been a different policy violation that triggered it)
        Date minCutoffTime = new Date(fixedPolicyViolation.getOpenTime().getTime()
            - TimeUnit.DAYS.toMillis(REMEDIATION_PR_LOOKBACK_DAYS));

        var remediationPullRequestEvents =
            sourceControlEventDAO.getCompletedRemediationPullRequestEventsForAppComponent(
                fixedPolicyViolation.getApplicationId(),
                fixedPolicyViolation.getComponentIdentifier(),
                minCutoffTime,
                fixedPolicyViolation.getFixTime()
            );

        if (CollectionUtils.isNotEmpty(remediationPullRequestEvents)) {
          var remediationEvent = remediationPullRequestEvents.get(0);
          var eventRemediationVersion = remediationEvent.getRemediationVersion();

          if (newComponent.getVersion().equals(eventRemediationVersion)) {
            // we can attribute the fixed policy violation to the pull request
            var pullRequestNumber = remediationEvent.getPullRequestNumber();
            telemetryData.put(PULL_REQUEST_NUMBER, pullRequestNumber);
            telemetryData.put(PULL_REQUEST_REMEDIATION_VERSION, eventRemediationVersion);
            var isGolden = componentHelper.isGoldenVersion(
                newComponent.getComponentIdentifier(),
                fixedPolicyViolation.getApplicationId()
            );
            telemetryData.put(PULL_REQUEST_IS_GOLDEN, isGolden);
          }
          else {
            log.debug("Remediation pull request event exists, but versions don't match: {} -> {}, event version is {}",
                fixedPolicyViolation.getComponentIdentifier(),
                newComponent.getComponentIdentifier(),
                eventRemediationVersion
            );
          }
        }

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
              .put(NEW_POLICY_VIOLATION_ID, newPolicyViolation.getId())
              .put(IS_LEGACY_VIOLATION, unwaivedPolicyViolation.isLegacyViolation());

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
              .put(POLICY_WAIVER_ID, policyWaiverId)
              .put(IS_LEGACY_VIOLATION, waivedPolicyViolation.isLegacyViolation());

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
      telemetryData.put(IS_LEGACY_VIOLATION, waivedPolicyViolation.isLegacyViolation());
      telemetryDataList.add(telemetryData);
    }
  }

  public void addTelemetryForConditionTypeViolation(
      PolicyViolation policyViolation,
      List<Component> components,
      List<Constraint> constraintsTelemetryData)
  {
    if (policyViolation != null) {
      TelemetryData telemetryData =
          createTelemetry(TelemetryPurpose.CONDITION_TYPE_VIOLATION, policyViolation, components)
              .put(POLICY_CONSTRAINTS, constraintsTelemetryData);

      telemetryDataList.add(telemetryData);
    }
  }

  /**
   * Records telemetry for existing, unchanged policy violations for auditing purposes.
   * Uses the CONDITION_TYPE_VIOLATION_AUDIT purpose.
   *
   * @param policyViolation         The policy violation to include in telemetry.
   * @param components              The associated component(s).
   * @param constraintsTelemetryData Formatted constraint data for telemetry.
   */
  public void addTelemetryForConditionTypeViolationAudit(
      final PolicyViolation policyViolation,
      final List<Component> components,
      final List<Constraint> constraintsTelemetryData)
  {
    if (policyViolation != null) {
      TelemetryData telemetryData =
          createTelemetry(TelemetryPurpose.CONDITION_TYPE_VIOLATION_AUDIT, policyViolation, components)
              .put(POLICY_CONSTRAINTS, constraintsTelemetryData);
      telemetryDataList.add(telemetryData);
    }
  }

  public Condition formatConditionForTelemetryData(ConditionFact conditionFact, String constraintFactOperatorName) {
    Condition condition = new Condition(conditionFact.getConditionTypeId(), constraintFactOperatorName);
    // conditionIndex may be null for violations created before trigger data feature was added
    if (conditionFact.getConditionIndex() != null) {
      condition.setConditionIndex(conditionFact.getConditionIndex());
    }
    TriggerReference conditionTriggerReference = conditionFact.getReference();
    if (conditionTriggerReference != null) {
      condition.setValue(conditionTriggerReference.getValue());
    }

    return condition;
  }

  public Constraint formatConstraintForTelemetryData(ConstraintFact cf, List<Condition> conditions) {
    LogicalOperator operator = null;
    try {
      operator = LogicalOperator.valueOf(cf.getOperatorName());
    }
    catch (Exception e) {
      log.debug("Unknown operator name '{}' in constraint fact, defaulting to AND", cf.getOperatorName());
      operator = LogicalOperator.AND;
    }
    Constraint constraint = new Constraint(cf.getConstraintId(), cf.getConstraintName(), operator);
    constraint.setConditions(conditions);
    return constraint;
  }

  public void addTelemetryForLegacyViolation(PolicyViolation legacyViolation, Component component) {
    if (legacyViolation != null) {
      TelemetryData telemetryData =
          createTelemetry(TelemetryPurpose.TIME_TO_LEGACY_VIOLATION, legacyViolation, component)
              .put(LEGACY_VIOLATION_TIME, timeOfPolicyEvaluation.getTime());

      telemetryDataList.add(telemetryData);
    }
  }

  /**
   * Records telemetry for existing, unchanged legacy violations for auditing purposes.
   * Uses the TIME_TO_LEGACY_VIOLATION_AUDIT purpose to enable tracking of specific legacy violations
   * that persist over time and detection of missing legacy violation data.
   *
   * This audit event is sent on every scan for unchanged legacy violations, allowing comparison
   * with original TIME_TO_LEGACY_VIOLATION events to identify missing data.
   *
   * @param legacyViolation The legacy policy violation to include in telemetry.
   * @param component       The associated component.
   */
  public void addTelemetryForLegacyViolationAudit(PolicyViolation legacyViolation, Component component) {
    if (legacyViolation != null) {
      TelemetryData telemetryData =
          createTelemetry(TelemetryPurpose.TIME_TO_LEGACY_VIOLATION_AUDIT, legacyViolation, component)
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
      // direct dependency value can be true / false / null (null means we don't know)
      telemetryData.put(DIRECT_DEPENDENCY, component.getDirectDependency());
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
