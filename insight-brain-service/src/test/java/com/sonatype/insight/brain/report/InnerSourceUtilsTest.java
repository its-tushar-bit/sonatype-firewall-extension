/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InnerSourceUtilsTest
{
  @Test
  public void testGetVersionlessPackageUrl_Maven() {
    ComponentIdentifier id = ComponentIdentifier.createMavenCoordinates("company", "name", "1.0.1");
    PackageUrlIdentifier versionlessPackageUrl = InnerSourceUtils.getVersionlessPackageUrl(id);
    assertThat(versionlessPackageUrl.getPackageUrl()).isEqualTo("pkg:maven/company/name");
  }

  @Test
  public void testGetVersionlessPackageUrl_Npm() {
    ComponentIdentifier id = ComponentIdentifier.createNpmCoordinates("@angular", "2.0.1");
    PackageUrlIdentifier versionlessPackageUrl = InnerSourceUtils.getVersionlessPackageUrl(id);
    assertThat(versionlessPackageUrl.getPackageUrl()).isEqualTo("pkg:npm/%40angular");
  }

  @Test
  public void testGetVersionlessPackageUrl_Null() {
    assertThat(InnerSourceUtils.getVersionlessPackageUrl(null)).isNull();
  }
}
