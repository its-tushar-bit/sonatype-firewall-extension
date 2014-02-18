/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.component;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ComponentTest
{
  @Test
  public void defaultDoesNotOverrideLicense() {
    assertThat(new Component().isLicenseOverridden(), is(false));
  }

  @Test
  public void licenseOverride() {
    Component overridden = new Component();
    overridden.setLicenseOverrideId("any-license-id");

    assertThat(overridden.isLicenseOverridden(), is(true));
  }
}
