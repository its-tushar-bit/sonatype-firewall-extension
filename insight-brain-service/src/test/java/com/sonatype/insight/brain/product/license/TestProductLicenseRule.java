/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import com.sonatype.insight.brain.TestProductLicenseManager;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class TestProductLicenseRule
    extends TestProductLicense
    implements TestRule
{
  public TestProductLicenseRule(TestProductLicenseManager testProductLicenseManager) {
    super(testProductLicenseManager, false);
  }

  @Override
  public Statement apply(final Statement base, final Description description) {
    return new Statement() {
      @Override public void evaluate() throws Throwable {
        reset();
        base.evaluate();
      }
    };
  }
}
