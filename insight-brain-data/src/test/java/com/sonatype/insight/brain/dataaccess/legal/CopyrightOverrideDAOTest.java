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
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.CopyrightOverride;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class CopyrightOverrideDAOTest
    extends AbstractDbDAOTest
{
  private CopyrightOverrideDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createCopyrightOverrideDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    ComponentCopyright componentCopyright = tempEntity.newComponentCopyright(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(), "legalContentHash");
    CopyrightOverride copyrightOverride = new CopyrightOverride("originalHash", "hash", "content",
        ComponentLegalPartStatus.ENABLED, componentCopyright.getId());
    dao.insert(copyrightOverride);
    assertThat(copyrightOverride.getId()).isNotNull();

    // Read
    assertThat(dao.getById(copyrightOverride.getId())).usingRecursiveComparison().isEqualTo(copyrightOverride);

    // Update
    copyrightOverride.setContentHash(copyrightOverride.getContentHash() + "2");
    copyrightOverride.setContent(copyrightOverride.getContent() + "2");
    copyrightOverride.setStatus(ComponentLegalPartStatus.DISABLED);
    ComponentCopyright componentCopyright2 = tempEntity.newComponentCopyright(
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"), application.getId(), "legalContentHash2");
    copyrightOverride.setComponentCopyrightId(componentCopyright2.getId());
    dao.update(copyrightOverride);
    assertThat(dao.getById(copyrightOverride.getId())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(copyrightOverride);

    // Delete
    dao.delete(copyrightOverride);
    assertThat(dao.getById(copyrightOverride.getId())).isNull();
  }

  @Test
  public void testGetByComponentCopyrightId() {
    ComponentCopyright componentCopyright = tempEntity.newComponentCopyright(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(), "legalContentHash");
    ComponentCopyright otherComponentCopyright = tempEntity.newComponentCopyright(
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"), organization.getId(), "legalContentHash2");
    CopyrightOverride copyrightOverride1 = tempEntity.newCopyrightOverride("originalHash1", "hash1", "content1",
        ComponentLegalPartStatus.ENABLED, componentCopyright.getId());
    CopyrightOverride copyrightOverride2 = tempEntity.newCopyrightOverride("originalHash2", "hash2", "content2",
        ComponentLegalPartStatus.ENABLED, componentCopyright.getId());
    tempEntity.newCopyrightOverride("originalHash3", "hash3", "content3", ComponentLegalPartStatus.ENABLED,
        otherComponentCopyright.getId());

    assertThat(dao.getByComponentCopyrightId(componentCopyright.getId())).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(copyrightOverride1, copyrightOverride2);
  }

  @Test
  public void testGetByOwnerIdAndComponentIdentifier() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentCopyright componentCopyright1 = tempEntity.newComponentCopyright(componentIdentifier, organization.getId(),
        "legalContentHash");
    ComponentCopyright componentCopyright2 = tempEntity.newComponentCopyright(componentIdentifier, application.getId(),
        "legalContentHash");
    ComponentCopyright componentCopyright3 = tempEntity.newComponentCopyright(
        componentIdentifier.createAlternativeVersion("v2"), organization.getId(), "legalContentHash");

    CopyrightOverride copyrightOverride1 = tempEntity.newCopyrightOverride("originalHash1", "hash1", "content1",
        ComponentLegalPartStatus.ENABLED, componentCopyright1.getId());
    CopyrightOverride copyrightOverride2 = tempEntity.newCopyrightOverride("originalHash2", "hash2", "content2",
        ComponentLegalPartStatus.ENABLED, componentCopyright1.getId());
    tempEntity.newCopyrightOverride("originalHash3", "hash3", "content3", ComponentLegalPartStatus.ENABLED,
        componentCopyright2.getId());
    tempEntity.newCopyrightOverride("originalHash4", "hash4", "content4", ComponentLegalPartStatus.ENABLED,
        componentCopyright3.getId());

    assertThat(dao.getByOwnerIdAndComponentIdentifier(organization.getId(), componentIdentifier))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(copyrightOverride1, copyrightOverride2);
  }

  @Test
  public void testGetByOwnerIdAndComponentIdentifierWithHierarchy() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");

    // Start with a copyright override at just the root org level
    ComponentCopyright componentCopyrightForRootOrganization =
        tempEntity.newComponentCopyright(componentIdentifier, Organization.ROOT_ORGANIZATION_ID, "legalContentHash1");
    CopyrightOverride copyrightOverrideForRootOrganization =
        tempEntity.newCopyrightOverride("originalHash1", "hash1", "content1",
            ComponentLegalPartStatus.ENABLED, componentCopyrightForRootOrganization.getId());

    assertThat(
        dao.getByOwnerIdAndComponentIdentifierWithHierarchy(Organization.ROOT_ORGANIZATION_ID, componentIdentifier))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(copyrightOverrideForRootOrganization);
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(organization.getId(), componentIdentifier))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(copyrightOverrideForRootOrganization);
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(application.getId(), componentIdentifier))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(copyrightOverrideForRootOrganization);

    // Add another copyright override at the org level
    ComponentCopyright componentCopyrightForOrganization =
        tempEntity.newComponentCopyright(componentIdentifier, organization.getId(), "legalContentHash2");
    CopyrightOverride copyrightOverrideForOrganization =
        tempEntity.newCopyrightOverride("originalHash2", "hash2", "content2",
            ComponentLegalPartStatus.ENABLED, componentCopyrightForOrganization.getId());

    assertThat(
        dao.getByOwnerIdAndComponentIdentifierWithHierarchy(Organization.ROOT_ORGANIZATION_ID, componentIdentifier))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(copyrightOverrideForRootOrganization);
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(organization.getId(), componentIdentifier))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(copyrightOverrideForOrganization);
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(application.getId(), componentIdentifier))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(copyrightOverrideForOrganization);

    // Add another copyright override at the app level
    ComponentCopyright componentCopyrightForApplication =
        tempEntity.newComponentCopyright(componentIdentifier, application.getId(), "legalContentHash3");
    CopyrightOverride copyrightOverrideForApplication =
        tempEntity.newCopyrightOverride("originalHash3", "hash3", "content3",
            ComponentLegalPartStatus.ENABLED, componentCopyrightForApplication.getId());

    assertThat(
        dao.getByOwnerIdAndComponentIdentifierWithHierarchy(Organization.ROOT_ORGANIZATION_ID, componentIdentifier))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(copyrightOverrideForRootOrganization);
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(organization.getId(), componentIdentifier))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(copyrightOverrideForOrganization);
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(application.getId(), componentIdentifier))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(copyrightOverrideForApplication);
  }

  @Test
  public void testUpdate_DoesNotExist() {
    ComponentCopyright componentCopyright = tempEntity.newComponentCopyright(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(), "legalContentHash");
    CopyrightOverride copyrightOverride = new CopyrightOverride("originalHash", "hash", "content",
        ComponentLegalPartStatus.ENABLED, componentCopyright.getId());
    copyrightOverride.setId("doesNotExist");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> dao.update(copyrightOverride))
        .withMessageContaining(
            "Cannot update copyright override with id " + copyrightOverride.getId() + " because it does not exist.");
  }
}
