/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

/**
 * Legacy placeholder for the removed Guice SecurityModule test.
 *
 * <p>
 * Spring-managed Shiro wiring is covered by {@link SecurityConfigurationTest}.
 */
@Deprecated
public class SecurityModuleTest
    extends SecurityConfigurationTest
{
  // Preserves the legacy test class name while hosting the Spring regression coverage.
}
