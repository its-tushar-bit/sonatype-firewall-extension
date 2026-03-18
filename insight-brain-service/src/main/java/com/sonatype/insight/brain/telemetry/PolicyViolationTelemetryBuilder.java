/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.license.LicenseNameProvider;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.ReachabilityStatus;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.model.policy.facts.ConditionTrigger;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.apache.commons.collections4.CollectionUtils;
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

  public static final String LICENSE_THREAT_GROUP_ATTRIBUTE = "license_threat_group";

  public static final String LICENSES_DECLARED = "licenses_declared";

  public static final String LICENSES_EFFECTIVE = "licenses_effective";

  public static final String LICENSES_OBSERVED = "licenses_observed";

  public static final String LICENSES_OVERRIDE_STATUS = "licenses_override_status";

  static final Pattern EXTRACT_LICENSE_THREAT_GROUP_PATTERN = Pattern.compile("License Threat Group is '([^']+)'");

  private final PolicyViolation policyViolation;

  private final TelemetryData telemetryData;

  private final TelemetryUtils telemetryUtils;

  private final LicenseNameProvider licenseNameProvider;

  private Component component;

  public PolicyViolationTelemetryBuilder(
      PolicyViolation policyViolation,
      TelemetryPurpose purpose,
      TelemetryUtils telemetryUtils,
      LicenseNameProvider licenseNameProvider)
  {
    this.policyViolation = policyViolation;
    this.telemetryData = new TelemetryData(purpose);
    this.telemetryUtils = telemetryUtils;
    this.licenseNameProvider = licenseNameProvider;
  }

  public PolicyViolationTelemetryBuilder forComponent(Component component) {
    this.component = component;
    return this;
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

  private void addVulnerabilityMetadataIfNeeded(
      TelemetryData telemetryData,
      PolicyViolation policyViolation)
  {
    if (!METADATA_THREAT_CATEGORIES.contains(policyViolation.getThreatCategory())) {
      return;
    }

    policyViolation.getConstraintFacts()
        .stream()
        .flatMap(constraintFact -> constraintFact.getConditionFacts().stream())
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

  private void addLicenseVulnerabilityMetadata(TelemetryData telemetryData, ConditionFact conditionFact) {
    if (!LicenseThreatGroupConditionType.ID.equals(conditionFact.getConditionTypeId())) {
      return;
    }

    var summary = conditionFact.getSummary();
    if (StringUtils.isBlank(summary)) {
      return;
    }

    var matcher = EXTRACT_LICENSE_THREAT_GROUP_PATTERN.matcher(summary);
    var licenseThreatGroup = matcher.find() ? matcher.group(1) : null;

    if (StringUtils.isNotBlank(licenseThreatGroup)) {
      telemetryData.put(LICENSE_THREAT_GROUP_ATTRIBUTE, licenseThreatGroup);

      if (null != component) {
        telemetryData.put(LICENSES_DECLARED, getDeclaredLicenses(component));
        telemetryData.put(LICENSES_EFFECTIVE, getEffectiveLicenses(component));
        telemetryData.put(LICENSES_OBSERVED, getObservedLicenses(component));
        telemetryData.put(LICENSES_OVERRIDE_STATUS, getOverrideStatus());
      }
    }
    else {
      log.warn("Unable to parse license threat group from condition fact summary: {}", conditionFact.getSummary());
    }
  }

  private void addSecurityVulnerabilityMetadata(TelemetryData telemetryData, ConditionFact conditionFact) {
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

  private String getDeclaredLicenses(Component component) {
    return joinNamedLicenses(component.getDeclaredMultiLicenseIds(), component.getDeclaredLicenseIds());
  }

  private String getEffectiveLicenses(Component component) {
    var effectiveLicenses = new LinkedHashSet<String>();
    if (CollectionUtils.isNotEmpty(component.getLicenseOverrideIds())) {
      effectiveLicenses.addAll(component.getLicenseOverrideIds());
    }
    else {
      effectiveLicenses.addAll(component.getDeclaredMultiLicenseIds());
      effectiveLicenses.addAll(component.getDeclaredLicenseIds());
      effectiveLicenses.addAll(component.getObservedMultiLicenseIds());
      effectiveLicenses.addAll(component.getObservedLicenseIds());
    }

    return joinNamedLicenses(effectiveLicenses, null);
  }

  public String getObservedLicenses(Component component) {
    return joinNamedLicenses(component.getObservedMultiLicenseIds(), component.getObservedLicenseIds());
  }

  public String getOverrideStatus() {
    var overrideStatus = component.getLicenseOverrideStatus();
    return null != overrideStatus ? overrideStatus.getName() : "";
  }

  private String joinNamedLicenses(Set<String> multiLicenseIds, Set<String> licenseIds) {
    multiLicenseIds = Objects.requireNonNullElse(multiLicenseIds, Collections.emptySet());
    licenseIds = Objects.requireNonNullElse(licenseIds, Collections.emptySet());

    Stream<String> multiLicenseStream = multiLicenseIds.stream()
        .map(id -> licenseNameProvider != null ? licenseNameProvider.getShortDisplayName(id, true) : id);

    Stream<String> licenseStream = licenseIds.stream()
        .map(id -> licenseNameProvider != null ? licenseNameProvider.getShortDisplayName(id, false) : id);

    return Stream.concat(multiLicenseStream, licenseStream).collect(Collectors.joining(", "));
  }
}
