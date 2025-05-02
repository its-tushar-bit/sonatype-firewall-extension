/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.license;

import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;

public record ResolvedLicenseDTO(String licenseId,
                                 String licenseName,
                                 String licenseUrl,
                                 LicenseOverrideStatus overrideStatus)
{ }
