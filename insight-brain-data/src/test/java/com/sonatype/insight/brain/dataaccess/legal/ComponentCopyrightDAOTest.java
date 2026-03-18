/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.legal;

import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.CopyrightOverride;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ComponentCopyrightDAOTest
    extends AbstractDbDAOTest
{
  private CopyrightOverrideDAO copyrightOverrideDAO;

  private ComponentCopyrightDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createComponentCopyrightDAO();
    copyrightOverrideDAO = daoFactory.createCopyrightOverrideDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    Date now = new Date();
    ComponentCopyright componentCopyright =
        new ComponentCopyright(ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(),
            "legalContentHash", "username");
    componentCopyright.setLastUpdatedAt(new Date(now.getTime() - 1));
    dao.insert(componentCopyright);
    assertThat(componentCopyright.getId()).isNotNull();

    // Read
    assertThat(dao.getById(componentCopyright.getId())).usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .isEqualTo(componentCopyright);

    // Update
    componentCopyright.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    componentCopyright.setOwnerId(application.getId());
    componentCopyright.setLastUpdatedByUsername("other");
    componentCopyright.setLastUpdatedAt(now);
    dao.update(componentCopyright);
    assertThat(dao.getById(componentCopyright.getId())).usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .ignoringFields(JPA.IGNORE_FIELDS)
        .usingOverriddenEquals()
        .isEqualTo(componentCopyright);

    // Delete
    dao.delete(componentCopyright);
    assertThat(dao.getById(componentCopyright.getId())).isNull();
  }

  @Test
  public void testInsert_SetsDateIfNull() {
    ComponentCopyright componentCopyright =
        new ComponentCopyright(ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(),
            "legalContentHash", "username");
    componentCopyright.setLastUpdatedAt(null);
    Date now = new Date();

    dao.insert(componentCopyright);

    assertThat(dao.getById(componentCopyright.getId()).getLastUpdatedAt()).isAfterOrEqualTo(now);
  }

  @Test
  public void testInsert_SameOwnerAndComponent() {
    ComponentCopyright componentCopyright1 =
        new ComponentCopyright(ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(),
            "legalContentHash1", "username1");
    dao.insert(componentCopyright1);
    ComponentCopyright componentCopyright2 =
        new ComponentCopyright(ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(),
            "legalContentHash2", "username2");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> dao.insert(componentCopyright2))
        .withMessageContaining(
            "Component copyright already exists for owner with id " + componentCopyright2.getOwnerId() +
                " and component " + componentCopyright2.getComponentIdentifier() + ".");
  }

  @Test
  public void testUpdate_SetsDate() {
    Date now = new Date();
    ComponentCopyright componentCopyright =
        new ComponentCopyright(ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(),
            "legalContentHash", "username");
    componentCopyright.setLastUpdatedAt(new Date(now.getTime() - 1));
    dao.insert(componentCopyright);
    assertThat(dao.getById(componentCopyright.getId()).getLastUpdatedAt()).isBefore(now);

    dao.update(componentCopyright);

    assertThat(dao.getById(componentCopyright.getId()).getLastUpdatedAt()).isAfterOrEqualTo(now);
  }

  @Test
  public void testUpdate_DoesNotExist() {
    ComponentCopyright componentCopyright =
        new ComponentCopyright(ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(),
            "legalContentHash", "username");
    componentCopyright.setId("doesNotExist");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> dao.update(componentCopyright))
        .withMessageContaining(
            "Cannot update component copyright with id " + componentCopyright.getId() + " because it does not exist.");
  }

  @Test
  public void testGetByOwnerId() {
    ComponentCopyright componentCopyright1 = tempEntity.newComponentCopyright(
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), organization.getId(), "legalContentHash1");
    ComponentCopyright componentCopyright2 = tempEntity.newComponentCopyright(
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"), organization.getId(), "legalContentHash2");
    tempEntity.newComponentCopyright(ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"), application.getId(),
        "legalContentHash3");

    assertThat(dao.getByOwnerId(organization.getId()))
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(componentCopyright1, componentCopyright2);
  }

  @Test
  public void testGetByOwnerIdAndComponentIdentifier() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentCopyright componentCopyright = tempEntity.newComponentCopyright(componentIdentifier, organization.getId(),
        "legalContentHash1");
    tempEntity.newComponentCopyright(componentIdentifier.createAlternativeVersion("v2"), organization.getId(),
        "legalContentHash2");
    tempEntity.newComponentCopyright(componentIdentifier, application.getId(), "legalContentHash3");

    assertThat(dao.getByOwnerIdAndComponentIdentifier(organization.getId(), componentIdentifier))
        .usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .isEqualTo(componentCopyright);
  }

  @Test
  public void testGetByOwnerIdAndComponentIdentifierWithHierarchy() {
    ComponentIdentifier compIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentCopyright rootOrgComponentCopyright = tempEntity.newComponentCopyright(compIdentifier,
        Organization.ROOT_ORGANIZATION_ID, "legalContentHash1");
    ComponentCopyright orgComponentCopyright = tempEntity.newComponentCopyright(compIdentifier,
        organization.getId(), "legalContentHash2");
    ComponentCopyright appComponentCopyright = tempEntity.newComponentCopyright(compIdentifier,
        application.getId(), "legalContentHash3");

    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(Organization.ROOT_ORGANIZATION_ID, compIdentifier))
        .usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .isEqualTo(rootOrgComponentCopyright);
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(organization.getId(), compIdentifier))
        .usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .isEqualTo(orgComponentCopyright);
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(application.getId(), compIdentifier))
        .usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .isEqualTo(appComponentCopyright);

    dao.delete(appComponentCopyright);

    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(Organization.ROOT_ORGANIZATION_ID, compIdentifier))
        .usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .isEqualTo(rootOrgComponentCopyright);
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(organization.getId(), compIdentifier))
        .usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .isEqualTo(orgComponentCopyright);
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(application.getId(), compIdentifier))
        .usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .isEqualTo(orgComponentCopyright);

    dao.delete(orgComponentCopyright);

    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(Organization.ROOT_ORGANIZATION_ID, compIdentifier))
        .usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .isEqualTo(rootOrgComponentCopyright);
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(organization.getId(), compIdentifier))
        .usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .isEqualTo(rootOrgComponentCopyright);
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(application.getId(), compIdentifier))
        .usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .isEqualTo(rootOrgComponentCopyright);

    dao.delete(rootOrgComponentCopyright);

    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(Organization.ROOT_ORGANIZATION_ID, compIdentifier))
        .isNull();
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(organization.getId(), compIdentifier)).isNull();
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(application.getId(), compIdentifier)).isNull();
  }

  @Test
  public void testDelete_CascadesToCopyrightOverrides() {
    ComponentCopyright componentCopyright = tempEntity.newComponentCopyright(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(), "legalContentHash");
    ComponentCopyright otherComponentCopyright = tempEntity.newComponentCopyright(
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"), organization.getId(), "legalContentHash2");
    CopyrightOverride copyrightOverride1 = tempEntity.newCopyrightOverride("originalHash1", "hash1", "content1",
        ComponentLegalPartStatus.ENABLED, componentCopyright.getId());
    CopyrightOverride copyrightOverride2 = tempEntity.newCopyrightOverride("originalHash2", "hash2", "content2",
        ComponentLegalPartStatus.ENABLED, componentCopyright.getId());
    CopyrightOverride otherCopyrightOverride = tempEntity.newCopyrightOverride("originalHash3", "hash3", "content3",
        ComponentLegalPartStatus.ENABLED, otherComponentCopyright.getId());

    dao.delete(componentCopyright);

    assertThat(dao.getById(componentCopyright.getId())).isNull();
    assertThat(copyrightOverrideDAO.getById(copyrightOverride1.getId())).isNull();
    assertThat(copyrightOverrideDAO.getById(copyrightOverride2.getId())).isNull();
    assertThat(dao.getById(otherComponentCopyright.getId())).isNotNull();
    assertThat(copyrightOverrideDAO.getById(otherCopyrightOverride.getId())).isNotNull();
  }
}
