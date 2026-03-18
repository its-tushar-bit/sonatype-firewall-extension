/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.telemetry;

import java.util.List;
import java.util.Map;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.actions.ApiComponentChangeActionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiSuggestedVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.NonBreakingRecommendationTelemetryStats.SourceEndpoint;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.junit.Test;

import static com.sonatype.insight.telemetry.model.TelemetryPurpose.NON_BREAKING_VERSION_CHANGE_RECOMMENDATION;
import static org.assertj.core.api.Assertions.assertThat;

public class NonBreakingRecommendationTelemetryMetricsTest
    extends AbstractComponentTest
{
  @Inject
  private NonBreakingRecommendationTelemetryMetrics metrics;

  @Test
  public void testComputeStatsAndReset_shouldReturnWhatWasCollected() {
    ApiSuggestedVersionChangeOptionDTO versionChange1 =
        createVersionChange("1.0.0", ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING, "pkg1:@1.0.0");
    ApiSuggestedVersionChangeOptionDTO versionChange2 =
        createVersionChange("3.0.0-alpha", ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES,
            "pkg2:@3.0.0-alpha");

    SourceEndpoint source1 = SourceEndpoint.IDE;
    SourceEndpoint source2 = SourceEndpoint.API_COMPONENT_REMEDIATION;

    metrics.collect(versionChange1, createApplication("app1"), source1);

    // collect three times so count would be 3
    metrics.collect(versionChange2, createApplication("app2"), source2);
    metrics.collect(versionChange2, createApplication("app2"), source2);
    metrics.collect(versionChange2, createApplication("app2"), source2);

    List<TelemetryData> stats = metrics.computeStatsAndReset();
    assertThat(stats).hasSize(2);

    assertThat(stats).satisfiesExactlyInAnyOrder(
        data -> assertTelemetryData(data, NON_BREAKING_VERSION_CHANGE_RECOMMENDATION,
            source1.toString(), "app1",
            versionChange1.getType().getNameForTelemetry(),
            "1.0.0",
            "pkg1:@1.0.0",
            1L),
        data -> assertTelemetryData(data, NON_BREAKING_VERSION_CHANGE_RECOMMENDATION,
            source2.toString(), "app2",
            versionChange2.getType().getNameForTelemetry(),
            "3.0.0-alpha",
            "pkg2:@3.0.0-alpha",
            3L));
  }

  @Test
  public void testComputeStatsAndReset_shouldResetReliably() {
    Application app1 = createApplication("app1");
    Application app2 = createApplication("app2");
    Application app3 = createApplication("app3");

    ApiSuggestedVersionChangeOptionDTO versionChange1 =
        createVersionChange(
            "2.0.0", ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING, "pkg1:@2.0.0");
    ApiSuggestedVersionChangeOptionDTO versionChange2 =
        createVersionChange(
            "2.5.0", ApiVersionChangeOptionType.RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES, "pkg2:@2.5.0");

    SourceEndpoint source1 = SourceEndpoint.IDE;
    SourceEndpoint source2 = SourceEndpoint.API_COMPONENT_REMEDIATION;
    SourceEndpoint source3 = SourceEndpoint.PULL_REQUEST_COMMENTING;

    for (long i = 0; i < 10; i++) {
      metrics.collect(versionChange1, app1, source1);
      metrics.collect(versionChange2, app2, source2);
      metrics.collect(null, app3, source3);

      List<TelemetryData> telemetryDataList = metrics.computeStatsAndReset();
      // No collection when suggestedVersionChange is null
      assertThat(telemetryDataList).hasSize(2);
      // Count resets to 0 each time and then adds one again
      assertThat(telemetryDataList).allMatch(
          data -> data.getAttributes()
              .get("recommended_non_breaking_version_count_of_the_same_suggestion")
              .equals(1L));
      // Nothing left before collecting again
      assertThat(metrics.computeStatsAndReset()).isEmpty();
    }
  }

  private Application createApplication(String id) {
    Application app = new Application();
    app.setId(id);
    app.setName(id);
    return app;
  }

  private ApiSuggestedVersionChangeOptionDTO createVersionChange(
      String version,
      ApiVersionChangeOptionType type,
      String purl)
  {
    ApiSuggestedVersionChangeOptionDTO versionChange = new ApiSuggestedVersionChangeOptionDTO();
    ApiComponentDTOV2 component = new ApiComponentDTOV2();
    component.packageUrl = purl;
    component.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates("group", "artifact", version));
    versionChange.setData(new ApiComponentChangeActionDTO(component));
    versionChange.setType(type);
    return versionChange;
  }

  private void assertTelemetryData(
      TelemetryData data,
      TelemetryPurpose purpose,
      String sourceEndpoint,
      String realApplicationId,
      String versionChangeOptionType,
      String version,
      String purl,
      Long count)
  {
    assertThat(data.getPurpose()).isEqualTo(purpose);
    Map<String, Object> attrs = data.getAttributes();
    assertThat(attrs.get("recommended_non_breaking_version_source_endpoint")).isEqualTo(sourceEndpoint);
    assertThat(attrs.get("real_application_id")).isEqualTo(realApplicationId);
    assertThat(attrs.get("recommended_non_breaking_version_change_option_type")).isEqualTo(
        versionChangeOptionType);
    assertThat(attrs.get("recommended_non_breaking_version")).isEqualTo(version);
    assertThat(attrs.get("recommended_non_breaking_version_package_url")).isEqualTo(purl);
    assertThat(attrs.get("recommended_non_breaking_version_count_of_the_same_suggestion")).isEqualTo(count);
  }
}
