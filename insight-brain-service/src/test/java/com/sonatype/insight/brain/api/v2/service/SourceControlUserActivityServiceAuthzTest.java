/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

@Category(SlowTest.class)
public class SourceControlUserActivityServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private SourceControlUserActivityService sourceControlUserActivityService;

  @Test(expected = UnauthorizedException.class)
  public void testSaveRepoUserList_Unauthorized() {
    login();

    Map<String, Collection<Instant>> activityToSave = new HashMap<>();
    activityToSave.put("key@email.com", Collections.singletonList(Instant.now()));
    sourceControlUserActivityService.saveRepoUserList(app.getPublicId(), activityToSave);
  }

  @Test
  public void testSaveRepoUserList() {
    login();
    grantEvaluateApplicationPermission(app.getId());

    Map<String, Collection<Instant>> activityToSave = new HashMap<>();
    activityToSave.put("key@email.com", Collections.singletonList(Instant.now()));
    sourceControlUserActivityService.saveRepoUserList(app.getPublicId(), activityToSave);
  }
}
