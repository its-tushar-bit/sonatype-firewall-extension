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
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.legal.ComponentObligation;
import com.sonatype.insight.brain.model.legal.ObligationStatus;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ComponentObligationDAOTest
    extends AbstractDbDAOTest
{
  private ComponentObligationDAO dao;

  private OwnerDAO ownerDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createComponentObligationDAO();
    ownerDAO = daoFactory.createOwnerDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    Date now = new Date();
    ComponentObligation componentObligation = new ComponentObligation(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(), "name", "comment",
        ObligationStatus.OPEN, "legalContentHash", "username");
    componentObligation.setLastUpdatedAt(new Date(now.getTime() - 1));
    dao.insert(componentObligation);
    assertThat(componentObligation.getId()).isNotNull();

    // Read
    assertThat(dao.getById(componentObligation.getId())).usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .isEqualTo(componentObligation);

    // Update
    componentObligation.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    componentObligation.setOwnerId(application.getId());
    componentObligation.setObligationName(componentObligation.getObligationName() + "2");
    componentObligation.setComment(componentObligation.getComment() + "2");
    componentObligation.setStatus(ObligationStatus.FULFILLED);
    componentObligation.setLastUpdatedByUsername("other");
    componentObligation.setLastUpdatedAt(now);
    dao.update(componentObligation);
    assertThat(dao.getById(componentObligation.getId())).usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .ignoringFields(JPA.IGNORE_FIELDS)
        .usingOverriddenEquals()
        .isEqualTo(componentObligation);

    // Delete
    dao.delete(componentObligation);
    assertThat(dao.getById(componentObligation.getId())).isNull();
  }

  @Test
  public void testGetByIdNotNull() {
    String id = "doesNotExist";
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> dao.getByIdNotNull(id))
        .withMessageContaining("ComponentObligation with ID " + id + " does not exist.");
  }

  @Test
  public void testInsert_SetsDateIfNull() {
    ComponentObligation componentObligation = new ComponentObligation(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(), "name", "comment",
        ObligationStatus.OPEN, "legalContentHash", "username");
    componentObligation.setLastUpdatedAt(null);
    Date now = new Date();

    dao.insert(componentObligation);

    assertThat(dao.getById(componentObligation.getId()).getLastUpdatedAt()).isAfterOrEqualTo(now);
  }

  @Test
  public void testInsert_SameOwnerAndComponentAndObligationName() {
    ComponentObligation componentObligation1 = new ComponentObligation(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(), "name", "comment1",
        ObligationStatus.OPEN, "legalContentHash1", "username1");
    dao.insert(componentObligation1);
    ComponentObligation componentObligation2 = new ComponentObligation(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(), "name", "comment2",
        ObligationStatus.IGNORED, "legalContentHash2", "username2");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> dao.insert(componentObligation2))
        .withMessageContaining(
            "Component obligation already exists for owner with id " + componentObligation2.getOwnerId() +
                " and component " + componentObligation2.getComponentIdentifier() + " and obligation name " +
                componentObligation2.getObligationName() + ".");
  }

  @Test
  public void testUpdate_SetsDate() {
    Date now = new Date();
    ComponentObligation componentObligation = new ComponentObligation(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(), "name", "comment",
        ObligationStatus.OPEN, "legalContentHash", "username");
    componentObligation.setLastUpdatedAt(new Date(now.getTime() - 1));
    dao.insert(componentObligation);
    assertThat(dao.getById(componentObligation.getId()).getLastUpdatedAt()).isBefore(now);

    dao.update(componentObligation);

    assertThat(dao.getById(componentObligation.getId()).getLastUpdatedAt()).isAfterOrEqualTo(now);
  }

  @Test
  public void testUpdate_DoesNotExist() {
    ComponentObligation componentObligation = new ComponentObligation(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(), "name", "comment",
        ObligationStatus.OPEN, "legalContentHash", "username");
    componentObligation.setId("doesNotExist");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> dao.update(componentObligation))
        .withMessageContaining("Cannot update component obligation with id " + componentObligation.getId() +
            " because it does not exist.");
  }

  @Test
  public void testGetByOwnerId() {
    ComponentObligation componentObligation1 = tempEntity
        .newComponentObligation(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), organization.getId(),
            "name1", "comment1", ObligationStatus.OPEN, "legalContentHash1");
    ComponentObligation componentObligation2 = tempEntity
        .newComponentObligation(ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"), organization.getId(),
            "name2", "comment2", ObligationStatus.OPEN, "legalContentHash2");
    tempEntity.newComponentObligation(ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"), application.getId(),
        "name3", "comment3", ObligationStatus.OPEN, "legalContentHash3");

    assertThat(dao.getByOwnerId(organization.getId()))
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(componentObligation1, componentObligation2);
  }

  @Test
  public void testGetByOwnerIdAndComponentIdentifierAndObligationNames() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    String obligationName = "name1";
    ComponentObligation componentObligation = tempEntity.newComponentObligation(componentIdentifier,
        organization.getId(), obligationName, "comment1", ObligationStatus.OPEN, "legalContentHash1");
    tempEntity.newComponentObligation(componentIdentifier.createAlternativeVersion("v2"),
        organization.getId(), obligationName, "comment1", ObligationStatus.OPEN, "legalContentHash1");
    tempEntity.newComponentObligation(componentIdentifier, application.getId(), obligationName, "comment1",
        ObligationStatus.OPEN, "legalContentHash1");
    tempEntity.newComponentObligation(componentIdentifier, organization.getId(), "name2", "comment1",
        ObligationStatus.OPEN, "legalContentHash1");

    assertGetByOwnerIdsAndComponentIdentifierAndObligationNames(
        organization.getId(), componentIdentifier, Collections.singleton(obligationName),
        componentObligation);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationName(organization.getId(), componentIdentifier,
        obligationName)).usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG).isEqualTo(componentObligation);
  }

  @Test
  public void testGetByOwnerIdsAndComponentIdentifierAndObligationNames_Single() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    String obligationName = "name";
    Set<String> obligationNames = Collections.singleton(obligationName);

    // Start with a component obligation at just the root org level
    ComponentObligation componentObligationForRootOrganization =
        tempEntity.newComponentObligation(componentIdentifier, Organization.ROOT_ORGANIZATION_ID,
            obligationName, "comment1", ObligationStatus.FULFILLED, "legalContentHash1");
    assertGetByOwnerIdsAndComponentIdentifierAndObligationNames(
        Organization.ROOT_ORGANIZATION_ID, componentIdentifier, obligationNames,
        componentObligationForRootOrganization);
    assertGetByOwnerIdsAndComponentIdentifierAndObligationNames(
        organization.getId(), componentIdentifier, obligationNames,
        componentObligationForRootOrganization);
    assertGetByOwnerIdsAndComponentIdentifierAndObligationNames(
        application.getId(), componentIdentifier, obligationNames,
        componentObligationForRootOrganization);

    // Add another component obligation at the org level
    ComponentObligation componentObligationForOrganization =
        tempEntity.newComponentObligation(componentIdentifier, organization.getId(),
            obligationName, "comment2", ObligationStatus.FULFILLED, "legalContentHash2");
    assertGetByOwnerIdsAndComponentIdentifierAndObligationNames(
        Organization.ROOT_ORGANIZATION_ID, componentIdentifier, obligationNames,
        componentObligationForRootOrganization);
    assertGetByOwnerIdsAndComponentIdentifierAndObligationNames(
        organization.getId(), componentIdentifier, obligationNames,
        componentObligationForOrganization);
    assertGetByOwnerIdsAndComponentIdentifierAndObligationNames(
        application.getId(), componentIdentifier, obligationNames,
        componentObligationForOrganization);

    // Add another component obligation at the app level
    ComponentObligation componentObligationForApplication =
        tempEntity.newComponentObligation(componentIdentifier, application.getId(),
            obligationName, "comment3", ObligationStatus.FULFILLED, "legalContentHash3");
    assertGetByOwnerIdsAndComponentIdentifierAndObligationNames(
        Organization.ROOT_ORGANIZATION_ID, componentIdentifier, obligationNames,
        componentObligationForRootOrganization);
    assertGetByOwnerIdsAndComponentIdentifierAndObligationNames(
        organization.getId(), componentIdentifier, obligationNames,
        componentObligationForOrganization);
    assertGetByOwnerIdsAndComponentIdentifierAndObligationNames(
        application.getId(), componentIdentifier, obligationNames,
        componentObligationForApplication);
  }

  @Test
  public void testGetByOwnerIdsAndComponentIdentifierAndObligationNames() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    String obligationName1 = "name1";
    String obligationName2 = "name2";
    String obligationName3 = "name3";
    String obligationName4 = "doesNotExist";
    Set<String> obligationNames =
        new HashSet<>(Arrays.asList(obligationName1, obligationName2, obligationName3, obligationName4));

    // Add different component obligations at different scopes
    ComponentObligation c1Root =
        tempEntity.newComponentObligation(componentIdentifier, Organization.ROOT_ORGANIZATION_ID,
            obligationName1, "comment1", ObligationStatus.FULFILLED, "legalContentHash1");
    ComponentObligation c2Org = tempEntity.newComponentObligation(componentIdentifier, organization.getId(),
        obligationName2, "comment2", ObligationStatus.FLAGGED, "legalContentHash2");
    ComponentObligation c3App = tempEntity.newComponentObligation(componentIdentifier, application.getId(),
        obligationName3, "comment3", ObligationStatus.IGNORED, "legalContentHash3");

    // Try to get all obligations at different scopes
    assertGetByOwnerIdsAndComponentIdentifierAndObligationNames(
        Organization.ROOT_ORGANIZATION_ID, componentIdentifier, obligationNames,
        c1Root);
    assertGetByOwnerIdsAndComponentIdentifierAndObligationNames(
        organization.getId(), componentIdentifier, obligationNames,
        c1Root, c2Org);
    assertGetByOwnerIdsAndComponentIdentifierAndObligationNames(
        application.getId(), componentIdentifier, obligationNames,
        c1Root, c2Org, c3App);

    // Add c1 at org scope
    ComponentObligation c1Org = tempEntity.newComponentObligation(componentIdentifier, organization.getId(),
        obligationName1, "comment4", ObligationStatus.FLAGGED, "legalContentHash4");
    assertGetByOwnerIdsAndComponentIdentifierAndObligationNames(
        Organization.ROOT_ORGANIZATION_ID, componentIdentifier, obligationNames,
        c1Root);
    assertGetByOwnerIdsAndComponentIdentifierAndObligationNames(
        organization.getId(), componentIdentifier, obligationNames,
        c1Org, c2Org);
    assertGetByOwnerIdsAndComponentIdentifierAndObligationNames(
        application.getId(), componentIdentifier, obligationNames,
        c1Org, c2Org, c3App);

    // Add c1 at app scope
    ComponentObligation c1App = tempEntity.newComponentObligation(componentIdentifier, application.getId(),
        obligationName1, "comment5", ObligationStatus.IGNORED, "legalContentHash5");
    assertGetByOwnerIdsAndComponentIdentifierAndObligationNames(
        Organization.ROOT_ORGANIZATION_ID, componentIdentifier, obligationNames,
        c1Root);
    assertGetByOwnerIdsAndComponentIdentifierAndObligationNames(
        organization.getId(), componentIdentifier, obligationNames,
        c1Org, c2Org);
    assertGetByOwnerIdsAndComponentIdentifierAndObligationNames(
        application.getId(), componentIdentifier, obligationNames,
        c1App, c2Org, c3App);
  }

  private void assertGetByOwnerIdsAndComponentIdentifierAndObligationNames(
      final String ownerId,
      final ComponentIdentifier componentIdentifier,
      final Set<String> obligationNames,
      final ComponentObligation... componentObligation)
  {
    List<String> ownerIds = ownerDAO.getOwnerIds(ownerId);
    List<ComponentObligation> actual;
    try (TransactionContext tx = dao.createTransactionContext()) {
      actual = dao.getByOwnerIdsAndComponentIdentifierAndObligationNames(tx,
          ownerIds, componentIdentifier, obligationNames);
    }

    assertThat(actual)
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(componentObligation);
  }

  @Test
  public void testGetByOwnerIdAndComponentIdentifierWithHierarchy() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

    ComponentObligation obligationOrg = tempEntity.newComponentObligation(componentIdentifier, organization.getId(),
        "name1", "comment1", ObligationStatus.OPEN, "hash1");

    List<ComponentObligation> result =
        dao.getByOwnerIdAndComponentIdentifierWithHierarchy(application.getId(), componentIdentifier);
    assertThat(result)
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactly(obligationOrg);

    ComponentObligation obligationApp1 = tempEntity.newComponentObligation(componentIdentifier, application.getId(),
        "name2", "comment2", ObligationStatus.FULFILLED, "hash2");
    ComponentObligation obligationApp2 = tempEntity.newComponentObligation(componentIdentifier, application.getId(),
        "name3", "comment3", ObligationStatus.FLAGGED, "hash3");

    result = dao.getByOwnerIdAndComponentIdentifierWithHierarchy(application.getId(), componentIdentifier);
    assertThat(result)
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(obligationOrg, obligationApp1, obligationApp2);
  }

  @Test
  public void testGetAddressedObligationsByOwnerIdWithHierarchy() {
    assertThat(dao.getAddressedObligationsByOwnerIdWithHierarchy(application.getId())).isEmpty();

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2");

    tempEntity.newComponentObligation(componentIdentifier1, Organization.ROOT_ORGANIZATION_ID, "name1", "comment1",
        ObligationStatus.OPEN, "hash1");

    assertThat(dao.getAddressedObligationsByOwnerIdWithHierarchy(application.getId())).isEmpty();

    tempEntity.newComponentObligation(componentIdentifier1, Organization.ROOT_ORGANIZATION_ID, "name2", "comment2",
        ObligationStatus.FULFILLED, "hash2");

    Map<ComponentIdentifier, Set<String>> result =
        dao.getAddressedObligationsByOwnerIdWithHierarchy(application.getId());
    assertThat(result).hasSize(1);
    assertThat(result.get(componentIdentifier1)).containsExactly("name2");

    tempEntity.newComponentObligation(componentIdentifier1, application.getOrganizationId(), "name3", "comment3",
        ObligationStatus.FLAGGED, "hash3");

    result = dao.getAddressedObligationsByOwnerIdWithHierarchy(application.getId());
    assertThat(result).hasSize(1);
    assertThat(result.get(componentIdentifier1)).containsExactly("name2");

    tempEntity.newComponentObligation(componentIdentifier1, application.getOrganizationId(), "name4", "comment4",
        ObligationStatus.IGNORED, "hash4");

    result = dao.getAddressedObligationsByOwnerIdWithHierarchy(application.getId());
    assertThat(result).hasSize(1);
    assertThat(result.get(componentIdentifier1)).containsExactlyInAnyOrder("name2", "name4");

    tempEntity.newComponentObligation(componentIdentifier1, application.getId(), "name4", "comment4",
        ObligationStatus.FULFILLED, "hash4");
    tempEntity.newComponentObligation(componentIdentifier2, application.getId(), "name5", "comment5",
        ObligationStatus.FULFILLED, "hash5");

    result = dao.getAddressedObligationsByOwnerIdWithHierarchy(application.getId());
    assertThat(result).hasSize(2);
    assertThat(result.get(componentIdentifier1)).containsExactlyInAnyOrder("name2", "name4");
    assertThat(result.get(componentIdentifier2)).containsExactly("name5");
  }

  // ---- Batch hierarchy tests ----

  @Test
  public void testBatchGetWithHierarchy_emptyComponentList() {
    Map<ComponentIdentifier, List<ComponentObligation>> result =
        dao.batchGetWithHierarchy(application.getId(), List.of());
    assertThat(result).isEmpty();
  }

  @Test
  public void testBatchGetWithHierarchy_accumulatesFromAllLevels() {
    ComponentIdentifier ci = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

    tempEntity.newComponentObligation(ci, Organization.ROOT_ORGANIZATION_ID,
        "NOTICE", "root comment", ObligationStatus.OPEN, "rootHash");
    tempEntity.newComponentObligation(ci, organization.getId(),
        "SOURCE", "org comment", ObligationStatus.FULFILLED, "orgHash");

    Map<ComponentIdentifier, List<ComponentObligation>> result =
        dao.batchGetWithHierarchy(application.getId(), List.of(ci));

    assertThat(result).containsKey(ci);
    assertThat(result.get(ci)).hasSize(2);
    assertThat(result.get(ci))
        .extracting(ComponentObligation::getObligationName)
        .containsExactlyInAnyOrder("NOTICE", "SOURCE");
  }

  @Test
  public void testBatchGetWithHierarchy_closestWinsPerName() {
    ComponentIdentifier ci = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

    tempEntity.newComponentObligation(ci, Organization.ROOT_ORGANIZATION_ID,
        "NOTICE", "root comment", ObligationStatus.OPEN, "rootHash");
    ComponentObligation orgObligation = tempEntity.newComponentObligation(ci, organization.getId(),
        "NOTICE", "org comment", ObligationStatus.FULFILLED, "orgHash");

    Map<ComponentIdentifier, List<ComponentObligation>> result =
        dao.batchGetWithHierarchy(application.getId(), List.of(ci));

    assertThat(result).containsKey(ci);
    assertThat(result.get(ci)).hasSize(1);
    assertThat(result.get(ci).getFirst().getId()).isEqualTo(orgObligation.getId());
  }

  @Test
  public void testBatchGetWithHierarchy_multipleComponents() {
    ComponentIdentifier ci1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentIdentifier ci2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2");

    ComponentObligation ob1 = tempEntity.newComponentObligation(ci1, organization.getId(),
        "NOTICE", "comment1", ObligationStatus.OPEN, "hash1");
    ComponentObligation ob2 = tempEntity.newComponentObligation(ci2, application.getId(),
        "SOURCE", "comment2", ObligationStatus.FLAGGED, "hash2");

    Map<ComponentIdentifier, List<ComponentObligation>> result =
        dao.batchGetWithHierarchy(application.getId(), List.of(ci1, ci2));

    assertThat(result.get(ci1).getFirst().getId()).isEqualTo(ob1.getId());
    assertThat(result.get(ci2).getFirst().getId()).isEqualTo(ob2.getId());
  }

  @Test
  public void testBatchGetWithHierarchy_noObligationsExist() {
    ComponentIdentifier ci = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

    Map<ComponentIdentifier, List<ComponentObligation>> result =
        dao.batchGetWithHierarchy(application.getId(), List.of(ci));

    assertThat(result).doesNotContainKey(ci);
  }

  @Test
  public void testBatchGetWithHierarchy_multipleComponentsMixOfPresenceAndAbsence() {
    ComponentIdentifier ci1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentIdentifier ci2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2");
    ComponentIdentifier ci3 = ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3");

    // ci1: obligations at root and org (closest wins per name)
    tempEntity.newComponentObligation(ci1, Organization.ROOT_ORGANIZATION_ID,
        "NOTICE", "root comment", ObligationStatus.OPEN, "rootHash");
    ComponentObligation ob1Org = tempEntity.newComponentObligation(ci1, organization.getId(),
        "NOTICE", "org comment", ObligationStatus.FULFILLED, "orgHash");
    ComponentObligation ob1Root = tempEntity.newComponentObligation(ci1, Organization.ROOT_ORGANIZATION_ID,
        "SOURCE", "root source comment", ObligationStatus.OPEN, "rootHash2");

    // ci2: no obligations at all
    // ci3: obligation at app level only
    ComponentObligation ob3 = tempEntity.newComponentObligation(ci3, application.getId(),
        "DISTRIBUTE", "app comment", ObligationStatus.FLAGGED, "appHash");

    Map<ComponentIdentifier, List<ComponentObligation>> result =
        dao.batchGetWithHierarchy(application.getId(), List.of(ci1, ci2, ci3));

    assertThat(result).containsKey(ci1);
    assertThat(result.get(ci1)).hasSize(2);
    assertThat(result.get(ci1))
        .extracting(ComponentObligation::getId)
        .containsExactlyInAnyOrder(ob1Org.getId(), ob1Root.getId());

    assertThat(result).doesNotContainKey(ci2);

    assertThat(result).containsKey(ci3);
    assertThat(result.get(ci3)).hasSize(1);
    assertThat(result.get(ci3).getFirst().getId()).isEqualTo(ob3.getId());
  }

  @Test
  public void testBatchGetWithHierarchy_multipleObligationNamesSameComponentMixedDistances() {
    ComponentIdentifier ci = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

    // NOTICE at root and org: org wins
    tempEntity.newComponentObligation(ci, Organization.ROOT_ORGANIZATION_ID,
        "NOTICE", "root NOTICE", ObligationStatus.OPEN, "rootNotice");
    ComponentObligation orgNotice = tempEntity.newComponentObligation(ci, organization.getId(),
        "NOTICE", "org NOTICE", ObligationStatus.FULFILLED, "orgNotice");

    // SOURCE only at root: root wins (no closer)
    ComponentObligation rootSource = tempEntity.newComponentObligation(ci, Organization.ROOT_ORGANIZATION_ID,
        "SOURCE", "root SOURCE", ObligationStatus.OPEN, "rootSource");

    // DISTRIBUTE only at app: direct match
    ComponentObligation appDistribute = tempEntity.newComponentObligation(ci, application.getId(),
        "DISTRIBUTE", "app DISTRIBUTE", ObligationStatus.FLAGGED, "appDistribute");

    Map<ComponentIdentifier, List<ComponentObligation>> result =
        dao.batchGetWithHierarchy(application.getId(), List.of(ci));

    assertThat(result).containsKey(ci);
    assertThat(result.get(ci)).hasSize(3);
    assertThat(result.get(ci))
        .extracting(ComponentObligation::getId)
        .containsExactlyInAnyOrder(orgNotice.getId(), rootSource.getId(), appDistribute.getId());
  }
}
