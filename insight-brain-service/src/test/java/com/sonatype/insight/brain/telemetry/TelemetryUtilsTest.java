/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.telemetry.model.TelemetryData;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class TelemetryUtilsTest
{
  @Test
  public void test_buildThirdPartyScanTelemetryData() {

    TelemetryData telemetryData =
        TelemetryUtils.buildThirdPartyScanTelemetryData("appId", new Stage(Stage.ID_RELEASE), "cli", "agent");
    assertThat(telemetryData.getAttributes())
        .contains(entry("application_id", "appId"), entry("stage_id", "release"), entry("source", "cli"),
            entry("user_agent", "agent"));
  }
}
