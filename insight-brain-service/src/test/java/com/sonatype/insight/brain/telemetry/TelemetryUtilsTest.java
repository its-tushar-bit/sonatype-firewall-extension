/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.HashMap;
import java.util.Map;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.telemetry.model.TelemetryData;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class TelemetryUtilsTest
{
  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Test
  public void test_buildThirdPartyScanTelemetryData() {

    TelemetryData telemetryData =
        TelemetryUtils.buildThirdPartyScanTelemetryData("appId", new Stage(Stage.ID_RELEASE), "cli", "agent");
    assertThat(telemetryData.getAttributes())
        .contains(entry("application_id", "appId"), entry("stage_id", "release"), entry("source", "cli"),
            entry("user_agent", "agent"));
  }

  @Test
  public void test_buildApplicationEvaluationTelemetryData_noComponents_noUA_noInstanceId() {
    // when:
    TelemetryData telemetryData = TelemetryUtils.buildApplicationEvaluationTelemetryData(
        "appId", "build", ScanTriggerType.CLI, null, null, new HashMap<>());
    // then:
    assertThat(telemetryData.getAttributes()).contains(
        entry("application_id", HdsClientAnalytics.obfuscate("appId")),
        entry("stage_id", "build"),
        entry("scan_trigger_type", "CLI"),
        entry("number_of_components", "0")
    );
    assertThat(telemetryData.getAttributes()).doesNotContainKeys(
        "client_id", "client_instance_id", "deployment_type"
    );
  }

  @Test
  public void test_buildApplicationEvaluationTelemetryData_noComponents_invalidUA_noInstanceId() {
    // when:
    TelemetryData telemetryData = TelemetryUtils.buildApplicationEvaluationTelemetryData(
        "appId", "build", ScanTriggerType.CLI, "user agent", null, null);
    // then:
    assertThat(telemetryData.getAttributes()).contains(
        entry("application_id", HdsClientAnalytics.obfuscate("appId")),
        entry("stage_id", "build"),
        entry("scan_trigger_type", "CLI"),
        entry("number_of_components", "0")
    );
    assertThat(telemetryData.getAttributes()).doesNotContainKeys(
        "client_id", "client_instance_id", "deployment_type"
    );
  }

  @Test
  public void test_buildApplicationEvaluationTelemetryData_noComponents_validUA_instanceId() {
    // given:
    String ua = "Sonatype_CLM_CI_Jenkins/3.13 (Java 1.8.0_201; Linux 5.4.144; Jenkins 2.319.2)";
    // when:
    TelemetryData telemetryData = TelemetryUtils.buildApplicationEvaluationTelemetryData(
        "appId", "build", ScanTriggerType.CLI, ua, "abc", null);
    // then:
    assertThat(telemetryData.getAttributes()).contains(
        entry("application_id", HdsClientAnalytics.obfuscate("appId")),
        entry("stage_id", "build"),
        entry("scan_trigger_type", "CLI"),
        entry("number_of_components", "0"),
        entry("client_id", "Sonatype_CLM_CI_Jenkins"),
        entry("client_version", "3.13"),
        entry("client_runtime", "Java"),
        entry("client_runtime_version", "1.8.0_201"),
        entry("client_os_name", "Linux"),
        entry("client_os_version", "5.4.144"),
        entry("client_other", "Jenkins 2.319.2"),
        entry("client_instance_id", "abc")
    );
    assertThat(telemetryData.getAttributes()).doesNotContainKeys("deployment_type");
  }

  @Test
  public void test_buildApplicationEvaluationTelemetryData_Components_noUA_noInstanceId() {
    // given:
    Map<String, Long> map = new HashMap<>();
    map.put("maven", 3L);
    map.put("npm", 2L);
    // when:
    TelemetryData telemetryData = TelemetryUtils.buildApplicationEvaluationTelemetryData(
        "appId", "build", ScanTriggerType.CLI, null, null, map);
    // then:
    assertThat(telemetryData.getAttributes()).contains(
        entry("application_id", HdsClientAnalytics.obfuscate("appId")),
        entry("stage_id", "build"),
        entry("scan_trigger_type", "CLI"),
        entry("number_of_maven_components", "3"),
        entry("number_of_npm_components", "2"),
        entry("number_of_components", "5")
    );
    assertThat(telemetryData.getAttributes()).doesNotContainKeys(
        "client_id", "client_instance_id", "deployment_type"
    );
  }

  @Test
  public void test_buildApplicationEvaluationTelemetryData_iqHostProvided() {
    // given:
    environmentVariables.set("SONATYPE_INTERNAL_HOST_SYSTEM", "Docker");
    // when:
    TelemetryData telemetryData = TelemetryUtils.buildApplicationEvaluationTelemetryData(
        "appId", "build", ScanTriggerType.CLI, null, null, null);
    // then:
    assertThat(telemetryData.getAttributes()).contains(
        entry("application_id", HdsClientAnalytics.obfuscate("appId")),
        entry("stage_id", "build"),
        entry("scan_trigger_type", "CLI"),
        entry("deployment_type", "Docker")
    );
  }
}
