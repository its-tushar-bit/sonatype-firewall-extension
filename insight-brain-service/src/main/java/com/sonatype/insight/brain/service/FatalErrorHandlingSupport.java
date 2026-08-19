/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.jaxrs.error.JavaLangErrorHandler;

public final class FatalErrorHandlingSupport
{
  private FatalErrorHandlingSupport() {
    // utility class
  }

  public static JavaLangErrorHandler configure(final JavaLangErrorHandler javaLangErrorHandler) {
    javaLangErrorHandler.setExitOnFatalErrorSupplier(
        () -> SystemConfigurationPropertyFeature.EXIT_ON_FATAL_ERROR.isEnabled());
    return javaLangErrorHandler;
  }
}
