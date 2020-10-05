/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.util.Collections;
import java.util.List;

/**
 * Native image config generation for the {@link DefaultPolicyEvaluatorReverseProxyAuthTest}. This extends that class
 * and will execute its tests except using the {@link NativeImageConfigGenerationTestRunner}.
 *
 * See readme.md in the nexus-iq-cli-native-image module for full details
 */
public class NativeImageConfigDefaultPolicyEvaluatorReverseProxyAuthTest
    extends DefaultPolicyEvaluatorReverseProxyAuthTest
{
  public NativeImageConfigDefaultPolicyEvaluatorReverseProxyAuthTest(final boolean rutEnabled) {
    super(rutEnabled);
  }

  @Override
  protected AbstractPolicyEvaluatorTestRunner withTestRunner(final List<String> params) {
    return new NativeImageConfigGenerationTestRunner(params, Collections.emptyMap(), logOutput).withSsl();
  }
}
