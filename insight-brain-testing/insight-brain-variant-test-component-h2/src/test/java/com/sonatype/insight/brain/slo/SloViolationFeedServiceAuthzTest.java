/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.slo;

import java.util.Date;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Feature-flag gating (SLO_VIOLATION_FEED) is enforced at the REST tier via @HasFeature on
// SloViolationsRestResource, not in this service; the disabled-feature 404 is covered by
// SloViolationsRestResourceTest.featureFlagOff_returns404.
@ComponentH2Test
public class SloViolationFeedServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private SloViolationFeedService sloViolationFeedService;

  @Test
  public void getSloViolations_Unauthenticated() {
    assertThatThrownBy(
        () -> sloViolationFeedService.getSloViolations(app.getPublicId(), Stage.ID_RELEASE, null, null, 10))
            .isInstanceOf(UnauthenticatedException.class);
  }

  @Test
  public void getSloViolations_Unauthorized() {
    login();
    assertThatThrownBy(
        () -> sloViolationFeedService.getSloViolations(app.getPublicId(), Stage.ID_RELEASE, null, null, 10))
            .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void getSloViolations_Authorized_NoEvaluation_NotFound() {
    grantReadPermission(app.getId());
    assertThatThrownBy(
        () -> sloViolationFeedService.getSloViolations(app.getPublicId(), Stage.ID_RELEASE, null, null, 10))
            .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void getSloViolations_Authorized_InvalidPageSize_BadRequest() {
    grantReadPermission(app.getId());
    assertThatThrownBy(
        () -> sloViolationFeedService.getSloViolations(app.getPublicId(), Stage.ID_RELEASE, null, null, 0))
            .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void getSloViolations_Authorized_UnknownCursorWithUpdatedSince_NotRejectedAsBadRequest() {
    grantReadPermission(app.getId());
    // With the frozen keyset design the cursor row need not still exist: an unknown afterViolationId is just an opaque
    // (updatedSince, id) position, not a 400. Here it advances past cursor validation and surfaces NotFound only
    // because no evaluation is seeded — proving the cursor itself is accepted rather than rejected.
    assertThatThrownBy(
        () -> sloViolationFeedService.getSloViolations(
            app.getPublicId(), Stage.ID_RELEASE, new Date(), "does-not-exist", 10))
                .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void getSloViolations_Authorized_CursorIdWithoutUpdatedSince_BadRequest() {
    grantReadPermission(app.getId());
    // afterViolationId is only the tiebreaker; without updatedSince there is no time component to continue from.
    assertThatThrownBy(
        () -> sloViolationFeedService.getSloViolations(
            app.getPublicId(), Stage.ID_RELEASE, null, "some-id", 10))
                .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void getSloViolations_Authorized_BlankCursorId_TreatedAsFirstPage_NotFoundNotBadRequest() {
    grantReadPermission(app.getId());
    // A blank afterViolationId is normalized to "first page"; with no evaluation seeded the request proceeds past
    // cursor validation and surfaces NotFound (not a cursor BadRequest, and not a silent page).
    assertThatThrownBy(
        () -> sloViolationFeedService.getSloViolations(app.getPublicId(), Stage.ID_RELEASE, null, "   ", 10))
            .isInstanceOf(NotFoundException.class);
  }
}
