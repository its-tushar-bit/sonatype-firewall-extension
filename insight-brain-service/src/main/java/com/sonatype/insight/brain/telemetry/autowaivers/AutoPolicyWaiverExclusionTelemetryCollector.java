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
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

public class AutoPolicyWaiverExclusionTelemetryCollector
{
  public static final String APPLICATION_ID = "application_id";

  public static final String ORGANIZATION_ID = "organization_id";

  public static final String OWNER_TYPE = "owner_type";

  public static final String AUTO_POLICY_WAIVER_EXCLUSION_ID = "auto_policy_waiver_revocation_id";

  public static final String AUTO_POLICY_WAIVER_EXCLUSION_ACTION =
      "auto_policy_waiver_revocation_action";

  public static final String AUTO_POLICY_WAIVER_EXCLUSION_THREAT_LEVEL =
      "auto_policy_waiver_revocation_thread_level";

  public static final String AUTO_POLICY_WAIVER_EXCLUSION_AUTO_POLICY_WAIVER_ID =
      "auto_policy_waiver_revocation_auto_policy_waiver_id";

  private final List<TelemetryData> telemetryDataList = new ArrayList<>();

  private final TelemetryUtils telemetryUtils;

  public AutoPolicyWaiverExclusionTelemetryCollector(TelemetryUtils telemetryUtils) {
    this.telemetryUtils = telemetryUtils;
  }

  public List<TelemetryData> getTelemetryData() {
    return Collections.unmodifiableList(telemetryDataList);
  }

  public void addTelemetryForCreateAutoWaiverExclusion(
      final AutoPolicyWaiverExclusion autoPolicyWaiverExclusion,
      final Owner owner)
  {
    createTelemetry(
        AutoPolicyWaiverExclusionTelemetry.AutoPolicyWaiverExclusionAction.CREATE,
        autoPolicyWaiverExclusion,
        owner
    );
  }

  public void addTelemetryForDeleteAutoWaiverExclusion(
      final AutoPolicyWaiverExclusion autoPolicyWaiverExclusion,
      final Owner owner)
  {
    createTelemetry(
        AutoPolicyWaiverExclusionTelemetry.AutoPolicyWaiverExclusionAction.DELETE,
        autoPolicyWaiverExclusion,
        owner
    );
  }

  private TelemetryData createTelemetry(
      final AutoPolicyWaiverExclusionTelemetry.AutoPolicyWaiverExclusionAction action,
      final AutoPolicyWaiverExclusion autoPolicyWaiverExclusion,
      final Owner owner)
  {
    if (autoPolicyWaiverExclusion == null) {
      return null;
    }

    final TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.AUTO_POLICY_WAIVER_REVOCATIONS);
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
    attributes.put(AUTO_POLICY_WAIVER_EXCLUSION_ID, autoPolicyWaiverExclusion.getId());
    attributes.put(AUTO_POLICY_WAIVER_EXCLUSION_ACTION, action.name());
    attributes.put(AUTO_POLICY_WAIVER_EXCLUSION_THREAT_LEVEL, autoPolicyWaiverExclusion.getThreatLevel());
    attributes.put(AUTO_POLICY_WAIVER_EXCLUSION_AUTO_POLICY_WAIVER_ID,
        autoPolicyWaiverExclusion.getAutoPolicyWaiverId());

    telemetryData.setAttributes(attributes);
    telemetryDataList.add(telemetryData);
    return telemetryData;
  }
}
