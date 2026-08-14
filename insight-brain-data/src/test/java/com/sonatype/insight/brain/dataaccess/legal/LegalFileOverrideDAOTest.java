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
import com.sonatype.insight.brain.model.legal.ComponentLegalFile;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.LegalFileOverride;
import com.sonatype.insight.brain.model.legal.LegalFileType;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class LegalFileOverrideDAOTest
    extends AbstractDbDAOTest
{
  private LegalFileOverrideDAO dao;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createLegalFileOverrideDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    ComponentLegalFile componentLegalFile = tempEntity.newComponentLegalFile(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(), LegalFileType.NOTICE,
        "legalContentHash");
    LegalFileOverride legalFileOverride = new LegalFileOverride("originalHash", "hash", "content",
        ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());
    dao.insert(legalFileOverride);
    assertThat(legalFileOverride.getId()).isNotNull();

    // Read
    assertThat(dao.getById(legalFileOverride.getId())).usingRecursiveComparison().isEqualTo(legalFileOverride);

    // Update
    legalFileOverride.setContentHash(legalFileOverride.getContentHash() + "2");
    legalFileOverride.setContent(legalFileOverride.getContent() + "2");
    legalFileOverride.setStatus(ComponentLegalPartStatus.DISABLED);
    ComponentLegalFile componentLegalFile2 = tempEntity.newComponentLegalFile(
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"), application.getId(), LegalFileType.NOTICE,
        "legalContentHash2");
    legalFileOverride.setComponentLegalFileId(componentLegalFile2.getId());
    dao.update(legalFileOverride);
    assertThat(dao.getById(legalFileOverride.getId())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(legalFileOverride);

    // Delete
    dao.delete(legalFileOverride);
    assertThat(dao.getById(legalFileOverride.getId())).isNull();
  }

  @Test
  public void testGetByComponentLegalFileId() {
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
    tempEntity.newLegalFileOverride("originalHash3", "hash3", "content3", ComponentLegalPartStatus.ENABLED,
        otherComponentLegalFile.getId());

    assertThat(dao.getByComponentLegalFileId(componentLegalFile.getId())).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(legalFileOverride1, legalFileOverride2);
  }

  @Test
  public void testGetByOwnerIdAndComponentIdentifierAndLegalFileType() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentLegalFile componentLegalFile = tempEntity.newComponentLegalFile(componentIdentifier, organization.getId(),
        LegalFileType.NOTICE, "legalContentHash");
    tempEntity.newComponentLegalFile(componentIdentifier, application.getId(),
        LegalFileType.NOTICE, "legalContentHash");
    tempEntity.newComponentLegalFile(
        componentIdentifier.createAlternativeVersion("v2"), organization.getId(), LegalFileType.NOTICE,
        "legalContentHash");
    tempEntity.newComponentLegalFile(componentIdentifier, organization.getId(),
        LegalFileType.LICENSE, "legalContentHash");

    LegalFileOverride legalFileOverride1 =
        tempEntity.newLegalFileOverride("originalHash1", "hash1", "content1",
            ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());
    LegalFileOverride legalFileOverride2 =
        tempEntity.newLegalFileOverride("originalHash2", "hash2", "content2",
            ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());

    assertThat(dao.getByOwnerIdAndComponentIdentifierAndType(organization.getId(), componentIdentifier,
        LegalFileType.NOTICE)).usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(legalFileOverride1, legalFileOverride2);
  }

  @Test
  public void testGetByOwnerIdAndComponentIdentifierAndTypeWithHierarchy() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");

    // Start with a legal file override at just the root org level
    ComponentLegalFile componentLegalFileForRootOrganization =
        tempEntity.newComponentLegalFile(componentIdentifier, Organization.ROOT_ORGANIZATION_ID, LegalFileType.NOTICE,
            "legalContentHash1");
    LegalFileOverride legalFileOverrideForRootOrganization =
        tempEntity.newLegalFileOverride("originalHash1", "hash1", "content1",
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
        tempEntity.newComponentLegalFile(componentIdentifier, organization.getId(), LegalFileType.NOTICE,
            "legalContentHash2");
    LegalFileOverride legalFileOverrideForOrganization =
        tempEntity.newLegalFileOverride("originalHash2", "hash2", "content2",
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
        tempEntity
            .newComponentLegalFile(componentIdentifier, application.getId(), LegalFileType.NOTICE, "legalContentHash3");
    LegalFileOverride legalFileOverrideForApplication =
        tempEntity.newLegalFileOverride("originalHash3", "hash3", "content3",
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

  @Test
  public void testUpdate_DoesNotExist() {
    ComponentLegalFile componentLegalFile = tempEntity.newComponentLegalFile(
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"), organization.getId(), LegalFileType.NOTICE,
        "legalContentHash");
    LegalFileOverride legalFileOverride = new LegalFileOverride("originalHash", "hash", "content",
        ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());
    legalFileOverride.setId("doesNotExist");

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> dao.update(legalFileOverride))
        .withMessageContaining(
            "Cannot update legal file override with id " + legalFileOverride.getId() + " because it does not exist.");
  }

  // ---- Batch hierarchy (all types) tests ----

  @Test
  public void testBatchGetWithHierarchyAllTypes_emptyComponentList() {
    var result = dao.batchGetWithHierarchyAllTypes(application.getId(), List.of());
    assertThat(result.isEmpty()).isTrue();
  }

  @Test
  public void testBatchGetWithHierarchyAllTypes_closestAncestorWins() {
    ComponentIdentifier ci = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

    ComponentLegalFile rootLicense =
        tempEntity.newComponentLegalFile(ci, Organization.ROOT_ORGANIZATION_ID, LegalFileType.LICENSE, "rootHash");
    tempEntity.newLegalFileOverride("origRoot", "hashRoot", "rootContent",
        ComponentLegalPartStatus.ENABLED, rootLicense.getId());

    ComponentLegalFile orgLicense =
        tempEntity.newComponentLegalFile(ci, organization.getId(), LegalFileType.LICENSE, "orgHash");
    LegalFileOverride orgOverride = tempEntity.newLegalFileOverride("origOrg", "hashOrg", "orgContent",
        ComponentLegalPartStatus.ENABLED, orgLicense.getId());

    Map<ComponentIdentifier, LegalFileOverrideDAO.BatchResult> result =
        dao.batchGetWithHierarchyAllTypes(application.getId(), List.of(ci)).row(LegalFileType.LICENSE);

    assertThat(result).containsKey(ci);
    LegalFileOverrideDAO.BatchResult batchResult = result.get(ci);
    assertThat(batchResult.componentLegalFile().getId()).isEqualTo(orgLicense.getId());
    assertThat(batchResult.overrides())
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(orgOverride);
  }

  @Test
  public void testBatchGetWithHierarchyAllTypes_separatesTypes() {
    ComponentIdentifier ci = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

    // Notice file at org level
    ComponentLegalFile noticeFile =
        tempEntity.newComponentLegalFile(ci, organization.getId(), LegalFileType.NOTICE, "noticeHash");
    tempEntity.newLegalFileOverride("origNotice", "hashNotice", "noticeContent",
        ComponentLegalPartStatus.ENABLED, noticeFile.getId());

    var allResults = dao.batchGetWithHierarchyAllTypes(application.getId(), List.of(ci));

    // LICENSE row should not contain this component
    assertThat(allResults.row(LegalFileType.LICENSE)).doesNotContainKey(ci);

    // NOTICE row should find it
    Map<ComponentIdentifier, LegalFileOverrideDAO.BatchResult> noticeResults = allResults.row(LegalFileType.NOTICE);
    assertThat(noticeResults).containsKey(ci);
    assertThat(noticeResults.get(ci).componentLegalFile().getId()).isEqualTo(noticeFile.getId());
  }

  @Test
  public void testBatchGetWithHierarchyAllTypes_noOverridesExist() {
    ComponentIdentifier ci = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

    // No legal file data at any hierarchy level
    Map<ComponentIdentifier, LegalFileOverrideDAO.BatchResult> result =
        dao.batchGetWithHierarchyAllTypes(application.getId(), List.of(ci)).row(LegalFileType.LICENSE);

    assertThat(result).doesNotContainKey(ci);
  }

  @Test
  public void testBatchGetWithHierarchyAllTypes_multipleOverridesPerComponent() {
    ComponentIdentifier ci = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

    ComponentLegalFile orgLicense =
        tempEntity.newComponentLegalFile(ci, organization.getId(), LegalFileType.LICENSE, "orgHash");
    LegalFileOverride override1 = tempEntity.newLegalFileOverride("origContent1", "hash1", "content1",
        ComponentLegalPartStatus.ENABLED, orgLicense.getId());
    LegalFileOverride override2 = tempEntity.newLegalFileOverride("origContent2", "hash2", "content2",
        ComponentLegalPartStatus.DISABLED, orgLicense.getId());
    LegalFileOverride override3 = tempEntity.newLegalFileOverride("origContent3", "hash3", "content3",
        ComponentLegalPartStatus.ENABLED, orgLicense.getId());

    Map<ComponentIdentifier, LegalFileOverrideDAO.BatchResult> result =
        dao.batchGetWithHierarchyAllTypes(application.getId(), List.of(ci)).row(LegalFileType.LICENSE);

    assertThat(result).containsKey(ci);
    LegalFileOverrideDAO.BatchResult batchResult = result.get(ci);
    assertThat(batchResult.componentLegalFile().getId()).isEqualTo(orgLicense.getId());
    assertThat(batchResult.overrides())
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(override1, override2, override3);
  }

  @Test
  public void testBatchGetWithHierarchyAllTypes_directAtAppLevel() {
    ComponentIdentifier ci = ComponentIdentifier.createMavenCoordinates("g", "a", "v");

    // Override directly at the application level
    ComponentLegalFile appLicense =
        tempEntity.newComponentLegalFile(ci, application.getId(), LegalFileType.LICENSE, "appHash");
    LegalFileOverride appOverride = tempEntity.newLegalFileOverride("origApp", "hashApp", "appContent",
        ComponentLegalPartStatus.ENABLED, appLicense.getId());

    // Also an override at root that should be ignored
    ComponentLegalFile rootLicense =
        tempEntity.newComponentLegalFile(ci, Organization.ROOT_ORGANIZATION_ID, LegalFileType.LICENSE, "rootHash");
    tempEntity.newLegalFileOverride("origRoot", "hashRoot", "rootContent",
        ComponentLegalPartStatus.ENABLED, rootLicense.getId());

    Map<ComponentIdentifier, LegalFileOverrideDAO.BatchResult> result =
        dao.batchGetWithHierarchyAllTypes(application.getId(), List.of(ci)).row(LegalFileType.LICENSE);

    assertThat(result).containsKey(ci);
    LegalFileOverrideDAO.BatchResult batchResult = result.get(ci);
    assertThat(batchResult.componentLegalFile().getId()).isEqualTo(appLicense.getId());
    assertThat(batchResult.overrides())
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(appOverride);
  }

  @Test
  public void testBatchGetWithHierarchyAllTypes_multipleComponents() {
    ComponentIdentifier ci1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentIdentifier ci2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2");
    ComponentIdentifier ci3 = ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3");

    // ci1: license overrides at org
    ComponentLegalFile orgLicense1 =
        tempEntity.newComponentLegalFile(ci1, organization.getId(), LegalFileType.LICENSE, "orgHash1");
    LegalFileOverride override1 = tempEntity.newLegalFileOverride("orig1", "hash1", "content1",
        ComponentLegalPartStatus.ENABLED, orgLicense1.getId());

    // ci2: license overrides at root org
    ComponentLegalFile rootLicense2 =
        tempEntity.newComponentLegalFile(ci2, Organization.ROOT_ORGANIZATION_ID, LegalFileType.LICENSE, "rootHash2");
    LegalFileOverride override2 = tempEntity.newLegalFileOverride("orig2", "hash2", "content2",
        ComponentLegalPartStatus.ENABLED, rootLicense2.getId());

    // ci3: no overrides

    Map<ComponentIdentifier, LegalFileOverrideDAO.BatchResult> result =
        dao.batchGetWithHierarchyAllTypes(application.getId(), List.of(ci1, ci2, ci3)).row(LegalFileType.LICENSE);

    assertThat(result).containsKey(ci1);
    assertThat(result.get(ci1).componentLegalFile().getId()).isEqualTo(orgLicense1.getId());
    assertThat(result.get(ci1).overrides()).usingRecursiveFieldByFieldElementComparator().containsExactly(override1);

    assertThat(result).containsKey(ci2);
    assertThat(result.get(ci2).componentLegalFile().getId()).isEqualTo(rootLicense2.getId());
    assertThat(result.get(ci2).overrides()).usingRecursiveFieldByFieldElementComparator().containsExactly(override2);

    assertThat(result).doesNotContainKey(ci3);
  }
}
