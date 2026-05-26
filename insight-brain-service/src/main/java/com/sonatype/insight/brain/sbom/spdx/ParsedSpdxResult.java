/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.spdx;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;

import org.apache.commons.lang3.tuple.Pair;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Dependency;

public record ParsedSpdxResult(
    List<Pair<ComponentIdentifier, Component>> resolvedComponents,
    List<Dependency> dependencies,
    List<ThirdPartyCoordinateSecurity> vulnerabilities,
    List<ThirdPartyVulnerabilityExploitabilityExchange> vexAnnotations,
    String specVersion,
    String unsupportedProfiles,
    Map<String, Set<String>> vulnerabilityToPackageUris,
    List<Set<String>> vexAffectedPackageUris,
    String rootComponentRef)
{
}
