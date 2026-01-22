/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.net.URISyntaxException;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiSearchResultsDTOV2;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoader;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.component.InnerSourceData;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.report.ReportTestUtils.createReportFile;
import static com.sonatype.insight.brain.report.ReportTestUtils.zipReportDir;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiSearchServiceV2Test
    extends AbstractComponentTest
{
  @Inject
  private ApiSearchServiceV2 apiSearchServiceV2;

  @Inject
  private InsightWork insightWork;

  @Before
  public void before() {
    setBaseUrl("http://localhost:8070");
  }

  @Test
  public void testSearchComponent_InnerSourceData_WithEnabledComponentSearchApiWithInnerSource()
      throws URISyntaxException, IOException
  {
    Application application = tempEntity.newApplication(ROOT_ORGANIZATION_ID);
    ApplicationComponent appComponent1 = tempEntity
        .newApplicationComponent(application.getId(), BuildStageType.ID, "2b8e230d2ab644e4ecaa",
            ComponentIdentifier.createMavenCoordinates("xmlpull", "xmlpull", "1.1.3.1"));
    ApplicationComponent appComponent2 = tempEntity
        .newApplicationComponent(application.getId(), BuildStageType.ID, "e3fd8ced1f52c7574af9",
            ComponentIdentifier.createMavenCoordinates("org.apache.httpcomponents", "httpcore", "4.4.6"));
    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-id");

    createReportFile(application.getId(), "scan-id", zipReportDir("/ApiSearchServiceV2Test/report-1", tempDir),
        insightWork);

    ApiSearchResultsDTOV2 result = apiSearchServiceV2
        .searchComponent(BuildStageType.ID, appComponent1.getHash(), appComponent1.getComponentIdentifier(),
            "pkg:maven/xmlpull/xmlpull@1.1.3.1");

    assertThat(result).isNotNull();
    assertThat(result.results.get(0).dependencyData.directDependency).isFalse();
    assertThat(result.results.get(0).dependencyData.parentComponentPurls)
        .containsExactly("pkg:maven/com.sonatype.insight.scan/insight-module-model@1.0.0-SNAPSHOT?type=jar");
    assertThat(result.results.get(0).dependencyData.innerSource).isFalse();
    assertThat(result.results.get(0).dependencyData).hasFieldOrProperty(ComponentLoader.INNER_SOURCE_DATA_FIELD);
    assertThat(result.results.get(0).dependencyData.innerSourceData).containsExactly(
        new InnerSourceData("insight-module-model", "7509f572645749eba3e19b826e111c8b",
            "pkg:maven/com.sonatype.insight.scan/insight-module-model@1.0.0-SNAPSHOT?type=jar"));

    result = apiSearchServiceV2
        .searchComponent(BuildStageType.ID, appComponent2.getHash(), appComponent2.getComponentIdentifier(),
            "pkg:maven/org.apache.httpcomponents/httpcore@4.4.6");
    assertThat(result).isNotNull();
    assertThat(result.results.get(0).dependencyData.directDependency).isFalse();
    assertThat(result.results.get(0).dependencyData.parentComponentPurls)
        .containsExactly("pkg:maven/com.sonatype.insight.scan/insight-client-utils@1.0.0-SNAPSHOT?type=jar");
    assertThat(result.results.get(0).dependencyData.innerSource).isFalse();
    assertThat(JsonUtils.writeUnformatted(result)).doesNotContain(ComponentLoader.INNER_SOURCE_DATA_FIELD);
  }

  @Test
  public void testSearchComponent_InnerSourceData_WithEnabledComponentSearchApiWithInnerSource_MultipleParentPurls()
      throws Exception
  {
    Application application = tempEntity.newApplicationWithParent();
    ApplicationComponent appComponent = tempEntity
        .newApplicationComponent(application.getId(), BuildStageType.ID, "0f5a654e4675769c716e",
            ComponentIdentifier.createMavenCoordinates("com.fasterxml.jackson.core", "jackson-core", "2.9.8"));
    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-id");
    createReportFile(application.getId(), "scan-id", zipReportDir("/ApiSearchServiceV2Test/report-2", tempDir),
        insightWork);

    ApiSearchResultsDTOV2 result = apiSearchServiceV2
        .searchComponent(BuildStageType.ID, appComponent.getHash(), appComponent.getComponentIdentifier(),
            "pkg:maven/com.fasterxml.jackson.core/jackson-core@2.9.8");

    assertThat(result).isNotNull();
    assertThat(result.results).hasSize(1);
    assertThat(result.results.get(0).dependencyData).isNotNull();
    assertThat(result.results.get(0).dependencyData.parentComponentPurls).containsExactlyInAnyOrder(
        "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.8?type=jar",
        "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.9?type=jar"
    );
  }

  @Test
  public void testSearchComponent_InnerSourceData_WithDisabledComponentSearchApiWithInnerSource()
      throws URISyntaxException, IOException
  {
    SystemConfigurationPropertyFeature.COMPONENT_SEARCH_API_WITH_INNERSOURCE.setEnabled(false);
    Application application = tempEntity.newApplication(ROOT_ORGANIZATION_ID);
    ApplicationComponent appComponent = tempEntity
        .newApplicationComponent(application.getId(), BuildStageType.ID, "2b8e230d2ab644e4ecaa",
            ComponentIdentifier.createMavenCoordinates("xmlpull", "xmlpull", "1.1.3.1"));
    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-id");
    createReportFile(application.getId(), "scan-id", zipReportDir("/ApiSearchServiceV2Test/report-1", tempDir),
        insightWork);

    ApiSearchResultsDTOV2 result = apiSearchServiceV2
        .searchComponent(BuildStageType.ID, appComponent.getHash(), appComponent.getComponentIdentifier(),
            "pkg:maven/xmlpull/xmlpull@1.1.3.1");

    assertThat(result).isNotNull();
    assertThat(JsonUtils.writeUnformatted(result.results.get(0))).doesNotContain("dependencyData");
  }
}
