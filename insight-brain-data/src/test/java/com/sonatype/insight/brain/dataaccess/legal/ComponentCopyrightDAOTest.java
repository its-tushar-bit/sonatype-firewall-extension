/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.legal;

import java.util.Date;
import java.util.List;
import java.util.Map;

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

  // ---- Batch tests ----

  @Test
  public void testBatchGetByOwnerIdAndComponentIdentifiers_emptyList() {
    Map<ComponentIdentifier, ComponentCopyright> result =
        dao.batchGetByOwnerIdAndComponentIdentifiers(application.getId(), List.of());
    assertThat(result).isEmpty();
  }

  @Test
  public void testBatchGetByOwnerIdAndComponentIdentifiers_directLookup() {
    ComponentIdentifier ci1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentIdentifier ci2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2");

    ComponentCopyright appCopyright = tempEntity.newComponentCopyright(ci1, application.getId(), "appHash");
    // ci2 only at org level - should NOT be found (no hierarchy)
    tempEntity.newComponentCopyright(ci2, organization.getId(), "orgHash");

    Map<ComponentIdentifier, ComponentCopyright> result =
        dao.batchGetByOwnerIdAndComponentIdentifiers(application.getId(), List.of(ci1, ci2));

    assertThat(result).containsKey(ci1);
    assertThat(result.get(ci1).getId()).isEqualTo(appCopyright.getId());
    assertThat(result).doesNotContainKey(ci2);
  }

  @Test
  public void testBatchGetByOwnerIdAndComponentIdentifiers_noDataForAnyComponent() {
    ComponentIdentifier ci1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentIdentifier ci2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2");

    // No copyright data exists at all
    Map<ComponentIdentifier, ComponentCopyright> result =
        dao.batchGetByOwnerIdAndComponentIdentifiers(application.getId(), List.of(ci1, ci2));

    assertThat(result).isEmpty();
  }

  @Test
  public void testBatchGetByOwnerIdAndComponentIdentifiers_multipleComponentsAtSameOwner() {
    ComponentIdentifier ci1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentIdentifier ci2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2");
    ComponentIdentifier ci3 = ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3");

    ComponentCopyright copyright1 = tempEntity.newComponentCopyright(ci1, organization.getId(), "hash1");
    ComponentCopyright copyright2 = tempEntity.newComponentCopyright(ci2, organization.getId(), "hash2");
    ComponentCopyright copyright3 = tempEntity.newComponentCopyright(ci3, organization.getId(), "hash3");

    Map<ComponentIdentifier, ComponentCopyright> result =
        dao.batchGetByOwnerIdAndComponentIdentifiers(organization.getId(), List.of(ci1, ci2, ci3));

    assertThat(result).hasSize(3);
    assertThat(result.get(ci1).getId()).isEqualTo(copyright1.getId());
    assertThat(result.get(ci2).getId()).isEqualTo(copyright2.getId());
    assertThat(result.get(ci3).getId()).isEqualTo(copyright3.getId());
  }

  @Test
  public void testBatchGetByOwnerIdAndComponentIdentifiers_mixOfFoundAndNotFound() {
    ComponentIdentifier ci1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentIdentifier ci2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2");
    ComponentIdentifier ci3 = ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3");

    // Only ci1 and ci3 have copyrights at the queried owner
    ComponentCopyright copyright1 = tempEntity.newComponentCopyright(ci1, application.getId(), "hash1");
    ComponentCopyright copyright3 = tempEntity.newComponentCopyright(ci3, application.getId(), "hash3");
    // ci2 has data at a different owner but not at the queried one
    tempEntity.newComponentCopyright(ci2, organization.getId(), "hash2");

    Map<ComponentIdentifier, ComponentCopyright> result =
        dao.batchGetByOwnerIdAndComponentIdentifiers(application.getId(), List.of(ci1, ci2, ci3));

    assertThat(result).hasSize(2);
    assertThat(result.get(ci1).getId()).isEqualTo(copyright1.getId());
    assertThat(result).doesNotContainKey(ci2);
    assertThat(result.get(ci3).getId()).isEqualTo(copyright3.getId());
  }

  @Test
  public void testBatchGetByOwnerIdAndComponentIdentifiers_dataAtMultipleHierarchyLevels_onlyReturnsExactOwner() {
    ComponentIdentifier ci = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

    // Copyright exists at root org, org, AND app levels for the same component
    tempEntity.newComponentCopyright(ci, Organization.ROOT_ORGANIZATION_ID, "rootHash");
    tempEntity.newComponentCopyright(ci, organization.getId(), "orgHash");
    ComponentCopyright appCopyright = tempEntity.newComponentCopyright(ci, application.getId(), "appHash");

    // Batch query at app level should return ONLY the app-level record
    Map<ComponentIdentifier, ComponentCopyright> result =
        dao.batchGetByOwnerIdAndComponentIdentifiers(application.getId(), List.of(ci));

    assertThat(result).hasSize(1);
    assertThat(result.get(ci).getId()).isEqualTo(appCopyright.getId());
    assertThat(result.get(ci).getOwnerId()).isEqualTo(application.getId());

    // Batch query at org level should return ONLY the org-level record
    result = dao.batchGetByOwnerIdAndComponentIdentifiers(organization.getId(), List.of(ci));

    assertThat(result).hasSize(1);
    assertThat(result.get(ci).getOwnerId()).isEqualTo(organization.getId());
  }
}
