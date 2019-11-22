/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.clm.dto.model.ComponentInfo;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class ComponentDAO
{
  private MultiLicenseDAO multiLicenseDAO = new MultiLicenseDAO();

  private LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();

  private LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();

  private SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO = new SecurityVulnerabilityOverrideDAO();

  private OwnerDAO ownerDAO = new OwnerDAO();

  private void processJsonLicenseData(Component component, JsonNode jsonLicenseData) {
    List<String> declaredLicenseNames = JsonUtils.getStringListFromArray(jsonLicenseData.get("declaredLicenses"));
    component.setDeclaredLicenseIds(multiLicenseNamesToLicenseIds(declaredLicenseNames));
    List<String> observedLicenseNames = JsonUtils.getStringListFromArray(jsonLicenseData.get("observedLicenses"));
    component.setObservedLicenseIds(multiLicenseNamesToLicenseIds(observedLicenseNames));
  }

  private void loadLicenseOverride(Owner owner, Component component) {
    LicenseOverride licenseOverride = licenseOverrideDAO.getAppliedByOwnerIdAndComponentIdentifier(owner.getId(),
        component.getComponentIdentifier());

    if (licenseOverride != null) {
      component.setLicenseOverrideStatus(licenseOverride.getStatus());
      component.setLicenseOverrideIds(licenseOverride.getLicenseIds());
    }
  }

  private void loadSVOverrides(Owner owner, Component component) {
    List<SecurityVulnerabilityOverride> overrides = securityVulnerabilityOverrideDAO.getByOwnerIdAndHash(owner.getId(),
        component.getHash());
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
          final IdentificationSource identificationSource = IdentificationSource.getById(identificationSourceString);
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

          components.add(component);
        }
      }
    }

    return components;
  }

  /**
   * Loads component data for an application from report data and from the database.
   * 
   * WARNING! This method is used by the PolicyEvaluationMigrator to load data for migration, so it must remain
   * compatible with the data format(s) and source(s) at the time the PolicyEvaluationMigrator was introduced.
   */
  public List<Component> getAll(Application application,
                                final byte[] licenseData,
                                final byte[] securityData,
                                final byte[] bomData)
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
              loadLicenseOverride(application, component);
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

    final List<Component> result = new ArrayList<>();
    result.addAll(componentsByHash.values());
    result.addAll(unhashedComponents);

    // Load license threat group data
    for (Component component : result) {
      loadLicenseThreatGroups(application.getId(), component);
    }

    // Load label data
    ComponentLabelDAO componentLabelDAO = new ComponentLabelDAO();
    for (Component component : result) {
      loadComponentLabels(application.getId(), component, componentLabelDAO);
    }
    return result;
  }

  public Component getComponent(Owner owner, ComponentInfo componentInfo) {
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
      loadLicenseOverride(owner, component);
      component.setDeclaredLicenseIds(multiLicenseIdsToLicenseIds(componentInfo.getDeclaredLicenseIds()));
      component.setObservedLicenseIds(multiLicenseIdsToLicenseIds(componentInfo.getObservedLicenseIds()));
      loadLicenseThreatGroups(owner.getId(), component);
    }

    if (componentInfo.getSecurityVulnerabilities() != null) {
      for (com.sonatype.clm.dto.model.SecurityVulnerability sv : componentInfo.getSecurityVulnerabilities()) {
        component.addSecurityVulnerability(new SecurityVulnerability(sv.getSource(), sv.getRefId(), sv.getSeverity()));
      }
      loadSVOverrides(owner, component);
    }

    loadComponentLabels(owner.getId(), component, new ComponentLabelDAO());

    return component;
  }

  public Component getComponent(Application application, JsonNode jsonLicenseNode) {

    Component component = new Component();
    component.setComponentIdentifier(ComponentIdentifierAdapter.getComponentIdentifier(jsonLicenseNode));
    processJsonLicenseData(component, jsonLicenseNode);
    loadLicenseOverride(application, component);

    loadLicenseThreatGroups(application.getId(), component);

    return component;
  }

  private void loadComponentLabels(String ownerId, Component component, ComponentLabelDAO componentLabelDAO) {
    List<ComponentLabel> componentLabels = componentLabelDAO.getByOwnerIdAndHash(ownerId, component.getHash());
    for (ComponentLabel componentLabel : componentLabels) {
      component.addLabelId(componentLabel.getLabelId());
    }
  }

  public void loadLicenseThreatGroups(String ownerId, Component component) {
    // Gather all license ids
    Set<String> licenseIds = new LinkedHashSet<>();
    if (component.isLicenseOverridden()) {
      licenseIds.addAll(component.getLicenseOverrideIds());
    }
    else {
      licenseIds.addAll(component.getDeclaredLicenseIds());
      licenseIds.addAll(component.getObservedLicenseIds());
    }

    Set<String> unassignedLicenseIds = new LinkedHashSet<>(licenseIds);
    // Gather all license threat groups from the application on up the organization hierarchy
    for (Owner owner : ownerDAO.walkHierarchy(ownerId)) {
      loadLicenseThreatGroups(component, unassignedLicenseIds, licenseIds, owner.getId());
    }

    component.setUnassignedLicenseIds(unassignedLicenseIds);
  }

  private void loadLicenseThreatGroups(Component component,
                                       Set<String> unassignedLicenseIds,
                                       Set<String> licenseIds,
                                       String ltgOwnerId)
  {
    for (String licenseId : licenseIds) {
      List<LicenseThreatGroup> licenseThreatGroups = licenseThreatGroupDAO.getByOwnerIdAndLicenseId(ltgOwnerId,
          licenseId);

      if (!licenseThreatGroups.isEmpty()) {
        unassignedLicenseIds.remove(licenseId);

        for (LicenseThreatGroup licenseThreatGroup : licenseThreatGroups) {
          component.addLicenseThreatGroup(licenseThreatGroup);
          component.addLicenseIdByThreatGroupId(licenseId, licenseThreatGroup.getId());
        }
      }
    }
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
