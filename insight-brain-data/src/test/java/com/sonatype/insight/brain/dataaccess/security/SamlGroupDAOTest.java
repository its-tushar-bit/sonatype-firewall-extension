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

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SamlGroupDAOTest
    extends AbstractDbDAOTest
{
  private SamlGroupDAO samlGroupDAO;

  private SamlUserGroupDAO samlUserGroupDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    samlGroupDAO = daoFactory.createSamlGroupDAO();
    samlUserGroupDAO = daoFactory.createSamlUserGroupDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    SamlGroup samlGroup = new SamlGroup("name");
    samlGroupDAO.insert(samlGroup);
    assertThat(samlGroup.getId()).isNotNull();

    // Read
    SamlGroup storedSamlGroup = samlGroupDAO.getById(samlGroup.getId());
    assertThat(storedSamlGroup).isNotNull();
    assertThat(storedSamlGroup).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(samlGroup);

    // Update
    samlGroup.setName(samlGroup.getName() + "2");
    samlGroupDAO.update(samlGroup);
    storedSamlGroup = samlGroupDAO.getById(samlGroup.getId());
    assertThat(storedSamlGroup).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(samlGroup);

    // Delete
    samlGroupDAO.delete(samlGroup);
    assertThat(samlGroupDAO.getById(samlGroup.getId())).isNull();
  }

  @Test
  public void testGetAll() {
    SamlGroup samlGroup1 = tempEntity.newSamlGroup("group1");
    SamlGroup samlGroup2 = tempEntity.newSamlGroup("group2");

    assertThat(samlGroupDAO.getAll()).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(samlGroup1, samlGroup2);
  }

  @Test
  public void testGetByIds_Empty() {
    assertThat(samlGroupDAO.getByIds(Collections.emptySet())).isEmpty();
  }

  @Test
  public void testGetByIds() {
    SamlGroup samlGroup1 = tempEntity.newSamlGroup("group1");
    SamlGroup samlGroup2 = tempEntity.newSamlGroup("group2");
    tempEntity.newSamlGroup("group3");

    assertThat(samlGroupDAO.getByIds(
        new HashSet<>(Arrays.asList(samlGroup1.getId(), samlGroup2.getId()))))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(samlGroup1, samlGroup2);
  }

  @Test
  public void testGetByName() {
    SamlGroup samlGroup = tempEntity.newSamlGroup();
    tempEntity.newSamlGroup();

    assertThat(samlGroupDAO.getByName(samlGroup.getName())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(samlGroup);
  }

  @Test
  public void testGetByNames_Empty() {
    assertThat(samlGroupDAO.getByNames(Collections.emptySet())).isEmpty();
  }

  @Test
  public void testGetByNames() {
    SamlGroup samlGroup1 = tempEntity.newSamlGroup("group1");
    SamlGroup samlGroup2 = tempEntity.newSamlGroup("group2");
    tempEntity.newSamlGroup("group3");

    assertThat(samlGroupDAO.getByNames(
        new HashSet<>(Arrays.asList(samlGroup1.getName(), samlGroup2.getName()))))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(samlGroup1, samlGroup2);
  }

  @Test
  public void testUpsertByName_Insert() {
    SamlGroup samlGroup = new SamlGroup("name");
    tempEntity.newSamlGroup();

    samlGroupDAO.upsertByName(samlGroup);

    assertThat(samlGroup.getId()).isNotNull();
    assertThat(samlGroupDAO.getById(samlGroup.getId())).usingRecursiveComparison().isEqualTo(samlGroup);
  }

  @Test
  public void testUpsertByName_Update() {
    SamlGroup existingSamlGroup = tempEntity.newSamlGroup("name");
    SamlGroup samlGroup = new SamlGroup(existingSamlGroup.getName());
    tempEntity.newSamlGroup();

    samlGroupDAO.upsertByName(samlGroup);

    assertThat(samlGroup.getId()).isNotNull();
    assertThat(samlGroupDAO.getById(samlGroup.getId())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(samlGroup);
  }

  @Test
  public void testFindGroupsByNameQuery_Exact() {
    SamlGroup samlGroup = tempEntity.newSamlGroup("group");
    tempEntity.newSamlGroup("other");

    assertThat(samlGroupDAO.findGroupsByNameQuery("gRoUp")).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(samlGroup);
  }

  @Test
  public void testFindGroupsByNameQuery_Suffix() {
    SamlGroup samlGroup1 = tempEntity.newSamlGroup("1GROUP");
    SamlGroup samlGroup2 = tempEntity.newSamlGroup("2group");
    tempEntity.newSamlGroup("other");

    assertThat(samlGroupDAO.findGroupsByNameQuery("%gRoUp")).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(samlGroup1, samlGroup2);
  }

  @Test
  public void testFindGroupsByNameQuery_Prefix() {
    SamlGroup samlGroup1 = tempEntity.newSamlGroup("GROUP1");
    SamlGroup samlGroup2 = tempEntity.newSamlGroup("group2");
    tempEntity.newSamlGroup("other");

    assertThat(samlGroupDAO.findGroupsByNameQuery("gRoUp%")).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(samlGroup1, samlGroup2);
  }

  @Test
  public void testDeleteCascadesToSamlUserGroups() {
    SamlUser samlUser1 = tempEntity.newSamlUser();
    SamlUser samlUser2 = tempEntity.newSamlUser();
    SamlGroup samlGroup1 = tempEntity.newSamlGroup();
    SamlGroup samlGroup2 = tempEntity.newSamlGroup();
    tempEntity.newSamlUserGroup(samlUser1.getId(), samlGroup1.getId());
    SamlUserGroup samlUserGroup12 = tempEntity.newSamlUserGroup(samlUser1.getId(), samlGroup2.getId());
    tempEntity.newSamlUserGroup(samlUser2.getId(), samlGroup1.getId());
    SamlUserGroup samlUserGroup22 = tempEntity.newSamlUserGroup(samlUser2.getId(), samlGroup2.getId());

    samlGroupDAO.delete(samlGroup1);

    assertThat(samlGroupDAO.getById(samlGroup1.getId())).isNull();
    assertThat(samlUserGroupDAO.getAll()).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(samlUserGroup12, samlUserGroup22);
  }
}
