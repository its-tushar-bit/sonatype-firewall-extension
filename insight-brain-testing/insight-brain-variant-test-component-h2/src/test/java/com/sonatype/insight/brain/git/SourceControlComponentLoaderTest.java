/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.git.SourceControlComponentDetails.ComponentInfo;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.nexus.scm.api.DiffPosition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.report.ReportTestUtils.createReportFile;
import static com.sonatype.insight.brain.report.ReportTestUtils.zipReportDir;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@ComponentH2Test
public class SourceControlComponentLoaderTest
    extends AbstractComponentH2Test
{
  @Inject
  private InsightWork insightWork;

  private static final ComponentIdentifier LOG4J_CORE_COMPONENT_ID =
      ComponentIdentifier.createMavenCoordinates("org.apache.logging.log4j", "log4j-core", "2.6.1", "", "jar");

  private static final ComponentIdentifier LOG4J_API_COMPONENT_ID =
      ComponentIdentifier.createMavenCoordinates("org.apache.logging.log4j", "log4j-api", "2.6.1", "", "jar");

  private Application application;

  private final String unknownComponentHash = "3c2501b9238143b17d2c";

  // Subject
  @Inject
  private SourceControlComponentLoader loader;

  @BeforeEach
  public void before() {
    application = tempEntity.newApplicationWithParent();
  }

  @Test
  public void testGetComponentMap() throws IOException, URISyntaxException {
    // given:
    createReportFile(application.getId(), "SCAN_ID_1",
        zipReportDir("/SourceControlComponentLoaderTest/complete", tempDir), insightWork);

    // when:
    SourceControlComponentDetails details = loader.getSourceControlComponentDetails(application.getId(), "SCAN_ID_1");

    // then: component details is populated
    assertThat(details).isNotNull();

    // and: the display name and dep. info is available for log4j-core
    ComponentInfo componentInfo = details.getComponentInfo(LOG4J_CORE_COMPONENT_ID);
    assertThat(componentInfo.getDisplayName()).isEqualTo("org.apache.logging.log4j : log4j-core : 2.6.1");
    assertThat(componentInfo.getDirectDependency()).isTrue();

    // and: the display name and dep. info is available for log4j-api
    componentInfo = details.getComponentInfo(LOG4J_API_COMPONENT_ID);
    assertThat(componentInfo.getDisplayName()).isEqualTo("org.apache.logging.log4j : log4j-api : 2.6.1");
    assertThat(componentInfo.getDirectDependency()).isFalse();

    // and: only the display name is available for an unknown component
    componentInfo = details.getComponentInfo(unknownComponentHash);
    assertThat(componentInfo.getDisplayName()).isEqualTo("line-comm-03-1.0.jar");
    assertThat(componentInfo.getDirectDependency()).isNull();
  }

  @Test
  public void testGetComponentMap_missingReport() {
    // given: no report zip

    // when:
    Throwable thrown = catchThrowable(() -> loader.getSourceControlComponentDetails(application.getId(), "SCAN_ID_2"));

    // then:
    assertThat(thrown)
        .isInstanceOf(NotFoundException.class)
        .hasMessage("Could not find a report with ID SCAN_ID_2");
  }

  @Test
  public void testGetComponentMap_missingReportEntries() throws IOException, URISyntaxException {
    // given: incomplete report zip
    createReportFile(application.getId(), "SCAN_ID_3",
        zipReportDir("/SourceControlComponentLoaderTest/partial", tempDir), insightWork);

    // when:
    SourceControlComponentDetails details = loader.getSourceControlComponentDetails(application.getId(), "SCAN_ID_3");

    // then: component details is populated
    assertThat(details).isNotNull();

    // and: only the display name is available for log4j-core
    ComponentInfo componentInfo = details.getComponentInfo(LOG4J_CORE_COMPONENT_ID);
    assertThat(componentInfo.getDisplayName()).isEqualTo("org.apache.logging.log4j : log4j-core : 2.6.1");
    assertThat(componentInfo.getDirectDependency()).isNull();

    // and: only the display name is available for log4j-api
    componentInfo = details.getComponentInfo(LOG4J_API_COMPONENT_ID);
    assertThat(componentInfo.getDisplayName()).isEqualTo("org.apache.logging.log4j : log4j-api : 2.6.1");
    assertThat(componentInfo.getDirectDependency()).isNull();

    // and: only the display name is available for an unknown component
    componentInfo = details.getComponentInfo(unknownComponentHash);
    assertThat(componentInfo.getDisplayName()).isEqualTo("line-comm-03-1.0.jar");
    assertThat(componentInfo.getDirectDependency()).isNull();
  }

  @Test
  public void testEnhanceSourceControlComponentDetails() throws IOException, URISyntaxException {
    // given:
    createReportFile(application.getId(), "SCAN_ID_4",
        zipReportDir("/SourceControlComponentLoaderTest/complete", tempDir), insightWork);

    List<PolicyViolation> policyViolations = new LinkedList<>();

    // one policy violation for a component already in the report
    PolicyViolation violation = new PolicyViolation();
    violation.setComponentIdentifier(LOG4J_CORE_COMPONENT_ID);
    policyViolations.add(violation);

    // second policy violation for a component not included in the report
    violation = new PolicyViolation();
    ComponentIdentifier junitComponentId =
        ComponentIdentifier.createMavenCoordinates("junit", "junit", "4.12", "", "jar");
    violation.setComponentIdentifier(junitComponentId);
    violation.setHash("junit-hash");
    policyViolations.add(violation);

    SourceControlComponentDetails details = loader.getSourceControlComponentDetails(application.getId(), "SCAN_ID_4");

    // when:
    loader.enhanceSourceControlComponentDetails(details, policyViolations);

    // then: component details is populated
    assertThat(details).isNotNull();

    // and: log4j-core component info is unchanged
    ComponentInfo componentInfo = details.getComponentInfo(LOG4J_CORE_COMPONENT_ID);
    assertThat(componentInfo.getDisplayName()).isEqualTo("org.apache.logging.log4j : log4j-core : 2.6.1");
    assertThat(componentInfo.getDirectDependency()).isTrue();

    // and: only the display name is available for junit
    componentInfo = details.getComponentInfo(junitComponentId);
    assertThat(componentInfo.getDisplayName()).isEqualTo("junit : junit : 4.12");
    assertThat(componentInfo.getDirectDependency()).isNull();
    // and: junit is accessible by hash as well
    componentInfo = details.getComponentInfo("junit-hash");
    assertThat(componentInfo.getDisplayName()).isEqualTo("junit : junit : 4.12");
    assertThat(componentInfo.getDirectDependency()).isNull();
  }

  @Test
  public void testEnhanceSourceControlComponentDetailsDirectDependency() throws IOException, URISyntaxException {
    // given:
    createReportFile(application.getId(), "SCAN_ID_4",
        zipReportDir("/SourceControlComponentLoaderTest/complete", tempDir), insightWork);

    SourceControlComponentDetails details = loader.getSourceControlComponentDetails(application.getId(), "SCAN_ID_4");

    Map<ComponentIdentifier, ComponentInfo> identifierToComponentInfoMap = details.getIdentifierToComponentInfoMap();
    ComponentInfo componentInfo =
        new ComponentInfo(ComponentDisplayNameUtil.fromIdentifier(LOG4J_CORE_COMPONENT_ID).toString(), false);
    identifierToComponentInfoMap.put(LOG4J_CORE_COMPONENT_ID, componentInfo);
    List<PullRequestLineCommentDTO> lineComments =
        Collections.singletonList(
            new PullRequestLineCommentDTO(LOG4J_CORE_COMPONENT_ID, new DiffPosition("path", 1, 0, 1, "456", 1)));

    componentInfo = details.getComponentInfo(LOG4J_CORE_COMPONENT_ID);
    assertThat(componentInfo.getDirectDependency()).isFalse();

    // when:
    loader.enhanceSourceControlComponentDetailsWithDirectDependencyInformation(details, lineComments);

    // and: log4j-core component info is unchanged
    componentInfo = details.getComponentInfo(LOG4J_CORE_COMPONENT_ID);
    assertThat(componentInfo.getDisplayName()).isEqualTo("org.apache.logging.log4j : log4j-core : 2.6.1");
    assertThat(componentInfo.getDirectDependency()).isTrue();
  }
}
