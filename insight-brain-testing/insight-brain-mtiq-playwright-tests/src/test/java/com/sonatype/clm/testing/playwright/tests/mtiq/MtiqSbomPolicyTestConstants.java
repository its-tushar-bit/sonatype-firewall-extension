/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

/**
 * Shared MTIQ SBOM Policy Editor test constants — avoids drift between
 * {@link MtiqSbomPolicyEditorPlaywrightTest} and the split single-@Test child-org classes
 * (see {@link AbstractMtiqSbomChildOrgPolicyPlaywrightTest}).
 */
final class MtiqSbomPolicyTestConstants
{
  static final int THREAT_LEVEL = 5;

  private MtiqSbomPolicyTestConstants() {
  }
}
