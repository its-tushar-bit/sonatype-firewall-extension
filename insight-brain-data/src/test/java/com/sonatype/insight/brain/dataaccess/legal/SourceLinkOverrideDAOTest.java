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
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.ComponentSourceLink;
import com.sonatype.insight.brain.model.legal.SourceLinkOverride;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class SourceLinkOverrideDAOTest
    extends AbstractDbDAOTest
{
  private SourceLinkOverrideDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createSourceLinkOverrideDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    ComponentSourceLink componentSourceLink = tempEntity.newComponentSourceLink(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId());
    SourceLinkOverride sourceLinkOverride = new SourceLinkOverride("content", "originalContent",
        ComponentLegalPartStatus.ENABLED, componentSourceLink.getId());
    dao.insert(sourceLinkOverride);
    assertThat(sourceLinkOverride.getId()).isNotNull();

    // Read
    assertThat(dao.getById(sourceLinkOverride.getId())).usingRecursiveComparison().isEqualTo(sourceLinkOverride);

    // Update
    sourceLinkOverride.setContent(sourceLinkOverride.getContent() + "2");
    sourceLinkOverride.setStatus(ComponentLegalPartStatus.DISABLED);
    ComponentSourceLink componentSourceLink2 = tempEntity.newComponentSourceLink(
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"), application.getId());
    sourceLinkOverride.setComponentSourceLinkId(componentSourceLink2.getId());
    dao.update(sourceLinkOverride);
    assertThat(dao.getById(sourceLinkOverride.getId())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(sourceLinkOverride);

    // Delete
    dao.delete(sourceLinkOverride);
    assertThat(dao.getById(sourceLinkOverride.getId())).isNull();
  }

  @Test
  public void testGetByOwnerIdAndComponentIdentifierWithHierarchy() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");

    // Start with a sourceLink override at just the root org level
    ComponentSourceLink componentSourceLinkForRootOrganization =
        tempEntity.newComponentSourceLink(componentIdentifier, Organization.ROOT_ORGANIZATION_ID);
    SourceLinkOverride sourceLinkOverrideForRootOrganization =
        tempEntity.newSourceLinkOverride("content1", "originalContent1",
            ComponentLegalPartStatus.ENABLED, componentSourceLinkForRootOrganization.getId());

    assertThat(
        dao.getByOwnerIdAndComponentIdentifierWithHierarchy(Organization.ROOT_ORGANIZATION_ID, componentIdentifier))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(sourceLinkOverrideForRootOrganization);
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(organization.getId(), componentIdentifier))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(sourceLinkOverrideForRootOrganization);
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(application.getId(), componentIdentifier))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(sourceLinkOverrideForRootOrganization);

    // Add another sourceLink override at the org level
    ComponentSourceLink componentSourceLinkForOrganization =
        tempEntity.newComponentSourceLink(componentIdentifier, organization.getId());
    SourceLinkOverride sourceLinkOverrideForOrganization =
        tempEntity.newSourceLinkOverride("content2", "originalContent2",
            ComponentLegalPartStatus.ENABLED, componentSourceLinkForOrganization.getId());

    assertThat(
        dao.getByOwnerIdAndComponentIdentifierWithHierarchy(Organization.ROOT_ORGANIZATION_ID, componentIdentifier))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(sourceLinkOverrideForRootOrganization);
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(organization.getId(), componentIdentifier))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(sourceLinkOverrideForOrganization);
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(application.getId(), componentIdentifier))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(sourceLinkOverrideForOrganization);

    // Add another sourceLink override at the app level
    ComponentSourceLink componentSourceLinkForApplication =
        tempEntity.newComponentSourceLink(componentIdentifier, application.getId());
    SourceLinkOverride sourceLinkOverrideForApplication =
        tempEntity.newSourceLinkOverride("content3", "originalContent3",
            ComponentLegalPartStatus.ENABLED, componentSourceLinkForApplication.getId());

    assertThat(
        dao.getByOwnerIdAndComponentIdentifierWithHierarchy(Organization.ROOT_ORGANIZATION_ID, componentIdentifier))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(sourceLinkOverrideForRootOrganization);
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(organization.getId(), componentIdentifier))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(sourceLinkOverrideForOrganization);
    assertThat(dao.getByOwnerIdAndComponentIdentifierWithHierarchy(application.getId(), componentIdentifier))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(sourceLinkOverrideForApplication);
  }

  @Test
  public void testUpdate_DoesNotExist() {
    ComponentSourceLink componentSourceLink = tempEntity
        .newComponentSourceLink(ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId());
    SourceLinkOverride sourceLinkOverride =
        new SourceLinkOverride("content", "originalContent", ComponentLegalPartStatus.ENABLED,
            componentSourceLink.getId());
    sourceLinkOverride.setId("doesNotExist");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> dao.update(sourceLinkOverride))
        .withMessageContaining(
            "Cannot update source link override with id " + sourceLinkOverride.getId() + " because it does not exist.");
  }
}
