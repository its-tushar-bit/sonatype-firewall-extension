/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.ComponentEndOfLifeStatus;
import com.sonatype.clm.dto.model.DerivedFromAiModel;
import com.sonatype.clm.dto.model.component.AggregateFile;
import com.sonatype.clm.dto.model.component.AiModelContentType;
import com.sonatype.clm.dto.model.component.AnalyzerFeatures;
import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.ComponentIdentifierAndHashComparable;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityGroupVulnerability;

import com.fasterxml.jackson.annotation.JsonIgnore;

import static com.sonatype.clm.dto.model.component.ComponentIdentifier.MAVEN_ARTIFACT_ID;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.MAVEN_GROUP_ID;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.VERSION;

public class Component
    implements ComponentIdentifierAndHashComparable
{
  private String hash;

  private String sha256;

  private Set<String> declaredMultiLicenseIds = new LinkedHashSet<>();

  private Set<String> observedMultiLicenseIds = new LinkedHashSet<>();

  private Set<String> declaredLicenseIds = new LinkedHashSet<>();

  private Set<String> observedLicenseIds = new LinkedHashSet<>();

  private Set<String> licenseOverrideIds = new LinkedHashSet<>();

  private Set<String> unassignedLicenseIds = new LinkedHashSet<>();

  private Map<String, LicenseThreatGroup> licenseThreatGroupsById = new LinkedHashMap<>();

  private Map<String, Set<String>> licenseIdsByThreatGroupId = new HashMap<>();

  private LicenseOverrideStatus licenseOverrideStatus;

  private List<SecurityVulnerability> securityVulnerabilities;

  private Map<String, List<VulnerabilityGroupVulnerability>> vulnerabilityGroupVulnerabilities;

  private Integer relativePopularity;

  private MatchState matchState = MatchState.UNKNOWN;

  private Long catalogDate;

  private List<String> labelIds = new ArrayList<>();

  private boolean proprietary;

  private Optional<ProprietaryComponentName> conflictingProprietaryName;

  private IdentificationSource identificationSource = IdentificationSource.SONATYPE;

  private List<String> pathnames = new ArrayList<>();

  private List<String> filenames = new ArrayList<>();

  private List<AggregateFile> aggregateFiles = new ArrayList<>();

  private String displayName;

  private List<ComponentCategory> componentCategories = new ArrayList<>();

  private HygieneRating hygieneRating;

  private IntegrityRating integrityRating;

  private ComponentIdentifier componentIdentifier;

  private AnalyzerFeatures analyzerFeatures;

  private Set<InnerSourceData> innerSourceData;

  private Boolean directDependency;

  private Boolean innerSource;

  private Set<String> parentComponentPurls;

  private boolean hiddenObservedLicenses;

  private ComponentEndOfLifeStatus endOfLife;

  private DerivedFromAiModel derivedFromAiModel;

  private Set<AiModelContentType> aiModelContentTypes;

  private String packageUrl;

  private String originalPurl;

  public Component() {
  }

  public Component(final ComponentIdentifier componentIdentifier) {
    this.componentIdentifier = componentIdentifier;
  }

  public String getGroupId() {
    return componentIdentifier != null ? componentIdentifier.get(MAVEN_GROUP_ID) : null;
  }

  public String getArtifactId() {
    return componentIdentifier != null ? componentIdentifier.get(MAVEN_ARTIFACT_ID) : null;
  }

  public String getVersion() {
    return componentIdentifier != null ? componentIdentifier.get(VERSION) : null;
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
      securityVulnerabilities = new ArrayList<>();
    }
    securityVulnerabilities.add(securityVulnerability);
  }

  public Map<String, List<VulnerabilityGroupVulnerability>> getVulnerabilityGroupVulnerabilities() {
    if (vulnerabilityGroupVulnerabilities == null) {
      return Collections.emptyMap();
    }
    return vulnerabilityGroupVulnerabilities;
  }

  public void setVulnerabilityGroupVulnerabilities(
      final Map<String, List<VulnerabilityGroupVulnerability>> vulnerabilityGroupVulnerabilities)
  {
    this.vulnerabilityGroupVulnerabilities = vulnerabilityGroupVulnerabilities;
  }

  @JsonIgnore
  public String getDisplayNameFromIdentifier() {
    ComponentDisplayName componentDisplayName = ComponentDisplayNameUtil.fromIdentifier(componentIdentifier);
    return componentDisplayName != null ? componentDisplayName.toString() : null;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public Set<String> getDeclaredMultiLicenseIds() {
    return declaredMultiLicenseIds;
  }

  public void setDeclaredMultiLicenseIds(Set<String> declaredMultiLicenseIds) {
    this.declaredMultiLicenseIds.clear();

    if (declaredMultiLicenseIds == null) {
      return;
    }

    this.declaredMultiLicenseIds.addAll(declaredMultiLicenseIds);
  }

  public Set<String> getObservedMultiLicenseIds() {
    return observedMultiLicenseIds;
  }

  public void setObservedMultiLicenseIds(Set<String> observedMultiLicenseIds) {
    this.observedMultiLicenseIds.clear();

    if (observedMultiLicenseIds == null) {
      return;
    }

    this.observedMultiLicenseIds.addAll(observedMultiLicenseIds);
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
    if (!licenseOverrideIds.isEmpty()) {
      return licenseOverrideIds.contains(licenseId);
    }
    if (declaredLicenseIds.contains(licenseId)) {
      return true;
    }
    return observedLicenseIds.contains(licenseId);
  }

  public Set<String> getLicenseIds() {
    final Set<String> licenseIds = new HashSet<>();
    if (!licenseOverrideIds.isEmpty()) {
      licenseIds.addAll(licenseOverrideIds);
    }
    else {
      licenseIds.addAll(declaredLicenseIds);
      licenseIds.addAll(observedLicenseIds);
    }
    return licenseIds;
  }

  public Integer getRelativePopularity() {
    return relativePopularity;
  }

  public void setRelativePopularity(Integer relativePopularity) {
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
    return !getLicenseOverrideIds().isEmpty();
  }

  public MatchState getMatchState() {
    return matchState;
  }

  public void setMatchState(MatchState matchState) {
    this.matchState = matchState;
  }

  @Override
  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public String getSha256() {
    return sha256;
  }

  public void setSha256(String sha256) {
    this.sha256 = sha256;
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

  @JsonIgnore
  public Set<LicenseThreatGroup> getLicenseThreatGroups() {
    final Set<LicenseThreatGroup> licenseThreatGroups = new LinkedHashSet<>();
    for (LicenseThreatGroup licenseThreatGroup : licenseThreatGroupsById.values()) {
      licenseThreatGroups.add(licenseThreatGroup);
    }
    return licenseThreatGroups;
  }

  public void addLicenseIdByThreatGroupId(String licenseId, String licenseThreatGroupId) {
    Set<String> licenseIds = licenseIdsByThreatGroupId.computeIfAbsent(licenseThreatGroupId, k -> new TreeSet<>());
    licenseIds.add(licenseId);
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

  /**
   * @return {@code null} if unknown/inapplicable, empty if no conflict, otherwise conflicting name.
   */
  public Optional<ProprietaryComponentName> getConflictingProprietaryName() {
    return conflictingProprietaryName;
  }

  public void setConflictingProprietaryName(ProprietaryComponentName conflictingProprietaryName) {
    this.conflictingProprietaryName = Optional.ofNullable(conflictingProprietaryName);
  }

  public IdentificationSource getIdentificationSource() {
    return identificationSource;
  }

  public void setIdentificationSource(IdentificationSource identificationSource) {
    this.identificationSource = identificationSource;
  }

  public void setIdentificationSource(final String id) {
    this.identificationSource = IdentificationSource.getById(id);
  }

  public void setIntegrityRating(final String id) {
    this.integrityRating = IntegrityRating.getById(id);
  }

  public void setLicenseOverrideIds(Set<String> licenseOverrideIds) {
    this.licenseOverrideIds.clear();

    if (licenseOverrideIds == null) {
      return;
    }

    this.licenseOverrideIds.addAll(licenseOverrideIds);
  }

  public Set<String> getLicenseOverrideIds() {
    return licenseOverrideIds;
  }

  public void addLicenseOverrideId(String licenseOverrideId) {
    licenseOverrideIds.add(licenseOverrideId);
  }

  public List<String> getPathnames() {
    return pathnames;
  }

  public void addPathname(String pathname) {
    pathnames.add(pathname);
  }

  public List<String> getFilenames() {
    return filenames;
  }

  public void addFilename(String filename) {
    filenames.add(filename);
  }

  public List<AggregateFile> getAggregateFiles() {
    return aggregateFiles;
  }

  public void addAggregateFile(AggregateFile aggregateFile) {
    aggregateFiles.add(aggregateFile);
  }

  @Override
  public ComponentIdentifier getComponentIdentifier() {
    return componentIdentifier;
  }

  public void setComponentIdentifier(final ComponentIdentifier componentIdentifier) {
    this.componentIdentifier = componentIdentifier;
  }

  public Set<String> getUnassignedLicenseIds() {
    return unassignedLicenseIds;
  }

  public void setUnassignedLicenseIds(Set<String> unassignedLicenseIds) {
    this.unassignedLicenseIds.clear();

    if (unassignedLicenseIds == null) {
      return;
    }

    this.unassignedLicenseIds.addAll(unassignedLicenseIds);
  }

  @JsonIgnore
  public Set<String> getLicenseIdsInLicenseThreatGroup(String licenseThreatGroupId) {
    return licenseIdsByThreatGroupId.getOrDefault(licenseThreatGroupId, Collections.emptySet());
  }

  public List<ComponentCategory> getComponentCategories() {
    return componentCategories;
  }

  public void addComponentCategory(ComponentCategory componentCategory) {
    componentCategories.add(componentCategory);
  }

  public HygieneRating getHygieneRating() {
    return hygieneRating;
  }

  public void setHygieneRating(final HygieneRating hygieneRating) {
    this.hygieneRating = hygieneRating;
  }

  public IntegrityRating getIntegrityRating() {
    return integrityRating;
  }

  public void setIntegrityRating(final IntegrityRating integrityRating) {
    this.integrityRating = integrityRating;
  }

  public AnalyzerFeatures getAnalyzerFeatures() {
    return analyzerFeatures;
  }

  public void setAnalyzerFeatures(final AnalyzerFeatures analyzerFeatures) {
    this.analyzerFeatures = analyzerFeatures;
  }

  public Boolean getDirectDependency() {
    return directDependency;
  }

  public void setDirectDependency(final Boolean directDependency) {
    this.directDependency = directDependency;
  }

  public Set<InnerSourceData> getInnerSourceData() {
    return innerSourceData;
  }

  public void setInnerSourceData(final Set<InnerSourceData> innerSourceData) {
    this.innerSourceData = innerSourceData;
  }

  public Boolean getInnerSource() {
    return innerSource;
  }

  public void setInnerSource(final Boolean innerSource) {
    this.innerSource = innerSource;
  }

  public Set<String> getParentComponentPurls() {
    return parentComponentPurls;
  }

  public ComponentEndOfLifeStatus getEndOfLife() {
    return endOfLife;
  }

  public void setEndOfLife(final ComponentEndOfLifeStatus componentEndOfLifeStatus) {
    this.endOfLife = componentEndOfLifeStatus;
  }

  public Set<String> getInnerComponentPurls() {
    return Optional.ofNullable(innerSourceData)
        .map(innerSourceElement -> innerSourceElement
            .stream()
            .map(InnerSourceData::getInnerSourceComponentPurl)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet()))
        .orElse(Collections.emptySet());
  }

  public void setParentComponentPurls(final Set<String> parentComponentPurls) {
    this.parentComponentPurls = parentComponentPurls;
  }

  public boolean isHiddenObservedLicenses() {
    return hiddenObservedLicenses;
  }

  public void setHiddenObservedLicenses(Boolean hiddenObservedLicenses) {
    this.hiddenObservedLicenses = hiddenObservedLicenses;
  }

  public DerivedFromAiModel getDerivedFromAiModel() {
    return derivedFromAiModel;
  }

  public void setDerivedFromAiModel(DerivedFromAiModel derivedFromAiModel) {
    this.derivedFromAiModel = derivedFromAiModel;
  }

  public Set<AiModelContentType> getAiModelContentTypes() {
    return aiModelContentTypes == null ? Collections.emptySet() : aiModelContentTypes;
  }

  public void setAiModelContentTypes(Set<AiModelContentType> aiModelContentTypes) {
    this.aiModelContentTypes = aiModelContentTypes;
  }

  public String getPackageUrl() {
    return packageUrl;
  }

  public void setPackageUrl(final String packageUrl) {
    this.packageUrl = packageUrl;
  }

  public String getOriginalPurl() {
    return originalPurl;
  }

  public void setOriginalPurl(final String originalPurl) {
    this.originalPurl = originalPurl;
  }
}
