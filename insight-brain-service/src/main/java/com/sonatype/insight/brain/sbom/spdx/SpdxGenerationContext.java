/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.spdx;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.model.thirdpartyscans.ResolvedLicenseDTO;

import org.cyclonedx.model.Dependency;

public record SpdxGenerationContext(
    List<ThirdPartyFileCoordinate> components,
    List<ThirdPartyCoordinateSecurity> vulnerabilities,
    Map<String, Set<ResolvedLicenseDTO>> licensesByCoordinateId,
    List<ThirdPartyVulnerabilityExploitabilityExchange> vexAnnotations,
    List<Dependency> dependencies,
    String applicationName,
    String sbomVersion,
    String targetSpecVersion,
    String extendedProfileElements,
    String documentUri,
    String companionCdxFilename,
    String rootComponentRef)
{
}
