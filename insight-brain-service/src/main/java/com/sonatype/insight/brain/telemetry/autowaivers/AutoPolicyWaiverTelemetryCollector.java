/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry.autowaivers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

public class AutoPolicyWaiverTelemetryCollector
{
  public static final String APPLICATION_ID = "application_id";

  public static final String ORGANIZATION_ID = "organization_id";

  public static final String OWNER_TYPE = "owner_type";

  public static final String AUTO_POLICY_WAIVER_ID = "auto_policy_waiver_id";

  public static final String AUTO_POLICY_WAIVER_ACTION = "auto_policy_waiver_action";

  public static final String AUTO_POLICY_WAIVER_REACHABILITY = "auto_policy_waiver_reachability";

  public static final String AUTO_POLICY_WAIVER_PATH_FORWARD = "auto_policy_waiver_path_forward";

  public static final String AUTO_POLICY_WAIVER_THREAT_LEVEL = "auto_policy_waiver_threat_level";

  public static final String AUTO_POLICY_WAIVER_POLICY_VIOLATION_ID =
      "auto_policy_waiver_policy_violation";

  private final List<TelemetryData> telemetryDataList = new ArrayList<>();

  private final TelemetryUtils telemetryUtils;

  public AutoPolicyWaiverTelemetryCollector(final TelemetryUtils telemetryUtils) {
    this.telemetryUtils = telemetryUtils;
  }

  public List<TelemetryData> getTelemetryData() {
    return Collections.unmodifiableList(telemetryDataList);
  }

  public void addTelemetryForApplyAutoWaiver(
      final AutoPolicyWaiver autoWaiver,
      final PolicyViolation policyViolation,
      final Owner owner)
  {
    createTelemetry(
        AutoPolicyWaiverTelemetry.AutoPolicyWaiverAction.APPLY,
        autoWaiver,
        policyViolation,
        owner);
  }

  public void addTelemetryForCreateAutoWaiver(final AutoPolicyWaiver autoWaiver, final Owner owner) {
    createTelemetry(
        AutoPolicyWaiverTelemetry.AutoPolicyWaiverAction.CREATE,
        autoWaiver,
        owner);
  }

  public void addTelemetryForUpdateAutoWaiver(final AutoPolicyWaiver autoWaiver, final Owner owner) {
    createTelemetry(
        AutoPolicyWaiverTelemetry.AutoPolicyWaiverAction.UPDATE,
        autoWaiver,
        owner);
  }

  public void addTelemetryForDeleteAutoWaiver(final AutoPolicyWaiver autoWaiver, final Owner owner) {
    createTelemetry(
        AutoPolicyWaiverTelemetry.AutoPolicyWaiverAction.DELETE,
        autoWaiver,
        owner);
  }

  private TelemetryData createTelemetry(
      final AutoPolicyWaiverTelemetry.AutoPolicyWaiverAction action,
      final AutoPolicyWaiver autoWaiver,
      final Owner owner)
  {
    return createTelemetry(action, autoWaiver, null, owner);
  }

  private TelemetryData createTelemetry(
      final AutoPolicyWaiverTelemetry.AutoPolicyWaiverAction action,
      final AutoPolicyWaiver autoWaiver,
      final PolicyViolation policyViolation,
      final Owner owner)
  {
    if (autoWaiver == null) {
      return null;
    }

    final TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.AUTO_POLICY_WAIVER);
    final Map<String, Object> attributes = new HashMap<>();
    final String ownerId = owner != null ? owner.getId() : null;
    final OwnerType ownerType = owner != null ? owner.getType() : null;

    if (OwnerType.APPLICATION.equals(ownerType)) {
      attributes.put(APPLICATION_ID, HdsClientAnalytics.obfuscate(ownerId));
      telemetryUtils.includeRealApplicationId(attributes, ownerId);
    }
    else if (OwnerType.ORGANIZATION.equals(ownerType)) {
      attributes.put(ORGANIZATION_ID, HdsClientAnalytics.obfuscate(ownerId));
    }

    attributes.put(OWNER_TYPE, ownerType);
    attributes.put(AUTO_POLICY_WAIVER_ID, autoWaiver.getId());
    attributes.put(AUTO_POLICY_WAIVER_ACTION, action.name());
    attributes.put(AUTO_POLICY_WAIVER_REACHABILITY, autoWaiver.hasReachability());
    attributes.put(AUTO_POLICY_WAIVER_PATH_FORWARD, autoWaiver.hasPathForward());
    attributes.put(AUTO_POLICY_WAIVER_THREAT_LEVEL, autoWaiver.getThreatLevel());
    attributes.put(AUTO_POLICY_WAIVER_POLICY_VIOLATION_ID, policyViolation != null ? policyViolation.getHash() : null);

    telemetryData.setAttributes(attributes);
    telemetryDataList.add(telemetryData);
    return telemetryData;
  }
}
