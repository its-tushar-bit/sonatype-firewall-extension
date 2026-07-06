/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.slo;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import jakarta.inject.Inject;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Feature-flag gating (SLO_VIOLATION_FEED) is enforced at the REST tier via @HasFeature on
// SloViolationsRestResource, not in this service; the disabled-feature 404 is covered by
// SloViolationsRestResourceTest.featureFlagOff_returns404.
public class SloViolationFeedServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private SloViolationFeedService sloViolationFeedService;

  @Test
  public void getSloViolations_Unauthenticated() {
    assertThatThrownBy(
        () -> sloViolationFeedService.getSloViolations(app.getPublicId(), Stage.ID_RELEASE, null, 1, 10))
            .isInstanceOf(UnauthenticatedException.class);
  }

  @Test
  public void getSloViolations_Unauthorized() {
    login();
    assertThatThrownBy(
        () -> sloViolationFeedService.getSloViolations(app.getPublicId(), Stage.ID_RELEASE, null, 1, 10))
            .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void getSloViolations_Authorized_NoEvaluation_NotFound() {
    grantReadPermission(app.getId());
    assertThatThrownBy(
        () -> sloViolationFeedService.getSloViolations(app.getPublicId(), Stage.ID_RELEASE, null, 1, 10))
            .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void getSloViolations_Authorized_InvalidPage_BadRequest() {
    grantReadPermission(app.getId());
    assertThatThrownBy(
        () -> sloViolationFeedService.getSloViolations(app.getPublicId(), Stage.ID_RELEASE, null, 0, 10))
            .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void getSloViolations_Authorized_InvalidPageSize_BadRequest() {
    grantReadPermission(app.getId());
    assertThatThrownBy(
        () -> sloViolationFeedService.getSloViolations(app.getPublicId(), Stage.ID_RELEASE, null, 1, 0))
            .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void getSloViolations_Authorized_PageAboveMax_BadRequest() {
    grantReadPermission(app.getId());
    // Guards against int overflow of the (page - 1) * pageSize offset; must be a clean 400, not a SQL error.
    assertThatThrownBy(
        () -> sloViolationFeedService.getSloViolations(
            app.getPublicId(), Stage.ID_RELEASE, null, Integer.MAX_VALUE, SloViolationFeedService.MAX_PAGE_SIZE))
                .isInstanceOf(BadRequestException.class);
  }
}
