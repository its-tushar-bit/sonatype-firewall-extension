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
import com.sonatype.insight.brain.model.legal.ComponentLegalFile;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.LegalFileOverride;
import com.sonatype.insight.brain.model.legal.LegalFileType;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ComponentLegalFileDAOTest
    extends AbstractDbDAOTest
{
  private LegalFileOverrideDAO legalFileOverrideDAO;

  private ComponentLegalFileDAO dao;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    legalFileOverrideDAO = daoFactory.createLegalFileOverrideDAO();
    dao = daoFactory.createComponentLegalFileDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    Date now = new Date();
    ComponentLegalFile componentLegalFile =
        new ComponentLegalFile(ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(),
            LegalFileType.NOTICE,
            "legalContentHash", "username");
    componentLegalFile.setLastUpdatedAt(new Date(now.getTime() - 1));
    dao.insert(componentLegalFile);
    assertThat(componentLegalFile.getId()).isNotNull();

    // Read
    assertThat(dao.getById(componentLegalFile.getId())).usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
        .isEqualTo(componentLegalFile);

    // Update
    componentLegalFile.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    componentLegalFile.setOwnerId(application.getId());
    componentLegalFile.setLastUpdatedByUsername("other");
    componentLegalFile.setLastUpdatedAt(now);
    dao.update(componentLegalFile);
    assertThat(dao.getById(componentLegalFile.getId())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .usingOverriddenEquals()
        .isEqualTo(componentLegalFile);

    // Delete
    dao.delete(componentLegalFile);
    assertThat(dao.getById(componentLegalFile.getId())).isNull();
  }

  @Test
  public void testInsert_SetsDateIfNull() {
    ComponentLegalFile componentLegalFile =
        new ComponentLegalFile(ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(),
            LegalFileType.NOTICE,
            "legalContentHash", "username");
    componentLegalFile.setLastUpdatedAt(null);
    Date now = new Date();

    dao.insert(componentLegalFile);

    assertThat(dao.getById(componentLegalFile.getId()).getLastUpdatedAt()).isAfterOrEqualTo(now);
  }

  @Test
  public void testInsert_SameOwnerAndComponentAndType() {
    ComponentLegalFile componentLegalFile1 =
        new ComponentLegalFile(ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(),
            LegalFileType.NOTICE,
            "legalContentHash1", "username1");
    dao.insert(componentLegalFile1);
    ComponentLegalFile componentLegalFile2 =
        new ComponentLegalFile(ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(),
            LegalFileType.NOTICE,
            "legalContentHash2", "username2");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> dao.insert(componentLegalFile2))
        .withMessageContaining(
            "Component legal file already exists for owner with id " + componentLegalFile2.getOwnerId() +
                " and component " + componentLegalFile2.getComponentIdentifier() +
                " and type " + componentLegalFile2.getType().toString() + ".");
  }

  @Test
  public void testUpdate_SetsDate() {
    Date now = new Date();
    ComponentLegalFile componentLegalFile = new ComponentLegalFile(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(), LegalFileType.NOTICE,
        "legalContentHash", "username");
    componentLegalFile.setLastUpdatedAt(new Date(now.getTime() - 1));
    dao.insert(componentLegalFile);
    assertThat(dao.getById(componentLegalFile.getId()).getLastUpdatedAt()).isBefore(now);

    dao.update(componentLegalFile);

    assertThat(dao.getById(componentLegalFile.getId()).getLastUpdatedAt()).isAfterOrEqualTo(now);
  }

  @Test
  public void testUpdate_DoesNotExist() {
    ComponentLegalFile componentLegalFile = new ComponentLegalFile(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(), LegalFileType.NOTICE,
        "legalContentHash", "username");
    componentLegalFile.setId("doesNotExist");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> dao.update(componentLegalFile))
        .withMessageContaining("Cannot update component legal file with id " + componentLegalFile.getId() +
            " because it does not exist.");
  }

  @Test
  public void testGetByOwnerId() {
    ComponentLegalFile componentLegalFile1 = tempEntity.newComponentLegalFile(
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), organization.getId(), LegalFileType.NOTICE,
        "legalContentHash1");
    ComponentLegalFile componentLegalFile2 = tempEntity.newComponentLegalFile(
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"), organization.getId(), LegalFileType.NOTICE,
        "legalContentHash2");
    tempEntity.newComponentLegalFile(ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"), application.getId(),
        LegalFileType.NOTICE, "legalContentHash3");

    assertThat(dao.getByOwnerId(organization.getId()))
        .usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(componentLegalFile1, componentLegalFile2);
  }

  @Test
  public void testGetByOwnerIdAndComponentIdentifierAndType() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentLegalFile componentLegalFile =
        tempEntity.newComponentLegalFile(componentIdentifier, organization.getId(), LegalFileType.NOTICE,
            "legalContentHash1");
    tempEntity.newComponentLegalFile(componentIdentifier.createAlternativeVersion("v2"), organization.getId(),
        LegalFileType.NOTICE,
        "legalContentHash2");
    tempEntity
        .newComponentLegalFile(componentIdentifier, application.getId(), LegalFileType.NOTICE, "legalContentHash3");
    tempEntity.newComponentLegalFile(componentIdentifier, organization.getId(), LegalFileType.LICENSE,
        "legalContentHash4");

    assertThat(
        dao.getByOwnerIdAndComponentIdentifierAndType(organization.getId(), componentIdentifier, LegalFileType.NOTICE))
            .usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
            .isEqualTo(componentLegalFile);
  }

  @Test
  public void testGetAll() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentLegalFile componentLegalFile1 =
        tempEntity.newComponentLegalFile(componentIdentifier, organization.getId(), LegalFileType.NOTICE,
            "legalContentHash1");
    ComponentLegalFile componentLegalFile2 =
        tempEntity.newComponentLegalFile(componentIdentifier, application.getId(), LegalFileType.NOTICE,
            "legalContentHash2");

    assertThat(dao.getAll()).usingRecursiveFieldByFieldElementComparator(JPA.RECURSIVE_COMPARISON_CONFIG)
        .containsExactlyInAnyOrder(componentLegalFile1, componentLegalFile2);
  }

  @Test
  public void testGetByOwnerIdAndComponentIdentifierAndTypeWithHierarchy() {
    ComponentIdentifier compIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentLegalFile rootOrgComponentLegalFile = tempEntity.newComponentLegalFile(compIdentifier,
        Organization.ROOT_ORGANIZATION_ID, LegalFileType.NOTICE, "legalContentHash1");
    ComponentLegalFile orgComponentLegalFile = tempEntity.newComponentLegalFile(compIdentifier,
        organization.getId(), LegalFileType.NOTICE, "legalContentHash2");
    ComponentLegalFile appComponentLegalFile = tempEntity.newComponentLegalFile(compIdentifier,
        application.getId(), LegalFileType.NOTICE, "legalContentHash3");

    assertThat(
        dao.getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(Organization.ROOT_ORGANIZATION_ID, compIdentifier,
            LegalFileType.NOTICE))
                .usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
                .isEqualTo(rootOrgComponentLegalFile);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(organization.getId(), compIdentifier,
        LegalFileType.NOTICE))
            .usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
            .isEqualTo(orgComponentLegalFile);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(application.getId(), compIdentifier,
        LegalFileType.NOTICE))
            .usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
            .isEqualTo(appComponentLegalFile);

    dao.delete(appComponentLegalFile);

    assertThat(
        dao.getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(Organization.ROOT_ORGANIZATION_ID, compIdentifier,
            LegalFileType.NOTICE))
                .usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
                .isEqualTo(rootOrgComponentLegalFile);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(organization.getId(), compIdentifier,
        LegalFileType.NOTICE))
            .usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
            .isEqualTo(orgComponentLegalFile);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(application.getId(), compIdentifier,
        LegalFileType.NOTICE))
            .usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
            .isEqualTo(orgComponentLegalFile);

    dao.delete(orgComponentLegalFile);

    assertThat(
        dao.getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(Organization.ROOT_ORGANIZATION_ID, compIdentifier,
            LegalFileType.NOTICE))
                .usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
                .isEqualTo(rootOrgComponentLegalFile);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(organization.getId(), compIdentifier,
        LegalFileType.NOTICE))
            .usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
            .isEqualTo(rootOrgComponentLegalFile);
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(application.getId(), compIdentifier,
        LegalFileType.NOTICE))
            .usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG)
            .isEqualTo(rootOrgComponentLegalFile);

    dao.delete(rootOrgComponentLegalFile);

    assertThat(
        dao.getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(Organization.ROOT_ORGANIZATION_ID, compIdentifier,
            LegalFileType.NOTICE))
                .isNull();
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(organization.getId(), compIdentifier,
        LegalFileType.NOTICE)).isNull();
    assertThat(dao.getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(application.getId(), compIdentifier,
        LegalFileType.NOTICE)).isNull();
  }

  @Test
  public void testDelete_CascadesToLegalFileOverrides() {
    ComponentLegalFile componentLegalFile = tempEntity.newComponentLegalFile(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(), LegalFileType.NOTICE,
        "legalContentHash");
    ComponentLegalFile otherComponentLegalFile = tempEntity.newComponentLegalFile(
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"), organization.getId(), LegalFileType.NOTICE,
        "legalContentHash2");
    LegalFileOverride legalFileOverride1 = tempEntity.newLegalFileOverride("originalHash1",
        "hash1", "content1", ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());
    LegalFileOverride legalFileOverride2 = tempEntity.newLegalFileOverride("originalHash2",
        "hash2", "content2", ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());
    LegalFileOverride otherLegalFileOverride = tempEntity.newLegalFileOverride("originalHash3",
        "hash3", "content3", ComponentLegalPartStatus.ENABLED, otherComponentLegalFile.getId());

    dao.delete(componentLegalFile);

    assertThat(dao.getById(componentLegalFile.getId())).isNull();
    assertThat(legalFileOverrideDAO.getById(legalFileOverride1.getId())).isNull();
    assertThat(legalFileOverrideDAO.getById(legalFileOverride2.getId())).isNull();
    assertThat(dao.getById(otherComponentLegalFile.getId())).isNotNull();
    assertThat(legalFileOverrideDAO.getById(otherLegalFileOverride.getId())).isNotNull();
  }
}
