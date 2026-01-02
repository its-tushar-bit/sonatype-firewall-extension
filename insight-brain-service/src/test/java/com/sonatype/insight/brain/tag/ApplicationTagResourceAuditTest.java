/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tag;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.ApplicationCategoryAuditDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

@Category(SlowTest.class)
public class ApplicationTagResourceAuditTest
    extends AbstractAuditTest
{
  private Application application;

  @Before
  public void before() {
    application = tempEntity.newApplicationWithParent("appPubId", "appName");
  }

  @Test
  public void testUpdateApplicationTags() throws Exception {
    List<Tag> tags = new ArrayList<>();
    tags.add(tempEntity.newTag(application.getOrganizationId(), "tag name 1"));
    tags.add(tempEntity.newTag(application.getOrganizationId(), "tag name 2"));
    updateApplicationTags(null, tags);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_APPLICATION_CATEGORY, null);
    assertApplicationData(auditDTO, application);
    assertCustomObject(auditDTO, "applicationCategories", ApplicationCategoryAuditDTO.transcribe(tags));
  }

  @Test
  public void testUpdateApplicationTags_NoTags() throws Exception {
    updateApplicationTags(null, new ArrayList<>());

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_APPLICATION_CATEGORY, null);
    assertApplicationData(auditDTO, application);
    assertCustomObject(auditDTO, "applicationCategories", new ArrayList<>());
  }

  @Test
  public void testUpdateApplicationTags_Unauthorized() throws Exception {
    updateApplicationTags(unauthorizedUser(), new ArrayList<>());

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_APPLICATION_CATEGORY, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  private void updateApplicationTags(Consumer<HttpRequest> user, List<Tag> newTags) throws Exception {
    restRequest().with(user).path(ApplicationTagResource.RESOURCE_PATH).parameter(application.getPublicId())
        .body(newTags).put();
  }
}
