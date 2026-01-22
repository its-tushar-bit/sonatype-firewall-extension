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
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiSuggestedVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.telemetry.NonBreakingRecommendationTelemetryStats.SourceEndpoint;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class NonBreakingRecommendationTelemetryMetrics
{
  private static final Logger logger = LoggerFactory.getLogger(NonBreakingRecommendationTelemetryMetrics.class);

  private final TenantUtil tenantUtil;

  private final TenantReference<Map<NonBreakingRecommendationTelemetryStats, LongAdder>> stats = new TenantReference<>(
      ConcurrentHashMap::new);

  private TelemetryUtils telemetryUtils;

  @Inject
  public NonBreakingRecommendationTelemetryMetrics(TenantUtil tenantUtil, final TelemetryUtils telemetryUtils) {
    this.tenantUtil = tenantUtil;
    this.telemetryUtils = telemetryUtils;
  }

  public void collect(final ApiSuggestedVersionChangeOptionDTO suggestedVersionChange,
                      final Owner owner,
                      final SourceEndpoint source)
  {
    synchronized (tenantUtil.getTenantSlugForSynchronization()) {
      if (suggestedVersionChange == null) {
        // We don't have to send telemetry data if there is no suggested version change
        return;
      }
      ApiVersionChangeOptionType type = suggestedVersionChange.getType();
      String recommendedVersionComponentPackageUrl = null;
      String recommendedVersion = null;
      if (suggestedVersionChange.getData() != null &&
          suggestedVersionChange.getData().getComponent() != null &&
          suggestedVersionChange.getData().getComponent().componentIdentifier != null) {
        recommendedVersion = PackageUrlIdentifier.fromComponentIdentifier(
            suggestedVersionChange.getData().getComponent().componentIdentifier.toComponentIdentifier()
        ).getVersion();
        recommendedVersionComponentPackageUrl = suggestedVersionChange.getData().getComponent().packageUrl;
      }
      stats.get().computeIfAbsent(new NonBreakingRecommendationTelemetryStats(
          type,
          recommendedVersion,
          recommendedVersionComponentPackageUrl,
          source,
          owner), key -> new LongAdder()).increment();
    }
  }

  List<TelemetryData> computeStatsAndReset() {
    synchronized (tenantUtil.getTenantSlugForSynchronization()) {
      List<TelemetryData> telemetryList = new ArrayList<>();
      stats.get().forEach((stat, counter) -> {
            TelemetryData telemetryData = new TelemetryData(
                TelemetryPurpose.NON_BREAKING_VERSION_CHANGE_RECOMMENDATION);
            Map<String, Object> attributes = new HashMap<>();
            if (stat.recommendedNonBreakingVersionChangeOptionType() != null) {
              attributes.put("recommended_non_breaking_version_change_option_type",
                  stat.recommendedNonBreakingVersionChangeOptionType().getNameForTelemetry());
              attributes.put("recommended_non_breaking_version",
                  stat.recommendedNonBreakingVersion());
              attributes.put("recommended_non_breaking_version_package_url",
                  stat.recommendedNonBreakingVersionPackageUrl());
            }

            if (stat.sourceEndpoint().name() != null) {
              attributes.put("recommended_non_breaking_version_source_endpoint",
                  stat.sourceEndpoint().name());
            }

            Owner owner = stat.owner();

            if (OwnerType.APPLICATION == owner.getType()) {
              attributes.put("application_id", HdsClientAnalytics.obfuscate(owner.getId()));
              telemetryUtils.includeRealApplicationId(attributes, owner.getId());
            }
            else {
              logger.error("Owner type expected to be application but is not, owner type: {}, owner id: {}",
                  owner.getType(), owner.getId());
            }
            attributes.put("recommended_non_breaking_version_count_of_the_same_suggestion",
                counter.sumThenReset());
            telemetryData.setAttributes(attributes);
            telemetryList.add(telemetryData);
          }
      );
      stats.get().clear();
      return telemetryList;
    }
  }
}
