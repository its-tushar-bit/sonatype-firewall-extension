/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.SecurityVulnerabilityResearch;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.SecurityVulnerabilityResearchValueType;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Test;

import static com.sonatype.insight.brain.model.policy.conditions.valuetype.SecurityVulnerabilityResearch.ALL_TYPES;
import static com.sonatype.insight.brain.model.policy.conditions.valuetype.SecurityVulnerabilityResearch.DEFAULT_TYPES;
import static org.assertj.core.api.Assertions.assertThat;

public class ConditionValueTypeResourceTest
    extends AbstractResourceTest
{
  private static final List<String> DEFAULT_RESEARCH_TYPES =
      DEFAULT_TYPES.values().stream().map(SecurityVulnerabilityResearch::getId).toList();

  private static final List<String> ALL_RESEARCH_TYPES =
      ALL_TYPES.values().stream().map(SecurityVulnerabilityResearch::getId).toList();

  private HttpRequest restRequest(OwnerType ownerType, String ownerId) {
    return restRequest().path(ConditionValueTypeResource.RESOURCE_PATH).parameter(ownerType, ownerId);
  }

  @Test
  public void testGetConditionValueTypes_Application() throws Exception {
    String appPublicId = "ConditionValueTypeResourceTest_AppId";
    tempEntity.newApplicationWithParent(appPublicId);

    final HttpResponse response = restRequest(OwnerType.APPLICATION, appPublicId).get();
    assertResponseStatus(200, response);
    final Object[] conditionValueTypes = response.getBody(Object[].class);
    assertThat(conditionValueTypes).isNotEmpty();
    assertResearchTypes(conditionValueTypes, false);
  }

  @Test
  public void testGetConditionValueTypes_Organization() throws Exception {
    String orgId = tempEntity.newOrganization("test").getId();

    final HttpResponse response = restRequest(OwnerType.ORGANIZATION, orgId).get();
    assertResponseStatus(200, response);
    final Object[] conditionValueTypes = response.getBody(Object[].class);
    assertThat(conditionValueTypes).isNotEmpty();
    assertResearchTypes(conditionValueTypes, false);
  }

  @Test
  public void testGetConditionValueTypes_RepositoryContainer() throws Exception {
    HttpResponse response =
        restRequest(OwnerType.REPOSITORY_CONTAINER, RepositoryContainer.REPOSITORY_CONTAINER_ID).get();
    assertResponseStatus(200, response);
    final Object[] conditionValueTypes = response.getBody(Object[].class);
    assertThat(conditionValueTypes).isNotEmpty();
    assertResearchTypes(conditionValueTypes, false);
  }

  @Test
  public void testGetConditionValueTypes_RepositoryManager() throws Exception {
    String ownerId = tempEntity.newRepositoryManager().getId();

    HttpResponse response = restRequest(OwnerType.REPOSITORY_MANAGER, ownerId).get();
    assertResponseStatus(200, response);
    Object[] conditionValueTypes = response.getBody(Object[].class);
    assertThat(conditionValueTypes).isNotEmpty();
    assertResearchTypes(conditionValueTypes, false);
  }

  @Test
  public void testGetConditionValueTypes_Repository() throws Exception {
    String ownerId = tempEntity.newRepository().getId();

    HttpResponse response = restRequest(OwnerType.REPOSITORY, ownerId).get();
    assertResponseStatus(200, response);
    Object[] conditionValueTypes = response.getBody(Object[].class);
    assertThat(conditionValueTypes).isNotEmpty();
    assertResearchTypes(conditionValueTypes, false);
  }

  @Test
  public void testGetConditionValueTypes_Application_CpeMatchingLicensed() throws Exception {
    testProductLicense.setFeatures(LicensedFeature.CPE_MATCHING);
    String appPublicId = "ConditionValueTypeResourceTest_AppId";
    tempEntity.newApplicationWithParent(appPublicId);

    final HttpResponse response = restRequest(OwnerType.APPLICATION, appPublicId).get();
    assertResponseStatus(200, response);
    final Object[] conditionValueTypes = response.getBody(Object[].class);
    assertThat(conditionValueTypes).isNotEmpty();
    assertResearchTypes(conditionValueTypes, true);
  }

  @Test
  public void testGetConditionValueTypes_Organization_CpeMatchingEnabled() throws Exception {
    testProductLicense.setFeatures(LicensedFeature.CPE_MATCHING);
    String orgId = tempEntity.newOrganization("test").getId();

    final HttpResponse response = restRequest(OwnerType.ORGANIZATION, orgId).get();
    assertResponseStatus(200, response);
    final Object[] conditionValueTypes = response.getBody(Object[].class);
    assertThat(conditionValueTypes).isNotEmpty();
    assertResearchTypes(conditionValueTypes, true);
  }

  @SuppressWarnings("unchecked")
  private static void assertResearchTypes(final Object[] conditionValueTypes, boolean includePublicResearch) {
    Optional<Object> researchTypeOptional = Arrays.stream(conditionValueTypes)
        .filter(conditionValueType -> ((Map<?, ?>) conditionValueType).get("id")
            .equals(SecurityVulnerabilityResearchValueType.ID))
        .findFirst();
    assertThat(researchTypeOptional).isPresent().get().satisfies(researchType -> {
      List<Map<?, ?>> availableValues = (List<Map<?, ?>>) ((Map<?, ?>) researchType).get("availableValues");
      if (includePublicResearch) {
        assertThat(availableValues).isNotEmpty().extracting("id")
            .containsExactlyInAnyOrderElementsOf(ALL_RESEARCH_TYPES);
      }
      else {
        assertThat(availableValues).isNotEmpty().extracting("id")
            .containsExactlyInAnyOrderElementsOf(DEFAULT_RESEARCH_TYPES);
      }
    });
  }
}
