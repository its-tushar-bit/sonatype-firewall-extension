/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.Before;

/**
 * Native image config generation for the {@link ExpandedCoveragePolicyEvaluatorTest}. This extends that class and will
 * execute its tests except using the {@link NativeImageConfigGenerationTestRunner}.
 *
 * See readme.md in the nexus-iq-cli-native-image module for full details
 */
public class NativeImageConfigExpandedCoveragePolicyEvaluatorTest
    extends ExpandedCoveragePolicyEvaluatorTest
{
  @Before
  public void setUp() throws Exception {
    super.setUp();

    tempEntity.newApplicationWithParent("the-app-id");
  }

  @Override
  protected AbstractPolicyEvaluatorTestRunner withTestRunner(final List<String> params) {
    List<String> expandedCoverageParams = new ArrayList<>(params);

    // enable XC mode
    expandedCoverageParams.add("-xc");

    // some XC tests do not pass in all the required params
    maybeAddParam(expandedCoverageParams, "-s", insightServerUrl);
    maybeAddParam(expandedCoverageParams, "-a", "admin:admin123");
    maybeAddParam(expandedCoverageParams, "-i", "the-app-id");

    return new NativeImageConfigGenerationTestRunner(ImmutableList.copyOf(expandedCoverageParams),
        Collections.emptyMap(), logOutput);
  }

  private void maybeAddParam(final List<String> params, final String key, final String value) {
    if (!params.contains(key)) {
      params.add(key);
      params.add(value);
    }
  }
}
