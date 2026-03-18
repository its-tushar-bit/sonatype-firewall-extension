/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.Arrays;
import java.util.Collections;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.hds.ComponentDetailsDTO;
import com.sonatype.insight.brain.hds.ComponentDetailsLoaderFactory;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PathForwardInspectorTest
    extends AbstractComponentTest
{
  private Application application;

  private PathForwardInspector pathForwardInspector;

  @Mock
  private ComponentInfoService componentInfoServiceMock;

  @Inject
  private ComponentDetailsLoaderFactory componentDetailsLoaderFactory;

  @Inject
  private ApplicationDAO applicationDAO;

  private static final ComponentIdentifier MAVEN_COORDINATES_V1 = ComponentIdentifier.createMavenCoordinates("g1", "a1",
      "1.0.0", "", "jar");

  private static final ComponentIdentifier MAVEN_COORDINATES_V2 = ComponentIdentifier.createMavenCoordinates("g1", "a1",
      "2.0.0", "", "jar");

  private static final ComponentIdentifier MAVEN_COORDINATES_V3 = ComponentIdentifier.createMavenCoordinates("g1", "a1",
      "3.0.0", "", "jar");

  private static final Component MAVEN_COMPONENT_V1 = new Component(MAVEN_COORDINATES_V1);

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    application = tempEntity.newApplicationWithParent();
    pathForwardInspector =
        new PathForwardInspector(componentInfoServiceMock, componentDetailsLoaderFactory, applicationDAO);

    MAVEN_COMPONENT_V1.setHash("hash");
    MAVEN_COMPONENT_V1.setMatchState(MatchState.EXACT);
  }

  @Test
  public void testDoesNotContainsUpgradeableVersion() {
    ComponentDetailsDTO componentDetailsDTO = new ComponentDetailsDTO();
    componentDetailsDTO.componentIdentifier = MAVEN_COORDINATES_V1;
    componentDetailsDTO.violatedPolicyCount = 1;

    when(componentInfoServiceMock.getComponentDetailsForAllVersionsNoAuth(
        any(), eq(MAVEN_COORDINATES_V1), eq("stageId"), any(), eq("scanId"), any(), any(), anyBoolean()))
            .thenReturn(Pair.of(Collections.singletonList(componentDetailsDTO), null));

    boolean result =
        pathForwardInspector.containsUpgradeableVersion(MAVEN_COMPONENT_V1.getComponentIdentifier(),
            application.getId(), "stageId", "scanId");

    assertFalse(result);
  }

  @Test
  public void testContainsUpgradeableVersion() {
    ComponentDetailsDTO componentDetailsDTO3 = new ComponentDetailsDTO();
    componentDetailsDTO3.componentIdentifier = MAVEN_COORDINATES_V3;
    componentDetailsDTO3.violatedPolicyCount = 0;
    ComponentDetailsDTO componentDetailsDTO2 = new ComponentDetailsDTO();
    componentDetailsDTO2.componentIdentifier = MAVEN_COORDINATES_V2;
    componentDetailsDTO2.violatedPolicyCount = 3;
    ComponentDetailsDTO componentDetailsDTO1 = new ComponentDetailsDTO();
    componentDetailsDTO1.componentIdentifier = MAVEN_COORDINATES_V1;
    componentDetailsDTO1.violatedPolicyCount = 1;

    when(componentInfoServiceMock.getComponentDetailsForAllVersionsNoAuth(
        any(), eq(MAVEN_COORDINATES_V1), any(), any(), any(), any(), any(), anyBoolean())).thenReturn(
            Pair.of(Arrays.asList(componentDetailsDTO1, componentDetailsDTO3), null));

    boolean result =
        pathForwardInspector.containsUpgradeableVersion(
            MAVEN_COMPONENT_V1.getComponentIdentifier(), application.getId(), "stageId", "scanId");

    assertTrue(result);
    assertThat(pathForwardInspector.getViolatedComponentMap(), hasEntry(MAVEN_COORDINATES_V1, true));

    pathForwardInspector.containsUpgradeableVersion(
        MAVEN_COMPONENT_V1.getComponentIdentifier(), application.getId(), "stageId", "scanId");

    // should only call once, as the result is cached
    verify(componentInfoServiceMock, times(1)).getComponentDetailsForAllVersionsNoAuth(
        any(), any(), eq("stageId"), any(), eq("scanId"), any(), any(), anyBoolean());
  }
}
