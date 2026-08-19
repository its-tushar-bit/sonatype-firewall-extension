/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration;

import java.util.ArrayList;
import java.util.Collections;

import com.sonatype.clm.dto.model.callflowanalysis.CallFlowAlgorithm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CallFlowAnalysisConfigTest
{
  @Test
  public void testCreationCallFlowAnalysisConfig() {
    CallFlowAnalysisConfig callFlowAnalysisConfig = new CallFlowAnalysisConfig(true, Collections.singletonList("foo"),
        CallFlowAlgorithm.CLASS_HIERARCHY_ANALYSIS, 2);

    assertThat(callFlowAnalysisConfig.getNamespaces()).isEqualTo(Collections.singletonList("foo"));
    assertThat(callFlowAnalysisConfig.isEnabled()).isTrue();
    assertThat(callFlowAnalysisConfig.getAlgorithm()).isEqualTo(CallFlowAlgorithm.CLASS_HIERARCHY_ANALYSIS);
    assertThat(callFlowAnalysisConfig.getThreadCount()).isEqualTo(2);
  }

  @Test
  public void testGetNamespacesMoreThanOne() {
    ArrayList<String> nameSpaces = new ArrayList();
    nameSpaces.add("nameSpace1");
    nameSpaces.add("nameSpace2");
    nameSpaces.add("nameSpace3");
    nameSpaces.add("nameSpace4");
    CallFlowAnalysisConfig callFlowAnalysisConfig = new CallFlowAnalysisConfig(true, nameSpaces,
        CallFlowAlgorithm.CLASS_HIERARCHY_ANALYSIS, 2);
    assertThat(callFlowAnalysisConfig.getNamespaces()).hasSize(4);
    assertThat(callFlowAnalysisConfig.getNamespaces())
        .containsExactly("nameSpace1", "nameSpace2", "nameSpace3", "nameSpace4");
  }
}
