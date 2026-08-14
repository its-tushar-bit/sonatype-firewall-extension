/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.component;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentTest
{
  @Test
  public void defaultDoesNotOverrideLicense() {
    assertThat(new Component().isLicenseOverridden()).isFalse();
  }

  @Test
  public void licenseOverride() {
    Component overridden = new Component();
    overridden.setLicenseOverrideIds(Collections.singleton("any-license-id"));

    assertThat(overridden.isLicenseOverridden()).isTrue();
  }

  @Test
  public void getInnerComponentPurls_EmptyCollection() {
    Set<String> innerComponentPurls = new Component().getInnerComponentPurls();
    assertThat(innerComponentPurls).isEmpty();
  }

  @Test
  public void getInnerComponentPurls_MultiplePurls() {
    InnerSourceData myPurl = new InnerSourceData(null, null, "myPurl");
    InnerSourceData myPurl1 = new InnerSourceData(null, null, "myPurl1");
    InnerSourceData myPurl2 = new InnerSourceData(null, null, "myPurl2");

    Component component = new Component();
    component.setInnerSourceData(Set.of(myPurl, myPurl1, myPurl2));

    assertThat(component.getInnerComponentPurls())
        .hasSize(3)
        .containsExactlyInAnyOrder("myPurl", "myPurl1", "myPurl2");
  }
}
