/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.legal;

import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.ComponentSourceLink;
import com.sonatype.insight.brain.model.legal.SourceLinkOverride;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class SourceLinkOverrideDAOTest
    extends AbstractDbDAOTest
{
  private SourceLinkOverrideDAO dao;

  @BeforeEach
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

  // ---- Batch hierarchy tests ----

  @Test
  public void testBatchGetWithHierarchy_emptyComponentList() {
    Map<ComponentIdentifier, List<SourceLinkOverride>> result =
        dao.batchGetWithHierarchy(application.getId(), List.of());
    assertThat(result).isEmpty();
  }

  @Test
  public void testBatchGetWithHierarchy_closestAncestorWins() {
    ComponentIdentifier ci = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

    ComponentSourceLink rootSourceLink =
        tempEntity.newComponentSourceLink(ci, Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newSourceLinkOverride("http://root.example.com",
        ComponentLegalPartStatus.ENABLED, rootSourceLink.getId());

    ComponentSourceLink orgSourceLink = tempEntity.newComponentSourceLink(ci, organization.getId());
    SourceLinkOverride orgOverride = tempEntity.newSourceLinkOverride("http://org.example.com",
        ComponentLegalPartStatus.ENABLED, orgSourceLink.getId());

    Map<ComponentIdentifier, List<SourceLinkOverride>> result =
        dao.batchGetWithHierarchy(application.getId(), List.of(ci));

    assertThat(result).containsKey(ci);
    assertThat(result.get(ci))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(orgOverride);
  }

  @Test
  public void testBatchGetWithHierarchy_multipleComponents() {
    ComponentIdentifier ci1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentIdentifier ci2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2");

    ComponentSourceLink sl1 = tempEntity.newComponentSourceLink(ci1, organization.getId());
    SourceLinkOverride override1 = tempEntity.newSourceLinkOverride("http://one.example.com",
        ComponentLegalPartStatus.ENABLED, sl1.getId());

    ComponentSourceLink sl2 = tempEntity.newComponentSourceLink(ci2, application.getId());
    SourceLinkOverride override2 = tempEntity.newSourceLinkOverride("http://two.example.com",
        ComponentLegalPartStatus.DISABLED, sl2.getId());

    Map<ComponentIdentifier, List<SourceLinkOverride>> result =
        dao.batchGetWithHierarchy(application.getId(), List.of(ci1, ci2));

    assertThat(result.get(ci1)).usingRecursiveFieldByFieldElementComparator().containsExactly(override1);
    assertThat(result.get(ci2)).usingRecursiveFieldByFieldElementComparator().containsExactly(override2);
  }

  @Test
  public void testBatchGetWithHierarchy_noOverridesExist() {
    ComponentIdentifier ci = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

    Map<ComponentIdentifier, List<SourceLinkOverride>> result =
        dao.batchGetWithHierarchy(application.getId(), List.of(ci));

    assertThat(result).doesNotContainKey(ci);
  }

  @Test
  public void testBatchGetWithHierarchy_inheritsFromRootOrg() {
    ComponentIdentifier ci = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

    ComponentSourceLink rootSourceLink =
        tempEntity.newComponentSourceLink(ci, Organization.ROOT_ORGANIZATION_ID);
    SourceLinkOverride rootOverride = tempEntity.newSourceLinkOverride("http://root.example.com",
        ComponentLegalPartStatus.ENABLED, rootSourceLink.getId());

    // No override at org or app level, should inherit from root
    Map<ComponentIdentifier, List<SourceLinkOverride>> result =
        dao.batchGetWithHierarchy(application.getId(), List.of(ci));

    assertThat(result).containsKey(ci);
    assertThat(result.get(ci))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(rootOverride);
  }

  @Test
  public void testBatchGetWithHierarchy_multipleOverridesPerComponentAtClosestAncestor() {
    ComponentIdentifier ci = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

    // Multiple overrides at org level (closest ancestor for the application)
    ComponentSourceLink orgSourceLink = tempEntity.newComponentSourceLink(ci, organization.getId());
    SourceLinkOverride override1 = tempEntity.newSourceLinkOverride("http://one.example.com",
        ComponentLegalPartStatus.ENABLED, orgSourceLink.getId());
    SourceLinkOverride override2 = tempEntity.newSourceLinkOverride("http://two.example.com",
        ComponentLegalPartStatus.DISABLED, orgSourceLink.getId());

    // Override at root level (farther away, should be ignored)
    ComponentSourceLink rootSourceLink =
        tempEntity.newComponentSourceLink(ci, Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newSourceLinkOverride("http://root.example.com",
        ComponentLegalPartStatus.ENABLED, rootSourceLink.getId());

    Map<ComponentIdentifier, List<SourceLinkOverride>> result =
        dao.batchGetWithHierarchy(application.getId(), List.of(ci));

    assertThat(result).containsKey(ci);
    assertThat(result.get(ci))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(override1, override2);
  }

  @Test
  public void testBatchGetWithHierarchy_multipleComponentsMixedPresence() {
    ComponentIdentifier ci1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentIdentifier ci2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2");
    ComponentIdentifier ci3 = ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3");

    // ci1: override at root
    ComponentSourceLink sl1 = tempEntity.newComponentSourceLink(ci1, Organization.ROOT_ORGANIZATION_ID);
    SourceLinkOverride override1 = tempEntity.newSourceLinkOverride("http://root-one.example.com",
        ComponentLegalPartStatus.ENABLED, sl1.getId());

    // ci2: no override anywhere
    // ci3: override at app level
    ComponentSourceLink sl3 = tempEntity.newComponentSourceLink(ci3, application.getId());
    SourceLinkOverride override3 = tempEntity.newSourceLinkOverride("http://app-three.example.com",
        ComponentLegalPartStatus.ENABLED, sl3.getId());

    Map<ComponentIdentifier, List<SourceLinkOverride>> result =
        dao.batchGetWithHierarchy(application.getId(), List.of(ci1, ci2, ci3));

    assertThat(result).containsKey(ci1);
    assertThat(result.get(ci1)).usingRecursiveFieldByFieldElementComparator().containsExactly(override1);
    assertThat(result).doesNotContainKey(ci2);
    assertThat(result).containsKey(ci3);
    assertThat(result.get(ci3)).usingRecursiveFieldByFieldElementComparator().containsExactly(override3);
  }
}
