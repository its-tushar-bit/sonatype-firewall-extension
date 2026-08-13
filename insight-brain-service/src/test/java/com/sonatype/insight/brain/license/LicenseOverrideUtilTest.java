/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseOverrideDTO;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.license.LicenseOverrideStatus.OVERRIDDEN;
import static org.assertj.core.api.Assertions.assertThat;

public class LicenseOverrideUtilTest
{
  @Test
  public void testToInternalLicenseOverrideConversion() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1",
        "a1", "v1");
    ApiComponentIdentifierDTOV2 componentIdentifierDTOV2 =
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);
    ApiLicenseOverrideDTO apiLicenseOverrideDTO = new ApiLicenseOverrideDTO("ownerId", "comment",
        "licenseId",
        componentIdentifierDTOV2, OVERRIDDEN);

    LicenseOverride licenseOverride =
        LicenseOverrideUtil.toInternalLicenseOverride(apiLicenseOverrideDTO);

    assertThat(licenseOverride.getComment()).isEqualTo("comment");
    assertThat(licenseOverride.getLicenseIds()).containsExactly("licenseId");
    assertThat(licenseOverride.getComponentIdentifier()).isEqualTo(componentIdentifier);
    assertThat(licenseOverride.getStatus()).isEqualTo(OVERRIDDEN);

    LicenseOverride licenseOverrideNull = LicenseOverrideUtil.toInternalLicenseOverride(null);
    assertThat(licenseOverrideNull).isNull();

    LicenseOverride licenseOverrideComponentIdNull = LicenseOverrideUtil.toInternalLicenseOverride(
        new ApiLicenseOverrideDTO("ownerId", "comment", "licenseId", null, OVERRIDDEN));
    assertThat(licenseOverrideComponentIdNull.getComponentIdentifier()).isNull();
  }

  @Test
  public void testToApiLicenseDTOOverrideConversion() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1",
        "a1", "v1");
    LicenseOverride licenseOverride = new LicenseOverride("ownerId", componentIdentifier,
        OVERRIDDEN, "licenseId", "comment");

    ApiLicenseOverrideDTO licenseOverrideDTO =
        LicenseOverrideUtil.toApiLicenseOverrideDTO(licenseOverride);

    assertThat(licenseOverrideDTO.comment).isEqualTo("comment");
    assertThat(licenseOverrideDTO.licenseIds).containsExactly("licenseId");
    assertThat(licenseOverrideDTO.componentIdentifier)
        .isEqualTo(ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier));
    assertThat(licenseOverrideDTO.status).isEqualTo(OVERRIDDEN);

    ApiLicenseOverrideDTO licenseOverrideDTONull = LicenseOverrideUtil.toApiLicenseOverrideDTO(null);
    assertThat(licenseOverrideDTONull).isNull();

    licenseOverride.setComponentIdentifier(null);
    ApiLicenseOverrideDTO licenseOverrideComponentIdNull =
        LicenseOverrideUtil.toApiLicenseOverrideDTO(licenseOverride);
    assertThat(licenseOverrideComponentIdNull.componentIdentifier).isNull();
  }
}
