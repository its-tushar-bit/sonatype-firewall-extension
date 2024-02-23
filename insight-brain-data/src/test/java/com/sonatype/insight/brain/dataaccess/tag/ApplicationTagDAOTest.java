/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.tag;

import java.util.ArrayList;
import java.util.Collections;
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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @since 1.9
 */
public class ApplicationTagDAOTest
    extends AbstractDbDAOTest
{
  private ApplicationTagDAO dao;

  private Tag tag;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createApplicationTagDAO();
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

  @Test
  public void testGetByApplicationIds() {
    ApplicationTag appTag = tempEntity.newApplicationTag(application.getId(), tag.getId());
    Application application1 = tempEntity.newApplicationWithParent();
    tempEntity.newApplicationTag(application1.getId(), tag.getId());
    List<ApplicationTag> result = dao.getByApplicationIds(Collections.singletonList(application.getId()));
    assertThat(result).usingRecursiveFieldByFieldElementComparator().containsExactly(appTag);
  }

  @Test
  public void testGetByApplicationIds_Batched() {
    List<String> appIds = new ArrayList<>();
    List<ApplicationTag> appTags = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      Application app = tempEntity.newApplicationWithParent();
      appIds.add(app.getId());
      appTags.add(tempEntity.newApplicationTag(app.getId(), tag.getId()));
    }

    dao = spy(dao);
    when(dao.getInOperatorThreshold()).thenReturn(2);

    List<ApplicationTag> result = dao.getByApplicationIds(appIds);
    assertThat(result).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(appTags.toArray(new ApplicationTag[0]));
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
