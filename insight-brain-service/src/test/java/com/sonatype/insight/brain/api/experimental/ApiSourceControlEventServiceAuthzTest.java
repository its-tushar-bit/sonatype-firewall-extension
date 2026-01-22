/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.Collections;
import java.util.Set;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;

import org.junit.Test;

@Category(SlowTest.class)
public class ApiSourceControlEventServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiSourceControlEventService apiSourceControlEventService;

  @Test(expected = UnauthenticatedException.class)
  public void testGetApiSourceControlEvents_Unauthenticated() {
    ApiSourceControlEventFilterDTO filter = new ApiSourceControlEventFilterDTO();
    apiSourceControlEventService.getApiSourceControlEventData(OwnerType.APPLICATION, app.getId(), filter);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApiSourceControlEvents_Unauthorized() {
    login();
    ApiSourceControlEventFilterDTO filter = new ApiSourceControlEventFilterDTO();
    apiSourceControlEventService.getApiSourceControlEventData(OwnerType.APPLICATION, app.getId(), filter);
  }

  @Test
  public void testGetApiSourceControlEvents() {
    grantReadPermission(app.getId());
    Set<String> appIds = Collections.singleton(app.getId());
    ApiSourceControlEventFilterDTO filter =
        new ApiSourceControlEventFilterDTO(appIds, System.currentTimeMillis(), true, 1, 0);
    apiSourceControlEventService.getApiSourceControlEventData(OwnerType.APPLICATION, app.getId(), filter);
  }
}
