/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.ReachabilityStatus;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.policy.facts.ConditionTrigger;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyViolationTelemetryBuilder
{
  private static final Logger log = LoggerFactory.getLogger(PolicyViolationTelemetryBuilder.class);

  private static final Set<PolicyThreatCategory> METADATA_THREAT_CATEGORIES =
      Set.of(PolicyThreatCategory.SECURITY, PolicyThreatCategory.LICENSE);

  static final String APPLICATION_ID = "application_id";

  static final String COUNT = "count";

  static final String ECOSYSTEM = "ecosystem";

  static final String COMPONENT_IDENTIFIER = "component_identifier";

  static final String COMPONENT_NAMESPACE = "component_namespace";

  static final String COMPONENT_NAME = "component_name";

  static final String COMPONENT_VERSION = "component_version";

  static final String FIX_TIME = "fix_time";

  static final String IS_SCM_ENABLED = "is_scm_enabled";

  static final String OPEN_TIME = "open_time";

  static final String POLICY_NAME = "policy_name";

  static final String POLICY_VIOLATION_ID = "policy_violation_id";

  static final String STAGE = "stage_id";

  static final String THREAT_CATEGORY = "threat_category";

  static final String THREAT_LEVEL = "threat_level";

  static final String TIME = "time";

  static final String WAIVE_TIME = "waive_time";

  static final String LEGACY_VIOLATION_TIME = "legacy_violation_time";

  static final String REACHABILITY_STATUS = "reachability_status";

  public static final String CVE_NUMBER = "cve_number";

  public static final String CVSS_SCORE = "cvss_score";

  public static final String LICENSE_THREAT_GROUP_CONDITION_TYPE = "License Threat Group";

  public static final String LICENSE_THREAT_GROUP_ATTRIBUTE = "license_threat_group";

  static final Pattern LICENSE_THREAT_GROUP_PATTERN = Pattern.compile("License Threat Group is '([^']+)'");

  private final PolicyViolation policyViolation;

  private final TelemetryData telemetryData;

  private final TelemetryUtils telemetryUtils;

  public PolicyViolationTelemetryBuilder(
      PolicyViolation policyViolation,
      TelemetryPurpose purpose,
      TelemetryUtils telemetryUtils)
  {
    this.policyViolation = policyViolation;
    this.telemetryData = new TelemetryData(purpose);
    this.telemetryUtils = telemetryUtils;
  }

  public PolicyViolationTelemetryBuilder withComponentIdentifier(ComponentIdentifier componentIdentifier) {
    if (null != componentIdentifier) {
      telemetryData.put(ECOSYSTEM, componentIdentifier.getFormat());
      telemetryData.put(COMPONENT_IDENTIFIER, componentIdentifier.toString());

      PackageUrlIdentifier urlIdentifier = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);
      telemetryData.put(COMPONENT_NAMESPACE, urlIdentifier.getNamespace());
      telemetryData.put(COMPONENT_NAME, urlIdentifier.getName());
      telemetryData.put(COMPONENT_VERSION, urlIdentifier.getVersion());
    }

    return this;
  }

  public PolicyViolationTelemetryBuilder withFixTime(Date fixTime) {
    telemetryData.put(FIX_TIME, fixTime);
    return this;
  }

  public PolicyViolationTelemetryBuilder withLegacyViolationTime(Date legacyViolationTime) {
    telemetryData.put(LEGACY_VIOLATION_TIME, legacyViolationTime);
    return this;
  }

  public PolicyViolationTelemetryBuilder withScmEnabled(boolean isScmEnabled) {
    telemetryData.put(IS_SCM_ENABLED, isScmEnabled);
    return this;
  }

  public PolicyViolationTelemetryBuilder withTime(long time) {
    telemetryData.put(TIME, time);
    return this;
  }

  public PolicyViolationTelemetryBuilder withWaiveTime(Date waiveTime) {
    telemetryData.put(WAIVE_TIME, waiveTime);
    return this;
  }

  public TelemetryData build() {

    ReachabilityStatus reachabilityStatus = policyViolation.getReachabilityStatus();

    telemetryData
        .put(APPLICATION_ID, HdsClientAnalytics.obfuscate(policyViolation.getApplicationId()))
        .put(COUNT, 1)
        .put(OPEN_TIME, policyViolation.getOpenTime().getTime())
        .put(POLICY_NAME, policyViolation.getPolicyName())
        .put(POLICY_VIOLATION_ID, policyViolation.getId())
        .put(STAGE, policyViolation.getStageTypeId())
        .put(THREAT_CATEGORY, policyViolation.getThreatCategory().getName())
        .put(THREAT_LEVEL, policyViolation.getThreatLevel())
        .put(REACHABILITY_STATUS, reachabilityStatus == null ? null : reachabilityStatus.getName());

    telemetryUtils.includeRealApplicationId(telemetryData.getAttributes(), policyViolation.getApplicationId());
    addVulnerabilityMetadataIfNeeded(telemetryData, policyViolation);

    return telemetryData;
  }

  private static void addVulnerabilityMetadataIfNeeded(
      TelemetryData telemetryData,
      PolicyViolation policyViolation)
  {
    if (!METADATA_THREAT_CATEGORIES.contains(policyViolation.getThreatCategory())) {
      return;
    }

    policyViolation.getConstraintFacts().stream().flatMap(constraintFact -> constraintFact.getConditionFacts().stream())
        .forEach(conditionFact -> {
          switch (policyViolation.getThreatCategory()) {
            case SECURITY:
              addSecurityVulnerabilityMetadata(telemetryData, conditionFact);
              break;

            case LICENSE:
              addLicenseVulnerabilityMetadata(telemetryData, conditionFact);
              break;

            default:
              break;
          }
        });
  }

  private static void addLicenseVulnerabilityMetadata(TelemetryData telemetryData, ConditionFact conditionFact) {
    if (!LicenseThreatGroupConditionType.ID.equals(conditionFact.getConditionTypeId())) {
      return;
    }

    var summary = conditionFact.getSummary();
    if (StringUtils.isBlank(summary)) {
      return;
    }

    var matcher = LICENSE_THREAT_GROUP_PATTERN.matcher(summary);
    var licenseThreatGroup = matcher.find() ? matcher.group(1) : null;

    if (StringUtils.isNotBlank(licenseThreatGroup)) {
      telemetryData.put(LICENSE_THREAT_GROUP_ATTRIBUTE, licenseThreatGroup);
    }
    else {
      log.warn("Unable to parse license threat group from condition fact summary: {}", conditionFact.getSummary());
    }
  }

  private static void addSecurityVulnerabilityMetadata(TelemetryData telemetryData, ConditionFact conditionFact) {
    String triggerJson = conditionFact.getTriggerJson();
    if (triggerJson != null) {
      try {
        ConditionTrigger conditionTrigger = JsonUtils.parse(triggerJson, ConditionTrigger.class);
        Map<String, Object> trigger = (Map<String, Object>) conditionTrigger.getTrigger();
        String refId = (String) trigger.get("refId");
        Object severity = trigger.get("severity");
        if (refId != null && severity != null) {
          telemetryData.put(CVE_NUMBER, refId);
          telemetryData.put(CVSS_SCORE, severity);
        }
      }
      catch (IOException e) {
        log.error("An error occurred while trying to read the cvss score related to the policy violation", e);
      }
    }
  }
}
