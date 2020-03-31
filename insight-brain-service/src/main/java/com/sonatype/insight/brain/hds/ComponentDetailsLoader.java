/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.component.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;

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

  private final LicenseDAO licenseDAO;

  private final HashComponentIdentifierDAO hashComponentIdentifierDAO;

  @Inject
  public ComponentDetailsLoader(LicenseDAO licenseDAO,
                                HashComponentIdentifierDAO hashComponentIdentifierDAO)
  {
    this.licenseDAO = licenseDAO;
    this.hashComponentIdentifierDAO = hashComponentIdentifierDAO;
  }

  /**
   * Gets component details without CLM-specific vulnerability or license augmentation.
   */
  public NamedComponentDetails getComponentDetails(ComponentIdentifier componentIdentifier,
                                                   String hash,
                                                   String matchState,
                                                   HostedDataServicesSource hdsSource) throws IOException
  {
    NamedComponentDetails componentDetails = getComponentDetailsLocally(componentIdentifier, hash);

    // Get component details from the HDS, if not found locally
    if (componentDetails == null) {
      componentDetails = hdsSource.getDetails();
      if (StringUtils.isNotBlank(hash)) {
        componentDetails.setHash(hash);
      }
      if (StringUtils.isNotBlank(matchState)) {
        componentDetails.setMatchState(matchState);
      }
      if (StringUtils.isBlank(componentDetails.getIdentificationSource())) {
        componentDetails.setIdentificationSource(IdentificationSource.SONATYPE.getId());
      }
    }

    return componentDetails;
  }

  public NamedComponentDetails getComponentDetailsLocally(ComponentIdentifier componentIdentifier, String hash) {
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

    return componentDetails;
  }

  /**
   * Augments the supplied component details with local data like labels, license and security vulnerability overrides.
   * The returned object is a transcript of the final component details suitable for policy evaluation.
   */
  public Component augmentComponentDetails(Owner owner, ComponentDetails componentDetails) {
    ComponentDAO componentDAO = new ComponentDAO();
    Component component = componentDAO.getComponent(owner, componentDetails);

    // Use CLM data to populate the component details
    for (String licenseId : component.getLicenseOverrideIds()) {
      com.sonatype.insight.brain.model.license.License overriddenLicense = licenseDAO.getByIdNotNull(licenseId);
      componentDetails.getOverriddenLicenses().add(
          new License(overriddenLicense.getId(), overriddenLicense.getShortDisplayName()));
    }

    // Calculate the effective licenses
    componentDetails.getEffectiveLicenses().addAll(calculateEffectiveLicenses(componentDetails));
    if (!componentDetails.getOverriddenLicenses().isEmpty()) {
      if (LicenseOverrideStatus.OVERRIDDEN.equals(component.getLicenseOverrideStatus())) {
        componentDetails.setEffectiveLicenseStatus(com.sonatype.clm.dto.model.ide.LicenseStatus.Overridden);
      }
      else if (LicenseOverrideStatus.SELECTED.equals(component.getLicenseOverrideStatus())) {
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
      licenseThreatGroupNames.sort(String.CASE_INSENSITIVE_ORDER);
      componentDetails.setLicenseThreatGroupNames(licenseThreatGroupNames);
    }
    if (componentDetails.getSecurityVulnerabilities() != null) {
      for (SecurityVulnerability issue : componentDetails.getSecurityVulnerabilities()) {
        issue.setStatus(SecurityVulnerabilityOverrideStatus.OPEN.getName());
        for (com.sonatype.insight.brain.model.component.SecurityVulnerability sv : component
            .getSecurityVulnerabilities()) {
          if (issue.getRefId().equals(sv.getRefId()) && isSameSource(issue.getSource(), sv.getSource())) {
            issue.setStatus(sv.getStatus().getName());
            break;
          }
        }
      }
    }
    return component;
  }

  private boolean isSameSource(final String issueSource, final String svSource) {
    //for third party components the source may not exist
    if (issueSource == null) {
      return svSource == null;
    }
    return issueSource.equals(svSource);
  }

  private static Set<License> calculateEffectiveLicenses(ComponentDetails componentDetails) {
    Set<String> effectiveLicenseIds = calculateEffectiveLicenses( //
        getLicenseIds(componentDetails.getDeclaredLicenses()), //
        getLicenseIds(componentDetails.getObservedLicenses()), //
        getLicenseIds(componentDetails.getOverriddenLicenses()));
    
    return loadLicenses(effectiveLicenseIds);
  }

  public static Set<String> calculateEffectiveLicenses(
      Set<String> declaredLicenseIds,
      Set<String> observedLicenseIds,
      Set<String> overriddenLicenseIds)
  {
    if (overriddenLicenseIds != null && !overriddenLicenseIds.isEmpty()) {
      return overriddenLicenseIds;
    }

    return calculateEffectiveLicenses(declaredLicenseIds, observedLicenseIds);
  }

  public static Set<String> calculateEffectiveLicenses(Set<String> declaredLicenseIds, Set<String> observedLicenseIds) {
    Set<String> effectiveLicenses = new LinkedHashSet<>();
    effectiveLicenses.addAll(declaredLicenseIds);
    effectiveLicenses.addAll(observedLicenseIds);
    return removeNonLicensesUnlessNoOtherLicensesExist(effectiveLicenses);
  }

  /**
   * Return a set containing the licenses other than (No-Source-License, No-Sources, Not-Declared, Not-Supported)
   * unless these are the only licenses in the given set, then return the given set.
   */
  private static Set<String> removeNonLicensesUnlessNoOtherLicensesExist(Set<String> licenseIds) {
    Set<String> filtered = new LinkedHashSet<>();
    for (String licenseId : licenseIds) {
      if (!com.sonatype.insight.brain.model.license.License.isEffectivelyUnspecified(licenseId)) {
        filtered.add(licenseId);
      }
    }

    if (filtered.isEmpty()) {
      return licenseIds;
    }

    return filtered;
  }

  private static Set<License> loadLicenses(Set<String> licenseIds) {
    MultiLicenseDAO multiLicenseDAO = new MultiLicenseDAO();
    Set<License> licenses = new HashSet<>();
    for (String licenseId : licenseIds) {
      License license = new License(licenseId, multiLicenseDAO.getByIdNotNull(licenseId).getShortDisplayName());
      licenses.add(license);
    }
    return licenses;
  }

  private static Set<String> getLicenseIds(Set<License> licenses) {
    return licenses.stream().map(License::getLicenseId).collect(Collectors.toSet());
  }
}
