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

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ComponentLegalFileDAOTest
    extends AbstractDbDAOTest
{
  private ComponentLegalFileDAO dao;

  @Before
  public void before() {
    dao = new ComponentLegalFileDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    Date now = new Date();
    ComponentLegalFile componentLegalFile =
        new ComponentLegalFile(ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(),
            "legalContentHash", "username");
    componentLegalFile.setLastUpdatedAt(new Date(now.getTime() - 1));
    dao.insert(componentLegalFile);
    assertThat(componentLegalFile.getId()).isNotNull();

    // Read
    assertThat(dao.getById(componentLegalFile.getId())).usingRecursiveComparison().isEqualTo(componentLegalFile);

    // Update
    componentLegalFile.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    componentLegalFile.setOwnerId(application.getId());
    componentLegalFile.setLastUpdatedByUsername("other");
    componentLegalFile.setLastUpdatedAt(now);
    dao.update(componentLegalFile);
    assertThat(dao.getById(componentLegalFile.getId())).usingRecursiveComparison(
        RecursiveComparisonConfiguration.builder().withIgnoredFields(JPA.IGNORE_FIELDS).build())
        .isEqualTo(componentLegalFile);

    // Delete
    dao.delete(componentLegalFile);
    assertThat(dao.getById(componentLegalFile.getId())).isNull();
  }

  @Test
  public void testInsert_SetsDateIfNull() {
    ComponentLegalFile componentLegalFile =
        new ComponentLegalFile(ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(),
            "legalContentHash", "username");
    componentLegalFile.setLastUpdatedAt(null);
    Date now = new Date();

    dao.insert(componentLegalFile);

    assertThat(dao.getById(componentLegalFile.getId()).getLastUpdatedAt()).isAfterOrEqualTo(now);
  }

  @Test
  public void testInsert_SameOwnerAndComponent() {
    ComponentLegalFile componentLegalFile1 =
        new ComponentLegalFile(ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(),
            "legalContentHash1", "username1");
    dao.insert(componentLegalFile1);
    ComponentLegalFile componentLegalFile2 =
        new ComponentLegalFile(ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(),
            "legalContentHash2", "username2");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> dao.insert(componentLegalFile2))
        .withMessageContaining(
            "Component legal file already exists for owner with id " + componentLegalFile2.getOwnerId() +
                " and component " + componentLegalFile2.getComponentIdentifier() + ".");
  }

  @Test
  public void testUpdate_SetsDate() {
    Date now = new Date();
    ComponentLegalFile componentLegalFile =
        new ComponentLegalFile(ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(),
            "legalContentHash", "username");
    componentLegalFile.setLastUpdatedAt(new Date(now.getTime() - 1));
    dao.insert(componentLegalFile);
    assertThat(dao.getById(componentLegalFile.getId()).getLastUpdatedAt()).isBefore(now);

    dao.update(componentLegalFile);

    assertThat(dao.getById(componentLegalFile.getId()).getLastUpdatedAt()).isAfterOrEqualTo(now);
  }

  @Test
  public void testUpdate_DoesNotExist() {
    ComponentLegalFile componentLegalFile =
        new ComponentLegalFile(ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(),
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
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"), organization.getId(), "legalContentHash1");
    ComponentLegalFile componentLegalFile2 = tempEntity.newComponentLegalFile(
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"), organization.getId(), "legalContentHash2");
    tempEntity.newComponentLegalFile(ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"), application.getId(),
        "legalContentHash3");

    assertThat(dao.getByOwnerId(organization.getId())).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(componentLegalFile1, componentLegalFile2);
  }

  @Test
  public void testGetByOwnerIdAndComponentIdentifier() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentLegalFile componentLegalFile = tempEntity.newComponentLegalFile(componentIdentifier, organization.getId(),
        "legalContentHash1");
    tempEntity.newComponentLegalFile(componentIdentifier.createAlternativeVersion("v2"), organization.getId(),
        "legalContentHash2");
    tempEntity.newComponentLegalFile(componentIdentifier, application.getId(), "legalContentHash3");

    assertThat(dao.getByOwnerIdAndComponentIdentifier(organization.getId(), componentIdentifier))
        .usingRecursiveComparison().isEqualTo(componentLegalFile);
  }

  @Test
  public void testGetByOwnerIdAndComponentIdentifierWithHierarchy() {
    ComponentIdentifier compIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    ComponentLegalFile rootOrgComponentLegalFile = tempEntity.newComponentLegalFile(compIdentifier,
        Organization.ROOT_ORGANIZATION_ID, "legalContentHash1");
    ComponentLegalFile orgComponentLegalFile = tempEntity.newComponentLegalFile(compIdentifier,
        organization.getId(), "legalContentHash2");
    ComponentLegalFile appComponentLegalFile = tempEntity.newComponentLegalFile(compIdentifier,
        application.getId(), "legalContentHash3");

    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(Organization.ROOT_ORGANIZATION_ID, compIdentifier))
        .usingRecursiveComparison().isEqualTo(rootOrgComponentLegalFile);
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(organization.getId(), compIdentifier))
        .usingRecursiveComparison().isEqualTo(orgComponentLegalFile);
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(application.getId(), compIdentifier))
        .usingRecursiveComparison().isEqualTo(appComponentLegalFile);

    dao.delete(appComponentLegalFile);

    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(Organization.ROOT_ORGANIZATION_ID, compIdentifier))
        .usingRecursiveComparison().isEqualTo(rootOrgComponentLegalFile);
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(organization.getId(), compIdentifier))
        .usingRecursiveComparison().isEqualTo(orgComponentLegalFile);
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(application.getId(), compIdentifier))
        .usingRecursiveComparison().isEqualTo(orgComponentLegalFile);

    dao.delete(orgComponentLegalFile);

    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(Organization.ROOT_ORGANIZATION_ID, compIdentifier))
        .usingRecursiveComparison().isEqualTo(rootOrgComponentLegalFile);
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(organization.getId(), compIdentifier))
        .usingRecursiveComparison().isEqualTo(rootOrgComponentLegalFile);
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(application.getId(), compIdentifier))
        .usingRecursiveComparison().isEqualTo(rootOrgComponentLegalFile);

    dao.delete(rootOrgComponentLegalFile);

    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(Organization.ROOT_ORGANIZATION_ID, compIdentifier))
        .isNull();
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(organization.getId(), compIdentifier)).isNull();
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(application.getId(), compIdentifier)).isNull();
  }

  @Test
  public void testDelete_CascadesToLegalFileOverrides() {
    ComponentLegalFile componentLegalFile = tempEntity.newComponentLegalFile(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(), "legalContentHash");
    ComponentLegalFile otherComponentLegalFile = tempEntity.newComponentLegalFile(
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"), organization.getId(), "legalContentHash2");
    LegalFileOverride legalFileOverride1 = tempEntity.newLegalFileOverride(LegalFileType.NOTICE, "originalHash1",
        "hash1", "content1", ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());
    LegalFileOverride legalFileOverride2 = tempEntity.newLegalFileOverride(LegalFileType.NOTICE, "originalHash2",
        "hash2", "content2", ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());
    LegalFileOverride otherLegalFileOverride = tempEntity.newLegalFileOverride(LegalFileType.NOTICE, "originalHash3",
        "hash3", "content3", ComponentLegalPartStatus.ENABLED, otherComponentLegalFile.getId());

    dao.delete(componentLegalFile);

    LegalFileOverrideDAO legalFileOverrideDAO = new LegalFileOverrideDAO();
    assertThat(dao.getById(componentLegalFile.getId())).isNull();
    assertThat(legalFileOverrideDAO.getById(legalFileOverride1.getId())).isNull();
    assertThat(legalFileOverrideDAO.getById(legalFileOverride2.getId())).isNull();
    assertThat(dao.getById(otherComponentLegalFile.getId())).isNotNull();
    assertThat(legalFileOverrideDAO.getById(otherLegalFileOverride.getId())).isNotNull();
  }
}
