/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.legal;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.legal.ComponentObligationAttribution;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ComponentObligationAttributionDAOTest
    extends AbstractDbDAOTest
{
  private ComponentObligationAttributionDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createComponentObligationAttributionDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    Date now = new Date();
    ComponentObligationAttribution componentObligationAttribution =
        new ComponentObligationAttribution(ComponentIdentifier.createMavenCoordinates("g", "a", "v"),
            organization.getId(), "name", "content", "legalContentHash", "username");
    componentObligationAttribution.setLastUpdatedAt(new Date(now.getTime() - 1));
    dao.insert(componentObligationAttribution);
    assertThat(componentObligationAttribution.getId()).isNotNull();

    // Read
    assertThat(dao.getById(componentObligationAttribution.getId()))
        .usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .isEqualTo(componentObligationAttribution);

    // Update
    componentObligationAttribution.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    componentObligationAttribution.setOwnerId(application.getId());
    componentObligationAttribution.setObligationName(componentObligationAttribution.getObligationName() + "2");
    componentObligationAttribution.setContent(componentObligationAttribution.getContent() + "2");
    componentObligationAttribution.setLastUpdatedByUsername("other");
    componentObligationAttribution.setLastUpdatedAt(now);
    dao.update(componentObligationAttribution);
    assertThat(dao.getById(componentObligationAttribution.getId())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .usingOverriddenEquals()
        .isEqualTo(componentObligationAttribution);

    // Delete
    dao.delete(componentObligationAttribution);
    assertThat(dao.getById(componentObligationAttribution.getId())).isNull();
  }

  @Test
  public void testGetByIdNotNull() {
    String id = "doesNotExist";
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> dao.getByIdNotNull(id))
        .withMessageContaining("ComponentObligationAttribution with ID " + id + " does not exist.");
  }

  @Test
  public void testInsert_SetsDateIfNull() {
    ComponentObligationAttribution componentObligationAttribution =
        new ComponentObligationAttribution(ComponentIdentifier.createMavenCoordinates("g", "a", "v"),
            organization.getId(), "name", "content", "legalContentHash", "username");
    componentObligationAttribution.setLastUpdatedAt(null);
    Date now = new Date();

    dao.insert(componentObligationAttribution);

    assertThat(dao.getById(componentObligationAttribution.getId()).getLastUpdatedAt()).isAfterOrEqualTo(now);
  }

  @Test
  public void testUpdate_SetsDate() {
    Date now = new Date();
    ComponentObligationAttribution componentObligationAttribution =
        new ComponentObligationAttribution(ComponentIdentifier.createMavenCoordinates("g", "a", "v"),
            organization.getId(), "name", "content", "legalContentHash", "username");
    componentObligationAttribution.setLastUpdatedAt(new Date(now.getTime() - 1));
    dao.insert(componentObligationAttribution);
    assertThat(dao.getById(componentObligationAttribution.getId()).getLastUpdatedAt()).isBefore(now);

    dao.update(componentObligationAttribution);

    assertThat(dao.getById(componentObligationAttribution.getId()).getLastUpdatedAt()).isAfterOrEqualTo(now);
  }

  @Test
  public void testUpdate_DoesNotExist() {
    ComponentObligationAttribution componentObligationAttribution =
        new ComponentObligationAttribution(ComponentIdentifier.createMavenCoordinates("g", "a", "v"),
            organization.getId(), "name", "content", "legalContentHash", "username");
    componentObligationAttribution.setId("doesNotExist");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> dao.update(componentObligationAttribution))
        .withMessageContaining(
            "Cannot update component obligation attribution with id " + componentObligationAttribution.getId() +
                " because it does not exist.");
  }

  @Test
  public void testGetByOwnerId() {
    ComponentObligationAttribution componentObligationAttribution1 = tempEntity
        .newComponentObligationAttribution(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"),
            organization.getId(), "name1", "content1", "legalContentHash1");
    ComponentObligationAttribution componentObligationAttribution2 = tempEntity
        .newComponentObligationAttribution(ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"),
            organization.getId(), "name2", "content2", "legalContentHash2");
    tempEntity.newComponentObligationAttribution(ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"),
        application.getId(), "name3", "content3", "legalContentHash3");

    assertThat(dao.getByOwnerId(organization.getId()))
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(componentObligationAttribution1, componentObligationAttribution2);
  }

  @Test
  public void testGetByOwnerIdAndComponentIdentifierAndObligationNames() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    String obligationName = "name1";
    ComponentObligationAttribution componentObligationAttribution1 = tempEntity.newComponentObligationAttribution(
        componentIdentifier, organization.getId(), obligationName, "content1", "legalContentHash1");
    ComponentObligationAttribution componentObligationAttribution2 = tempEntity.newComponentObligationAttribution(
        componentIdentifier, organization.getId(), obligationName, "content2", "legalContentHash2");
    tempEntity.newComponentObligationAttribution(componentIdentifier.createAlternativeVersion("v2"),
        organization.getId(), obligationName, "content3", "legalContentHash3");
    tempEntity.newComponentObligationAttribution(componentIdentifier, application.getId(), "name1", "content4",
        "legalContentHash4");

    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNames(organization.getId(), componentIdentifier,
        Collections.singleton(obligationName)))
            .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
            .containsExactlyInAnyOrder(componentObligationAttribution1, componentObligationAttribution2);
  }

  @Test
  public void testGetByOwnerIdAndComponentIdentifierAndObligationNames_Null() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentObligationAttribution componentObligationAttribution1 = tempEntity.newComponentObligationAttribution(
        componentIdentifier, organization.getId(), null, "content1", "legalContentHash1");
    ComponentObligationAttribution componentObligationAttribution2 = tempEntity.newComponentObligationAttribution(
        componentIdentifier, organization.getId(), null, "content2", "legalContentHash2");
    tempEntity.newComponentObligationAttribution(componentIdentifier.createAlternativeVersion("v2"),
        organization.getId(), null, "content3", "legalContentHash3");
    tempEntity.newComponentObligationAttribution(componentIdentifier, application.getId(), null, "content4",
        "legalContentHash4");
    tempEntity.newComponentObligationAttribution(componentIdentifier, organization.getId(), "name", "content5",
        "legalContentHash5");

    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNames(organization.getId(), componentIdentifier,
        Collections.singleton(null))).usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
            .containsExactlyInAnyOrder(componentObligationAttribution1, componentObligationAttribution2);
  }

  @Test
  public void testGetByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy_Single() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    String obligationName = "name";

    // Start with a component obligation attribution at just the root org level
    ComponentObligationAttribution componentObligationAttributionForRootOrganization =
        tempEntity.newComponentObligationAttribution(componentIdentifier, Organization.ROOT_ORGANIZATION_ID,
            obligationName, "content1", "legalContentHash1");

    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
        Organization.ROOT_ORGANIZATION_ID, componentIdentifier, Collections.singleton(obligationName)))
            .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
            .containsExactly(componentObligationAttributionForRootOrganization);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
        organization.getId(), componentIdentifier, Collections.singleton(obligationName)))
            .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
            .containsExactly(componentObligationAttributionForRootOrganization);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
        application.getId(), componentIdentifier, Collections.singleton(obligationName)))
            .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
            .containsExactly(componentObligationAttributionForRootOrganization);

    // Add another component obligation attribution at the org level
    ComponentObligationAttribution componentObligationAttributionForOrganization =
        tempEntity.newComponentObligationAttribution(componentIdentifier, organization.getId(),
            obligationName, "content2", "legalContentHash2");

    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
        Organization.ROOT_ORGANIZATION_ID, componentIdentifier, Collections.singleton(obligationName)))
            .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
            .containsExactly(componentObligationAttributionForRootOrganization);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
        organization.getId(), componentIdentifier, Collections.singleton(obligationName)))
            .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
            .containsExactly(componentObligationAttributionForOrganization);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
        application.getId(), componentIdentifier, Collections.singleton(obligationName)))
            .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
            .containsExactly(componentObligationAttributionForOrganization);

    // Add another component obligation attribution at the app level
    ComponentObligationAttribution componentObligationAttributionForApplication =
        tempEntity.newComponentObligationAttribution(componentIdentifier, application.getId(),
            obligationName, "content3", "legalContentHash3");

    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
        Organization.ROOT_ORGANIZATION_ID, componentIdentifier, Collections.singleton(obligationName)))
            .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
            .containsExactly(componentObligationAttributionForRootOrganization);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
        organization.getId(), componentIdentifier, Collections.singleton(obligationName)))
            .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
            .containsExactly(componentObligationAttributionForOrganization);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
        application.getId(), componentIdentifier, Collections.singleton(obligationName)))
            .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
            .containsExactly(componentObligationAttributionForApplication);
  }

  @Test
  public void testGetByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    String obligationName1 = "name1";
    String obligationName2 = "name2";
    String obligationName3 = "name3";
    String obligationName4 = "doesNotExist";
    Set<String> obligationNames =
        new HashSet<>(Arrays.asList(obligationName1, obligationName2, obligationName3, obligationName4));

    // Add different component obligation attributions at different scopes
    ComponentObligationAttribution c1Root = tempEntity.newComponentObligationAttribution(componentIdentifier,
        Organization.ROOT_ORGANIZATION_ID, obligationName1, "content1", "legalContentHash1");
    ComponentObligationAttribution c2Org = tempEntity.newComponentObligationAttribution(componentIdentifier,
        organization.getId(), obligationName2, "content2", "legalContentHash2");
    ComponentObligationAttribution c3App = tempEntity.newComponentObligationAttribution(componentIdentifier,
        application.getId(), obligationName3, "content3", "legalContentHash3");

    // Try to get all obligation attributions at different scopes
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(Organization.ROOT_ORGANIZATION_ID,
        componentIdentifier, obligationNames))
            .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
            .containsExactlyInAnyOrder(c1Root);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(organization.getId(),
        componentIdentifier, obligationNames))
            .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
            .containsExactlyInAnyOrder(c1Root, c2Org);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(application.getId(),
        componentIdentifier, obligationNames))
            .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
            .containsExactlyInAnyOrder(c1Root, c2Org, c3App);

    // Add c1 at org scope
    ComponentObligationAttribution c1Org = tempEntity.newComponentObligationAttribution(componentIdentifier,
        organization.getId(), obligationName1, "content4", "legalContentHash4");
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(Organization.ROOT_ORGANIZATION_ID,
        componentIdentifier, obligationNames))
            .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
            .containsExactlyInAnyOrder(c1Root);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(organization.getId(),
        componentIdentifier, obligationNames))
            .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
            .containsExactlyInAnyOrder(c1Org, c2Org);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(application.getId(),
        componentIdentifier, obligationNames))
            .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
            .containsExactlyInAnyOrder(c1Org, c2Org, c3App);

    // Add c1 at app scope
    ComponentObligationAttribution c1App = tempEntity.newComponentObligationAttribution(componentIdentifier,
        application.getId(), obligationName1, "content5", "legalContentHash5");
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(Organization.ROOT_ORGANIZATION_ID,
        componentIdentifier, obligationNames))
            .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
            .containsExactlyInAnyOrder(c1Root);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(organization.getId(),
        componentIdentifier, obligationNames))
            .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
            .containsExactlyInAnyOrder(c1Org, c2Org);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(application.getId(),
        componentIdentifier, obligationNames))
            .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
            .containsExactlyInAnyOrder(c1App, c2Org, c3App);
  }

  @Test
  public void testGetByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy_Null() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

    ComponentObligationAttribution rootNull1 = tempEntity.newComponentObligationAttribution(componentIdentifier,
        Organization.ROOT_ORGANIZATION_ID, null, "content1", "legalContentHash1");
    ComponentObligationAttribution rootNull2 = tempEntity.newComponentObligationAttribution(componentIdentifier,
        Organization.ROOT_ORGANIZATION_ID, null, "content2", "legalContentHash2");
    tempEntity.newComponentObligationAttribution(componentIdentifier, Organization.ROOT_ORGANIZATION_ID, "name",
        "content3", "legalContentHash3");

    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(application.getId(),
        componentIdentifier, Collections.singleton(null)))
            .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
            .containsExactlyInAnyOrder(rootNull1, rootNull2);

    ComponentObligationAttribution orgNull = tempEntity.newComponentObligationAttribution(componentIdentifier,
        organization.getId(), null, "content4", "legalContentHash4");

    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(application.getId(),
        componentIdentifier, Collections.singleton(null)))
            .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
            .containsExactlyInAnyOrder(orgNull);
  }

  @Test
  public void testGetByOwnerIdAndComponentIdentifierWithHierarchy() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    String obligationName1 = "name1";
    String obligationName2 = "name2";
    String obligationName3 = "name3";

    // Add different component obligation attributions at different scopes
    ComponentObligationAttribution c1Root = tempEntity.newComponentObligationAttribution(componentIdentifier,
        Organization.ROOT_ORGANIZATION_ID, obligationName1, "content1", "legalContentHash1");
    ComponentObligationAttribution c2Org = tempEntity.newComponentObligationAttribution(componentIdentifier,
        organization.getId(), obligationName2, "content2", "legalContentHash2");
    ComponentObligationAttribution c3App = tempEntity.newComponentObligationAttribution(componentIdentifier,
        application.getId(), obligationName3, "content3", "legalContentHash3");

    // Try to get all obligation attributions at different scopes
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(Organization.ROOT_ORGANIZATION_ID,
        componentIdentifier)).usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
            .containsExactlyInAnyOrder(c1Root);
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(organization.getId(),
        componentIdentifier)).usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
            .containsExactlyInAnyOrder(c1Root, c2Org);
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(application.getId(),
        componentIdentifier)).usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
            .containsExactlyInAnyOrder(c1Root, c2Org, c3App);

    // Add c1 at org scope
    ComponentObligationAttribution c1Org = tempEntity.newComponentObligationAttribution(componentIdentifier,
        organization.getId(), obligationName1, "content4", "legalContentHash4");
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(Organization.ROOT_ORGANIZATION_ID,
        componentIdentifier)).usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
            .containsExactlyInAnyOrder(c1Root);
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(organization.getId(),
        componentIdentifier)).usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
            .containsExactlyInAnyOrder(c1Org, c2Org);
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(application.getId(),
        componentIdentifier)).usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
            .containsExactlyInAnyOrder(c1Org, c2Org, c3App);

    // Add c1 at app scope
    ComponentObligationAttribution c1App = tempEntity.newComponentObligationAttribution(componentIdentifier,
        application.getId(), obligationName1, "content5", "legalContentHash5");
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(Organization.ROOT_ORGANIZATION_ID,
        componentIdentifier)).usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
            .containsExactlyInAnyOrder(c1Root);
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(organization.getId(),
        componentIdentifier)).usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
            .containsExactlyInAnyOrder(c1Org, c2Org);
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(application.getId(),
        componentIdentifier)).usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
            .containsExactlyInAnyOrder(c1App, c2Org, c3App);
  }

  // ---- Batch hierarchy tests ----

  @Test
  public void testBatchGetWithHierarchy_emptyComponentList() {
    Map<ComponentIdentifier, List<ComponentObligationAttribution>> result =
        dao.batchGetWithHierarchy(application.getId(), List.of());
    assertThat(result).isEmpty();
  }

  @Test
  public void testBatchGetWithHierarchy_accumulatesFromAllLevels() {
    ComponentIdentifier ci = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

    tempEntity.newComponentObligationAttribution(ci, Organization.ROOT_ORGANIZATION_ID,
        "NOTICE", "root content", "rootHash");
    tempEntity.newComponentObligationAttribution(ci, organization.getId(),
        "SOURCE", "org content", "orgHash");

    Map<ComponentIdentifier, List<ComponentObligationAttribution>> result =
        dao.batchGetWithHierarchy(application.getId(), List.of(ci));

    assertThat(result).containsKey(ci);
    assertThat(result.get(ci)).hasSize(2);
    assertThat(result.get(ci))
        .extracting(ComponentObligationAttribution::getObligationName)
        .containsExactlyInAnyOrder("NOTICE", "SOURCE");
  }

  @Test
  public void testBatchGetWithHierarchy_closestWinsPerName() {
    ComponentIdentifier ci = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

    tempEntity.newComponentObligationAttribution(ci, Organization.ROOT_ORGANIZATION_ID,
        "NOTICE", "root content", "rootHash");
    ComponentObligationAttribution orgAttribution = tempEntity.newComponentObligationAttribution(ci,
        organization.getId(), "NOTICE", "org content", "orgHash");

    Map<ComponentIdentifier, List<ComponentObligationAttribution>> result =
        dao.batchGetWithHierarchy(application.getId(), List.of(ci));

    assertThat(result).containsKey(ci);
    assertThat(result.get(ci)).hasSize(1);
    assertThat(result.get(ci).getFirst().getId()).isEqualTo(orgAttribution.getId());
  }

  @Test
  public void testBatchGetWithHierarchy_noAttributionsExist() {
    ComponentIdentifier ci = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

    Map<ComponentIdentifier, List<ComponentObligationAttribution>> result =
        dao.batchGetWithHierarchy(application.getId(), List.of(ci));

    assertThat(result).doesNotContainKey(ci);
  }

  @Test
  public void testBatchGetWithHierarchy_nullObligationNameDeduplication() {
    ComponentIdentifier ci = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

    // Null-named attribution at root
    tempEntity.newComponentObligationAttribution(ci, Organization.ROOT_ORGANIZATION_ID,
        null, "root null-named content", "rootHash");
    // Null-named attribution at org (closer - should win)
    ComponentObligationAttribution orgNullAttribution = tempEntity.newComponentObligationAttribution(ci,
        organization.getId(), null, "org null-named content", "orgHash");
    // Named attribution at root (different name, should also appear)
    ComponentObligationAttribution rootNamedAttribution = tempEntity.newComponentObligationAttribution(ci,
        Organization.ROOT_ORGANIZATION_ID, "NOTICE", "root NOTICE content", "rootHash2");

    Map<ComponentIdentifier, List<ComponentObligationAttribution>> result =
        dao.batchGetWithHierarchy(application.getId(), List.of(ci));

    assertThat(result).containsKey(ci);
    assertThat(result.get(ci)).hasSize(2);
    assertThat(result.get(ci))
        .extracting(ComponentObligationAttribution::getId)
        .containsExactlyInAnyOrder(orgNullAttribution.getId(), rootNamedAttribution.getId());
  }

  @Test
  public void testBatchGetWithHierarchy_multipleComponentsDifferentLevels() {
    ComponentIdentifier ci1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentIdentifier ci2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2");
    ComponentIdentifier ci3 = ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3");

    // ci1: only at root org
    ComponentObligationAttribution attr1 = tempEntity.newComponentObligationAttribution(ci1,
        Organization.ROOT_ORGANIZATION_ID, "NOTICE", "root content", "hash1");
    // ci2: at app level (direct)
    ComponentObligationAttribution attr2 = tempEntity.newComponentObligationAttribution(ci2,
        application.getId(), "SOURCE", "app content", "hash2");
    // ci3: no data at all

    Map<ComponentIdentifier, List<ComponentObligationAttribution>> result =
        dao.batchGetWithHierarchy(application.getId(), List.of(ci1, ci2, ci3));

    assertThat(result).containsKey(ci1);
    assertThat(result.get(ci1).getFirst().getId()).isEqualTo(attr1.getId());
    assertThat(result).containsKey(ci2);
    assertThat(result.get(ci2).getFirst().getId()).isEqualTo(attr2.getId());
    assertThat(result).doesNotContainKey(ci3);
  }
}
