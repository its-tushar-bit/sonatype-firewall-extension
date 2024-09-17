/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.hds.AbstractComponentInfoResourceTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IDEComponentInfoResourceTest
    extends AbstractComponentInfoResourceTest
{
  @Override
  protected String getResourcePath() {
    return IDEComponentInfoResource.RESOURCE_PATH;
  }

  @Test
  public void testGetComponentDetails() throws Exception {
    super.testGetComponentDetails_EvaluateComponentPermission();
  }

  @Test
  public void testGetComponentDetailsList() throws Exception {
    super.testGetComponentDetailsList_EvaluateComponentPermission();
  }

  // Remove this override when SDEV-1534 is implemented since only one of the two remediations of the same version
  // would be returned.
  @Override
  protected void assertRemediation(ApiComponentRemediationValueDTO remediationValue) {
    assertThat(remediationValue.componentOverrides).isEmpty();
    assertThat(remediationValue.policyWaivers).isEmpty();
    assertThat(remediationValue.versionChanges).hasSize(2);

    ApiVersionChangeOptionDTO versionChange = remediationValue.versionChanges.get(0);
    assertThat(versionChange.getType()).isEqualTo(ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS);
    assertThat(versionChange.getData().getComponent().packageUrl).isEqualTo("pkg:maven/g1/a1@v1?type=jar");
    assertThat(versionChange.getData().getComponent().displayName).isEqualTo(
        com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil
            .fromIdentifier(versionChange.getData().getComponent().componentIdentifier.toComponentIdentifier())
            .toString());

    ApiVersionChangeOptionDTO versionChange1 = remediationValue.versionChanges.get(1);
    assertThat(versionChange1.getType()).isEqualTo(ApiVersionChangeOptionType.NEXT_NON_FAILING);
    assertThat(versionChange1.getData().getComponent().packageUrl).isEqualTo("pkg:maven/g1/a1@v1?type=jar");
    assertThat(versionChange1.getData().getComponent().displayName).isEqualTo(
        com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil
            .fromIdentifier(versionChange1.getData().getComponent().componentIdentifier.toComponentIdentifier())
            .toString());
  }
}
