/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

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
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion;
import com.sonatype.insight.brain.telemetry.AutoPolicyWaiverExclusionTelemetry.AutoPolicyWaiverExclusionAction;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

@Named
@Singleton
public class AutoPolicyWaiverExclusionTelemetryMetrics
{
  public static final String APPLICATION_ID = "application_id";

  public static final String ORGANIZATION_ID = "organization_id";

  public static final String OWNER_TYPE = "owner_type";

  public static final String AUTO_POLICY_WAIVER_EXCLUSION_ID = "auto_policy_waiver_revocation_id";

  public static final String AUTO_POLICY_WAIVER_EXCLUSION_ACTION =
      "auto_policy_waiver_revocation_action";

  public static final String AUTO_POLICY_WAIVER_EXCLUSION_THREAD_LEVEL =
      "auto_policy_waiver_revocation_thread_level";

  public static final String AUTO_POLICY_WAIVER_EXCLUSION_AUTO_POLICY_WAIVER_ID =
      "auto_policy_waiver_revocation_auto_policy_waiver_id";

  public static final String AUTO_POLICY_WAIVER_EXCLUSION_COUNT_FOR_SAME_ACTION =
      "auto_policy_waiver_revocation_count_for_same_action";

  private final TenantUtil tenantUtil;

  private final TenantReference<Map<AutoPolicyWaiverExclusionTelemetry, LongAdder>> stats =
      new TenantReference<>(ConcurrentHashMap::new);

  private TelemetryUtils telemetryUtils;

  @Inject
  public AutoPolicyWaiverExclusionTelemetryMetrics(final TenantUtil tenantUtil, final TelemetryUtils telemetryUtils) {
    this.tenantUtil = tenantUtil;
    this.telemetryUtils = telemetryUtils;
  }

  public void collect(
      final AutoPolicyWaiverExclusion autoPolicyWaiverExclusion, final OwnerType ownerType,
      final AutoPolicyWaiverExclusionAction autoPolicyWaiverExclusionAction)
  {
    synchronized (tenantUtil.getTenantSlugForSynchronization()) {
      stats.get().computeIfAbsent(new AutoPolicyWaiverExclusionTelemetry(autoPolicyWaiverExclusion, ownerType,
              autoPolicyWaiverExclusionAction), k -> new LongAdder())
          .increment();
    }
  }

  public List<TelemetryData> computeStatsAndReset() {
    synchronized (tenantUtil.getTenantSlugForSynchronization()) {
      List<TelemetryData> telemetryDataList = new ArrayList<>();
      stats.get().forEach((stat, counter) -> {
        TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.AUTO_POLICY_WAIVER_REVOCATIONS);
        Map<String, Object> attributes = new HashMap<>();

        if (OwnerType.APPLICATION.equals(stat.ownerType())) {
          attributes.put(APPLICATION_ID, HdsClientAnalytics.obfuscate(stat.ownerId()));
          telemetryUtils.includeRealApplicationId(attributes, stat.ownerId());
        }
        else if (OwnerType.ORGANIZATION.equals(stat.ownerType())) {
          attributes.put(ORGANIZATION_ID, HdsClientAnalytics.obfuscate(stat.ownerId()));
        }

        attributes.put(OWNER_TYPE, stat.ownerType());
        attributes.put(AUTO_POLICY_WAIVER_EXCLUSION_ID, stat.autoPolicyWaiverExclusionId());
        attributes.put(AUTO_POLICY_WAIVER_EXCLUSION_ACTION, stat.action());
        attributes.put(AUTO_POLICY_WAIVER_EXCLUSION_THREAD_LEVEL, stat.threadLevel());
        attributes.put(AUTO_POLICY_WAIVER_EXCLUSION_AUTO_POLICY_WAIVER_ID, stat.autoPolicyWaiverId());
        attributes.put(AUTO_POLICY_WAIVER_EXCLUSION_COUNT_FOR_SAME_ACTION, counter.sumThenReset());

        telemetryData.setAttributes(attributes);
        telemetryDataList.add(telemetryData);
      });
      stats.get().clear();
      return telemetryDataList;
    }
  }
}
