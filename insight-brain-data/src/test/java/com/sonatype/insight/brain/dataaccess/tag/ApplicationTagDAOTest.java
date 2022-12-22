/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.tag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.Tag;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.9
 */
public class ApplicationTagDAOTest
    extends AbstractDbDAOTest
{
  private ApplicationTagDAO dao = new ApplicationTagDAO();

  private Tag tag;

  @Before
  public void before() {
    tag = tempEntity.newTag(organization.getId());
  }

  @Test
  public void testCRUD() {
    // Create
    ApplicationTag appTag = new ApplicationTag(application.getId(), tag.getId());
    dao.insert(appTag);
    assertThat(appTag.getId()).isNotNull();

    // Get
    appTag = dao.getById(appTag.getId());
    assertThat(appTag).isNotNull();
    assertAppTag(application.getId(), tag.getId(), appTag);

    // Update
    Tag newTag = tempEntity.newTag(organization.getId());
    appTag.setTagId(newTag.getId());
    dao.update(appTag);
    appTag = dao.getById(appTag.getId());
    assertThat(appTag).isNotNull();
    assertAppTag(application.getId(), newTag.getId(), appTag);

    // Delete
    dao.delete(appTag);

    // Get
    appTag = dao.getById(appTag.getId());
    assertThat(appTag).isNull();
  }

  @Test
  public void testGetByApplicationId() {
    Application app1 = tempEntity.newApplication(organization.getId());
    Application app2 = tempEntity.newApplication(organization.getId());

    List<Tag> app1Tags = new ArrayList<>();
    List<Tag> app2Tags = new ArrayList<>();

    app1Tags.add(tempEntity.newTag(organization.getId()));
    app1Tags.add(tempEntity.newTag(organization.getId()));
    app2Tags.add(tempEntity.newTag(organization.getId()));
    app2Tags.add(tempEntity.newTag(organization.getId()));

    for (Tag tag : app1Tags) {
      tempEntity.newApplicationTag(app1.getId(), tag.getId());
    }

    for (Tag tag : app2Tags) {
      tempEntity.newApplicationTag(app2.getId(), tag.getId());
    }

    assertAppTags(app1.getId(), app1Tags, dao.getByApplicationId(app1.getId()));
    assertAppTags(app2.getId(), app2Tags, dao.getByApplicationId(app2.getId()));
  }

  @Test
  public void testGetByApplicationIdAndTagId() {
    tempEntity.newApplicationTag(application.getId(), tag.getId());
    ApplicationTag appTag = dao.getByApplicationIdAndTagId(application.getId(), tag.getId());
    assertAppTag(application.getId(), tag.getId(), appTag);
  }

  private void assertAppTag(String appId, String tagId, ApplicationTag actual) {
    assertThat(actual.getApplicationId()).isEqualTo(appId);
    assertThat(actual.getTagId()).isEqualTo(tagId);
  }

  private void assertAppTags(String appId, List<Tag> expected, List<ApplicationTag> actual) {
    assertThat(actual).hasSameSizeAs(expected);

    Set<String> tagIds = new HashSet<>();
    for (Tag tag : expected) {
      tagIds.add(tag.getId());
    }

    for (ApplicationTag appTag : actual) {
      assertThat(appTag.getApplicationId()).isEqualTo(appId);
      assertThat(appTag.getTagId()).isIn(tagIds);
    }
  }
}
