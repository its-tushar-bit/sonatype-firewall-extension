/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.legal;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.legal.ComponentLegalFile;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.LegalFileOverride;
import com.sonatype.insight.brain.model.legal.LegalFileType;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LegalFileOverrideDAOTest
    extends AbstractDbDAOTest
{
  private LegalFileOverrideDAO dao;

  @Before
  public void before() {
    dao = new LegalFileOverrideDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    ComponentLegalFile componentLegalFile = tempEntity.newComponentLegalFile(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(), "legalContentHash");
    LegalFileOverride legalFileOverride = new LegalFileOverride(LegalFileType.NOTICE, "originalHash", "hash", "content",
        ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());
    dao.insert(legalFileOverride);
    assertThat(legalFileOverride.getId()).isNotNull();

    // Read
    assertThat(dao.getById(legalFileOverride.getId())).usingRecursiveComparison().isEqualTo(legalFileOverride);

    // Update
    legalFileOverride.setType(LegalFileType.LICENSE);
    legalFileOverride.setContentHash(legalFileOverride.getContentHash() + "2");
    legalFileOverride.setContent(legalFileOverride.getContent() + "2");
    legalFileOverride.setStatus(ComponentLegalPartStatus.DISABLED);
    ComponentLegalFile componentLegalFile2 = tempEntity.newComponentLegalFile(
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"), application.getId(), "legalContentHash2");
    legalFileOverride.setComponentLegalFileId(componentLegalFile2.getId());
    dao.update(legalFileOverride);
    assertThat(dao.getById(legalFileOverride.getId())).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(legalFileOverride);

    // Delete
    dao.delete(legalFileOverride);
    assertThat(dao.getById(legalFileOverride.getId())).isNull();
  }

  @Test
  public void testGetByComponentLegalFileId() {
    ComponentLegalFile componentLegalFile = tempEntity.newComponentLegalFile(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(), "legalContentHash");
    ComponentLegalFile otherComponentLegalFile = tempEntity.newComponentLegalFile(
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"), organization.getId(), "legalContentHash2");
    LegalFileOverride legalFileOverride1 = tempEntity.newLegalFileOverride(LegalFileType.NOTICE, "originalHash1",
        "hash1", "content1", ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());
    LegalFileOverride legalFileOverride2 = tempEntity.newLegalFileOverride(LegalFileType.NOTICE, "originalHash2",
        "hash2", "content2", ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());
    tempEntity.newLegalFileOverride(LegalFileType.NOTICE, "originalHash3", "hash3", "content3",
        ComponentLegalPartStatus.ENABLED, otherComponentLegalFile.getId());

    assertThat(dao.getByComponentLegalFileId(componentLegalFile.getId())).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(legalFileOverride1, legalFileOverride2);
  }

  @Test
  public void testGetByOwnerIdAndComponentIdentifierAndLegalFileType() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentLegalFile componentLegalFile1 = tempEntity.newComponentLegalFile(componentIdentifier, organization.getId(),
        "legalContentHash");
    ComponentLegalFile componentLegalFile2 = tempEntity.newComponentLegalFile(componentIdentifier, application.getId(),
        "legalContentHash");
    ComponentLegalFile componentLegalFile3 = tempEntity.newComponentLegalFile(
        componentIdentifier.createAlternativeVersion("v2"), organization.getId(), "legalContentHash");

    LegalFileOverride legalFileOverride1 =
        tempEntity.newLegalFileOverride(LegalFileType.NOTICE, "originalHash1", "hash1", "content1",
            ComponentLegalPartStatus.ENABLED, componentLegalFile1.getId());
    LegalFileOverride legalFileOverride2 =
        tempEntity.newLegalFileOverride(LegalFileType.NOTICE, "originalHash2", "hash2", "content2",
            ComponentLegalPartStatus.ENABLED, componentLegalFile1.getId());
    tempEntity.newLegalFileOverride(LegalFileType.LICENSE, "originalHash3", "hash3", "content3",
        ComponentLegalPartStatus.ENABLED, componentLegalFile1.getId());
    tempEntity.newLegalFileOverride(LegalFileType.NOTICE, "originalHash4", "hash4", "content4",
        ComponentLegalPartStatus.ENABLED, componentLegalFile2.getId());
    tempEntity.newLegalFileOverride(LegalFileType.NOTICE, "originalHash5", "hash5", "content5",
        ComponentLegalPartStatus.ENABLED, componentLegalFile3.getId());

    assertThat(dao.getByOwnerIdAndComponentIdentifierAndType(organization.getId(), componentIdentifier,
        LegalFileType.NOTICE)).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(legalFileOverride1, legalFileOverride2);
  }

  @Test
  public void testGetByOwnerIdAndComponentIdentifierAndTypeWithHierarchy() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");

    // Start with a legal file override at just the root org level
    ComponentLegalFile componentLegalFileForRootOrganization =
        tempEntity.newComponentLegalFile(componentIdentifier, Organization.ROOT_ORGANIZATION_ID, "legalContentHash1");
    LegalFileOverride legalFileOverrideForRootOrganization =
        tempEntity.newLegalFileOverride(LegalFileType.NOTICE, "originalHash1", "hash1", "content1",
            ComponentLegalPartStatus.ENABLED, componentLegalFileForRootOrganization.getId());

    assertThat(dao.getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(Organization.ROOT_ORGANIZATION_ID,
        componentIdentifier, LegalFileType.NOTICE)).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(legalFileOverrideForRootOrganization);
    assertThat(
        dao.getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(organization.getId(), componentIdentifier,
            LegalFileType.NOTICE)).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(legalFileOverrideForRootOrganization);
    assertThat(
        dao.getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(application.getId(), componentIdentifier,
            LegalFileType.NOTICE)).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(legalFileOverrideForRootOrganization);

    // Add another legal file override at the org level
    ComponentLegalFile componentLegalFileForOrganization =
        tempEntity.newComponentLegalFile(componentIdentifier, organization.getId(), "legalContentHash2");
    LegalFileOverride legalFileOverrideForOrganization =
        tempEntity.newLegalFileOverride(LegalFileType.NOTICE, "originalHash2", "hash2", "content2",
            ComponentLegalPartStatus.ENABLED, componentLegalFileForOrganization.getId());

    assertThat(dao.getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(Organization.ROOT_ORGANIZATION_ID,
        componentIdentifier, LegalFileType.NOTICE)).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(legalFileOverrideForRootOrganization);
    assertThat(
        dao.getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(organization.getId(), componentIdentifier,
            LegalFileType.NOTICE)).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(legalFileOverrideForOrganization);
    assertThat(
        dao.getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(application.getId(), componentIdentifier,
            LegalFileType.NOTICE)).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(legalFileOverrideForOrganization);

    // Add another legal file override at the app level
    ComponentLegalFile componentLegalFileForApplication =
        tempEntity.newComponentLegalFile(componentIdentifier, application.getId(), "legalContentHash3");
    LegalFileOverride legalFileOverrideForApplication =
        tempEntity.newLegalFileOverride(LegalFileType.NOTICE, "originalHash3", "hash3", "content3",
            ComponentLegalPartStatus.ENABLED, componentLegalFileForApplication.getId());

    assertThat(dao.getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(Organization.ROOT_ORGANIZATION_ID,
        componentIdentifier, LegalFileType.NOTICE)).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(legalFileOverrideForRootOrganization);
    assertThat(
        dao.getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(organization.getId(), componentIdentifier,
            LegalFileType.NOTICE)).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(legalFileOverrideForOrganization);
    assertThat(
        dao.getByOwnerIdAndComponentIdentifierAndTypeWithHierarchy(application.getId(), componentIdentifier,
            LegalFileType.NOTICE)).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(legalFileOverrideForApplication);
  }
}
