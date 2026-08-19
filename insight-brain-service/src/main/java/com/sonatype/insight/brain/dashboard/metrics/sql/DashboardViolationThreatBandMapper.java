/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.metrics.sql;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO.RawThreatLevelCount;
import com.sonatype.insight.brain.utils.ThreatLevel;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class DashboardViolationThreatBandMapper
{
  private static final Logger log = LoggerFactory.getLogger(DashboardViolationThreatBandMapper.class);

  private static final String FALLBACK_BAND = "low";

  public Map<String, Long> map(List<RawThreatLevelCount> rawCounts) {
    Map<String, int[]> bands = ThreatLevel.searchAggregationBands();
    Map<String, Long> result = new LinkedHashMap<>();
    bands.keySet().forEach(key -> result.put(key, 0L));
    for (RawThreatLevelCount raw : rawCounts) {
      String band = bands.entrySet()
          .stream()
          .filter(entry -> raw.threatLevel() >= entry.getValue()[0]
              && raw.threatLevel() <= entry.getValue()[1])
          .map(Map.Entry::getKey)
          .findFirst()
          .orElseGet(() -> {
            // searchAggregationBands is open-ended for short values today; keep a defensive
            // fallback so a future band change or corrupt stored level cannot blank the tile.
            log.warn(
                "Unrecognized threat level {} in dashboard violation band mapping; counting under {}",
                raw.threatLevel(),
                FALLBACK_BAND);
            return FALLBACK_BAND;
          });
      result.merge(band, raw.count(), Long::sum);
    }
    return Collections.unmodifiableMap(result);
  }
}
