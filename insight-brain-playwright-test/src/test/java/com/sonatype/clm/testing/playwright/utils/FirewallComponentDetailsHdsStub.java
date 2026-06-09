/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.utils;

import java.util.Date;

import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.service.HdsMockServerRule;

/**
 * Registers HDS responses for {@code rest/ci/componentDetails} used by the Firewall Component
 * Details page ({@code loadComponentDetails()} in the SPA).
 */
public final class FirewallComponentDetailsHdsStub
{
  private static final String COMPONENT_DETAILS_URI = "rest/ci/componentDetails";

  private static final String DEFAULT_WEBSITE = "http://www.example.com";

  private FirewallComponentDetailsHdsStub() {
  }

  /**
   * Stub component metadata only — sufficient for policy-violations tab / security tab tables
   * that do not assert vulnerability rows.
   */
  public static void stubRepositoryComponentDetails(HdsMockServerRule hdsServer, RepositoryComponent component) {
    registerComponentDetails(hdsServer, buildComponentDetails(component, false, null, 0, null, 0));
  }

  /**
   * Stub component metadata plus security vulnerability rows (for vulnerability popover tests).
   */
  public static void stubRepositoryComponentDetailsWithVulnerabilities(
      HdsMockServerRule hdsServer,
      RepositoryComponent component,
      String highSeverityCveId,
      float highSeverity,
      String lowSeverityCveId,
      float lowSeverity)
  {
    registerComponentDetails(hdsServer,
        buildComponentDetails(component, true, highSeverityCveId, highSeverity, lowSeverityCveId, lowSeverity));
  }

  private static void registerComponentDetails(HdsMockServerRule hdsServer, ComponentDetails componentDetails) {
    hdsServer.respondWith(componentDetails).atUri(COMPONENT_DETAILS_URI);
  }

  private static ComponentDetails buildComponentDetails(
      RepositoryComponent component,
      boolean withVulnerabilities,
      String highSeverityCveId,
      float highSeverity,
      String lowSeverityCveId,
      float lowSeverity)
  {
    ComponentDetails componentDetails = new ComponentDetails(component.getComponentIdentifier());
    componentDetails.setHash(component.getHash());
    componentDetails.setMatchState(MatchState.EXACT.getId());
    componentDetails.setIdentificationSource(IdentificationSource.SONATYPE.getId());
    componentDetails.setCatalogDate(new Date().getTime());
    componentDetails.setWebsite(DEFAULT_WEBSITE);

    if (withVulnerabilities) {
      componentDetails.addSecurityVulnerability(securityVulnerability(highSeverityCveId, highSeverity));
      componentDetails.addSecurityVulnerability(securityVulnerability(lowSeverityCveId, lowSeverity));
    }
    return componentDetails;
  }

  private static SecurityVulnerability securityVulnerability(String refId, float severity) {
    SecurityVulnerability vuln = new SecurityVulnerability();
    vuln.setRefId(refId);
    vuln.setSeverity(severity);
    vuln.setSource("cve");
    return vuln;
  }
}
