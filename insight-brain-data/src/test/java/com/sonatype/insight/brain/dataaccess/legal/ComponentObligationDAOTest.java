/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.legal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.legal.ComponentObligation;
import com.sonatype.insight.brain.model.legal.ObligationStatus;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentObligationDAOTest
    extends AbstractDbDAOTest
{
  private ComponentObligationDAO dao;

  @Before
  public void before() {
    dao = new ComponentObligationDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    ComponentObligation componentObligation = new ComponentObligation(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(), "name", "comment",
        ObligationStatus.OPEN, "legalContentHash");
    dao.insert(componentObligation);
    assertThat(componentObligation.getId()).isNotNull();

    // Read
    assertThat(dao.getById(componentObligation.getId())).usingRecursiveComparison().isEqualTo(componentObligation);

    // Update
    componentObligation.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    componentObligation.setOwnerId(application.getId());
    componentObligation.setObligationName(componentObligation.getObligationName() + "2");
    componentObligation.setComment(componentObligation.getComment() + "2");
    componentObligation.setStatus(ObligationStatus.FULFILLED);
    dao.update(componentObligation);
    assertThat(dao.getById(componentObligation.getId())).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(componentObligation);

    // Delete
    dao.delete(componentObligation);
    assertThat(dao.getById(componentObligation.getId())).isNull();
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

    assertThat(dao.getByOwnerId(organization.getId())).usingRecursiveFieldByFieldElementComparator()
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

    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNames(organization.getId(), componentIdentifier,
        Collections.singleton(obligationName))).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(componentObligation);
  }

  @Test
  public void testGetByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy_Single() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    String obligationName = "name";

    // Start with a component obligation at just the root org level
    ComponentObligation componentObligationForRootOrganization =
        tempEntity.newComponentObligation(componentIdentifier, Organization.ROOT_ORGANIZATION_ID,
            obligationName, "comment1", ObligationStatus.FULFILLED, "legalContentHash1");

    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
        Organization.ROOT_ORGANIZATION_ID, componentIdentifier, Collections.singleton(obligationName)))
        .usingRecursiveFieldByFieldElementComparator().containsExactly(componentObligationForRootOrganization);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
        organization.getId(), componentIdentifier, Collections.singleton(obligationName)))
        .usingRecursiveFieldByFieldElementComparator().containsExactly(componentObligationForRootOrganization);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
        application.getId(), componentIdentifier, Collections.singleton(obligationName)))
        .usingRecursiveFieldByFieldElementComparator().containsExactly(componentObligationForRootOrganization);

    // Add another component obligation at the org level
    ComponentObligation componentObligationForOrganization =
        tempEntity.newComponentObligation(componentIdentifier, organization.getId(),
            obligationName, "comment2", ObligationStatus.FULFILLED, "legalContentHash2");

    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
        Organization.ROOT_ORGANIZATION_ID, componentIdentifier, Collections.singleton(obligationName)))
        .usingRecursiveFieldByFieldElementComparator().containsExactly(componentObligationForRootOrganization);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
        organization.getId(), componentIdentifier, Collections.singleton(obligationName)))
        .usingRecursiveFieldByFieldElementComparator().containsExactly(componentObligationForOrganization);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
        application.getId(), componentIdentifier, Collections.singleton(obligationName)))
        .usingRecursiveFieldByFieldElementComparator().containsExactly(componentObligationForOrganization);

    // Add another component obligation at the app level
    ComponentObligation componentObligationForApplication =
        tempEntity.newComponentObligation(componentIdentifier, application.getId(),
            obligationName, "comment3", ObligationStatus.FULFILLED, "legalContentHash3");

    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
        Organization.ROOT_ORGANIZATION_ID, componentIdentifier, Collections.singleton(obligationName)))
        .usingRecursiveFieldByFieldElementComparator().containsExactly(componentObligationForRootOrganization);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
        organization.getId(), componentIdentifier, Collections.singleton(obligationName)))
        .usingRecursiveFieldByFieldElementComparator().containsExactly(componentObligationForOrganization);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
        application.getId(), componentIdentifier, Collections.singleton(obligationName)))
        .usingRecursiveFieldByFieldElementComparator().containsExactly(componentObligationForApplication);
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

    // Add different component obligations at different scopes
    ComponentObligation c1Root =
        tempEntity.newComponentObligation(componentIdentifier, Organization.ROOT_ORGANIZATION_ID,
            obligationName1, "comment1", ObligationStatus.FULFILLED, "legalContentHash1");
    ComponentObligation c2Org = tempEntity.newComponentObligation(componentIdentifier, organization.getId(),
        obligationName2, "comment2", ObligationStatus.FLAGGED, "legalContentHash2");
    ComponentObligation c3App = tempEntity.newComponentObligation(componentIdentifier, application.getId(),
        obligationName3, "comment3", ObligationStatus.IGNORED, "legalContentHash3");

    // Try to get all obligations at different scopes
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(Organization.ROOT_ORGANIZATION_ID,
        componentIdentifier, obligationNames)).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(c1Root);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(organization.getId(),
        componentIdentifier, obligationNames)).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(c1Root, c2Org);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(application.getId(),
        componentIdentifier, obligationNames)).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(c1Root, c2Org, c3App);

    // Add c1 at org scope
    ComponentObligation c1Org = tempEntity.newComponentObligation(componentIdentifier, organization.getId(),
        obligationName1, "comment4", ObligationStatus.FLAGGED, "legalContentHash4");
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(Organization.ROOT_ORGANIZATION_ID,
        componentIdentifier, obligationNames)).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(c1Root);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(organization.getId(),
        componentIdentifier, obligationNames)).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(c1Org, c2Org);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(application.getId(),
        componentIdentifier, obligationNames)).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(c1Org, c2Org, c3App);

    // Add c1 at app scope
    ComponentObligation c1App = tempEntity.newComponentObligation(componentIdentifier, application.getId(),
        obligationName1, "comment5", ObligationStatus.IGNORED, "legalContentHash5");
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(Organization.ROOT_ORGANIZATION_ID,
        componentIdentifier, obligationNames)).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(c1Root);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(organization.getId(),
        componentIdentifier, obligationNames)).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(c1Org, c2Org);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(application.getId(),
        componentIdentifier, obligationNames)).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(c1App, c2Org, c3App);
  }
}
