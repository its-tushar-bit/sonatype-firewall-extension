/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@ComponentH2Test
public class SourceControlUserActivityServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private SourceControlUserActivityService sourceControlUserActivityService;

  @Test
  public void testSaveRepoUserList_Unauthorized() {
    login();

    Map<String, Collection<Instant>> activityToSave = new HashMap<>();
    activityToSave.put("key@email.com", Collections.singletonList(Instant.now()));
    Assertions.assertThrows(UnauthorizedException.class,
        () -> sourceControlUserActivityService.saveRepoUserList(app.getPublicId(), activityToSave));
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
