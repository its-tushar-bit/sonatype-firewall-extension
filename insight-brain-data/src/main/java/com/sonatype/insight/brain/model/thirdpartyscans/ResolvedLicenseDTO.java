/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.thirdpartyscans;

import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public record ResolvedLicenseDTO(
    String licenseId,
    String licenseName,
    @JsonIgnore String licenseUrl,
    @JsonIgnore String identificationSources,
    @JsonInclude(Include.NON_NULL) LicenseOverrideStatus overrideStatus)
{
}
