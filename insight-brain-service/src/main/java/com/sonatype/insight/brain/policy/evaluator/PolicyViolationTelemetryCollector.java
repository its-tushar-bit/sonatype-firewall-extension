/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.facts.ConditionTrigger;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.clm.dto.model.policy.TriggerReference.Type.SECURITY_VULNERABILITY_REFID;

public class PolicyViolationTelemetryCollector
{
  @VisibleForTesting
  static final String APPLICATION_ID = "application_id";

  static final String CONDITION_TYPE = "condition_type";

  static final String COUNT = "count";

  static final String ECOSYSTEM = "ecosystem";

  static final String COMPONENT_IDENTIFIER = "component_identifier";

  static final String FIX_TIME = "fix_time";

  static final String IS_SCM_ENABLED = "is_scm_enabled";

  static final String OPEN_TIME = "open_time";

  static final String POLICY_NAME = "policy_name";

  static final String POLICY_VIOLATION_ID = "policy_violation_id";

  static final String STAGE = "stage_id";

  static final String THREAT_CATEGORY = "threat_category";

  static final String THREAT_LEVEL = "threat_level";

  static final String TIME = "time";

  static final String UNWAIVE_TIME = "waive_time"; // yes, same as WAIVE_TIME for now

  static final String WAIVE_TIME = "waive_time";

  static final String LEGACY_VIOLATION_TIME = "legacy_violation_time";

  static final String INNERSOURCE_DEPENDENCY = "innersource_dependency";

  static final String DIRECT_DEPENDENCY = "direct_dependency";

  static final String WAIVER_EXPIRATION = "waiver_expiration";

  static final String POLICY_WAIVER_ID = "policy_waiver_id";

  static final String FIX_BY_VERSION_CHANGE = "fix_by_version_change";

  static final String CVE_NUMBER = "cve_number";

  static final String CVSS_SCORE = "cvss_score";

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final PolicyWaiverDAO policyWaiverDAO;

  private List<TelemetryData> telemetryDataList = new ArrayList<>();

  private final TelemetryUtils telemetryUtils;

  private boolean isScmEnabled;

  private Date timeOfPolicyEvaluation;

  public PolicyViolationTelemetryCollector(
      final PolicyWaiverDAO policyWaiverDAO,
      TelemetryUtils telemetryUtils,
      boolean isScmEnabled)
  {
    this.policyWaiverDAO = policyWaiverDAO;
    this.telemetryUtils = telemetryUtils;
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
        addComponentMetadata(telemetryData, components);
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
        addComponentMetadata(telemetryData, components);
        telemetryData.put(FIX_BY_VERSION_CHANGE, fixByVersionChange);

        telemetryData.put(FIX_TIME, timeOfPolicyEvaluation.getTime());
        telemetryDataList.add(telemetryData);
      }
    }
  }

  public void addTelemetryForUnwaivedViolation(
      final PolicyViolation unwaivedPolicyViolation,
      final Component component,
      final String oldPolicyWaiverId
  )
  {
    if (unwaivedPolicyViolation != null) {
      TelemetryData telemetryData =
          createTelemetry(TelemetryPurpose.TIME_TO_WAIVE_POLICY_VIOLATION, unwaivedPolicyViolation);
      addComponentMetadata(telemetryData, component);
      telemetryData.put(UNWAIVE_TIME, timeOfPolicyEvaluation.getTime());
      telemetryData.put(COUNT, -1);
      telemetryData.put(POLICY_WAIVER_ID, oldPolicyWaiverId);

      telemetryDataList.add(telemetryData);
    }
  }

  public void addTelemetryForWaivedViolation(PolicyViolation waivedPolicyViolation, Component component) {
    if (waivedPolicyViolation != null) {
      String policyWaiverId = waivedPolicyViolation.getPolicyWaiverId();
      String waiverExpirationInDays = getPolicyWaiverExpirationDays(policyWaiverId);

      TelemetryData telemetryData =
          createTelemetry(TelemetryPurpose.TIME_TO_WAIVE_POLICY_VIOLATION, waivedPolicyViolation);
      addComponentMetadata(telemetryData, component);
      telemetryData.put(WAIVER_EXPIRATION, waiverExpirationInDays);
      telemetryData.put(WAIVE_TIME, timeOfPolicyEvaluation.getTime());
      telemetryData.put(POLICY_WAIVER_ID, policyWaiverId);
      telemetryDataList.add(telemetryData);
    }
  }

  public void addTelemetryForConditionTypeViolation(
      PolicyViolation policyViolation,
      String conditionType,
      List<Component> components)
  {
    if (policyViolation != null) {
      TelemetryData telemetryData = createTelemetry(TelemetryPurpose.CONDITION_TYPE_VIOLATION, policyViolation)
          .put(CONDITION_TYPE, conditionType);
      addComponentMetadata(telemetryData, components);
      telemetryDataList.add(telemetryData);
    }
  }

  public void addTelemetryForLegacyViolation(PolicyViolation legacyViolation, Component component) {
    if (legacyViolation != null) {
      TelemetryData telemetryData =
          createTelemetry(TelemetryPurpose.TIME_TO_LEGACY_VIOLATION, legacyViolation);
      telemetryData.put(LEGACY_VIOLATION_TIME, timeOfPolicyEvaluation.getTime());
      addComponentMetadata(telemetryData, component);

      telemetryDataList.add(telemetryData);
    }
  }

  public void setTimeOfPolicyEvaluation(final Date timeOfPolicyEvaluation) {
    if (null == timeOfPolicyEvaluation) {
      throw new IllegalArgumentException("time of evaluation is required");
    }
    this.timeOfPolicyEvaluation = timeOfPolicyEvaluation;
  }

  private void addComponentMetadata(TelemetryData telemetryData, List<Component> components) {
    if (CollectionUtils.isNotEmpty(components)) {
      addComponentMetadata(telemetryData, components.get(0));
    }
  }

  private void addComponentMetadata(TelemetryData telemetryData, Component component) {
    if (component != null) {
      telemetryData.put(INNERSOURCE_DEPENDENCY, component.getInnerSourceData() != null);

      if (component.getDirectDependency() != null) {
        telemetryData.put(DIRECT_DEPENDENCY, component.getDirectDependency());
      }

      Optional<ComponentIdentifier> componentIdentifierOptional =
          Optional.ofNullable(component.getComponentIdentifier());

      componentIdentifierOptional
          .map(ComponentIdentifier::getFormat)
          .ifPresent(ecosystem -> telemetryData.put(ECOSYSTEM, ecosystem));

      componentIdentifierOptional
          .map(PackageUrlIdentifier::fromComponentIdentifier)
          .ifPresent(urlIdentifier -> {
            String componentName = urlIdentifier.getName();
            String componentVersion = urlIdentifier.getVersion();

            telemetryData.put(COMPONENT_IDENTIFIER, String.format("%s:%s", componentName, componentVersion));
          });
    }
  }

  private TelemetryData createTelemetry(TelemetryPurpose telemetryPurpose, PolicyViolation policyViolation) {
    final TelemetryData telemetryData = new TelemetryData(telemetryPurpose)
        .put(APPLICATION_ID, HdsClientAnalytics.obfuscate(policyViolation.getApplicationId()))
        .put(STAGE, policyViolation.getStageTypeId())
        .put(IS_SCM_ENABLED, isScmEnabled)
        .put(COUNT, 1)
        .put(OPEN_TIME, policyViolation.getOpenTime().getTime())
        .put(POLICY_NAME, policyViolation.getPolicyName())
        .put(POLICY_VIOLATION_ID, policyViolation.getId())
        .put(TIME, computeTimeBetween(policyViolation.getOpenTime(), timeOfPolicyEvaluation))
        .put(THREAT_CATEGORY, policyViolation.getThreatCategory().getName())
        .put(THREAT_LEVEL, policyViolation.getThreatLevel());
    telemetryUtils.includeRealApplicationId(telemetryData.getAttributes(), policyViolation.getApplicationId());
    addCVMetadata(telemetryData, policyViolation);

    return telemetryData;
  }

  private long computeTimeBetween(Date date1, Date date2) {
    return Math.abs(date1.getTime() - date2.getTime());
  }

  public void addCVMetadata(TelemetryData telemetryData, PolicyViolation policyViolation) {
    Optional.ofNullable(policyViolation.getConstraintFacts())
        .orElse(Collections.emptyList())
        .stream()
        .filter(Objects::nonNull)
        .flatMap(constraintFact -> Optional.ofNullable(constraintFact.getConditionFacts())
            .orElse(Collections.emptyList())
            .stream()
            .filter(Objects::nonNull))
        .filter(conditionFact -> {
          TriggerReference triggerReference = conditionFact.getReference();
          return triggerReference != null && triggerReference.getType() == SECURITY_VULNERABILITY_REFID;
        })
        .findFirst()
        .ifPresent(conditionFact -> {
          TriggerReference triggerReference = conditionFact.getReference();
          String cve = triggerReference.getValue();
          telemetryData.put(CVE_NUMBER, cve);

          String triggerJson = conditionFact.getTriggerJson();
          try {
            ConditionTrigger conditionTrigger = JsonUtils.parse(triggerJson, ConditionTrigger.class);
            Map<String, Object> trigger = (Map<String, Object>) conditionTrigger.getTrigger();
            telemetryData.put(CVSS_SCORE, trigger.get("severity"));
          }
          catch (IOException e) {
            log.error("An error occurred while trying to read the cvss score related to the policy violation", e);
          }
        });
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
      PolicyWaiver policyWaiver = policyWaiverDAO.getById(policyWaiverId);
      if (policyWaiver != null && policyWaiver.getExpiryTime() != null) {
        return String.valueOf(TimeUnit.MILLISECONDS
            .toDays(policyWaiver.getExpiryTime().getTime() - policyWaiver.getCreateTime().getTime()));
      }
    }
    return "never";
  }
}
