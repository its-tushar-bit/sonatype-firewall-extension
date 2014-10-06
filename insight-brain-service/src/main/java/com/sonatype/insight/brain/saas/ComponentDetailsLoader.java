/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
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
import com.sonatype.clm.dto.model.ide.ComponentDetails;
import com.sonatype.clm.dto.model.ide.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.component.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.component.HashGAVDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.HashGAV;
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
    ComponentDetails getDetails() throws IOException;
  }

  private final InsightWork work;

  private final LicenseDAO licenseDAO;

  private final HashGAVDAO hashGAVDAO;

  @Inject
  public ComponentDetailsLoader(InsightWork work, LicenseDAO licenseDAO, HashGAVDAO hashGAVDAO) {
    this.work = work;
    this.licenseDAO = licenseDAO;
    this.hashGAVDAO = hashGAVDAO;
  }

  /**
   * Gets component details without CLM-specific vulnerability or license augmentation.
   * 
   * @deprecated As of 1.13
   */
  @Deprecated
  public ComponentDetails getComponentDetails(String groupId, String artifactId, String version, String hash,
      String matchState, HostedDataServicesSource hdsSource) throws IOException
  {
    ComponentDetails componentDetails = null;

    // Look among claimed components first
    final HashGAV hashGAV;
    if (StringUtils.isNotBlank(hash)) {
      hashGAV = hashGAVDAO.getByHash(hash);
    }
    else {
      hashGAV = hashGAVDAO.getByGAV(groupId, artifactId, version);
    }
    if (hashGAV != null) {
      componentDetails = new ComponentDetails(hashGAV.getGroupId(), hashGAV.getArtifactId(), hashGAV.getVersion());
      componentDetails.setHash(hashGAV.getHash());
      componentDetails.setMatchState(MatchState.EXACT.getId());
      componentDetails.setCatalogDate(hashGAV.getCreateTimeLong());
      componentDetails.setIdentificationSource(IdentificationSource.MANUAL.getId());
      componentDetails.setIdentificationSourceComment(hashGAV.getComment());
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
   * Gets component details without CLM-specific vulnerability or license augmentation.
   */
  public ComponentDetails getComponentDetails(ComponentIdentifier identifier, String hash,
      String matchState, HostedDataServicesSource hdsSource) throws IOException
  {
    ComponentDetails componentDetails = null;

    // Look among claimed components first
    HashGAV hashGAV = null;
    if (StringUtils.isNotBlank(hash)) {
      hashGAV = hashGAVDAO.getByHash(hash);
      if (hashGAV != null) {
        identifier = ComponentIdentifier.createMavenCoordinates(hashGAV.getGroupId(), hashGAV.getArtifactId(),
            hashGAV.getVersion(), hashGAV.getClassifier(), hashGAV.getExtension());
      }
    }
    else if (ComponentIdentifier.FORMAT_MAVEN.equals(identifier.format)) {
      hashGAV = hashGAVDAO.getByGAV(identifier.get(ComponentIdentifier.MAVEN_GROUP_ID),
          identifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID), identifier.get(ComponentIdentifier.VERSION));
    }

    if (hashGAV != null) {
      componentDetails = new ComponentDetails(hashGAV.getGroupId(), hashGAV.getArtifactId(), hashGAV.getVersion());
      componentDetails.setHash(hashGAV.getHash());
      componentDetails.setMatchState(MatchState.EXACT.getId());
      componentDetails.setCatalogDate(hashGAV.getCreateTimeLong());
      componentDetails.setIdentificationSource(IdentificationSource.MANUAL.getId());
      componentDetails.setIdentificationSourceComment(hashGAV.getComment());
      componentDetails.setIdentifier(identifier);
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
    ArrayNode svData = AugmentUtil.getSVData(work, application.getId(), componentDetails.getGroupId(),
        componentDetails.getArtifactId(), componentDetails.getVersion(), componentDetails.getSecurityVulnerabilities());
    ComponentDAO componentDAO = new ComponentDAO();
    Component component = componentDAO.getComponent(application, componentDetails, svData);

    // Use CLM data to populate the component details
    if (component.getLicenseOverrideId() != null) {
      com.sonatype.insight.brain.model.license.License overriddenLicense = licenseDAO.getByIdNotNull(component
          .getLicenseOverrideId());
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
