/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class SourceControlPullRequestResultDAOTest
    extends AbstractDbDAOTest
{
  private SourceControlPullRequestResultDAO dao;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createSourceControlPullRequestResultDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    Application application = tempEntity.newApplicationWithParent();
    SourceControlPullRequestResult entity = new SourceControlPullRequestResult(application.getId(), "json");
    dao.insert(entity);
    assertThat(entity.getId()).isNotNull();

    // Read
    assertThat(dao.getById(entity.getId()))
        .usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(entity);

    // Update
    Application otherApplication = tempEntity.newApplicationWithParent();
    entity.setApplicationId(otherApplication.getId());
    entity.setPullRequestResultJson("otherJson");
    dao.update(entity);
    assertThat(dao.getById(entity.getId()))
        .usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(entity);

    // Delete
    dao.delete(entity);
    assertThat(dao.getById(entity.getId())).isNull();
  }

  @Test
  public void testGetAll() {
    Application application1 = tempEntity.newApplicationWithParent();
    Application application2 = tempEntity.newApplicationWithParent();
    SourceControlPullRequestResult entity1 =
        tempEntity.newSourceControlPullRequestResult(application1.getId(), "json1");
    SourceControlPullRequestResult entity2 =
        tempEntity.newSourceControlPullRequestResult(application2.getId(), "json2");

    assertThat(dao.getAll()).usingRecursiveFieldByFieldElementComparatorIgnoringFields(JPA.IGNORE_FIELDS)
        .containsExactlyInAnyOrder(entity1, entity2);
  }

  @Test
  public void testGetByApplicationId() {
    Application application = tempEntity.newApplicationWithParent();
    SourceControlPullRequestResult entity1 = tempEntity.newSourceControlPullRequestResult(application.getId(), "json1");
    SourceControlPullRequestResult entity2 = tempEntity.newSourceControlPullRequestResult(application.getId(), "json2");
    tempEntity.newSourceControlPullRequestResult(tempEntity.newApplicationWithParent().getId(), "json3");

    assertThat(dao.getByApplicationId(application.getId())).usingRecursiveFieldByFieldElementComparatorIgnoringFields(
        JPA.IGNORE_FIELDS).containsExactlyInAnyOrder(entity1, entity2);
  }

  @Test
  public void testDeleteAll() {
    Application application1 = tempEntity.newApplicationWithParent();
    Application application2 = tempEntity.newApplicationWithParent();
    SourceControlPullRequestResult entity1 =
        tempEntity.newSourceControlPullRequestResult(application1.getId(), "json1");
    SourceControlPullRequestResult entity2 =
        tempEntity.newSourceControlPullRequestResult(application2.getId(), "json2");

    dao.deleteAll();

    assertThat(dao.getById(entity1.getId())).isNull();
    assertThat(dao.getById(entity2.getId())).isNull();
  }
}
