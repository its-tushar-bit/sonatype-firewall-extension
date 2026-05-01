/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.ComponentEndOfLifeStatus;
import com.sonatype.clm.dto.model.ComponentInfo;
import com.sonatype.clm.dto.model.DerivedFromAiModel;
import com.sonatype.clm.dto.model.EpssData;
import com.sonatype.clm.dto.model.KevData;
import com.sonatype.clm.dto.model.component.AggregateFile;
import com.sonatype.clm.dto.model.component.AiModelContentType;
import com.sonatype.clm.dto.model.component.AnalyzerFeatures;
import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCvssSeverityDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCvssVectorDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCweDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomRemediationDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityGroupDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityGroupVulnerabilityDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.ComponentCategory;
import com.sonatype.insight.brain.model.component.HygieneRating;
import com.sonatype.insight.brain.model.component.InnerSourceData;
import com.sonatype.insight.brain.model.component.IntegrityRating;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.component.SecurityVulnerabilityCategory;
import com.sonatype.insight.brain.model.component.SecurityVulnerabilityCustomData;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCvssSeverity;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCvssVector;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCwe;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomRemediation;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityGroup;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityGroupVulnerability;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityDetectionType;
import com.sonatype.insight.vulnerability.model.SecurityVulnerabilityResearchType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class ComponentLoader
{
  private static final String DIRECT_DEPENDENCY_FIELD = "directDependency";

  public static final String PARENT_COMPONENT_PURLS_FIELD = "parentComponentPurls";

  public static final String INNER_SOURCE_DATA_FIELD = "innerSourceData";

  public static final String DISPLAY_NAME_FIELD = "displayName";

  private final MultiLicenseDAO multiLicenseDAO;

  private final LicenseThreatGroupDAO licenseThreatGroupDAO;

  private final LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO;

  private final LicenseOverrideDAO licenseOverrideDAO;

  private final SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO;

  private final OwnerDAO ownerDAO;

  private final VulnerabilityCustomRemediationDAO vulnerabilityCustomRemediationDAO;

  private final VulnerabilityCustomCweDAO vulnerabilityCustomCweDAO;

  private final VulnerabilityCustomCvssVectorDAO vulnerabilityCustomCvssVectorDAO;

  private final VulnerabilityCustomCvssSeverityDAO vulnerabilityCustomCvssSeverityDAO;

  private final ComponentLabelDAO componentLabelDAO;

  private final VulnerabilityGroupDAO vulnerabilityGroupDAO;

  private final VulnerabilityGroupVulnerabilityDAO vulnerabilityGroupVulnerabilityDAO;

  private final Owner owner;

  private List<String> ownerIds;

  private Map<String, LicenseThreatGroup> licenseThreatGroupsById;

  private Collection<LicenseThreatGroupLicense> licenseThreatGroupLicenses;

  private Map<ComponentIdentifier, LicenseOverride> licenseOverridesByComponentIdentifier;

  private Map<String, Collection<SecurityVulnerabilityOverride>> securityVulnerabilityOverridesByHash;

  private Map<String, Collection<ComponentLabel>> componentLabelsByHash;

  private Map<SimpleEntry<String, ComponentIdentifier>, VulnerabilityCustomRemediation> customRemediations;

  private Map<SimpleEntry<String, ComponentIdentifier>, VulnerabilityCustomCwe> customCwes;

  private Map<SimpleEntry<String, ComponentIdentifier>, VulnerabilityCustomCvssVector> customCvssVectors;

  private Map<SimpleEntry<String, ComponentIdentifier>, VulnerabilityCustomCvssSeverity> customCvssSeverities;

  private Map<String, List<VulnerabilityGroupVulnerability>> vulnerabilityGroupVulnerabilitiesByGroupId;

  public ComponentLoader(
      final Owner owner,
      final MultiLicenseDAO multiLicenseDAO,
      final LicenseThreatGroupDAO licenseThreatGroupDAO,
      final LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO,
      final LicenseOverrideDAO licenseOverrideDAO,
      final SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO,
      final OwnerDAO ownerDAO,
      final ComponentLabelDAO componentLabelDAO,
      final VulnerabilityCustomRemediationDAO vulnerabilityCustomRemediationDAO,
      final VulnerabilityCustomCweDAO vulnerabilityCustomCweDAO,
      final VulnerabilityCustomCvssVectorDAO vulnerabilityCustomCvssVectorDAO,
      final VulnerabilityCustomCvssSeverityDAO vulnerabilityCustomCvssSeverityDAO,
      final VulnerabilityGroupDAO vulnerabilityGroupDAO,
      final VulnerabilityGroupVulnerabilityDAO vulnerabilityGroupVulnerabilityDAO)
  {
    this.ownerDAO = ownerDAO;
    this.multiLicenseDAO = multiLicenseDAO;
    this.licenseThreatGroupDAO = licenseThreatGroupDAO;
    this.licenseThreatGroupLicenseDAO = licenseThreatGroupLicenseDAO;
    this.licenseOverrideDAO = licenseOverrideDAO;
    this.securityVulnerabilityOverrideDAO = securityVulnerabilityOverrideDAO;
    this.componentLabelDAO = componentLabelDAO;
    this.vulnerabilityCustomRemediationDAO = vulnerabilityCustomRemediationDAO;
    this.vulnerabilityCustomCweDAO = vulnerabilityCustomCweDAO;
    this.vulnerabilityCustomCvssVectorDAO = vulnerabilityCustomCvssVectorDAO;
    this.vulnerabilityCustomCvssSeverityDAO = vulnerabilityCustomCvssSeverityDAO;
    this.vulnerabilityGroupDAO = vulnerabilityGroupDAO;
    this.vulnerabilityGroupVulnerabilityDAO = vulnerabilityGroupVulnerabilityDAO;
    this.owner = owner;
  }

  private List<String> getOwnerIds() {
    if (ownerIds == null) {
      ownerIds = ownerDAO.getOwnerIds(owner);
    }
    return ownerIds;
  }

  private Map<String, LicenseThreatGroup> getLicenseThreatGroups() {
    if (licenseThreatGroupsById == null) {
      licenseThreatGroupsById = licenseThreatGroupDAO.getByOwnerIds(getOwnerIds())
          .stream()
          .collect(toMap(LicenseThreatGroup::getId, Function.identity()));
    }
    return licenseThreatGroupsById;
  }

  private Collection<LicenseThreatGroupLicense> getLicenseThreatGroupLicenses() {
    if (licenseThreatGroupLicenses == null) {
      licenseThreatGroupLicenses = licenseThreatGroupLicenseDAO.getByOwnerIds(getOwnerIds());
    }
    return licenseThreatGroupLicenses;
  }

  static ComponentIdentifier normalizeComponentIdentifier(ComponentIdentifier ci) {
    if (ci == null) {
      return null;
    }
    Map<String, String> coords = ci.getCoordinates();
    Map<String, String> normalized = new TreeMap<>();
    for (Map.Entry<String, String> entry : coords.entrySet()) {
      if (StringUtils.isNotBlank(entry.getValue())) {
        normalized.put(entry.getKey(), entry.getValue());
      }
    }
    if (coords.size() == normalized.size() || normalized.isEmpty()) {
      return ci;
    }
    return new ComponentIdentifier(ci.getFormat(), normalized);
  }

  private Map<ComponentIdentifier, LicenseOverride> getLicenseOverrides() {
    if (licenseOverridesByComponentIdentifier == null) {
      licenseOverridesByComponentIdentifier = new HashMap<>();
      for (String ownerId : getOwnerIds()) {
        for (LicenseOverride licenseOverride : licenseOverrideDAO.getByOwnerId(ownerId)) {
          licenseOverridesByComponentIdentifier.putIfAbsent(
              normalizeComponentIdentifier(licenseOverride.getComponentIdentifier()), licenseOverride);
        }
      }
    }
    return licenseOverridesByComponentIdentifier;
  }

  private Map<String, Collection<ComponentLabel>> getComponentLabels() {
    if (componentLabelsByHash == null) {
      componentLabelsByHash = new HashMap<>();
      for (ComponentLabel componentLabel : componentLabelDAO.getByOwnerIds(getOwnerIds())) {
        componentLabelsByHash.computeIfAbsent(componentLabel.getHash(), hash -> new ArrayList<>()).add(componentLabel);
      }
    }
    return componentLabelsByHash;
  }

  private Map<String, Collection<SecurityVulnerabilityOverride>> getSecurityVulnerabilityOverrides() {
    if (securityVulnerabilityOverridesByHash == null) {
      securityVulnerabilityOverridesByHash = new HashMap<>();
      for (SecurityVulnerabilityOverride override : securityVulnerabilityOverrideDAO.getByOwnerId(owner.getId())) {
        securityVulnerabilityOverridesByHash.computeIfAbsent(override.getHash(), hash -> new ArrayList<>())
            .add(override);
      }
    }
    return securityVulnerabilityOverridesByHash;
  }

  private Map<String, List<VulnerabilityGroupVulnerability>> getVulnerabilityGroupVulnerabilities() {
    if (vulnerabilityGroupVulnerabilitiesByGroupId == null) {
      vulnerabilityGroupVulnerabilitiesByGroupId = new HashMap<>();

      List<String> ownerIds = getOwnerIds();
      if (ownerIds.isEmpty()) {
        return vulnerabilityGroupVulnerabilitiesByGroupId;
      }

      List<VulnerabilityGroup> allGroups = vulnerabilityGroupDAO.getByOwnerIds(ownerIds);
      if (allGroups.isEmpty()) {
        return vulnerabilityGroupVulnerabilitiesByGroupId;
      }

      List<String> groupIds = allGroups.stream().map(VulnerabilityGroup::getId).collect(toList());

      List<VulnerabilityGroupVulnerability> allVulnerabilities =
          vulnerabilityGroupVulnerabilityDAO.getByGroupIds(groupIds);

      for (VulnerabilityGroupVulnerability vulnerabilityGroupVulnerability : allVulnerabilities) {
        vulnerabilityGroupVulnerabilitiesByGroupId
            .computeIfAbsent(vulnerabilityGroupVulnerability.getVulnerabilityGroupId(), k -> new ArrayList<>())
            .add(vulnerabilityGroupVulnerability);
      }
    }
    return vulnerabilityGroupVulnerabilitiesByGroupId;
  }

  private void processJsonLicenseData(
      Component component,
      JsonNode jsonLicenseData,
      boolean useLicensesJsonOverriddenLicenses)
  {
    List<String> declaredLicenseNames = JsonUtils.getStringListFromArray(jsonLicenseData.get("declaredLicenses"));
    Set<String> declaredMultiLicenseIds = getMultiLicenseIdsByNames(declaredLicenseNames);
    component.setDeclaredMultiLicenseIds(declaredMultiLicenseIds);
    component.setDeclaredLicenseIds(multiLicenseIdsToLicenseIds(declaredMultiLicenseIds));

    List<String> observedLicenseNames = JsonUtils.getStringListFromArray(jsonLicenseData.get("observedLicenses"));
    Set<String> observedMultiLicenseIds = getMultiLicenseIdsByNames(observedLicenseNames);
    component.setObservedMultiLicenseIds(observedMultiLicenseIds);
    component.setObservedLicenseIds(multiLicenseIdsToLicenseIds(observedMultiLicenseIds));

    if (useLicensesJsonOverriddenLicenses) {
      String licenseOverrideStatusName = jsonLicenseData.path("status").asText();
      if (!licenseOverrideStatusName.isEmpty()) {
        component.setLicenseOverrideStatus(LicenseOverrideStatus.getByName(licenseOverrideStatusName));
      }
      List<String> overriddenLicenseNames = JsonUtils.getStringListFromArray(jsonLicenseData.get("overriddenLicenses"));
      Set<String> overriddenMultiLicenseIds = getMultiLicenseIdsByNames(overriddenLicenseNames);
      component.setLicenseOverrideIds(overriddenMultiLicenseIds);
    }
  }

  private Set<String> getMultiLicenseIdsByNames(List<String> multiLicenseNames) {
    return multiLicenseNames == null
        ? Collections.emptySet()
        : multiLicenseNames.stream()
            .map(multiLicenseDAO::getByNameNotNull)
            .map(MultiLicense::getId)
            .collect(Collectors.toSet());
  }

  private void loadLicenseOverride(Component component, boolean useLicensesJsonOverriddenLicenses) {
    if (useLicensesJsonOverriddenLicenses) {
      return;
    }
    ComponentIdentifier componentIdentifier = normalizeComponentIdentifier(component.getComponentIdentifier());
    LicenseOverride licenseOverride = getLicenseOverrides().get(componentIdentifier);
    if (componentIdentifier != null && componentIdentifier.isMaven()) {
      // for Maven components, there can still be legacy license overrides that only use the GAV coordinates
      ComponentIdentifier legacyComponentIdentifier = new ComponentIdentifier(ComponentIdentifier.FORMAT_MAVEN,
          ComponentIdentifierAdapter.toGavOnlyCoordinates(componentIdentifier.getCoordinates()));
      LicenseOverride legacyLicenseOverride = getLicenseOverrides().get(legacyComponentIdentifier);
      if (licenseOverride == null) {
        licenseOverride = legacyLicenseOverride;
      }
      else if (legacyLicenseOverride != null && getOwnerIds()
          .indexOf(legacyLicenseOverride.getOwnerId()) < getOwnerIds().indexOf(licenseOverride.getOwnerId()))
      {
        licenseOverride = legacyLicenseOverride;
      }
    }
    if (licenseOverride != null) {
      component.setLicenseOverrideStatus(licenseOverride.getStatus());
      component.setLicenseOverrideIds(licenseOverride.getLicenseIds());
    }
  }

  private void loadSVOverrides(Component component) {
    Collection<SecurityVulnerabilityOverride> overrides =
        getSecurityVulnerabilityOverrides().getOrDefault(component.getHash(), Collections.emptyList());
    for (SecurityVulnerabilityOverride override : overrides) {
      for (SecurityVulnerability sv : component.getSecurityVulnerabilities()) {
        if (sv.getSource().equals(override.getSource()) && sv.getRefId().equals(override.getReferenceId())) {
          sv.setStatus(override.getStatus());
          break;
        }
      }
    }
  }

  private void loadSecurityVulnerabilityCustomData() {
    if (customRemediations == null) {
      customRemediations = vulnerabilityCustomRemediationDAO.getByOwnerIdWithHierarchy(owner.getId());
    }
    if (customCwes == null) {
      customCwes = vulnerabilityCustomCweDAO.getByOwnerIdWithHierarchy(owner.getId());
    }
    if (customCvssVectors == null) {
      customCvssVectors = vulnerabilityCustomCvssVectorDAO.getByOwnerIdWithHierarchy(owner.getId());
    }
    if (customCvssSeverities == null) {
      customCvssSeverities = vulnerabilityCustomCvssSeverityDAO.getByOwnerIdWithHierarchy(owner.getId());
    }
  }

  private void fillSecurityVulnerabilityCustomData(Component component, SecurityVulnerability securityVulnerability) {
    String refId = securityVulnerability.getRefId();
    ComponentIdentifier componentIdentifier = component.getComponentIdentifier();
    SimpleEntry<String, ComponentIdentifier> entryWithComponent = new SimpleEntry<>(refId, componentIdentifier);
    SimpleEntry<String, ComponentIdentifier> entryWithoutComponent = new SimpleEntry<>(refId, null);

    VulnerabilityCustomRemediation customRemediation =
        customRemediations.getOrDefault(entryWithComponent, customRemediations.get(entryWithoutComponent));

    VulnerabilityCustomCwe customCwe =
        customCwes.getOrDefault(entryWithComponent, customCwes.get(entryWithoutComponent));

    VulnerabilityCustomCvssVector customCvssVector =
        customCvssVectors.getOrDefault(entryWithComponent, customCvssVectors.get(entryWithoutComponent));

    VulnerabilityCustomCvssSeverity customCvssSeverity =
        customCvssSeverities.getOrDefault(entryWithComponent, customCvssSeverities.get(entryWithoutComponent));

    if (customRemediation != null || customCwe != null || customCvssVector != null || customCvssSeverity != null) {
      SecurityVulnerabilityCustomData customData = new SecurityVulnerabilityCustomData();
      if (customRemediation != null) {
        customData.setRemediation(customRemediation.getRemediation());
      }
      if (customCwe != null) {
        customData.setCweId(customCwe.getCwe());
      }
      if (customCvssVector != null) {
        customData.setCvssVector(customCvssVector.getVector());
      }
      if (customCvssSeverity != null) {
        customData.setCvssSeverity(customCvssSeverity.getSeverity());
      }
      securityVulnerability.setSecurityVulnerabilityCustomData(customData);
    }
  }

  private BomData getAll(byte[] bomData) {
    BomData bomComponents = new BomData();

    JsonNode bomJson = loadJson(bomData);
    if (bomJson != null) {
      bomJson = bomJson.get("aaData");
      if (bomJson != null) {
        final ArrayNode bomJsonArray = (ArrayNode) bomJson;
        for (int i = 0; i < bomJsonArray.size(); i++) {
          final JsonNode componentJson = bomJsonArray.get(i);
          final String matchStateString = componentJson.get("matchState").asText();
          final MatchState matchState = MatchState.getById(matchStateString);
          final String identificationSourceString = JsonUtils.getNullableString(componentJson
              .get("identificationSource"));
          final IdentificationSource identificationSource = IdentificationSource.getOrMake(identificationSourceString);
          final boolean proprietary = componentJson.get("proprietary").booleanValue();
          String hash = JsonUtils.getNullableString(componentJson.get("hash"));
          String sha256 = JsonUtils.getNullableString(componentJson.get("sha256"));
          String packageUrl = JsonUtils.getNullableString(componentJson.get("packageUrl"));
          String originalPurl = JsonUtils.getNullableString(componentJson.get("originalPurl"));

          Component component = new Component();
          component.setHash(hash);
          component.setSha256(sha256);
          component.setMatchState(matchState);
          component.setProprietary(proprietary);
          component.setIdentificationSource(identificationSource);
          for (JsonNode path : componentJson.path("pathnames")) {
            component.addPathname(path.asText());
          }
          for (JsonNode filename : componentJson.path("filenames")) {
            component.addFilename(filename.asText());
          }
          for (JsonNode aggregateFileNode : componentJson.path("aggregateFiles")) {
            addAggregateFile(aggregateFileNode, component);
          }
          if (componentJson.has(DISPLAY_NAME_FIELD)) {
            component.setDisplayName(JsonUtils.getTypeToString(componentJson.path(DISPLAY_NAME_FIELD),
                ComponentDisplayName.class));
          }

          if (StringUtils.isNotBlank(packageUrl)) {
            component.setPackageUrl(packageUrl);
          }

          if (StringUtils.isNotBlank(originalPurl)) {
            component.setOriginalPurl(originalPurl);
          }

          component.setComponentIdentifier(ComponentIdentifierAdapter.getComponentIdentifier(componentJson));
          if (!matchState.equals(MatchState.UNKNOWN)) {
            Integer relativePopularity = null;
            final JsonNode relativePopularityJson = componentJson.get("relativePopularity");
            if (!relativePopularityJson.isNull()) {
              relativePopularity = (int) (relativePopularityJson.asDouble() * 100);
            }
            final long catalogDate = componentJson.get("createTime").asLong();

            component.setRelativePopularity(relativePopularity);
            component.setCatalogDate(catalogDate);
          }

          JsonNode componentCategoriesNode = componentJson.get("componentCategories");
          if (componentCategoriesNode != null) {
            for (JsonNode componentCategory : componentCategoriesNode) {
              JsonNode idNode = componentCategory.get("componentCategoryId");
              JsonNode pathNode = componentCategory.get("path");
              if (idNode != null && pathNode != null) {
                component.addComponentCategory(new ComponentCategory(idNode.asText(), pathNode.asText()));
              }
            }
          }

          JsonNode hygieneRatingNode = componentJson.get("hygieneRating");
          if (hygieneRatingNode != null) {
            JsonNode idNode = hygieneRatingNode.get("id");
            JsonNode labelNode = hygieneRatingNode.get("label");
            if (idNode != null && labelNode != null) {
              component.setHygieneRating(new HygieneRating(idNode.asText(), labelNode.asText()));
            }
          }

          JsonNode integrityRatingNode = componentJson.get("integrityRating");
          if (integrityRatingNode != null) {
            JsonNode idNode = integrityRatingNode.get("id");
            JsonNode labelNode = integrityRatingNode.get("label");
            if (idNode != null && labelNode != null) {
              component.setIntegrityRating(new IntegrityRating(idNode.asText(), labelNode.asText()));
            }
          }

          JsonNode analyzerFeaturesNode = componentJson.get("analyzerFeatures");
          setAnalyzerFeatures(analyzerFeaturesNode, component);

          JsonNode innerSourceDataNode = componentJson.get(INNER_SOURCE_DATA_FIELD);
          component.setInnerSourceData(JsonUtils.getObjectSetFromArray(innerSourceDataNode, InnerSourceData.class));

          JsonNode directDependencyNode = componentJson.get(DIRECT_DEPENDENCY_FIELD);
          if (directDependencyNode != null) {
            component.setDirectDependency(directDependencyNode.asBoolean());
            if (bomComponents.dependenciesResolved == null) {
              bomComponents.dependenciesResolved = true;
            }
          }

          JsonNode innerSourceNode = componentJson.get("innerSource");
          if (innerSourceNode != null) {
            component.setInnerSource(innerSourceNode.asBoolean());
          }

          JsonNode endOfLife = componentJson.get("endOfLife");
          if (endOfLife != null && !endOfLife.isNull()) {
            component.setEndOfLife(ComponentEndOfLifeStatus.valueOf(endOfLife.asText()));
          }
          else {
            component.setEndOfLife(ComponentEndOfLifeStatus.END_OF_LIFE_UNKNOWN);
          }

          component.setParentComponentPurls(
              JsonUtils.getStringSetFromArray(componentJson.path(PARENT_COMPONENT_PURLS_FIELD)));

          setDerivedFromAiModel(componentJson.get("derivedFromAiModel"), component);
          setAiModelContentTypes(componentJson.get("aiModelContentTypes"), component);

          bomComponents.components.add(component);
        }
      }
    }

    return bomComponents;
  }

  private void setDerivedFromAiModel(JsonNode derivedFromAiModelJsonNode, Component component) {
    if (derivedFromAiModelJsonNode != null && !derivedFromAiModelJsonNode.isNull()) {
      try {
        component.setDerivedFromAiModel(JsonUtils.asPojo(derivedFromAiModelJsonNode, DerivedFromAiModel.class));
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  private void setAiModelContentTypes(JsonNode aiModelContentTypesJsonNode, Component component) {
    if (aiModelContentTypesJsonNode != null && !aiModelContentTypesJsonNode.isNull()) {
      try {
        Set<AiModelContentType> aiModelContentTypes = new LinkedHashSet<>();
        for (JsonNode aiModelContentTypeNode : aiModelContentTypesJsonNode) {
          AiModelContentType aiModelContentType = JsonUtils.asPojo(aiModelContentTypeNode, AiModelContentType.class);
          aiModelContentTypes.add(aiModelContentType);
        }
        component.setAiModelContentTypes(aiModelContentTypes);
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  private void setAnalyzerFeatures(JsonNode analyzerFeaturesNode, Component component) {
    try {
      if (analyzerFeaturesNode != null) {
        AnalyzerFeatures analyzerFeatures = JsonUtils.asPojo(analyzerFeaturesNode, AnalyzerFeatures.class);

        if (analyzerFeatures != null) {
          component.setAnalyzerFeatures(analyzerFeatures);
        }
      }
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void addAggregateFile(JsonNode aggregateFileNode, Component component) {
    try {
      if (!aggregateFileNode.isNull()) {
        AggregateFile aggregateFile = JsonUtils.asPojo(aggregateFileNode, AggregateFile.class);

        if (aggregateFile != null) {
          component.addAggregateFile(aggregateFile);
        }
      }
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public List<Component> getAll(
      final byte[] licenseData,
      final byte[] securityData,
      final byte[] bomData,
      final byte[] dependencyData)
  {
    return getAll(licenseData, false, securityData, bomData, dependencyData);
  }

  /**
   * Loads component data for an application from report data and from the database.
   *
   * WARNING! This method is used by the PolicyEvaluationMigrator to load data for migration, so it must remain
   * compatible with the data format(s) and source(s) at the time the PolicyEvaluationMigrator was introduced.
   */
  public List<Component> getAll(
      final byte[] licenseData,
      final boolean useLicensesJsonOverriddenLicenses,
      final byte[] securityData,
      final byte[] bomData,
      final byte[] dependencyData)
  {
    // Load bom data
    BomData bomComponents = getAll(bomData);

    final Map<ComponentIdentifier, List<Component>> componentsByIdentifier = new LinkedHashMap<>();
    final Map<String, Component> componentsByHash = new LinkedHashMap<>();
    List<Component> unhashedComponents = new ArrayList<>();
    for (Component component : bomComponents.components) {
      ComponentIdentifier componentIdentifier = component.getComponentIdentifier();
      List<Component> components = componentsByIdentifier.get(componentIdentifier);
      if (components == null) {
        components = new ArrayList<>();
        componentsByIdentifier.put(componentIdentifier, components);
      }
      components.add(component);

      String hash = component.getHash();
      if (hash != null) {
        componentsByHash.put(hash, component);
      }
      else {
        unhashedComponents.add(component);
      }
    }

    List<ComponentIdentifier> componentIdentifiersWithLicenses = new ArrayList<>();

    // Load license data
    JsonNode licenseJson = loadJson(licenseData);
    if (licenseJson != null) {
      licenseJson = licenseJson.get("aaData");
      if (licenseJson != null) {
        final ArrayNode licenseJsonArray = (ArrayNode) licenseJson;
        for (int i = 0; i < licenseJsonArray.size(); i++) {
          final JsonNode jsonLicenseNode = licenseJsonArray.get(i);
          ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(jsonLicenseNode);
          List<Component> components = componentsByIdentifier.get(componentIdentifier);
          if (components != null) {
            componentIdentifiersWithLicenses.add(componentIdentifier);
            for (Component component : components) {
              processJsonLicenseData(component, jsonLicenseNode, useLicensesJsonOverriddenLicenses);
              loadLicenseOverride(component, useLicensesJsonOverriddenLicenses);
            }
          }
        }
      }
    }

    processComponentsWithoutLicenses(componentsByIdentifier,
        componentIdentifiersWithLicenses,
        useLicensesJsonOverriddenLicenses);

    // Load security data
    JsonNode securityJson = loadJson(securityData);
    if (securityJson != null) {
      securityJson = securityJson.get("aaData");
      if (securityJson != null) {
        loadSecurityVulnerabilityCustomData();

        final ArrayNode securityJsonArray = (ArrayNode) securityJson;
        for (int i = 0; i < securityJsonArray.size(); i++) {
          final JsonNode securityVulnerabilityJson = securityJsonArray.get(i);
          final String hash = securityVulnerabilityJson.get("hash").asText();
          final String source = securityVulnerabilityJson.get("source").asText();
          final String reference = securityVulnerabilityJson.get("reference").asText();
          final Float severity = JsonUtils.getNullableFloat(securityVulnerabilityJson.get("score"));
          final String statusString = JsonUtils.getNullableString(securityVulnerabilityJson.get("status"));
          final String urlString = JsonUtils.getNullableString(securityVulnerabilityJson.get("url"));
          final SecurityVulnerabilityOverrideStatus status = SecurityVulnerabilityOverrideStatus
              .getByName(statusString);
          final List<String> vulnerabilityCategories =
              JsonUtils.getStringListFromArray(securityVulnerabilityJson.get("vulnerabilityCategories"));
          final String researchType =
              JsonUtils.getNullableString(securityVulnerabilityJson.get("researchType"));
          final String detectionType = JsonUtils.getNullableString(securityVulnerabilityJson.get("detectionType"));
          final List<String> aliases = JsonUtils.getStringListFromArray(securityVulnerabilityJson.get("aliases"));
          final String cweString = JsonUtils.getNullableString(securityVulnerabilityJson.get("cwe"));
          final String cvssVectorString =
              JsonUtils.getNullableString(securityVulnerabilityJson.get("cvssVectorString"));
          final String cvssVectorSource =
              JsonUtils.getNullableString(securityVulnerabilityJson.get("cvssVectorSource"));
          final String identificationSource =
              JsonUtils.getNullableString(securityVulnerabilityJson.get("identificationSource"));
          final JsonNode kevDataNode = securityVulnerabilityJson.path("kevData");
          final JsonNode epssDataNode = securityVulnerabilityJson.path("epssData");

          Component component = componentsByHash.get(hash);

          if (component != null) {
            SecurityVulnerability securityVulnerability = new SecurityVulnerability();
            securityVulnerability.setSource(source);
            securityVulnerability.setRefId(reference);
            securityVulnerability.setSeverity(severity);
            securityVulnerability.setStatus(status);
            securityVulnerability.setUrl(urlString);
            securityVulnerability.setCwe(cweString);
            securityVulnerability.setVector(cvssVectorString);
            securityVulnerability.setVectorSource(cvssVectorSource);
            // The Identification source Enum for vulnerability will be the same as the component initially,
            // and it might be updated later. This is for consistency with what was done for SBOM manager,
            // and a new Enum for vulnerability will be created in the future.
            securityVulnerability.setIdentificationSource(IdentificationSource.getById(identificationSource));
            securityVulnerability.setKevData(toKevData(kevDataNode));
            securityVulnerability.setEpssData(toEpssData(epssDataNode));
            if (vulnerabilityCategories != null) {
              for (String categoryStr : vulnerabilityCategories) {
                SecurityVulnerabilityCategory category = SecurityVulnerabilityCategory.getById(categoryStr);
                securityVulnerability.addVulnerabilityCategory(category);
              }
            }
            securityVulnerability.setResearchType(SecurityVulnerabilityResearchType.getResearchType(researchType));
            securityVulnerability.setDetectionType(SecurityVulnerabilityDetectionType.getDetectionType(detectionType));
            if (aliases != null) {
              for (String alias : aliases) {
                securityVulnerability.addAlias(alias);
              }
            }

            JsonNode analysisNode = securityVulnerabilityJson.get("analysis");
            if (analysisNode != null) {
              ThirdPartyVulnerabilityExploitabilityExchange analysis =
                  new ThirdPartyVulnerabilityExploitabilityExchange();
              analysis.setDetail(analysisNode.get("detail").textValue());
              analysis.setJustification(analysisNode.get("justification").textValue());
              analysis.setResponse(analysisNode.get("response").textValue());
              analysis.setState(analysisNode.get("state").textValue());
              securityVulnerability.setAnalysis(analysis);
            }

            fillSecurityVulnerabilityCustomData(component, securityVulnerability);
            component.addSecurityVulnerability(securityVulnerability);
          }
        }
      }
    }

    if (bomComponents.dependenciesResolved == null || !bomComponents.dependenciesResolved) {
      // Load dependency data based on dependencyGraph (for reports produced prior to 108)
      JsonNode dependencyJson = loadJson(dependencyData);
      if (dependencyJson != null) {
        Map<ComponentIdentifier, Boolean> componentDependencyType = getDependencyTypes(dependencyJson);
        for (ComponentIdentifier componentIdentifier : componentsByIdentifier.keySet()) {
          List<Component> components = componentsByIdentifier.get(componentIdentifier);
          if (components != null) {
            for (Component component : components) {
              component.setDirectDependency(componentDependencyType.get(componentIdentifier));
            }
          }
        }
      }
    }

    final List<Component> result = new ArrayList<>();
    result.addAll(componentsByHash.values());
    result.addAll(unhashedComponents);

    // Load license threat group, label data and vulnerability group vulnerabilities
    for (Component component : result) {
      setNotDeclaredLicensesForClaimedComponent(component);
      loadLicenseThreatGroups(component);
      loadComponentLabels(component);
      loadVulnerabilityGroupVulnerabilities(component);
    }
    return result;
  }

  private void loadVulnerabilityGroupVulnerabilities(final Component component) {
    var vulnGroupData = getVulnerabilityGroupVulnerabilities();
    if (!vulnGroupData.isEmpty()) {
      component.setVulnerabilityGroupVulnerabilities(vulnGroupData);
    }
  }

  @VisibleForTesting
  void processComponentsWithoutLicenses(
      Map<ComponentIdentifier, List<Component>> componentsByIdentifier,
      List<ComponentIdentifier> componentIdentifiersWithLicenses,
      boolean useLicensesJsonOverriddenLicenses)
  {
    for (Entry<ComponentIdentifier, List<Component>> entry : componentsByIdentifier.entrySet()) {
      if (!componentIdentifiersWithLicenses.contains(entry.getKey())) {
        for (Component componentWithoutLicenses : entry.getValue()) {
          loadLicenseOverride(componentWithoutLicenses, useLicensesJsonOverriddenLicenses);
        }
      }
    }
  }

  private Map<ComponentIdentifier, Boolean> getDependencyTypes(final JsonNode dependencyJson) {
    JsonNode dependencyGraphNode = dependencyJson.path("dependencyGraph");
    if (dependencyGraphNode == null) {
      return Collections.emptyMap();
    }

    Map<ComponentIdentifier, Boolean> componentDependencyType = new HashMap<>();
    for (JsonNode child : dependencyGraphNode) {
      ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(child);
      if (componentIdentifier != null && child.has(DIRECT_DEPENDENCY_FIELD)) {
        componentDependencyType.put(componentIdentifier, child.get(DIRECT_DEPENDENCY_FIELD).asBoolean());
      }
    }

    return componentDependencyType;
  }

  public Component getComponent(ComponentInfo componentInfo, boolean isAlpObservedLicenseDetectionEnabled) {
    Component component = new Component();

    component.setHash(componentInfo.getHash());
    component.setComponentIdentifier(componentInfo.getComponentIdentifier());

    component.setMatchState(MatchState.getById(componentInfo.getMatchState()));
    if (componentInfo.getIdentificationSource() != null) {
      component.setIdentificationSource(IdentificationSource.getById(componentInfo.getIdentificationSource()));
    }

    component.setCatalogDate(componentInfo.getCatalogDate());
    if (componentInfo.getRelativePopularity() != null) {
      component.setRelativePopularity(componentInfo.getRelativePopularity());
    }

    if (component.getComponentIdentifier() != null) {
      loadLicenseOverride(component, false);

      Set<String> declaredMultiLicenseIds = componentInfo.getDeclaredLicenseIds();
      Set<String> observedMultiLicenseIds = componentInfo.getObservedLicenseIds();
      Set<String> notSupportedLicenseIdSet = Collections.singleton(License.NOT_SUPPORTED_ID);

      if (!isAlpObservedLicenseDetectionEnabled
          && License.isAlpObservedLicenseFormatHidden(componentInfo.getComponentIdentifier().getFormat())
          && CollectionUtils.isNotEmpty(observedMultiLicenseIds)
          && !observedMultiLicenseIds.equals(notSupportedLicenseIdSet))
      {
        observedMultiLicenseIds = notSupportedLicenseIdSet;
        component.setHiddenObservedLicenses(true);
      }

      component.setDeclaredMultiLicenseIds(declaredMultiLicenseIds);
      component.setObservedMultiLicenseIds(observedMultiLicenseIds);
      component.setDeclaredLicenseIds(multiLicenseIdsToLicenseIds(declaredMultiLicenseIds));
      component.setObservedLicenseIds(multiLicenseIdsToLicenseIds(observedMultiLicenseIds));

      component.setEndOfLife(componentInfo.getEndOfLife());

      loadLicenseThreatGroups(component);
    }

    if (componentInfo.getSecurityVulnerabilities() != null) {
      loadSecurityVulnerabilityCustomData();

      for (com.sonatype.clm.dto.model.SecurityVulnerability dtoSv : componentInfo.getSecurityVulnerabilities()) {
        SecurityVulnerability sv = new SecurityVulnerability(dtoSv.getSource(), dtoSv.getRefId(), dtoSv.getSeverity());
        sv.setCwe(dtoSv.getCwe());
        if (dtoSv.getVulnerabilityCategories() != null) {
          for (String vulnCategory : dtoSv.getVulnerabilityCategories()) {
            sv.addVulnerabilityCategory(SecurityVulnerabilityCategory.getById(vulnCategory));
          }
        }
        if (dtoSv.getAliases() != null) {
          for (String alias : dtoSv.getAliases()) {
            sv.addAlias(alias);
          }
        }
        sv.setKevData(dtoSv.getKevData());
        sv.setEpssData(dtoSv.getEpssData());
        sv.setVector(dtoSv.getCvssVector());
        sv.setVectorSource(dtoSv.getCvssVectorSource());
        sv.setThreatTypes(dtoSv.getThreatTypes());
        sv.setAttackVector(dtoSv.getAttackVector());

        fillSecurityVulnerabilityCustomData(component, sv);
        component.addSecurityVulnerability(sv);
      }
      loadSVOverrides(component);
    }

    if (componentInfo.getHygieneRating() != null) {
      component.setHygieneRating(new HygieneRating(String.valueOf(componentInfo.getHygieneRating().getId()),
          componentInfo.getHygieneRating().getLabel()));
    }
    if (componentInfo.getComponentCategories() != null) {
      for (com.sonatype.clm.dto.model.component.ComponentCategory componentCategory : componentInfo
          .getComponentCategories())
      {
        component.addComponentCategory(new ComponentCategory(String.valueOf(componentCategory.getComponentCategoryId()),
            componentCategory.getPath()));
      }
    }
    if (componentInfo.getIntegrityRating() != null) {
      component.setIntegrityRating(new IntegrityRating(String.valueOf(componentInfo.getIntegrityRating().getId()),
          componentInfo.getIntegrityRating().getLabel()));
    }

    loadComponentLabels(component);
    loadVulnerabilityGroupVulnerabilities(component);

    component.setDerivedFromAiModel(componentInfo.getDerivedFromAiModel());
    component.setAiModelContentTypes(componentInfo.getAiModelContentTypes());

    return component;
  }

  public Component getComponent(JsonNode jsonLicenseNode) {

    Component component = new Component();
    component.setComponentIdentifier(ComponentIdentifierAdapter.getComponentIdentifier(jsonLicenseNode));
    processJsonLicenseData(component, jsonLicenseNode, false);
    loadLicenseOverride(component, false);

    loadLicenseThreatGroups(component);
    loadVulnerabilityGroupVulnerabilities(component);

    return component;
  }

  private void setNotDeclaredLicensesForClaimedComponent(Component component) {
    if (component.getIdentificationSource() == IdentificationSource.MANUAL) {
      component.setDeclaredMultiLicenseIds(Sets.newHashSet(License.NO_SOURCES_ID, License.NOT_DECLARED_ID));
      component.setObservedMultiLicenseIds(Sets.newHashSet(License.NO_SOURCES_ID, License.NOT_DECLARED_ID));
      component.setDeclaredLicenseIds(Sets.newHashSet(License.NO_SOURCES_ID, License.NOT_DECLARED_ID));
      component.setObservedLicenseIds(Sets.newHashSet(License.NO_SOURCES_ID, License.NOT_DECLARED_ID));
    }
  }

  private void loadComponentLabels(Component component) {
    Collection<ComponentLabel> componentLabels =
        getComponentLabels().getOrDefault(component.getHash(), Collections.emptyList());
    for (ComponentLabel componentLabel : componentLabels) {
      component.addLabelId(componentLabel.getLabelId());
    }
  }

  public void loadLicenseThreatGroups(Component component) {
    // Gather all license ids
    Set<String> licenseIds = new LinkedHashSet<>();
    if (component.isLicenseOverridden()) {
      licenseIds.addAll(component.getLicenseOverrideIds());
    }
    else {
      licenseIds.addAll(component.getDeclaredLicenseIds());
      licenseIds.addAll(component.getObservedLicenseIds());
    }

    // Add license ids by license threat group ids and
    // determine licenses not assigned to any license threat group.
    Set<String> unassignedLicenseIds = new LinkedHashSet<>(licenseIds);
    Set<String> licenseThreatGroupIds = new HashSet<>();
    Collection<LicenseThreatGroupLicense> licenseThreatGroupLicenses = getLicenseThreatGroupLicenses();
    for (LicenseThreatGroupLicense licenseThreatGroupLicense : licenseThreatGroupLicenses) {
      if (!licenseIds.contains(licenseThreatGroupLicense.getLicenseId())) {
        continue;
      }
      component.addLicenseIdByThreatGroupId(licenseThreatGroupLicense.getLicenseId(),
          licenseThreatGroupLicense.getLicenseThreatGroupId());
      licenseThreatGroupIds.add(licenseThreatGroupLicense.getLicenseThreatGroupId());
      unassignedLicenseIds.remove(licenseThreatGroupLicense.getLicenseId());
    }
    component.setUnassignedLicenseIds(unassignedLicenseIds);

    // Add license threat groups
    Map<String, LicenseThreatGroup> licenseThreatGroupsById = getLicenseThreatGroups();
    licenseThreatGroupIds.stream().map(licenseThreatGroupsById::get).forEach(component::addLicenseThreatGroup);
  }

  private Set<String> multiLicenseIdsToLicenseIds(Set<String> multiLicenseIds) {
    if (multiLicenseIds == null) {
      return null;
    }
    Set<String> licenseIds = new LinkedHashSet<>();
    for (String multiLicenseId : multiLicenseIds) {
      Set<License> licenses = multiLicenseDAO.getLicensesByMultiLicenseIdNotNull(multiLicenseId);
      for (License license : licenses) {
        licenseIds.add(license.getId());
      }
    }
    return licenseIds;
  }

  private static JsonNode loadJson(final byte[] data) {
    if (data == null) {
      return null;
    }
    try {
      return JsonUtils.parse(data);
    }
    catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static class BomData
  {
    Boolean dependenciesResolved;

    List<Component> components = new ArrayList<>();
  }

  /**
   * Convert JSON representation of a given type to the concrete class.
   */
  private <T> T toPojo(final JsonNode jsonNode, Class<T> clazz, String errorMessage) {
    if (jsonNode == null || jsonNode.isMissingNode() || jsonNode.isNull() || jsonNode.isEmpty()) {
      return null;
    }

    try {
      return JsonUtils.asPojo(jsonNode, clazz);
    }
    catch (IOException e) {
      throw new UncheckedIOException(errorMessage, e);
    }
  }

  /**
   * Convert JSON representation of KevData to the concrete class.
   */
  private KevData toKevData(final JsonNode kevDataNode) {
    return toPojo(kevDataNode, KevData.class, "Error deserializing KevData");
  }

  /**
   * Convert JSON representation of EpssData to the concrete class.
   */
  private EpssData toEpssData(final JsonNode epssDataNode) {
    return toPojo(epssDataNode, EpssData.class, "Error deserializing EpssData");
  }
}
