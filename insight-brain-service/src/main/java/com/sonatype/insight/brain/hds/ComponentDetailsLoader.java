/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoader;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoaderFactory;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.DependencyType;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.repository.ProprietaryComponentNameDetector;
import com.sonatype.insight.brain.service.Configuration;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.IdentificationSource.isThirdPartyIdentificationSource;

/**
 * Assists in loading data for the CIP.
 */
public class ComponentDetailsLoader
{
  private static final Logger log = LoggerFactory.getLogger(ComponentDetailsLoader.class);

  private static HashComponentIdentifierDAO hashComponentIdentifierDAO;

  private static MultiLicenseDAO multiLicenseDAO;

  @Inject
  public static void inject(
      final HashComponentIdentifierDAO hashComponentIdentifierDAO,
      final MultiLicenseDAO multiLicenseDAO)
  {
    ComponentDetailsLoader.hashComponentIdentifierDAO = hashComponentIdentifierDAO;
    ComponentDetailsLoader.multiLicenseDAO = multiLicenseDAO;
  }

  private final LicenseDAO licenseDAO;

  private final ComponentLoader componentLoader;

  private final ProprietaryComponentNameDetector proprietaryComponentNameDetector;

  private final Configuration configuration;

  ComponentDetailsLoader(
      Owner owner,
      ProprietaryComponentNameDetector proprietaryComponentNameDetector,
      Configuration configuration,
      LicenseDAO licenseDAO,
      ComponentLoaderFactory componentLoaderFactory)
  {
    this.licenseDAO = licenseDAO;
    componentLoader = componentLoaderFactory.createComponentLoader(owner);
    this.proprietaryComponentNameDetector =
        OwnerType.REPOSITORY.equals(owner.getType()) ? proprietaryComponentNameDetector : null;
    this.configuration = configuration;
  }

  /**
   * Gets component details without CLM-specific vulnerability or license augmentation.
   */
  public static NamedComponentDetails getComponentDetails(
      ComponentIdentifier componentIdentifier,
      String hash,
      String matchState,
      HostedDataServicesSource hdsSource) throws IOException
  {
    NamedComponentDetails componentDetails = ComponentDetailsLoader
        .getComponentDetailsLocally(componentIdentifier, hash);

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

  public static Map<String, NamedComponentDetails> getComponentDetailsLocallyByHashes(List<String> hashes) {
    List<String> sanitizedHashes = hashes.stream().filter(StringUtils::isNotBlank).collect(Collectors.toList());

    List<HashComponentIdentifier> hashComponentIdentifiers = hashComponentIdentifierDAO.getByHashes(sanitizedHashes);

    return hashComponentIdentifiers.stream()
        .map(i -> toComponentDetails(i, i.getComponentIdentifier()))
        .collect(Collectors.toMap(NamedComponentDetails::getHash, Function.identity()));
  }

  private static NamedComponentDetails toComponentDetails(
      HashComponentIdentifier hashComponentIdentifier,
      ComponentIdentifier componentIdentifier)
  {
    NamedComponentDetails componentDetails = null;

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

  public static NamedComponentDetails getComponentDetailsLocally(ComponentIdentifier componentIdentifier, String hash) {

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

    return toComponentDetails(hashComponentIdentifier, componentIdentifier);
  }

  private static Set<License> calculateEffectiveLicenses(ComponentDetails componentDetails) {
    Set<String> effectiveLicenseIds = ComponentDetailsLoader.calculateEffectiveLicenses( //
        ComponentDetailsLoader.getLicenseIds(componentDetails.getDeclaredLicenses()), //
        ComponentDetailsLoader.getLicenseIds(componentDetails.getObservedLicenses()), //
        ComponentDetailsLoader.getLicenseIds(componentDetails.getOverriddenLicenses()));

    return ComponentDetailsLoader.loadLicenses(effectiveLicenseIds);
  }

  public static Set<String> calculateEffectiveLicenses(
      Set<String> declaredLicenseIds,
      Set<String> observedLicenseIds,
      Set<String> overriddenLicenseIds)
  {
    if (overriddenLicenseIds != null && !overriddenLicenseIds.isEmpty()) {
      return overriddenLicenseIds;
    }

    return ComponentDetailsLoader.calculateEffectiveLicenses(declaredLicenseIds, observedLicenseIds);
  }

  public static Set<String> calculateEffectiveLicenses(Set<String> declaredLicenseIds, Set<String> observedLicenseIds) {
    Set<String> effectiveLicenses = new LinkedHashSet<>();
    effectiveLicenses.addAll(declaredLicenseIds);
    effectiveLicenses.addAll(observedLicenseIds);
    return ComponentDetailsLoader.removeNonLicensesUnlessNoOtherLicensesExist(effectiveLicenses);
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

  /**
   * Augments the supplied component details with local data like labels, license and security vulnerability overrides.
   * This overloaded method accepts a collection of ComponentDetails objects and
   * calls another overloaded method that sets match state and identification source
   * The returned list of Component objects is a transcript of the final component details
   * suitable for policy evaluation.
   *
   * Purpose: This method is used by ComponentInfoService and ComponentRemediationService
   * to additionally set match state and identification source prior to augmenting other details
   *
   * @param componentDetailsList the list of ComponentDetails objects to be augmented
   * @param matchState the MatchState to set
   * @param dependencyType the DependencyType to set
   * @return List<Component> augmented list of Component objects
   */
  public List<Component> augmentComponentDetails(
      Collection<ComponentDetails> componentDetailsList,
      String matchState,
      DependencyType dependencyType)
  {
    long start = System.currentTimeMillis();

    List<Component> components = new ArrayList<>(componentDetailsList.size());
    for (ComponentDetails componentDetails : componentDetailsList) {
      components.add(augmentComponentDetails(componentDetails, matchState, dependencyType));
    }

    log.debug("Augmented component details for {} components in {} ms.", componentDetailsList.size(),
        System.currentTimeMillis() - start);
    return components;
  }

  /**
   * Augments the supplied component details with local data like labels, license and security vulnerability overrides.
   * This overloaded method sets match state to what is supplied or exact if not supplied and
   * overrides identification source to SONATYPE if it is set to anything other than MANUAL or SONATYPE
   * The returned Component object is a transcript of the final component details suitable for policy evaluation.
   *
   * Purpose: This method is used by ComponentInfoService and ComponentRemediationService
   * to additionally set match state and identification source prior to augmenting other details
   *
   * @param componentDetails the ComponentDetails object to be augmented
   * @param matchState the MatchState to set
   * @return Component augmented Component object
   */
  public Component augmentComponentDetails(
      ComponentDetails componentDetails,
      String matchState,
      DependencyType dependencyType)
  {
    componentDetails.setMatchState(StringUtils.isBlank(matchState) ? MatchState.EXACT.getId() : matchState);

    if (!isThirdPartyIdentificationSource(componentDetails.getIdentificationSource())
        && !IdentificationSource.PACKAGE_MANIFEST.getId().equals(componentDetails.getIdentificationSource()))
    {
      componentDetails.setIdentificationSource(IdentificationSource.SONATYPE.getId());
    }

    return augmentComponentDetails(componentDetails, dependencyType);
  }

  public Component augmentComponentDetails(ComponentDetails componentDetails, DependencyType dependencyType) {
    Component component = augmentComponentDetails(componentDetails);

    if (dependencyType != null) {
      component.setDirectDependency(dependencyType == DependencyType.DIRECT);
    }

    return component;
  }

  /**
   * Augments the supplied component details with local data like labels, license and security vulnerability overrides.
   * The returned Component object is a transcript of the final component details suitable for policy evaluation.
   *
   * Purpose: This method is used to augmenting basic details as indicated above
   *
   * @param componentDetails the ComponentDetails object to be augmented
   * @return Component augmented Component object
   */
  public Component augmentComponentDetails(ComponentDetails componentDetails) {
    Component component = getComponent(componentDetails);
    componentDetails.setObservedLicenses(loadLicenses(component.getObservedMultiLicenseIds()));

    // Use CLM data to populate the component details
    for (String licenseId : component.getLicenseOverrideIds()) {
      com.sonatype.insight.brain.model.license.License overriddenLicense = getLicense(licenseId);
      componentDetails.getOverriddenLicenses()
          .add(
              new License(overriddenLicense.getId(), overriddenLicense.getShortDisplayName()));
    }

    // Calculate the effective licenses
    componentDetails.getEffectiveLicenses().addAll(ComponentDetailsLoader.calculateEffectiveLicenses(componentDetails));
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
            .getSecurityVulnerabilities())
        {
          if (issue.getRefId().equals(sv.getRefId()) && isSameSource(issue.getSource(), sv.getSource())) {
            issue.setStatus(sv.getStatus().getName());
            break;
          }
        }
      }
    }
    return component;
  }

  private Component getComponent(ComponentDetails componentDetails) {
    Component component =
        componentLoader.getComponent(componentDetails, configuration.isALPObservedLicenseDetectionEnabled());
    if (proprietaryComponentNameDetector != null) {
      component.setConflictingProprietaryName(
          proprietaryComponentNameDetector.findProprietaryComponentName(component.getComponentIdentifier()));
    }

    if (componentDetails.getAnalyzerFeatures() != null) {
      component.setAnalyzerFeatures(componentDetails.getAnalyzerFeatures());
    }
    return component;
  }

  private com.sonatype.insight.brain.model.license.License getLicense(final String licenseId) {
    return licenseDAO.getByIdNotNull(licenseId);
  }

  private boolean isSameSource(final String issueSource, final String svSource) {
    // for third party components the source may not exist
    if (issueSource == null) {
      return svSource == null;
    }
    return issueSource.equals(svSource);
  }

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
}
