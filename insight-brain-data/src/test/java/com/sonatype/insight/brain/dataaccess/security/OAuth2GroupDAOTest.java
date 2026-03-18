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
import com.sonatype.insight.brain.model.security.OAuth2Group;
import com.sonatype.insight.brain.model.security.OAuth2User;
import com.sonatype.insight.brain.model.security.OAuth2UserGroup;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OAuth2GroupDAOTest
    extends AbstractDbDAOTest
{
  private OAuth2GroupDAO oAuth2GroupDAO;

  private OAuth2UserGroupDAO oAuth2UserGroupDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    oAuth2GroupDAO = daoFactory.createOAuth2GroupDAO();
    oAuth2UserGroupDAO = daoFactory.createOAuth2UserGroupDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    OAuth2Group oAuth2Group = new OAuth2Group("name");
    oAuth2GroupDAO.insert(oAuth2Group);
    assertThat(oAuth2Group.getId()).isNotNull();

    // Read
    OAuth2Group storedOAuth2Group = oAuth2GroupDAO.getById(oAuth2Group.getId());
    assertThat(storedOAuth2Group).isNotNull();
    assertThat(storedOAuth2Group).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(oAuth2Group);

    // Update
    oAuth2Group.setName(oAuth2Group.getName() + "2");
    oAuth2GroupDAO.update(oAuth2Group);
    storedOAuth2Group = oAuth2GroupDAO.getById(oAuth2Group.getId());
    assertThat(storedOAuth2Group).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(oAuth2Group);

    // Delete
    oAuth2GroupDAO.delete(oAuth2Group);
    assertThat(oAuth2GroupDAO.getById(oAuth2Group.getId())).isNull();
  }

  @Test
  public void testGetAll() {
    OAuth2Group oAuth2Group1 = tempEntity.newOAuth2Group("group1");
    OAuth2Group oAuth2Group2 = tempEntity.newOAuth2Group("group2");

    assertThat(oAuth2GroupDAO.getAll()).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(oAuth2Group1, oAuth2Group2);
  }

  @Test
  public void testGetByIds_Empty() {
    assertThat(oAuth2GroupDAO.getByIds(Collections.emptySet())).isEmpty();
  }

  @Test
  public void testGetByIds() {
    OAuth2Group oAuth2Group1 = tempEntity.newOAuth2Group("group1");
    OAuth2Group oAuth2Group2 = tempEntity.newOAuth2Group("group2");
    tempEntity.newOAuth2Group("group3");

    assertThat(oAuth2GroupDAO.getByIds(
        new HashSet<>(Arrays.asList(oAuth2Group1.getId(), oAuth2Group2.getId()))))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(oAuth2Group1, oAuth2Group2);
  }

  @Test
  public void testGetByName() {
    OAuth2Group oAuth2Group = tempEntity.newOAuth2Group();
    tempEntity.newOAuth2Group();

    assertThat(oAuth2GroupDAO.getByName(oAuth2Group.getName())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(oAuth2Group);
  }

  @Test
  public void testGetByNames_Empty() {
    assertThat(oAuth2GroupDAO.getByNames(Collections.emptySet())).isEmpty();
  }

  @Test
  public void testGetByNames() {
    OAuth2Group oAuth2Group1 = tempEntity.newOAuth2Group("group1");
    OAuth2Group oAuth2Group2 = tempEntity.newOAuth2Group("group2");
    tempEntity.newOAuth2Group("group3");

    assertThat(oAuth2GroupDAO.getByNames(
        new HashSet<>(Arrays.asList(oAuth2Group1.getName(), oAuth2Group2.getName()))))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(oAuth2Group1, oAuth2Group2);
  }

  @Test
  public void testUpsertByName_Insert() {
    OAuth2Group oAuth2Group = new OAuth2Group("name");
    tempEntity.newOAuth2Group();

    oAuth2GroupDAO.upsertByName(oAuth2Group);

    assertThat(oAuth2Group.getId()).isNotNull();
    assertThat(oAuth2GroupDAO.getById(oAuth2Group.getId())).usingRecursiveComparison().isEqualTo(oAuth2Group);
  }

  @Test
  public void testUpsertByName_Update() {
    OAuth2Group existingOAuth2Group = tempEntity.newOAuth2Group("name");
    OAuth2Group oAuth2Group = new OAuth2Group(existingOAuth2Group.getName());
    tempEntity.newOAuth2Group();

    oAuth2GroupDAO.upsertByName(oAuth2Group);

    assertThat(oAuth2Group.getId()).isNotNull();
    assertThat(oAuth2GroupDAO.getById(oAuth2Group.getId())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(oAuth2Group);
  }

  @Test
  public void testFindGroupsByNameQuery_Exact() {
    OAuth2Group oAuth2Group = tempEntity.newOAuth2Group("group");
    tempEntity.newOAuth2Group("other");

    assertThat(oAuth2GroupDAO.findGroupsByNameQuery("gRoUp")).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(oAuth2Group);
  }

  @Test
  public void testFindGroupsByNameQuery_Suffix() {
    OAuth2Group oAuth2Group1 = tempEntity.newOAuth2Group("1GROUP");
    OAuth2Group oAuth2Group2 = tempEntity.newOAuth2Group("2group");
    tempEntity.newOAuth2Group("other");

    assertThat(oAuth2GroupDAO.findGroupsByNameQuery("%gRoUp")).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(oAuth2Group1, oAuth2Group2);
  }

  @Test
  public void testFindGroupsByNameQuery_Prefix() {
    OAuth2Group oAuth2Group1 = tempEntity.newOAuth2Group("GROUP1");
    OAuth2Group oAuth2Group2 = tempEntity.newOAuth2Group("group2");
    tempEntity.newOAuth2Group("other");

    assertThat(oAuth2GroupDAO.findGroupsByNameQuery("gRoUp%")).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(oAuth2Group1, oAuth2Group2);
  }

  @Test
  public void testDeleteCascadesToOauth2UserGroups() {
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User();
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User();
    OAuth2Group oAuth2Group1 = tempEntity.newOAuth2Group();
    OAuth2Group oAuth2Group2 = tempEntity.newOAuth2Group();
    tempEntity.newOAuth2UserGroup(oAuth2User1.getId(), oAuth2Group1.getId());
    OAuth2UserGroup oAuth2UserGroup12 = tempEntity.newOAuth2UserGroup(oAuth2User1.getId(), oAuth2Group2.getId());
    tempEntity.newOAuth2UserGroup(oAuth2User2.getId(), oAuth2Group1.getId());
    OAuth2UserGroup oAuth2UserGroup22 = tempEntity.newOAuth2UserGroup(oAuth2User2.getId(), oAuth2Group2.getId());

    oAuth2GroupDAO.delete(oAuth2Group1);

    assertThat(oAuth2GroupDAO.getById(oAuth2Group1.getId())).isNull();
    assertThat(oAuth2UserGroupDAO.getAll()).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(oAuth2UserGroup12, oAuth2UserGroup22);
  }
}
