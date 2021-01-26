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
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.legal.ComponentObligationAttribution;

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentObligationAttributionDAOTest
    extends AbstractDbDAOTest
{
  private ComponentObligationAttributionDAO dao;

  @Before
  public void before() {
    dao = new ComponentObligationAttributionDAO();
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
    assertThat(dao.getById(componentObligationAttribution.getId())).usingRecursiveComparison()
        .isEqualTo(componentObligationAttribution);

    // Update
    componentObligationAttribution.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    componentObligationAttribution.setOwnerId(application.getId());
    componentObligationAttribution.setObligationName(componentObligationAttribution.getObligationName() + "2");
    componentObligationAttribution.setContent(componentObligationAttribution.getContent() + "2");
    componentObligationAttribution.setLastUpdatedByUsername("other");
    componentObligationAttribution.setLastUpdatedAt(now);
    dao.update(componentObligationAttribution);
    assertThat(dao.getById(componentObligationAttribution.getId())).usingRecursiveComparison(
        RecursiveComparisonConfiguration.builder().withIgnoredFields(JPA.IGNORE_FIELDS).build())
        .isEqualTo(componentObligationAttribution);

    // Delete
    dao.delete(componentObligationAttribution);
    assertThat(dao.getById(componentObligationAttribution.getId())).isNull();
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
  public void testGetByOwnerId() {
    ComponentObligationAttribution componentObligationAttribution1 = tempEntity
        .newComponentObligationAttribution(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"),
            organization.getId(), "name1", "content1", "legalContentHash1");
    ComponentObligationAttribution componentObligationAttribution2 = tempEntity
        .newComponentObligationAttribution(ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"),
            organization.getId(), "name2", "content2", "legalContentHash2");
    tempEntity.newComponentObligationAttribution(ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"),
        application.getId(), "name3", "content3", "legalContentHash3");

    assertThat(dao.getByOwnerId(organization.getId())).usingRecursiveFieldByFieldElementComparator()
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
        Collections.singleton(obligationName))).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(
        componentObligationAttribution1, componentObligationAttribution2);
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
        .usingRecursiveFieldByFieldElementComparator().containsExactly(
        componentObligationAttributionForRootOrganization);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
        organization.getId(), componentIdentifier, Collections.singleton(obligationName)))
        .usingRecursiveFieldByFieldElementComparator().containsExactly(
        componentObligationAttributionForRootOrganization);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
        application.getId(), componentIdentifier, Collections.singleton(obligationName)))
        .usingRecursiveFieldByFieldElementComparator().containsExactly(
        componentObligationAttributionForRootOrganization);

    // Add another component obligation attribution at the org level
    ComponentObligationAttribution componentObligationAttributionForOrganization =
        tempEntity.newComponentObligationAttribution(componentIdentifier, organization.getId(),
            obligationName, "content2", "legalContentHash2");

    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
        Organization.ROOT_ORGANIZATION_ID, componentIdentifier, Collections.singleton(obligationName)))
        .usingRecursiveFieldByFieldElementComparator().containsExactly(
        componentObligationAttributionForRootOrganization);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
        organization.getId(), componentIdentifier, Collections.singleton(obligationName)))
        .usingRecursiveFieldByFieldElementComparator().containsExactly(
        componentObligationAttributionForOrganization);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
        application.getId(), componentIdentifier, Collections.singleton(obligationName)))
        .usingRecursiveFieldByFieldElementComparator().containsExactly(
        componentObligationAttributionForOrganization);

    // Add another component obligation attribution at the app level
    ComponentObligationAttribution componentObligationAttributionForApplication =
        tempEntity.newComponentObligationAttribution(componentIdentifier, application.getId(),
            obligationName, "content3", "legalContentHash3");

    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
        Organization.ROOT_ORGANIZATION_ID, componentIdentifier, Collections.singleton(obligationName)))
        .usingRecursiveFieldByFieldElementComparator().containsExactly(
        componentObligationAttributionForRootOrganization);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
        organization.getId(), componentIdentifier, Collections.singleton(obligationName)))
        .usingRecursiveFieldByFieldElementComparator().containsExactly(
        componentObligationAttributionForOrganization);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(
        application.getId(), componentIdentifier, Collections.singleton(obligationName)))
        .usingRecursiveFieldByFieldElementComparator().containsExactly(
        componentObligationAttributionForApplication);
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
        componentIdentifier, obligationNames)).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(c1Root);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(organization.getId(),
        componentIdentifier, obligationNames)).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(c1Root, c2Org);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndObligationNamesWithHierarchy(application.getId(),
        componentIdentifier, obligationNames)).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(c1Root, c2Org, c3App);

    // Add c1 at org scope
    ComponentObligationAttribution c1Org = tempEntity.newComponentObligationAttribution(componentIdentifier,
        organization.getId(), obligationName1, "content4", "legalContentHash4");
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
    ComponentObligationAttribution c1App = tempEntity.newComponentObligationAttribution(componentIdentifier,
        application.getId(), obligationName1, "content5", "legalContentHash5");
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
