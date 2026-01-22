/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.sbom.SbomComponentInfoTelemetry;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetry.ReleaseQuarantineType;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetry.RepositoryComponentTelemetryEventType;
import com.sonatype.insight.brain.testing.BrainInjectedTest;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetryCreator.POLICY_VIOLATION_TELEMETRY;
import static com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetryCreator.REPOSITORY_COMPONENT_TELEMETRY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

public class TelemetryUtilsTest
    extends BrainInjectedTest
{
  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Inject
  private TelemetryUtils telemetryUtils;

  @Inject
  private TelemetryDataObfuscator telemetryDataObfuscator;

  @Inject
  private Configuration configuration;

  @Inject
  private ApiConfigurationService configurationService;

  @Test
  public void test_buildThirdPartyScanTelemetryData() {
    TelemetryData telemetryData =
        telemetryUtils.buildThirdPartyScanTelemetryData("appId", new Stage(Stage.ID_RELEASE), "cli",
            ScanTriggerType.SBOM_UI, "agent");
    assertThat(telemetryData.getAttributes()).contains(
        entry("application_id", HdsClientAnalytics.obfuscate("appId")),
        entry("real_application_id", "appId"),
        entry("stage_id", "release"), entry("source", "cli"), entry("scan_type", "SBOM_UI"),
        entry("user_agent", "agent"));
  }

  @Test
  public void test_buildThirdPartyScanComponentInfoTelemetryData() {
    SbomComponentInfoTelemetry componentInfoTelemetry =
        new SbomComponentInfoTelemetry(0, 1, 2, 3, 4);
    componentInfoTelemetry.incrementPurlCount();
    componentInfoTelemetry.incrementCpeCount();
    componentInfoTelemetry.incrementSwidCount();
    componentInfoTelemetry.incrementHashCount();
    componentInfoTelemetry.incrementCoordinateCount();
    componentInfoTelemetry.incrementValidLicensesCount();
    componentInfoTelemetry.incrementInvalidLicensesCount();
    componentInfoTelemetry.incrementInvalidLicensesCount();
    componentInfoTelemetry.setValidationErrorsCount(2);
    componentInfoTelemetry.setHasDependencies(true);
    TelemetryData telemetryData =
        telemetryUtils.buildThirdPartyScanComponentInfoTelemetryData(componentInfoTelemetry, true, true);
    SbomComponentInfoTelemetry componentIdCounts =
        (SbomComponentInfoTelemetry) telemetryData.getAttributes().get("sbom_data_summary");
    assertThat(componentIdCounts.getPurlCount()).isEqualTo(1);
    assertThat(componentIdCounts.getCpeCount()).isEqualTo(2);
    assertThat(componentIdCounts.getSwidCount()).isEqualTo(3);
    assertThat(componentIdCounts.getHashCount()).isEqualTo(4);
    assertThat(componentIdCounts.getValidationErrorsCount()).isEqualTo(2);
    assertThat(componentIdCounts.getCoordinateCount()).isEqualTo(5);
    assertThat(componentIdCounts.getHasDependencies()).isTrue();
    assertThat(componentIdCounts.getValidLicensesCount()).isEqualTo(1);
    assertThat(componentIdCounts.getInvalidLicensesCount()).isEqualTo(2);
    assertThat(telemetryData.getAttributes().get("is_skip_sbom_validation_feature_flag_enabled")).isEqualTo(true);
    assertThat(telemetryData.getAttributes().get("is_sbom_valid")).isEqualTo(true);
  }

  @Test
  public void test_buildApplicationEvaluationTelemetryData_noComponents_noUA_noInstanceId() {
    // when:
    TelemetryData telemetryData = telemetryUtils.buildApplicationEvaluationTelemetryData(
        "scanId", "appId", "build", ScanTriggerType.CLI, null, null, new HashMap<>());
    // then:
    assertThat(telemetryData.getAttributes()).contains(
        entry("scan_id", "scanId"),
        entry("application_id", HdsClientAnalytics.obfuscate("appId")),
        entry("stage_id", "build"),
        entry("scan_trigger_type", "CLI"),
        entry("number_of_components", "0")
    );
    assertThat(telemetryData.getAttributes()).doesNotContainKeys(
        "client_id", "client_instance_id", "deployment_type", "ide_theme"
    );
  }

  @Test
  public void test_buildApplicationEvaluationTelemetryData_noComponents_noUA_noInstanceId_IEREnabled() {
    // When
    TelemetryData telemetryData = telemetryUtils.buildApplicationEvaluationTelemetryData(
        "scanId", "appId", "build", ScanTriggerType.CLI, null, null, new HashMap<>());

    // Then
    assertThat(telemetryData.getAttributes()).contains(
        entry("scan_id", "scanId"),
        entry("application_id", HdsClientAnalytics.obfuscate("appId")),
        entry("real_application_id", "appId"),
        entry("stage_id", "build"),
        entry("scan_trigger_type", "CLI"),
        entry("number_of_components", "0")
    );
    assertThat(telemetryData.getAttributes()).doesNotContainKeys(
        "client_id", "client_instance_id", "deployment_type", "ide_theme"
    );
  }

  @Test
  public void test_buildApplicationEvaluationTelemetryData_noComponents_invalidUA_noInstanceId() {
    // when:
    TelemetryData telemetryData = telemetryUtils.buildApplicationEvaluationTelemetryData(
        "scanId", "appId", "build", ScanTriggerType.CLI, "user agent", null,
        Collections.singletonMap("component_counts", null));

    // then:
    assertThat(telemetryData.getAttributes()).contains(
        entry("scan_id", "scanId"),
        entry("application_id", HdsClientAnalytics.obfuscate("appId")),
        entry("stage_id", "build"),
        entry("scan_trigger_type", "CLI"),
        entry("number_of_components", "0")
    );
    assertThat(telemetryData.getAttributes()).doesNotContainKeys(
        "client_id", "client_instance_id", "deployment_type", "ide_theme"
    );
  }

  @Test
  public void test_buildApplicationEvaluationTelemetryData_noComponents_validUA_instanceId() {
    // given:
    String ua = "Sonatype_CLM_CI_Jenkins/3.13 (Java 1.8.0_201; Linux 5.4.144; Jenkins 2.319.2)";
    // when:
    TelemetryData telemetryData = telemetryUtils.buildApplicationEvaluationTelemetryData(
        "scanId", "appId", "build", ScanTriggerType.CLI, ua, "abc", Collections.singletonMap("component_counts", null));
    // then:
    assertThat(telemetryData.getAttributes()).contains(
        entry("scan_id", "scanId"),
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
    assertThat(telemetryData.getAttributes()).doesNotContainKeys("deployment_type", "ide_theme");
  }

  @Test
  public void test_buildApplicationEvaluationTelemetryData_Components_noUA_noInstanceId() {
    // given:
    Map<String, Long> map = new HashMap<>();
    map.put("maven", 3L);
    map.put("npm", 2L);
    // when:
    TelemetryData telemetryData = telemetryUtils.buildApplicationEvaluationTelemetryData(
        "scanId", "appId", "build", ScanTriggerType.CLI, null, null, Collections.singletonMap("component_counts", map));
    // then:
    assertThat(telemetryData.getAttributes()).contains(
        entry("scan_id", "scanId"),
        entry("application_id", HdsClientAnalytics.obfuscate("appId")),
        entry("stage_id", "build"),
        entry("scan_trigger_type", "CLI"),
        entry("number_of_maven_components", "3"),
        entry("number_of_npm_components", "2"),
        entry("number_of_components", "5")
    );
    assertThat(telemetryData.getAttributes()).doesNotContainKeys(
        "client_id", "client_instance_id", "deployment_type", "ide_theme"
    );
  }

  @Test
  public void test_buildApplicationEvaluationTelemetryData_AcceptsIntegerComponentCounts() {
    // given:
    Map<String, Integer> componentCounts = new HashMap<>();
    componentCounts.put("maven", 15);
    componentCounts.put("npm", 10);
    // when:
    TelemetryData telemetryData = telemetryUtils.buildApplicationEvaluationTelemetryData(
        "scanId", "appId", "build", ScanTriggerType.CLI, null, null,
        Collections.singletonMap("component_counts", componentCounts));
    // then:
    assertThat(telemetryData.getAttributes()).contains(
        entry("scan_id", "scanId"),
        entry("application_id", HdsClientAnalytics.obfuscate("appId")),
        entry("stage_id", "build"),
        entry("scan_trigger_type", "CLI"),
        entry("number_of_maven_components", "15"),
        entry("number_of_npm_components", "10"),
        entry("number_of_components", "25")
    );
    assertThat(telemetryData.getAttributes()).doesNotContainKeys(
        "client_id", "client_instance_id", "deployment_type", "ide_theme"
    );
  }

  @Test
  public void test_buildApplicationEvaluationTelemetryData_iqHostProvided() {
    // given:
    environmentVariables.set("SONATYPE_INTERNAL_HOST_SYSTEM", "Docker");
    // when:
    TelemetryData telemetryData = telemetryUtils.buildApplicationEvaluationTelemetryData(
        "scanId", "appId", "build", ScanTriggerType.CLI, null, null,
        Collections.singletonMap("component_counts", null));

    // then:
    assertThat(telemetryData.getAttributes()).contains(
        entry("scan_id", "scanId"),
        entry("application_id", HdsClientAnalytics.obfuscate("appId")),
        entry("stage_id", "build"),
        entry("scan_trigger_type", "CLI"),
        entry("deployment_type", "Docker")
    );
  }

  @Test
  public void test_buildApplicationEvaluationTelemetryData_ideThemeProvided() {
    // given:
    Map<String, Object> requestedAttributes = new HashMap<>();
    requestedAttributes.put("ide_theme", "light");
    requestedAttributes.put("component_counts", null);
    // when:
    TelemetryData telemetryData = telemetryUtils.buildApplicationEvaluationTelemetryData(
        null, "appId", "build", ScanTriggerType.CLI, null, null, requestedAttributes);

    // then:
    assertThat(telemetryData.getAttributes()).contains(
        entry("scan_id", null),
        entry("application_id", HdsClientAnalytics.obfuscate("appId")),
        entry("stage_id", "build"),
        entry("scan_trigger_type", "CLI"),
        entry("ide_theme", "light")
    );
  }

  @Test
  public void testObfuscate() {
    String potentialApplicationId = "potentialApplicationId";
    String obfuscated = telemetryUtils.obfuscate(potentialApplicationId);
    assertThat(obfuscated).isEqualTo(telemetryDataObfuscator.obfuscate(potentialApplicationId));
  }

  @Test
  public void testObfuscateIfAdvancedReportingDisabled_propertyIsEnabled_doesNotObfuscate() {
    Map<String, Object> properties =
        Collections.singletonMap(SystemConfigurationProperty.ADVANCED_REPORTING_INSIGHTS_ENABLED, true);
    configurationService.setConfigurationInDatabaseNoAuthz(properties);
    configuration.configurationChanged(properties.keySet());

    String potentialApplicationId = "potentialApplicationId";
    String obfuscated = telemetryUtils.obfuscateIfAdvancedReportingDisabled(potentialApplicationId);
    assertThat(obfuscated).isEqualTo(potentialApplicationId);
  }

  @Test
  public void testObfuscateIfAdvancedReportingDisabled_propertyIsDisabled() {
    Map<String, Object> properties =
        Collections.singletonMap(SystemConfigurationProperty.ADVANCED_REPORTING_INSIGHTS_ENABLED, false);
    configurationService.setConfigurationInDatabaseNoAuthz(properties);
    configuration.configurationChanged(properties.keySet());

    String potentialApplicationId = "potentialApplicationId";
    String obfuscated = telemetryUtils.obfuscateIfAdvancedReportingDisabled(potentialApplicationId);
    assertThat(obfuscated).isEqualTo(telemetryDataObfuscator.obfuscate(potentialApplicationId));
  }

  @Test
  public void testIncludeRealOwnerId_obfuscatesValueIfAdvancedReportingDisabled() {
    Map<String, Object> properties =
        Collections.singletonMap(SystemConfigurationProperty.ADVANCED_REPORTING_INSIGHTS_ENABLED, false);
    configurationService.setConfigurationInDatabaseNoAuthz(properties);
    configuration.configurationChanged(properties.keySet());

    String potentialApplicationId = "potentialApplicationId";
    Map<String, Object> telemetryAttributes = new HashMap<>();
    telemetryUtils.includeRealOwnerId(telemetryAttributes, potentialApplicationId);
    assertThat(telemetryAttributes.entrySet()).extracting("key", "value")
        .containsOnlyOnce(tuple("real_owner_id", telemetryDataObfuscator.obfuscate(potentialApplicationId)));
  }

  @Test
  public void testIncludeRealOwnerId() {
    Map<String, Object> properties =
        Collections.singletonMap(SystemConfigurationProperty.ADVANCED_REPORTING_INSIGHTS_ENABLED, true);
    configurationService.setConfigurationInDatabaseNoAuthz(properties);
    configuration.configurationChanged(properties.keySet());

    String potentialApplicationId = "potentialApplicationId";
    Map<String, Object> telemetryAttributes = new HashMap<>();
    telemetryUtils.includeRealOwnerId(telemetryAttributes, potentialApplicationId);
    assertThat(telemetryAttributes.entrySet()).extracting("key", "value")
        .containsOnlyOnce(tuple("real_owner_id", potentialApplicationId));
  }

  @Test
  public void testIncludeRealApplicationId_obfuscatesValueIfAdvancedReportingDisabled() {
    Map<String, Object> properties =
        Collections.singletonMap(SystemConfigurationProperty.ADVANCED_REPORTING_INSIGHTS_ENABLED, false);
    configurationService.setConfigurationInDatabaseNoAuthz(properties);
    configuration.configurationChanged(properties.keySet());

    String potentialApplicationId = "potentialApplicationId";
    Map<String, Object> telemetryAttributes = new HashMap<>();
    telemetryUtils.includeRealApplicationId(telemetryAttributes, potentialApplicationId);
    assertThat(telemetryAttributes.entrySet()).extracting("key", "value")
        .containsOnlyOnce(tuple("real_application_id", telemetryDataObfuscator.obfuscate(potentialApplicationId)));
  }

  @Test
  public void testIncludeRealApplicationId() {
    Map<String, Object> properties =
        Collections.singletonMap(SystemConfigurationProperty.ADVANCED_REPORTING_INSIGHTS_ENABLED, true);
    configurationService.setConfigurationInDatabaseNoAuthz(properties);
    configuration.configurationChanged(properties.keySet());

    String potentialApplicationId = "potentialApplicationId";
    Map<String, Object> telemetryAttributes = new HashMap<>();
    telemetryUtils.includeRealApplicationId(telemetryAttributes, potentialApplicationId);
    assertThat(telemetryAttributes.entrySet()).extracting("key", "value")
        .containsOnlyOnce(tuple("real_application_id", potentialApplicationId));
  }

  @Test
  public void testBuildContinuousMonitoringMetricsAttributes() {
    HashSet<String> testStages = new HashSet<>();
    testStages.add(Stage.ID_STAGE_RELEASE);
    testStages.add(Stage.ID_BUILD);
    TelemetryData telemetryData = telemetryUtils.buildContinuousMonitoringMetricsAttributes(45, 12345, String
        .join(", ", testStages));

    HashMap<String, Object> expectedContinuousMonitoringMetricsAttributes = new HashMap<>();
    expectedContinuousMonitoringMetricsAttributes.put("appsEvaluatedCount", 45L);
    expectedContinuousMonitoringMetricsAttributes.put("totalExecutionTimeInSeconds", 12345L);
    expectedContinuousMonitoringMetricsAttributes.put("stageIds", "build, stage-release");

    assertThat(telemetryData.getAttributes()).contains(
        entry("continuous_monitoring_metrics", expectedContinuousMonitoringMetricsAttributes)
    );
  }

  @Test
  public void test_buildRepositoryComponentTelemetryData() {
    // Arrange
    String repositoryManagerId = "docker-p1";
    String repositoryId = "docker-p1";
    String componentFormat = "container";
    String componentHash = "foo";
    RepositoryComponentTelemetryEventType eventType = RepositoryComponentTelemetryEventType.QUARANTINE;
    Long quarantineTime = 1234567890L;
    Long releaseQuarantineTime = 1234567900L;
    String releaseQuarantineType = ReleaseQuarantineType.MANUAL.getDescription();

    PolicyViolation violation1 = new PolicyViolation();
    violation1.setId("violation-1");
    violation1.setConstraintFacts(List.of(new ConstraintFact()));
    violation1.setThreatLevel(8);
    violation1.setThreatCategory(PolicyThreatCategory.SECURITY);

    PolicyViolation violation2 = new PolicyViolation();
    violation2.setId("violation-2");
    violation2.setConstraintFacts(List.of(new ConstraintFact()));
    violation2.setThreatLevel(5);
    violation2.setThreatCategory(PolicyThreatCategory.LICENSE);

    List<PolicyViolation> policyViolations = Arrays.asList(violation1, violation2);

    // Act
    TelemetryData telemetryData = telemetryUtils.buildRepositoryComponentTelemetryData(
        repositoryManagerId,
        repositoryId,
        componentFormat,
        componentHash,
        eventType,
        quarantineTime,
        releaseQuarantineTime,
        releaseQuarantineType,
        policyViolations
    );

    // Assert
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.REPOSITORY_COMPONENT);

    RepositoryComponentTelemetry componentTelemetry =
        (RepositoryComponentTelemetry) telemetryData.getAttributes().get(REPOSITORY_COMPONENT_TELEMETRY);
    assertThat(componentTelemetry).isNotNull();
    assertThat(componentTelemetry.getRepositoryManagerId()).isEqualTo(repositoryManagerId);
    assertThat(componentTelemetry.getRepositoryId()).isEqualTo(repositoryId);
    assertThat(componentTelemetry.getComponentFormat()).isEqualTo(componentFormat);
    assertThat(componentTelemetry.getComponentHash()).isEqualTo(HdsClientAnalytics.obfuscate(componentHash));
    assertThat(componentTelemetry.getEventType()).isEqualTo(eventType.getDescription());
    assertThat(componentTelemetry.getQuarantineTime()).isEqualTo(quarantineTime);
    assertThat(componentTelemetry.getReleaseQuarantineTime()).isEqualTo(releaseQuarantineTime);
    assertThat(componentTelemetry.getReleaseQuarantineType()).isEqualTo(releaseQuarantineType);

    @SuppressWarnings("unchecked")
    List<PolicyViolationTelemetry> violationsTelemetry =
        (List<PolicyViolationTelemetry>) telemetryData.getAttributes().get(POLICY_VIOLATION_TELEMETRY);
    assertThat(violationsTelemetry).hasSize(2);

    PolicyViolationTelemetry violationTelemetry1 = violationsTelemetry.get(0);
    assertThat(violationTelemetry1.getThreatLevel()).isEqualTo(8);
    assertThat(violationTelemetry1.getThreatCategory()).isEqualTo(PolicyThreatCategory.SECURITY.getName());

    PolicyViolationTelemetry violationTelemetry2 = violationsTelemetry.get(1);
    assertThat(violationTelemetry2.getThreatLevel()).isEqualTo(5);
    assertThat(violationTelemetry2.getThreatCategory()).isEqualTo(PolicyThreatCategory.LICENSE.getName());
  }

  @Test
  public void testConvertGoldenStatusToString() {
    // Test true value
    String goldenResult = telemetryUtils.convertGoldenStatusToString(true);
    assertThat(goldenResult).isEqualTo("golden");

    // Test false value
    String notGoldenResult = telemetryUtils.convertGoldenStatusToString(false);
    assertThat(notGoldenResult).isEqualTo("not_golden");

    // Test null value
    String unknownResult = telemetryUtils.convertGoldenStatusToString(null);
    assertThat(unknownResult).isEqualTo("unknown");
  }
}
