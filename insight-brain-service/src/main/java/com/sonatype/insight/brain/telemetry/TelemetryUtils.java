/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.HashMap;
import java.util.Map;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

public final class TelemetryUtils
{
  private TelemetryUtils() {
  }

  public static TelemetryData buildThirdPartyScanTelemetryData(
      final String applicationPublicId,
      final Stage stage,
      final String thirdPartyScanType, final String userAgent)
  {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("application_id", applicationPublicId);
    attributes.put("stage_id", stage.getStageTypeId());
    attributes.put("source", thirdPartyScanType);
    if (userAgent != null) {
      attributes.put("user_agent", userAgent);
    }

    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.THIRD_PARTY_SCAN_USAGE);
    telemetryData.setAttributes(attributes);
    return telemetryData;
  }
}
