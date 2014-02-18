/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Component
{
  private String groupId;

  private String artifactId;

  private String version;

  private String hash;

  private Set<String> declaredLicenseIds = new LinkedHashSet<String>();

  private Set<String> observedLicenseIds = new LinkedHashSet<String>();

  private String licenseOverrideId;

  private Map<String, LicenseThreatGroup> licenseThreatGroupsById = new LinkedHashMap<String, LicenseThreatGroup>();

  private LicenseOverrideStatus licenseOverrideStatus;

  private List<SecurityVulnerability> securityVulnerabilities;

  private int relativePopularity;

  private MatchState matchState = MatchState.UNKNOWN;

  private Long catalogDate;

  private List<String> labelIds = new ArrayList<String>();

  private boolean proprietary;

  private IdentificationSource identificationSource = IdentificationSource.SONATYPE;

  private List<String> pathnames = new ArrayList<>();

  public Component() {
  }

  public Component(final String groupId, final String artifactId, final String version, MatchState matchState) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    this.matchState = matchState;
  }

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(final String groupId) {
    this.groupId = groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public void setArtifactId(final String artifactId) {
    this.artifactId = artifactId;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(final String version) {
    this.version = version;
  }

  public List<SecurityVulnerability> getSecurityVulnerabilitiesByStatusId(String securityVulnerabilityStatusId) {
    if (getSecurityVulnerabilities().isEmpty()) {
      return Collections.emptyList();
    }

    SecurityVulnerabilityStatus status = SecurityVulnerabilityStatus.getById(securityVulnerabilityStatusId);
    List<SecurityVulnerability> result = new ArrayList<SecurityVulnerability>();
    for (SecurityVulnerability securityVulnerability : securityVulnerabilities) {
      if (status.equals(securityVulnerability.getStatus())) {
        result.add(securityVulnerability);
      }
    }
    return result;
  }

  public List<SecurityVulnerability> getSecurityVulnerabilities() {
    if (securityVulnerabilities == null) {
      return Collections.emptyList();
    }
    return securityVulnerabilities;
  }

  public void setSecurityVulnerabilities(final List<SecurityVulnerability> securityVulnerabilities) {
    this.securityVulnerabilities = securityVulnerabilities;
  }

  public void addSecurityVulnerability(final SecurityVulnerability securityVulnerability) {
    if (securityVulnerabilities == null) {
      securityVulnerabilities = new ArrayList<SecurityVulnerability>();
    }
    securityVulnerabilities.add(securityVulnerability);
  }

  @JsonIgnore
  public String getGAV() {
    return groupId + ':' + artifactId + ':' + version;
  }

  public Set<String> getDeclaredLicenseIds() {
    return declaredLicenseIds;
  }

  public void setDeclaredLicenseIds(Set<String> declaredLicenseIds) {
    this.declaredLicenseIds.clear();

    if (declaredLicenseIds == null) {
      return;
    }

    this.declaredLicenseIds.addAll(declaredLicenseIds);
  }

  public void addDeclaredLicenseId(String licenseId) {
    declaredLicenseIds.add(licenseId);
  }

  public Set<String> getObservedLicenseIds() {
    return observedLicenseIds;
  }

  public void setObservedLicenseIds(Set<String> observedLicenseIds) {
    this.observedLicenseIds.clear();

    if (observedLicenseIds == null) {
      return;
    }

    this.observedLicenseIds.addAll(observedLicenseIds);
  }

  public void addObservedLicenseId(String licenseId) {
    observedLicenseIds.add(licenseId);
  }

  public boolean hasLicenseId(String licenseId) {
    if (licenseOverrideId != null) {
      return licenseOverrideId.equals(licenseId);
    }
    if (declaredLicenseIds.contains(licenseId)) {
      return true;
    }
    return observedLicenseIds.contains(licenseId);
  }

  public Set<String> getLicenseIds() {
    final Set<String> licenseIds = new HashSet<String>();
    if (licenseOverrideId != null) {
      licenseIds.add(licenseOverrideId);
    }
    else {
      licenseIds.addAll(declaredLicenseIds);
      licenseIds.addAll(observedLicenseIds);
    }
    return licenseIds;
  }

  public int getRelativePopularity() {
    return relativePopularity;
  }

  public void setRelativePopularity(int relativePopularity) {
    this.relativePopularity = relativePopularity;
  }

  public LicenseOverrideStatus getLicenseOverrideStatus() {
    if (licenseOverrideStatus == null) {
      licenseOverrideStatus = LicenseOverrideStatus.OPEN;
    }
    return licenseOverrideStatus;
  }

  public void setLicenseOverrideStatus(LicenseOverrideStatus licenseOverrideStatus) {
    this.licenseOverrideStatus = licenseOverrideStatus;
  }

  @JsonIgnore
  public boolean isLicenseOverridden() {
    return getLicenseOverrideId() != null;
  }

  public MatchState getMatchState() {
    return matchState;
  }

  public void setMatchState(MatchState matchState) {
    this.matchState = matchState;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public Long getCatalogDate() {
    return catalogDate;
  }

  public void setCatalogDate(Long catalogDate) {
    this.catalogDate = catalogDate;
  }

  public void addLabelId(String labelId) {
    labelIds.add(labelId);
  }

  public boolean hasLabelId(String labelId) {
    return labelIds.contains(labelId);
  }

  public List<String> getLabelIds() {
    return labelIds;
  }

  public void addLicenseThreatGroup(LicenseThreatGroup licenseThreatGroup) {
    if (licenseThreatGroup == null) {
      return;
    }
    licenseThreatGroupsById.put(licenseThreatGroup.getId(), licenseThreatGroup);
  }

  public boolean hasLicenseInLicenseThreatGroup(String licenseThreatGroupId) {
    return licenseThreatGroupsById.keySet().contains(licenseThreatGroupId);
  }

  @JsonIgnore
  public Set<LicenseThreatGroup> getLicenseThreatGroups() {
    final Set<LicenseThreatGroup> licenseThreatGroups = new LinkedHashSet<LicenseThreatGroup>();
    for (LicenseThreatGroup licenseThreatGroup : licenseThreatGroupsById.values()) {
      licenseThreatGroups.add(licenseThreatGroup);
    }
    return licenseThreatGroups;
  }

  public Integer getLicenseThreatLevel() {
    Integer threatLevel = null;

    for (LicenseThreatGroup licenseThreatGroup : getLicenseThreatGroups()) {
      threatLevel = Math.max(threatLevel != null ? threatLevel : 0, licenseThreatGroup.getThreatLevel());
    }

    return threatLevel;
  }

  @Override
  public String toString() {
    return getHash() + " " + getMatchState();
  }

  public boolean isProprietary() {
    return proprietary;
  }

  public void setProprietary(boolean proprietary) {
    this.proprietary = proprietary;
  }

  public IdentificationSource getIdentificationSource() {
    return identificationSource;
  }

  public void setIdentificationSource(IdentificationSource identificationSource) {
    this.identificationSource = identificationSource;
  }

  public void setLicenseOverrideId(String licenseOverrideId) {
    this.licenseOverrideId = licenseOverrideId;
  }

  public String getLicenseOverrideId() {
    return licenseOverrideId;
  }

  public List<String> getPathnames() {
    return pathnames;
  }

  public void addPathname(String pathname) {
    pathnames.add(pathname);
  }
}
