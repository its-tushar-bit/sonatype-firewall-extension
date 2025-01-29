/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry.autowaivers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.insight.brain.telemetry.autowaivers.AutoPolicyWaiverTelemetry.AutoPolicyWaiverAction;

@Named
@Singleton
public class AutoPolicyWaiverTelemetryMetrics
{
  public static final String APPLICATION_ID = "application_id";

  public static final String ORGANIZATION_ID = "organization_id";

  public static final String OWNER_TYPE = "owner_type";

  public static final String AUTO_POLICY_WAIVER_ID = "auto_policy_waiver_id";

  public static final String AUTO_POLICY_WAIVER_ACTION = "auto_policy_waiver_action";

  public static final String AUTO_POLICY_WAIVER_REACHABILITY = "auto_policy_waiver_reachability";

  public static final String AUTO_POLICY_WAIVER_PATH_FORWARD = "auto_policy_waiver_path_forward";

  public static final String AUTO_POLICY_WAIVER_THREAT_LEVEL = "auto_policy_waiver_threat_level";

  public static final String AUTO_POLICY_WAIVER_COUNT_FOR_SAME_ACTION =
      "auto_policy_waiver_count_of_the_same_action";

  public static final String AUTO_POLICY_WAIVER_POLICY_VIOLATION_ID =
      "auto_policy_waiver_policy_violation";

  private final TenantUtil tenantUtil;

  private TelemetryUtils telemetryUtils;

  private final TenantReference<Map<AutoPolicyWaiverTelemetry, LongAdder>> stats =
      new TenantReference<>(ConcurrentHashMap::new);

  @Inject
  public AutoPolicyWaiverTelemetryMetrics(final TenantUtil tenantUtil, final TelemetryUtils telemetryUtils) {
    this.tenantUtil = tenantUtil;
    this.telemetryUtils = telemetryUtils;
  }

  public void collect(final AutoPolicyWaiver autoPolicyWaiver,
                      final OwnerType ownerType, final AutoPolicyWaiverAction autoWaiverAction,
                      final PolicyViolation policyViolation)
  {

    synchronized (tenantUtil.getTenantSlugForSynchronization()) {
      stats.get().computeIfAbsent(new AutoPolicyWaiverTelemetry(autoPolicyWaiver,
              ownerType, autoWaiverAction, policyViolation), k -> new LongAdder())
          .increment();
    }
  }

  public List<TelemetryData> computeStatsAndReset() {
    synchronized (tenantUtil.getTenantSlugForSynchronization()) {
      List<TelemetryData> telemetryDataList = new ArrayList<>();
      stats.get().forEach((stat, counter) -> {
        TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.AUTO_POLICY_WAIVER);
        Map<String, Object> attributes = new HashMap<>();

        if (OwnerType.APPLICATION.equals(stat.ownerType())) {
          attributes.put(APPLICATION_ID, HdsClientAnalytics.obfuscate(stat.ownerId()));
          telemetryUtils.includeRealApplicationId(attributes, stat.ownerId());
        }
        else if (OwnerType.ORGANIZATION.equals(stat.ownerType())) {
          attributes.put(ORGANIZATION_ID, HdsClientAnalytics.obfuscate(stat.ownerId()));
        }

        attributes.put(OWNER_TYPE, stat.ownerType());
        attributes.put(AUTO_POLICY_WAIVER_ID, stat.toAutoPolicyWaiver().getId());
        attributes.put(AUTO_POLICY_WAIVER_ACTION, stat.action().toString());
        attributes.put(AUTO_POLICY_WAIVER_COUNT_FOR_SAME_ACTION,
            counter.sumThenReset());
        attributes.put(AUTO_POLICY_WAIVER_REACHABILITY, stat.reachable());
        attributes.put(AUTO_POLICY_WAIVER_PATH_FORWARD, stat.pathForward());
        attributes.put(AUTO_POLICY_WAIVER_THREAT_LEVEL, stat.threatLevel());
        attributes.put(AUTO_POLICY_WAIVER_POLICY_VIOLATION_ID,
            stat.policyViolation() != null ? stat.policyViolation().getHash() : null);

        telemetryData.setAttributes(attributes);
        telemetryDataList.add(telemetryData);
      });
      stats.get().clear();
      return telemetryDataList;
    }
  }
}
