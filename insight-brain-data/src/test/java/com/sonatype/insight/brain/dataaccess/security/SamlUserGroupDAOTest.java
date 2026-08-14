/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.model.security.SamlGroup;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.SamlUserGroup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SamlUserGroupDAOTest
    extends AbstractDbDAOTest
{
  private SamlUserGroupDAO samlUserGroupDAO;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    samlUserGroupDAO = daoFactory.createSamlUserGroupDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    SamlUser samlUser1 = tempEntity.newSamlUser();
    SamlGroup samlGroup1 = tempEntity.newSamlGroup();
    SamlUserGroup samlUserGroup = new SamlUserGroup(samlUser1.getId(), samlGroup1.getId());
    samlUserGroupDAO.insert(samlUserGroup);
    assertThat(samlUserGroup.getId()).isNotNull();

    // Read
    SamlUserGroup storedSamlUserGroup = samlUserGroupDAO.getById(samlUserGroup.getId());
    assertThat(storedSamlUserGroup).isNotNull();
    assertThat(storedSamlUserGroup).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(samlUserGroup);

    // Update
    SamlUser samlUser2 = tempEntity.newSamlUser();
    SamlGroup samlGroup2 = tempEntity.newSamlGroup();
    samlUserGroup.setSamlUserId(samlUser2.getId());
    samlUserGroup.setSamlGroupId(samlGroup2.getId());
    samlUserGroupDAO.update(samlUserGroup);
    storedSamlUserGroup = samlUserGroupDAO.getById(storedSamlUserGroup.getId());
    assertThat(storedSamlUserGroup).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(samlUserGroup);

    // Delete
    samlUserGroupDAO.delete(samlUserGroup);
    assertThat(samlUserGroupDAO.getById(samlUserGroup.getId())).isNull();
  }

  @Test
  public void testGetAll() {
    SamlUser samlUser1 = tempEntity.newSamlUser();
    SamlUser samlUser2 = tempEntity.newSamlUser();
    SamlGroup samlGroup1 = tempEntity.newSamlGroup();
    SamlGroup samlGroup2 = tempEntity.newSamlGroup();
    SamlUserGroup samlUserGroup11 = tempEntity.newSamlUserGroup(samlUser1.getId(), samlGroup1.getId());
    SamlUserGroup samlUserGroup22 = tempEntity.newSamlUserGroup(samlUser2.getId(), samlGroup2.getId());

    assertThat(samlUserGroupDAO.getAll()).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(samlUserGroup11, samlUserGroup22);
  }

  @Test
  public void testGetBySamlUserIdAndSamlGroupId() {
    SamlUser samlUser1 = tempEntity.newSamlUser();
    SamlUser samlUser2 = tempEntity.newSamlUser();
    SamlGroup samlGroup1 = tempEntity.newSamlGroup();
    SamlGroup samlGroup2 = tempEntity.newSamlGroup();
    SamlUserGroup samlUserGroup11 = tempEntity.newSamlUserGroup(samlUser1.getId(), samlGroup1.getId());
    tempEntity.newSamlUserGroup(samlUser1.getId(), samlGroup2.getId());
    tempEntity.newSamlUserGroup(samlUser2.getId(), samlGroup1.getId());
    tempEntity.newSamlUserGroup(samlUser2.getId(), samlGroup2.getId());

    assertThat(samlUserGroupDAO.getBySamlUserIdAndSamlGroupId(samlUser1.getId(),
        samlGroup1.getId())).usingRecursiveComparison().isEqualTo(samlUserGroup11);
  }

  @Test
  public void testGetBySamlUserId() {
    SamlUser samlUser1 = tempEntity.newSamlUser();
    SamlUser samlUser2 = tempEntity.newSamlUser();
    SamlGroup samlGroup1 = tempEntity.newSamlGroup();
    SamlGroup samlGroup2 = tempEntity.newSamlGroup();
    SamlGroup samlGroup3 = tempEntity.newSamlGroup();
    tempEntity.newSamlUser();
    tempEntity.newSamlGroup();
    SamlUserGroup samlUserGroup11 = tempEntity.newSamlUserGroup(samlUser1.getId(), samlGroup1.getId());
    tempEntity.newSamlUserGroup(samlUser2.getId(), samlGroup2.getId());
    SamlUserGroup samlUserGroup13 = tempEntity.newSamlUserGroup(samlUser1.getId(), samlGroup3.getId());
    tempEntity.newSamlUserGroup(samlUser2.getId(), samlGroup3.getId());

    assertThat(samlUserGroupDAO.getBySamlUserId(samlUser1.getId())).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(samlUserGroup11, samlUserGroup13);
  }

  @Test
  public void testGetBySamlGroupId() {
    SamlUser samlUser1 = tempEntity.newSamlUser();
    SamlUser samlUser2 = tempEntity.newSamlUser();
    SamlGroup samlGroup1 = tempEntity.newSamlGroup();
    SamlGroup samlGroup2 = tempEntity.newSamlGroup();
    SamlGroup samlGroup3 = tempEntity.newSamlGroup();
    tempEntity.newSamlUser();
    tempEntity.newSamlGroup();
    tempEntity.newSamlUserGroup(samlUser1.getId(), samlGroup1.getId());
    tempEntity.newSamlUserGroup(samlUser2.getId(), samlGroup2.getId());
    SamlUserGroup samlUserGroup13 = tempEntity.newSamlUserGroup(samlUser1.getId(), samlGroup3.getId());
    SamlUserGroup samlUserGroup23 = tempEntity.newSamlUserGroup(samlUser2.getId(), samlGroup3.getId());

    assertThat(samlUserGroupDAO.getBySamlGroupId(samlGroup3.getId())).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(samlUserGroup13, samlUserGroup23);
  }

  @Test
  public void testUpsertBySamlUserIdAndSamlGroupId_Insert() {
    SamlUser samlUser = tempEntity.newSamlUser();
    SamlGroup samlGroup = tempEntity.newSamlGroup();
    SamlUserGroup samlUserGroup = new SamlUserGroup(samlUser.getId(), samlGroup.getId());

    samlUserGroupDAO.upsertBySamlUserIdAndSamlGroupId(samlUserGroup);

    assertThat(samlUserGroup.getId()).isNotNull();
    assertThat(samlUserGroupDAO.getById(samlUserGroup.getId())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(samlUserGroup);
  }

  @Test
  public void testUpsertBySamlUserIdAndSamlGroupId_Update() {
    SamlUser samlUser = tempEntity.newSamlUser();
    SamlGroup samlGroup = tempEntity.newSamlGroup();
    tempEntity.newSamlUserGroup(samlUser.getId(), samlGroup.getId());
    SamlUserGroup samlUserGroup = new SamlUserGroup(samlUser.getId(), samlGroup.getId());

    samlUserGroupDAO.upsertBySamlUserIdAndSamlGroupId(samlUserGroup);

    assertThat(samlUserGroup.getId()).isNotNull();
    assertThat(samlUserGroupDAO.getById(samlUserGroup.getId())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(samlUserGroup);
  }

  @Test
  public void testDeleteBySamlUserId() {
    SamlUser samlUser1 = tempEntity.newSamlUser();
    SamlUser samlUser2 = tempEntity.newSamlUser();
    SamlGroup samlGroup1 = tempEntity.newSamlGroup();
    SamlGroup samlGroup2 = tempEntity.newSamlGroup();
    SamlGroup samlGroup3 = tempEntity.newSamlGroup();
    tempEntity.newSamlUser();
    tempEntity.newSamlGroup();
    tempEntity.newSamlUserGroup(samlUser1.getId(), samlGroup1.getId());
    SamlUserGroup samlUserGroup22 = tempEntity.newSamlUserGroup(samlUser2.getId(), samlGroup2.getId());
    tempEntity.newSamlUserGroup(samlUser1.getId(), samlGroup3.getId());
    SamlUserGroup samlUserGroup23 = tempEntity.newSamlUserGroup(samlUser2.getId(), samlGroup3.getId());

    samlUserGroupDAO.deleteBySamlUserId(samlUser1.getId());
    assertThat(samlUserGroupDAO.getAll()).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(samlUserGroup22, samlUserGroup23);
  }

  @Test
  public void testDeleteBySamlGroupId() {
    SamlUser samlUser1 = tempEntity.newSamlUser();
    SamlUser samlUser2 = tempEntity.newSamlUser();
    SamlGroup samlGroup1 = tempEntity.newSamlGroup();
    SamlGroup samlGroup2 = tempEntity.newSamlGroup();
    SamlGroup samlGroup3 = tempEntity.newSamlGroup();
    tempEntity.newSamlUser();
    tempEntity.newSamlGroup();
    SamlUserGroup samlUserGroup11 = tempEntity.newSamlUserGroup(samlUser1.getId(), samlGroup1.getId());
    SamlUserGroup samlUserGroup22 = tempEntity.newSamlUserGroup(samlUser2.getId(), samlGroup2.getId());
    tempEntity.newSamlUserGroup(samlUser1.getId(), samlGroup3.getId());
    tempEntity.newSamlUserGroup(samlUser2.getId(), samlGroup3.getId());

    samlUserGroupDAO.deleteBySamlGroupId(samlGroup3.getId());
    assertThat(samlUserGroupDAO.getAll()).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(samlUserGroup11, samlUserGroup22);
  }

  @Test
  public void testDeleteBySamlUserIdAndGroupIds_Empty() {
    SamlUser samlUser1 = tempEntity.newSamlUser();
    SamlUser samlUser2 = tempEntity.newSamlUser();
    SamlGroup samlGroup1 = tempEntity.newSamlGroup();
    SamlGroup samlGroup2 = tempEntity.newSamlGroup();
    SamlUserGroup samlUserGroup11 = tempEntity.newSamlUserGroup(samlUser1.getId(), samlGroup1.getId());
    SamlUserGroup samlUserGroup22 = tempEntity.newSamlUserGroup(samlUser2.getId(), samlGroup2.getId());

    samlUserGroupDAO.deleteBySamlUserIdAndGroupIds(samlUser1.getId(), Collections.emptySet());

    assertThat(samlUserGroupDAO.getAll()).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(samlUserGroup11, samlUserGroup22);
  }

  @Test
  public void testDeleteBySamlUserIdAndGroupIds() {
    SamlUser samlUser1 = tempEntity.newSamlUser();
    SamlUser samlUser2 = tempEntity.newSamlUser();
    SamlGroup samlGroup1 = tempEntity.newSamlGroup();
    SamlGroup samlGroup2 = tempEntity.newSamlGroup();
    SamlGroup samlGroup3 = tempEntity.newSamlGroup();
    tempEntity.newSamlUserGroup(samlUser1.getId(), samlGroup1.getId());
    tempEntity.newSamlUserGroup(samlUser1.getId(), samlGroup2.getId());
    SamlUserGroup samlUserGroup13 = tempEntity.newSamlUserGroup(samlUser1.getId(), samlGroup3.getId());
    SamlUserGroup samlUserGroup21 = tempEntity.newSamlUserGroup(samlUser2.getId(), samlGroup1.getId());
    SamlUserGroup samlUserGroup22 = tempEntity.newSamlUserGroup(samlUser2.getId(), samlGroup2.getId());
    SamlUserGroup samlUserGroup23 = tempEntity.newSamlUserGroup(samlUser2.getId(), samlGroup3.getId());

    samlUserGroupDAO.deleteBySamlUserIdAndGroupIds(samlUser1.getId(),
        new HashSet<>(Arrays.asList(samlGroup1.getId(), samlGroup2.getId())));

    assertThat(samlUserGroupDAO.getAll()).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(samlUserGroup13, samlUserGroup21, samlUserGroup22, samlUserGroup23);
  }
}
