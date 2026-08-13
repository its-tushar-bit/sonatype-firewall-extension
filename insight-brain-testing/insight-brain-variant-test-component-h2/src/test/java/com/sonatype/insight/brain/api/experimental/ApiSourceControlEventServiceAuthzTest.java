/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.util.Collections;
import java.util.Set;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ComponentH2Test
public class ApiSourceControlEventServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ApiSourceControlEventService apiSourceControlEventService;

  @Test
  public void testGetApiSourceControlEvents_Unauthenticated() {
    ApiSourceControlEventFilterDTO filter = new ApiSourceControlEventFilterDTO();
    assertThatExceptionOfType(UnauthenticatedException.class)
        .isThrownBy(() -> apiSourceControlEventService.getApiSourceControlEventData(OwnerType.APPLICATION,
            app.getId(), filter));
  }

  @Test
  public void testGetApiSourceControlEvents_Unauthorized() {
    login();
    ApiSourceControlEventFilterDTO filter = new ApiSourceControlEventFilterDTO();
    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> apiSourceControlEventService.getApiSourceControlEventData(OwnerType.APPLICATION,
            app.getId(), filter));
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
