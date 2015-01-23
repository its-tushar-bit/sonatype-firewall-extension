/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.insight.brain.dataaccess.component.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.component.SecurityVulnerabilityStatus;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.service.InsightWork;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.codehaus.plexus.util.StringUtils;

/**
 * Assists in loading data for the CIP.
 */
@Named
public class ComponentDetailsLoader
{
  /**
   * Hook to get the details from the HDS.
   */
  public interface HostedDataServicesSource
  {
    /**
     * @return The component details, never {@code null}.
     */
    NamedComponentDetails getDetails() throws IOException;
  }

  private final InsightWork work;

  private final LicenseDAO licenseDAO;

  private final HashComponentIdentifierDAO hashComponentIdentifierDAO;

  @Inject
  public ComponentDetailsLoader(InsightWork work, LicenseDAO licenseDAO,
      HashComponentIdentifierDAO hashComponentIdentifierDAO)
  {
    this.work = work;
    this.licenseDAO = licenseDAO;
    this.hashComponentIdentifierDAO = hashComponentIdentifierDAO;
  }

  /**
   * Gets component details without CLM-specific vulnerability or license augmentation.
   */
  public NamedComponentDetails getComponentDetails(ComponentIdentifier componentIdentifier, String hash,
      String matchState, HostedDataServicesSource hdsSource) throws IOException
  {
    NamedComponentDetails componentDetails = null;

    // Look among claimed components first
    HashComponentIdentifier hashComponentIdentifier = null;
    if (StringUtils.isNotBlank(hash)) {
      hashComponentIdentifier = hashComponentIdentifierDAO.getByHash(hash);
      if (hashComponentIdentifier != null) {
        componentIdentifier = hashComponentIdentifier.getComponentIdentifier();
      }
    }
    else {
      hashComponentIdentifier = hashComponentIdentifierDAO.getByComponentIdentifier(componentIdentifier);
    }

    if (hashComponentIdentifier != null) {
      componentDetails = new NamedComponentDetails();
      componentDetails.setComponentIdentifier(componentIdentifier);
      componentDetails.setHash(hashComponentIdentifier.getHash());
      componentDetails.setMatchState(MatchState.EXACT.getId());
      componentDetails.setCatalogDate(hashComponentIdentifier.getCreateTimeLong());
      componentDetails.setIdentificationSource(IdentificationSource.MANUAL.getId());
      componentDetails.setIdentificationSourceComment(hashComponentIdentifier.getComment());
    }

    // Get component details from the HDS, if not found locally
    if (componentDetails == null) {
      componentDetails = hdsSource.getDetails();
      componentDetails.setHash(hash); // HDS does not set hash
      if (StringUtils.isNotBlank(matchState)) {
        componentDetails.setMatchState(matchState);
      }
      componentDetails.setIdentificationSource(IdentificationSource.SONATYPE.getId());
    }

    return componentDetails;
  }

  /**
   * Augments the supplied component details with vulnerability and license overrides. The returned object is a
   * transcript of the final component details suitable for policy evaluation.
   */
  public Component augmentComponentDetails(Application application, ComponentDetails componentDetails)
      throws IOException
  {
    // Load the augmented data for licenses and security vulnerabilities
    ArrayNode svData = AugmentUtil.getSVData(work, application.getId(), componentDetails.getComponentIdentifier(),
        componentDetails.getSecurityVulnerabilities());
    ComponentDAO componentDAO = new ComponentDAO();
    Component component = componentDAO.getComponent(application, componentDetails, svData);

    // Use CLM data to populate the component details
    for (String licenseId : component.getLicenseOverrideIds()) {
      com.sonatype.insight.brain.model.license.License overriddenLicense = licenseDAO.getByIdNotNull(licenseId);
      componentDetails.getOverriddenLicenses().add(
          new License(overriddenLicense.getId(), overriddenLicense.getShortDisplayName()));
    }

    // Calculate the effective licenses
    Set<License> overriddenLicenses = componentDetails.getOverriddenLicenses();
    if (overriddenLicenses.isEmpty()) {
      Set<License> effectiveLicenses = new LinkedHashSet<>();
      effectiveLicenses.addAll(componentDetails.getDeclaredLicenses());
      effectiveLicenses.addAll(componentDetails.getObservedLicenses());
      effectiveLicenses = removeNonLicensesUnlessNoOtherLicensesExist(effectiveLicenses);
      componentDetails.getEffectiveLicenses().addAll(effectiveLicenses);
    } else {
      componentDetails.getEffectiveLicenses().addAll(componentDetails.getOverriddenLicenses());
      if (LicenseOverrideStatus.OVERRIDDEN.equals(component.getLicenseOverrideStatus())) {
        componentDetails.setEffectiveLicenseStatus(com.sonatype.clm.dto.model.ide.LicenseStatus.Overridden);
      } else if (LicenseOverrideStatus.SELECTED.equals(component.getLicenseOverrideStatus())) {
        componentDetails.setEffectiveLicenseStatus(com.sonatype.clm.dto.model.ide.LicenseStatus.Selected);
      }
    }

    if (!component.getLicenseThreatGroups().isEmpty()) {
      int licenseThreatLevel = 0;
      List<String> licenseThreatGroupNames = new ArrayList<>();
      for (LicenseThreatGroup licenseThreatGroup : component.getLicenseThreatGroups()) {
        final int groupThreatLevel = licenseThreatGroup.getThreatLevel();
        if (groupThreatLevel > licenseThreatLevel) {
          licenseThreatLevel = groupThreatLevel;
          licenseThreatGroupNames.clear();
          licenseThreatGroupNames.add(licenseThreatGroup.getName());
        }
        else if (groupThreatLevel == licenseThreatLevel) {
          licenseThreatGroupNames.add(licenseThreatGroup.getName());
        }
      }
      componentDetails.setLicenseThreatLevel(licenseThreatLevel);
      Collections.sort(licenseThreatGroupNames, String.CASE_INSENSITIVE_ORDER);
      componentDetails.setLicenseThreatGroupNames(licenseThreatGroupNames);
    }
    if (componentDetails.getSecurityVulnerabilities() != null) {
      for (SecurityVulnerability issue : componentDetails.getSecurityVulnerabilities()) {
        issue.setStatus(SecurityVulnerabilityStatus.OPEN.getName());
        for (com.sonatype.insight.brain.model.component.SecurityVulnerability sv : component
            .getSecurityVulnerabilities()) {
          if (issue.getRefId().equals(sv.getRefId()) && issue.getSource().equals(sv.getSource())) {
            issue.setStatus(sv.getStatus().getName());
            break;
          }
        }
      }
    }
    return component;
  }

  /**
   * Return a set containing the licenses other than (No-Source-License, No-Sources, Not-Declared) unless these are the
   * only licenses in the given set, then return the given set.
   */
  private Set<License> removeNonLicensesUnlessNoOtherLicensesExist(Set<License> licenses) {
    Set<License> filtered = new LinkedHashSet<>();
    for (License license : licenses) {
      if (!com.sonatype.insight.brain.model.license.License.isEffectivelyUnspecified(
          license.getLicenseId())) {
        filtered.add(license);
      }
    }

    if (filtered.isEmpty()) {
      return licenses;
    }

    return filtered;
  }
}
