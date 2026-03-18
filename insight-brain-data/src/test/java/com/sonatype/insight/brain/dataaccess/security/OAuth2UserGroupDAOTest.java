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

public class OAuth2UserGroupDAOTest
    extends AbstractDbDAOTest
{
  private OAuth2UserGroupDAO oAuth2UserGroupDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    oAuth2UserGroupDAO = daoFactory.createOAuth2UserGroupDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User();
    OAuth2Group oAuth2Group1 = tempEntity.newOAuth2Group();
    OAuth2UserGroup oAuth2UserGroup = new OAuth2UserGroup(oAuth2User1.getId(), oAuth2Group1.getId());
    oAuth2UserGroupDAO.insert(oAuth2UserGroup);
    assertThat(oAuth2UserGroup.getId()).isNotNull();

    // Read
    OAuth2UserGroup storedOAuth2UserGroup = oAuth2UserGroupDAO.getById(oAuth2UserGroup.getId());
    assertThat(storedOAuth2UserGroup).isNotNull();
    assertThat(storedOAuth2UserGroup).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(oAuth2UserGroup);

    // Update
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User();
    OAuth2Group oAuth2Group2 = tempEntity.newOAuth2Group();
    oAuth2UserGroup.setOAuth2UserId(oAuth2User2.getId());
    oAuth2UserGroup.setOAuth2GroupId(oAuth2Group2.getId());
    oAuth2UserGroupDAO.update(oAuth2UserGroup);
    storedOAuth2UserGroup = oAuth2UserGroupDAO.getById(storedOAuth2UserGroup.getId());
    assertThat(storedOAuth2UserGroup).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(oAuth2UserGroup);

    // Delete
    oAuth2UserGroupDAO.delete(oAuth2UserGroup);
    assertThat(oAuth2UserGroupDAO.getById(oAuth2UserGroup.getId())).isNull();
  }

  @Test
  public void testGetAll() {
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User();
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User();
    OAuth2Group oAuth2Group1 = tempEntity.newOAuth2Group();
    OAuth2Group oAuth2Group2 = tempEntity.newOAuth2Group();
    OAuth2UserGroup oAuth2UserGroup11 = tempEntity.newOAuth2UserGroup(oAuth2User1.getId(), oAuth2Group1.getId());
    OAuth2UserGroup oAuth2UserGroup22 = tempEntity.newOAuth2UserGroup(oAuth2User2.getId(), oAuth2Group2.getId());

    assertThat(oAuth2UserGroupDAO.getAll()).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(oAuth2UserGroup11, oAuth2UserGroup22);
  }

  @Test
  public void testGetByOauth2UserIdAndOauth2GroupId() {
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User();
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User();
    OAuth2Group oAuth2Group1 = tempEntity.newOAuth2Group();
    OAuth2Group oAuth2Group2 = tempEntity.newOAuth2Group();
    OAuth2UserGroup oAuth2UserGroup11 = tempEntity.newOAuth2UserGroup(oAuth2User1.getId(), oAuth2Group1.getId());
    tempEntity.newOAuth2UserGroup(oAuth2User1.getId(), oAuth2Group2.getId());
    tempEntity.newOAuth2UserGroup(oAuth2User2.getId(), oAuth2Group1.getId());
    tempEntity.newOAuth2UserGroup(oAuth2User2.getId(), oAuth2Group2.getId());

    assertThat(oAuth2UserGroupDAO.getByOAuth2UserIdAndSamlGroupId(oAuth2User1.getId(),
        oAuth2Group1.getId())).usingRecursiveComparison().isEqualTo(oAuth2UserGroup11);
  }

  @Test
  public void testGetByOauth2UserId() {
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User();
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User();
    OAuth2Group oAuth2Group1 = tempEntity.newOAuth2Group();
    OAuth2Group oAuth2Group2 = tempEntity.newOAuth2Group();
    OAuth2Group oAuth2Group3 = tempEntity.newOAuth2Group();
    tempEntity.newOAuth2User();
    tempEntity.newOAuth2Group();
    OAuth2UserGroup oAuth2UserGroup11 = tempEntity.newOAuth2UserGroup(oAuth2User1.getId(), oAuth2Group1.getId());
    tempEntity.newOAuth2UserGroup(oAuth2User2.getId(), oAuth2Group2.getId());
    OAuth2UserGroup oAuth2UserGroup13 = tempEntity.newOAuth2UserGroup(oAuth2User1.getId(), oAuth2Group3.getId());
    tempEntity.newOAuth2UserGroup(oAuth2User2.getId(), oAuth2Group3.getId());

    assertThat(oAuth2UserGroupDAO.getByOAuth2UserId(oAuth2User1.getId())).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(oAuth2UserGroup11, oAuth2UserGroup13);
  }

  @Test
  public void testGetByOauth2GroupId() {
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User();
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User();
    OAuth2Group oAuth2Group1 = tempEntity.newOAuth2Group();
    OAuth2Group oAuth2Group2 = tempEntity.newOAuth2Group();
    OAuth2Group oAuth2Group3 = tempEntity.newOAuth2Group();
    tempEntity.newOAuth2User();
    tempEntity.newOAuth2Group();
    tempEntity.newOAuth2UserGroup(oAuth2User1.getId(), oAuth2Group1.getId());
    tempEntity.newOAuth2UserGroup(oAuth2User2.getId(), oAuth2Group2.getId());
    OAuth2UserGroup oAuth2UserGroup13 = tempEntity.newOAuth2UserGroup(oAuth2User1.getId(), oAuth2Group3.getId());
    OAuth2UserGroup oAuth2UserGroup23 = tempEntity.newOAuth2UserGroup(oAuth2User2.getId(), oAuth2Group3.getId());

    assertThat(
        oAuth2UserGroupDAO.getByOAuth2GroupId(oAuth2Group3.getId())).usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(oAuth2UserGroup13, oAuth2UserGroup23);
  }

  @Test
  public void testUpsertByOauth2UserIdAndOauth2GroupId_Insert() {
    OAuth2User oAuth2User = tempEntity.newOAuth2User();
    OAuth2Group oAuth2Group = tempEntity.newOAuth2Group();
    OAuth2UserGroup oAuth2UserGroup = new OAuth2UserGroup(oAuth2User.getId(), oAuth2Group.getId());

    oAuth2UserGroupDAO.upsertByOAuth2UserIdAndOAuth2GroupId(oAuth2UserGroup);

    assertThat(oAuth2UserGroup.getId()).isNotNull();
    assertThat(oAuth2UserGroupDAO.getById(oAuth2UserGroup.getId())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(oAuth2UserGroup);
  }

  @Test
  public void testUpsertByOauth2UserIdAndOauth2GroupId_Update() {
    OAuth2User oAuth2User = tempEntity.newOAuth2User();
    OAuth2Group oAuth2Group = tempEntity.newOAuth2Group();
    tempEntity.newOAuth2UserGroup(oAuth2User.getId(), oAuth2Group.getId());
    OAuth2UserGroup oAuth2UserGroup = new OAuth2UserGroup(oAuth2User.getId(), oAuth2Group.getId());

    oAuth2UserGroupDAO.upsertByOAuth2UserIdAndOAuth2GroupId(oAuth2UserGroup);

    assertThat(oAuth2UserGroup.getId()).isNotNull();
    assertThat(oAuth2UserGroupDAO.getById(oAuth2UserGroup.getId())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(oAuth2UserGroup);
  }

  @Test
  public void testDeleteByOauth2UserId() {
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User();
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User();
    OAuth2Group oAuth2Group1 = tempEntity.newOAuth2Group();
    OAuth2Group oAuth2Group2 = tempEntity.newOAuth2Group();
    OAuth2Group oAuth2Group3 = tempEntity.newOAuth2Group();
    tempEntity.newOAuth2User();
    tempEntity.newOAuth2Group();
    tempEntity.newOAuth2UserGroup(oAuth2User1.getId(), oAuth2Group1.getId());
    OAuth2UserGroup oAuth2UserGroup22 = tempEntity.newOAuth2UserGroup(oAuth2User2.getId(), oAuth2Group2.getId());
    tempEntity.newOAuth2UserGroup(oAuth2User1.getId(), oAuth2Group3.getId());
    OAuth2UserGroup oAuth2UserGroup23 = tempEntity.newOAuth2UserGroup(oAuth2User2.getId(), oAuth2Group3.getId());

    oAuth2UserGroupDAO.deleteByOAuth2UserId(oAuth2User1.getId());
    assertThat(oAuth2UserGroupDAO.getAll()).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(oAuth2UserGroup22, oAuth2UserGroup23);
  }

  @Test
  public void testDeleteByOauth2GroupId() {
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User();
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User();
    OAuth2Group oAuth2Group1 = tempEntity.newOAuth2Group();
    OAuth2Group oAuth2Group2 = tempEntity.newOAuth2Group();
    OAuth2Group oAuth2Group3 = tempEntity.newOAuth2Group();
    tempEntity.newOAuth2User();
    tempEntity.newOAuth2Group();
    OAuth2UserGroup oAuth2UserGroup11 = tempEntity.newOAuth2UserGroup(oAuth2User1.getId(), oAuth2Group1.getId());
    OAuth2UserGroup oAuth2UserGroup22 = tempEntity.newOAuth2UserGroup(oAuth2User2.getId(), oAuth2Group2.getId());
    tempEntity.newOAuth2UserGroup(oAuth2User1.getId(), oAuth2Group3.getId());
    tempEntity.newOAuth2UserGroup(oAuth2User2.getId(), oAuth2Group3.getId());

    oAuth2UserGroupDAO.deleteByOAuth2GroupId(oAuth2Group3.getId());
    assertThat(oAuth2UserGroupDAO.getAll()).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(oAuth2UserGroup11, oAuth2UserGroup22);
  }

  @Test
  public void testDeleteByOauth2UserIdAndGroupIds_Empty() {
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User();
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User();
    OAuth2Group oAuth2Group1 = tempEntity.newOAuth2Group();
    OAuth2Group oAuth2Group2 = tempEntity.newOAuth2Group();
    OAuth2UserGroup oAuth2UserGroup11 = tempEntity.newOAuth2UserGroup(oAuth2User1.getId(), oAuth2Group1.getId());
    OAuth2UserGroup oAuth2UserGroup22 = tempEntity.newOAuth2UserGroup(oAuth2User2.getId(), oAuth2Group2.getId());

    oAuth2UserGroupDAO.deleteByOAuth2UserIdAndGroupIds(oAuth2User1.getId(), Collections.emptySet());

    assertThat(oAuth2UserGroupDAO.getAll()).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(oAuth2UserGroup11, oAuth2UserGroup22);
  }

  @Test
  public void testDeleteByOauth2UserIdAndGroupIds() {
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User();
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User();
    OAuth2Group oAuth2Group1 = tempEntity.newOAuth2Group();
    OAuth2Group oAuth2Group2 = tempEntity.newOAuth2Group();
    OAuth2Group oAuth2Group3 = tempEntity.newOAuth2Group();
    tempEntity.newOAuth2UserGroup(oAuth2User1.getId(), oAuth2Group1.getId());
    tempEntity.newOAuth2UserGroup(oAuth2User1.getId(), oAuth2Group2.getId());
    OAuth2UserGroup oAuth2UserGroup13 = tempEntity.newOAuth2UserGroup(oAuth2User1.getId(), oAuth2Group3.getId());
    OAuth2UserGroup oAuth2UserGroup21 = tempEntity.newOAuth2UserGroup(oAuth2User2.getId(), oAuth2Group1.getId());
    OAuth2UserGroup oAuth2UserGroup22 = tempEntity.newOAuth2UserGroup(oAuth2User2.getId(), oAuth2Group2.getId());
    OAuth2UserGroup oAuth2UserGroup23 = tempEntity.newOAuth2UserGroup(oAuth2User2.getId(), oAuth2Group3.getId());

    oAuth2UserGroupDAO.deleteByOAuth2UserIdAndGroupIds(oAuth2User1.getId(),
        new HashSet<>(Arrays.asList(oAuth2Group1.getId(), oAuth2Group2.getId())));

    assertThat(oAuth2UserGroupDAO.getAll()).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(oAuth2UserGroup13, oAuth2UserGroup21, oAuth2UserGroup22, oAuth2UserGroup23);
  }
}
