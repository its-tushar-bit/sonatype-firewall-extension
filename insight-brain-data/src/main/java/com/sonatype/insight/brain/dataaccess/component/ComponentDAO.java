/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.sonatype.clm.dto.model.ComponentInfo;
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
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.ComponentCategory;
import com.sonatype.insight.brain.model.component.HygieneRating;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import static java.util.stream.Collectors.toMap;

public class ComponentDAO
{
  private MultiLicenseDAO multiLicenseDAO = new MultiLicenseDAO();

  private LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();

  private LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();

  private LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();

  private SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO = new SecurityVulnerabilityOverrideDAO();

  private OwnerDAO ownerDAO = new OwnerDAO();

  private final Owner owner;

  private List<String> ownerIds;

  private Map<String, LicenseThreatGroup> licenseThreatGroupsById;

  private Collection<LicenseThreatGroupLicense> licenseThreatGroupLicenses;

  private Map<ComponentIdentifier, LicenseOverride> licenseOverridesByComponentIdentifier;

  private Map<String, Collection<SecurityVulnerabilityOverride>> securityVulnerabilityOverridesByHash;

  private Map<String, Collection<ComponentLabel>> componentLabelsByHash;

  public ComponentDAO(Owner owner) {
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
      licenseThreatGroupsById = licenseThreatGroupDAO.getByOwnerIds(getOwnerIds()).stream()
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

  private Map<ComponentIdentifier, LicenseOverride> getLicenseOverrides() {
    if (licenseOverridesByComponentIdentifier == null) {
      licenseOverridesByComponentIdentifier = new HashMap<>();
      for (String ownerId : getOwnerIds()) {
        for (LicenseOverride licenseOverride : licenseOverrideDAO.getByOwnerId(ownerId)) {
          licenseOverridesByComponentIdentifier.putIfAbsent(licenseOverride.getComponentIdentifier(), licenseOverride);
        }
      }
    }
    return licenseOverridesByComponentIdentifier;
  }

  private Map<String, Collection<ComponentLabel>> getComponentLabels() {
    if (componentLabelsByHash == null) {
      componentLabelsByHash = new HashMap<>();
      for (ComponentLabel componentLabel : new ComponentLabelDAO().getByOwnerIds(getOwnerIds())) {
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

  private void processJsonLicenseData(Component component, JsonNode jsonLicenseData) {
    List<String> declaredLicenseNames = JsonUtils.getStringListFromArray(jsonLicenseData.get("declaredLicenses"));
    component.setDeclaredLicenseIds(multiLicenseNamesToLicenseIds(declaredLicenseNames));
    List<String> observedLicenseNames = JsonUtils.getStringListFromArray(jsonLicenseData.get("observedLicenses"));
    component.setObservedLicenseIds(multiLicenseNamesToLicenseIds(observedLicenseNames));
  }

  private void loadLicenseOverride(Component component) {
    ComponentIdentifier componentIdentifier = component.getComponentIdentifier();
    LicenseOverride licenseOverride = getLicenseOverrides().get(componentIdentifier);
    if (componentIdentifier.isMaven()) {
      // for Maven components, there can still be legacy license overrides that only use the GAV coordinates
      ComponentIdentifier legacyComponentIdentifier = new ComponentIdentifier(ComponentIdentifier.FORMAT_MAVEN,
          ComponentIdentifierAdapter.toGavOnlyCoordinates(componentIdentifier.getCoordinates()));
      LicenseOverride legacyLicenseOverride = getLicenseOverrides().get(legacyComponentIdentifier);
      if (licenseOverride == null) {
        licenseOverride = legacyLicenseOverride;
      }
      else if (legacyLicenseOverride != null && getOwnerIds()
          .indexOf(legacyLicenseOverride.getOwnerId()) < getOwnerIds().indexOf(licenseOverride.getOwnerId())) {
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

  private List<Component> getAll(byte[] bomData) {
    List<Component> components = new ArrayList<>();

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

          Component component = new Component();
          component.setHash(hash);
          component.setMatchState(matchState);
          component.setProprietary(proprietary);
          component.setIdentificationSource(identificationSource);
          for (JsonNode path : componentJson.path("pathnames")) {
            component.addPathname(path.asText());
          }
          component.setDisplayName(getDisplayName(componentJson));
          if (!matchState.equals(MatchState.UNKNOWN)) {
            component.setComponentIdentifier(ComponentIdentifierAdapter.getComponentIdentifier(componentJson));

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

          JsonNode analyzerFeaturesNode = componentJson.get("analyzerFeatures");
          setAnalyzerFeatures(analyzerFeaturesNode, component);

          components.add(component);
        }
      }
    }

    return components;
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

  public static String getDisplayName(JsonNode componentNode) {
    try {
      return JsonUtils.asPojo(componentNode.path("displayName"), ComponentDisplayName.class).toString();
    }
    catch (IOException e) {
      throw new UncheckedIOException(e.getMessage(), e);
    }
  }

  /**
   * Loads component data for an application from report data and from the database.
   * 
   * WARNING! This method is used by the PolicyEvaluationMigrator to load data for migration, so it must remain
   * compatible with the data format(s) and source(s) at the time the PolicyEvaluationMigrator was introduced.
   */
  public List<Component> getAll(
      final byte[] licenseData,
      final byte[] securityData,
      final byte[] bomData,
      final byte[] dependencyData)
  {
    // Load bom data
    List<Component> bomComponents = getAll(bomData);

    final Map<ComponentIdentifier, List<Component>> componentsByIdentifier = new LinkedHashMap<>();
    final Map<String, Component> componentsByHash = new LinkedHashMap<>();
    List<Component> unhashedComponents = new ArrayList<>();
    for (Component component : bomComponents) {
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
            for (Component component : components) {
              processJsonLicenseData(component, jsonLicenseNode);
              loadLicenseOverride(component);
            }
          }
        }
      }
    }

    // Load security data
    JsonNode securityJson = loadJson(securityData);
    if (securityJson != null) {
      securityJson = securityJson.get("aaData");
      if (securityJson != null) {
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

          Component component = componentsByHash.get(hash);
          if (component != null) {
            SecurityVulnerability securityVulnerability = new SecurityVulnerability();
            securityVulnerability.setSource(source);
            securityVulnerability.setRefId(reference);
            securityVulnerability.setSeverity(severity);
            securityVulnerability.setStatus(status);
            securityVulnerability.setUrl(urlString);

            component.addSecurityVulnerability(securityVulnerability);
          }
        }
      }
    }

    // Load dependency data
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

    final List<Component> result = new ArrayList<>();
    result.addAll(componentsByHash.values());
    result.addAll(unhashedComponents);

    // Load license threat group data
    for (Component component : result) {
      loadLicenseThreatGroups(component);
    }

    // Load label data
    for (Component component : result) {
      loadComponentLabels(component);
    }
    return result;
  }

  private Map<ComponentIdentifier, Boolean> getDependencyTypes(final JsonNode dependencyJson) {
    JsonNode dependencyGraphNode = dependencyJson.path("dependencyGraph");
    if (dependencyGraphNode == null) {
      return Collections.emptyMap();
    }

    Map<ComponentIdentifier, Boolean> componentDependencyType = new HashMap<>();
    for (JsonNode child : dependencyGraphNode) {
      ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(child);
      if (componentIdentifier != null && child.has("directDependency")) {
        componentDependencyType.put(componentIdentifier, child.get("directDependency").asBoolean());
      }
    }

    return componentDependencyType;
  }

  public Component getComponent(ComponentInfo componentInfo) {
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
      loadLicenseOverride(component);
      component.setDeclaredLicenseIds(multiLicenseIdsToLicenseIds(componentInfo.getDeclaredLicenseIds()));
      component.setObservedLicenseIds(multiLicenseIdsToLicenseIds(componentInfo.getObservedLicenseIds()));
      loadLicenseThreatGroups(component);
    }

    if (componentInfo.getSecurityVulnerabilities() != null) {
      for (com.sonatype.clm.dto.model.SecurityVulnerability sv : componentInfo.getSecurityVulnerabilities()) {
        component.addSecurityVulnerability(new SecurityVulnerability(sv.getSource(), sv.getRefId(), sv.getSeverity()));
      }
      loadSVOverrides(component);
    }

    loadComponentLabels(component);

    return component;
  }

  public Component getComponent(JsonNode jsonLicenseNode) {

    Component component = new Component();
    component.setComponentIdentifier(ComponentIdentifierAdapter.getComponentIdentifier(jsonLicenseNode));
    processJsonLicenseData(component, jsonLicenseNode);
    loadLicenseOverride(component);

    loadLicenseThreatGroups(component);

    return component;
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

  private Set<String> multiLicenseNamesToLicenseIds(List<String> multiLicenseNames) {
    if (multiLicenseNames == null) {
      return null;
    }
    Set<String> licenseIds = new LinkedHashSet<>();
    for (String multiLicenseName : multiLicenseNames) {
      String multiLicenseId = multiLicenseDAO.getByNameNotNull(multiLicenseName).getId();
      Set<License> licenses = multiLicenseDAO.getLicensesByMultiLicenseIdNotNull(multiLicenseId);
      for (License license : licenses) {
        licenseIds.add(license.getId());
      }
    }
    return licenseIds;
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
}
