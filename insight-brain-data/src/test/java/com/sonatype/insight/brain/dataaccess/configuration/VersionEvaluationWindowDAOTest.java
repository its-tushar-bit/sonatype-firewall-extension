/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.VersionEvaluationWindow;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class VersionEvaluationWindowDAOTest
    extends AbstractDbDAOTest
{
  private VersionEvaluationWindowDAO dao;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createVersionEvaluationWindowDAO();
  }

  @Test
  public void testCRUD() {
    VersionEvaluationWindow window = new VersionEvaluationWindow();
    window.setOwnerId("owner1");
    window.setContextId("context1");
    window.setMaxVersions(10);
    window.setMaxAgeInDays(30);

    dao.insert(window);

    assertThat(window.getId()).isNotNull();

    VersionEvaluationWindow stored = dao.getById(window.getId());

    assertThat(stored).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(window);

    window.setMaxVersions(20);
    window.setMaxAgeInDays(60);

    dao.update(window);

    stored = dao.getById(window.getId());

    assertThat(stored).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(window);

    dao.delete(window);

    assertThat(dao.getById(window.getId())).isNull();
  }

  @Test
  public void testInsert_ValidateEntity_Null() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(null))
        .withMessage("entity cannot be null.");
  }

  @Test
  public void testInsert_ValidateOwnerId_Null() {
    VersionEvaluationWindow window = new VersionEvaluationWindow();
    window.setContextId("context1");
    window.setMaxVersions(10);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(window))
        .withMessage("ownerId is required.");
  }

  @Test
  public void testInsert_ValidateContextId_Null() {
    VersionEvaluationWindow window = new VersionEvaluationWindow();
    window.setOwnerId("owner1");
    window.setMaxVersions(10);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(window))
        .withMessage("contextId is required.");
  }

  @Test
  public void testInsert_ValidateMaxVersions_Negative() {
    VersionEvaluationWindow window = new VersionEvaluationWindow();
    window.setOwnerId("owner1");
    window.setContextId("context1");
    window.setMaxVersions(-1);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(window))
        .withMessage("maxVersions cannot be negative.");
  }

  @Test
  public void testInsert_ValidateMaxAgeInDays_Negative() {
    VersionEvaluationWindow window = new VersionEvaluationWindow();
    window.setOwnerId("owner1");
    window.setContextId("context1");
    window.setMaxAgeInDays(-1);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(window))
        .withMessage("maxAgeInDays cannot be negative.");
  }

  @Test
  public void testInsert_ValidateBothNull() {
    VersionEvaluationWindow window = new VersionEvaluationWindow();
    window.setOwnerId("owner1");
    window.setContextId("context1");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(window))
        .withMessage("At least one of maxVersions or maxAgeInDays must be specified.");
  }

  @Test
  public void testInsert_NullMaxVersions() {
    VersionEvaluationWindow window = new VersionEvaluationWindow();
    window.setOwnerId("owner1");
    window.setContextId("context1");
    window.setMaxAgeInDays(30);

    dao.insert(window);

    assertThat(window.getId()).isNotNull();
    assertThat(dao.getById(window.getId()).getMaxVersions()).isNull();
  }

  @Test
  public void testInsert_NullMaxAgeInDays() {
    VersionEvaluationWindow window = new VersionEvaluationWindow();
    window.setOwnerId("owner1");
    window.setContextId("context1");
    window.setMaxVersions(10);

    dao.insert(window);

    assertThat(window.getId()).isNotNull();
    assertThat(dao.getById(window.getId()).getMaxAgeInDays()).isNull();
  }

  @Test
  public void testUpdate_ValidateEntity_Null() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(null))
        .withMessage("entity cannot be null.");
  }

  @Test
  public void testUpdate_ValidateOwnerId_Null() {
    VersionEvaluationWindow window = tempEntity.newVersionEvaluationWindow("owner1", "context1", 10, 30);
    window.setOwnerId(null);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(window))
        .withMessage("ownerId is required.");
  }

  @Test
  public void testUpdate_ValidateContextId_Null() {
    VersionEvaluationWindow window = tempEntity.newVersionEvaluationWindow("owner1", "context1", 10, 30);
    window.setContextId(null);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(window))
        .withMessage("contextId is required.");
  }

  @Test
  public void testUpdate_ValidateMaxVersions_Negative() {
    VersionEvaluationWindow window = tempEntity.newVersionEvaluationWindow("owner1", "context1", 10, 30);
    window.setMaxVersions(-1);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(window))
        .withMessage("maxVersions cannot be negative.");
  }

  @Test
  public void testUpdate_ValidateMaxAgeInDays_Negative() {
    VersionEvaluationWindow window = tempEntity.newVersionEvaluationWindow("owner1", "context1", 10, 30);
    window.setMaxAgeInDays(-1);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(window))
        .withMessage("maxAgeInDays cannot be negative.");
  }

  @Test
  public void testUpdate_ValidateBothNull() {
    VersionEvaluationWindow window = tempEntity.newVersionEvaluationWindow("owner1", "context1", 10, 30);
    window.setMaxVersions(null);
    window.setMaxAgeInDays(null);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(window))
        .withMessage("At least one of maxVersions or maxAgeInDays must be specified.");
  }

  @Test
  public void testGetByOwnerId() {
    VersionEvaluationWindow window1 = tempEntity.newVersionEvaluationWindow("owner1", "context1", 10, 30);
    VersionEvaluationWindow window2 = tempEntity.newVersionEvaluationWindow("owner1", "context2", 20, 60);
    tempEntity.newVersionEvaluationWindow("owner2", "context3", 15, 45);

    List<VersionEvaluationWindow> result = dao.getByOwnerId("owner1");

    assertThat(result).hasSize(2)
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(window1, window2);
  }

  @Test
  public void testGetByOwnerId_NoResults() {
    tempEntity.newVersionEvaluationWindow("owner1", "context1", 10, 30);

    List<VersionEvaluationWindow> result = dao.getByOwnerId("nonexistent");

    assertThat(result).isEmpty();
  }

  @Test
  public void testGetByOwnerIdAndContextId() {
    VersionEvaluationWindow window1 = tempEntity.newVersionEvaluationWindow("owner1", "context1", 10, 30);
    tempEntity.newVersionEvaluationWindow("owner1", "context2", 20, 60);
    tempEntity.newVersionEvaluationWindow("owner2", "context1", 15, 45);

    VersionEvaluationWindow result = dao.getByOwnerIdAndContextId("owner1", "context1");

    assertThat(result).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(window1);
  }

  @Test
  public void testGetByOwnerIdAndContextId_NoResult() {
    tempEntity.newVersionEvaluationWindow("owner1", "context1", 10, 30);

    VersionEvaluationWindow result = dao.getByOwnerIdAndContextId("owner1", "nonexistent");

    assertThat(result).isNull();
  }

  @Test
  public void testDeleteByOwnerId() {
    VersionEvaluationWindow window1 = tempEntity.newVersionEvaluationWindow("owner1", "context1", 10, 30);
    VersionEvaluationWindow window2 = tempEntity.newVersionEvaluationWindow("owner1", "context2", 20, 60);
    VersionEvaluationWindow window3 = tempEntity.newVersionEvaluationWindow("owner2", "context3", 15, 45);

    dao.deleteByOwnerId("owner1");

    assertThat(dao.getById(window1.getId())).isNull();
    assertThat(dao.getById(window2.getId())).isNull();
    assertThat(dao.getById(window3.getId())).isNotNull();
  }

  @Test
  public void testDeleteByOwnerIdAndContextId() {
    VersionEvaluationWindow window1 = tempEntity.newVersionEvaluationWindow("owner1", "context1", 10, 30);
    VersionEvaluationWindow window2 = tempEntity.newVersionEvaluationWindow("owner1", "context2", 20, 60);
    VersionEvaluationWindow window3 = tempEntity.newVersionEvaluationWindow("owner2", "context1", 15, 45);

    dao.deleteByOwnerIdAndContextId("owner1", "context1");

    assertThat(dao.getById(window1.getId())).isNull();
    assertThat(dao.getById(window2.getId())).isNotNull();
    assertThat(dao.getById(window3.getId())).isNotNull();
  }

  @Test
  public void testGetByOwnerIdsAndContextIdWithInheritance_NullOwnerIds() {
    tempEntity.newVersionEvaluationWindow(organization.getId(), "context1", 10, 30);

    Map<String, VersionEvaluationWindow> result = dao.getByOwnerIdsAndContextIdWithInheritance(null, "context1");

    assertThat(result).isEmpty();
  }

  @Test
  public void testGetByOwnerIdsAndContextIdWithInheritance_EmptyOwnerIds() {
    tempEntity.newVersionEvaluationWindow(organization.getId(), "context1", 10, 30);

    Map<String, VersionEvaluationWindow> result = dao.getByOwnerIdsAndContextIdWithInheritance(Set.of(), "context1");

    assertThat(result).isEmpty();
  }

  @Test
  public void testGetByOwnerIdsAndContextIdWithInheritance_SingleOwnerDirectMatch() {
    VersionEvaluationWindow window = tempEntity.newVersionEvaluationWindow(organization.getId(), "context1", 10, 30);

    Map<String, VersionEvaluationWindow> result =
        dao.getByOwnerIdsAndContextIdWithInheritance(Set.of(organization.getId()), "context1");

    assertThat(result).hasSize(1);
    assertThat(result.get(organization.getId()))
        .usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(window);
  }

  @Test
  public void testGetByOwnerIdsAndContextIdWithInheritance_SingleOwnerInheritFromParent() {
    VersionEvaluationWindow parentWindow =
        tempEntity.newVersionEvaluationWindow(organization.getId(), "context1", 10, 30);

    Map<String, VersionEvaluationWindow> result =
        dao.getByOwnerIdsAndContextIdWithInheritance(Set.of(application.getId()), "context1");

    assertThat(result).hasSize(1);
    assertThat(result.get(application.getId()))
        .usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(parentWindow);
  }

  @Test
  public void testGetByOwnerIdsAndContextIdWithInheritance_SingleOwnerPrefersClosest() {
    tempEntity.newVersionEvaluationWindow(Organization.ROOT_ORGANIZATION_ID, "context1", 5, 15);
    VersionEvaluationWindow closerWindow =
        tempEntity.newVersionEvaluationWindow(organization.getId(), "context1", 10, 30);

    Map<String, VersionEvaluationWindow> result =
        dao.getByOwnerIdsAndContextIdWithInheritance(Set.of(application.getId()), "context1");

    assertThat(result).hasSize(1);
    assertThat(result.get(application.getId()))
        .usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(closerWindow);
  }

  @Test
  public void testGetByOwnerIdsAndContextIdWithInheritance_SingleOwnerNoMatch() {
    tempEntity.newVersionEvaluationWindow(organization.getId(), "context1", 10, 30);

    Map<String, VersionEvaluationWindow> result =
        dao.getByOwnerIdsAndContextIdWithInheritance(Set.of(application.getId()), "context-nonexistent");

    assertThat(result).isEmpty();
  }

  @Test
  public void testGetByOwnerIdsAndContextIdWithInheritance_MultipleOwnersAllMatch() {
    VersionEvaluationWindow orgWindow = tempEntity.newVersionEvaluationWindow(organization.getId(), "context1", 10, 30);
    VersionEvaluationWindow appWindow = tempEntity.newVersionEvaluationWindow(application.getId(), "context1", 20, 60);

    Map<String, VersionEvaluationWindow> result =
        dao.getByOwnerIdsAndContextIdWithInheritance(Set.of(organization.getId(), application.getId()), "context1");

    assertThat(result).hasSize(2);
    assertThat(result.get(organization.getId()))
        .usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(orgWindow);
    assertThat(result.get(application.getId()))
        .usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(appWindow);
  }

  @Test
  public void testGetByOwnerIdsAndContextIdWithInheritance_MultipleOwnersPartialMatch() {
    VersionEvaluationWindow orgWindow = tempEntity.newVersionEvaluationWindow(organization.getId(), "context1", 10, 30);
    String nonMatchingOwnerId = "non-matching-owner";

    Map<String, VersionEvaluationWindow> result =
        dao.getByOwnerIdsAndContextIdWithInheritance(Set.of(organization.getId(), nonMatchingOwnerId), "context1");

    assertThat(result).hasSize(1);
    assertThat(result.get(organization.getId()))
        .usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(orgWindow);
    assertThat(result.get(nonMatchingOwnerId)).isNull();
  }

  @Test
  public void testGetByOwnerIdsAndContextIdWithInheritance_MultipleOwnersWithInheritance() {
    tempEntity.newVersionEvaluationWindow(Organization.ROOT_ORGANIZATION_ID, "context1", 5, 15);
    VersionEvaluationWindow orgWindow = tempEntity.newVersionEvaluationWindow(organization.getId(), "context1", 10, 30);

    Map<String, VersionEvaluationWindow> result =
        dao.getByOwnerIdsAndContextIdWithInheritance(Set.of(organization.getId(), application.getId()), "context1");

    assertThat(result).hasSize(2);
    assertThat(result.get(organization.getId()))
        .usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(orgWindow);
    assertThat(result.get(application.getId()))
        .usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(orgWindow);
  }

  @Test
  public void testGetByOwnerIdsAndContextIdWithInheritance_DifferentContextNoMatch() {
    tempEntity.newVersionEvaluationWindow(organization.getId(), "context1", 10, 30);
    tempEntity.newVersionEvaluationWindow(application.getId(), "context2", 20, 60);

    Map<String, VersionEvaluationWindow> result =
        dao.getByOwnerIdsAndContextIdWithInheritance(Set.of(organization.getId(), application.getId()), "context3");

    assertThat(result).isEmpty();
  }

  @Test
  public void testGetByOwnerIdsAndContextIdWithInheritance_OrderingByAncestorDistance() {
    tempEntity.newVersionEvaluationWindow(Organization.ROOT_ORGANIZATION_ID, "context1", 5, 15);
    tempEntity.newVersionEvaluationWindow(organization.getId(), "context1", 10, 30);
    VersionEvaluationWindow appWindow = tempEntity.newVersionEvaluationWindow(application.getId(), "context1", 20, 60);

    Map<String, VersionEvaluationWindow> result =
        dao.getByOwnerIdsAndContextIdWithInheritance(Set.of(application.getId()), "context1");

    assertThat(result).hasSize(1);
    assertThat(result.get(application.getId()))
        .usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(appWindow);
  }

  @Test
  public void testGetByOwnerIdsAndContextIdWithInheritance_OrderingMultipleOwnersGroupedCorrectly() {
    Organization org2 = tempEntity.newOrganization("org2");
    tempEntity.newApplication("app2", org2.getId());

    tempEntity.newVersionEvaluationWindow(Organization.ROOT_ORGANIZATION_ID, "context1", 5, 15);
    VersionEvaluationWindow org1Window =
        tempEntity.newVersionEvaluationWindow(organization.getId(), "context1", 10, 30);
    VersionEvaluationWindow org2Window = tempEntity.newVersionEvaluationWindow(org2.getId(), "context1", 15, 45);

    Map<String, VersionEvaluationWindow> result =
        dao.getByOwnerIdsAndContextIdWithInheritance(Set.of(organization.getId(), org2.getId()), "context1");

    assertThat(result).hasSize(2);
    assertThat(result.get(organization.getId()))
        .usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(org1Window);
    assertThat(result.get(org2.getId()))
        .usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(org2Window);
  }

  @Test
  public void testGetByOwnerIdsAndContextIdWithInheritance_OrderingSelectsLowestDistancePerOwner() {
    tempEntity.newVersionEvaluationWindow(Organization.ROOT_ORGANIZATION_ID, "context1", 5, 15);
    VersionEvaluationWindow orgWindow = tempEntity.newVersionEvaluationWindow(organization.getId(), "context1", 10, 30);

    Map<String, VersionEvaluationWindow> result =
        dao.getByOwnerIdsAndContextIdWithInheritance(Set.of(application.getId()), "context1");

    assertThat(result).hasSize(1);
    assertThat(result.get(application.getId()))
        .usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(orgWindow);
  }
}
