/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.license.LicenseThreatGroupResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupCount;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * REST parity tests for the counts endpoint at {@code GET /rest/licenseThreatGroup/{type}/{id}/counts}
 * (CLM-39702). Focuses on HTTP contract (200/404, JSON shape) and correct plumbing through
 * {@code LicenseThreatGroupService} to the DAO; semantic correctness of the aggregation is covered by
 * {@code LicenseThreatGroupDAOTest}. Authorization is covered by {@code LicenseThreatGroupServiceAuthzTest}.
 */
@IqH2Test
class IqH2LicenseThreatGroupCountsResourceTest
{
  private IqTestContext ctx;

  private HttpRequest countsRequest(String ownerType, String ownerPublicId) {
    return ctx.restRequest()
        .path(LicenseThreatGroupResource.RESOURCE_PATH)
        .parameter(ownerType, ownerPublicId)
        .path("counts");
  }

  @Test
  void testCounts_Application_200_ReturnsJsonArrayOfExpectedShape() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent("counts-app");
    // Unique name avoids collision with LTGs seeded at the root org by LicenseThreatGroupDataHelper.
    LicenseThreatGroup ltg =
        ctx.tempEntity().newLicenseThreatGroup(application.getId(), "CLM-39702-Counts-Banned", 10, "GPL-2.0");

    HttpResponse response = countsRequest("application", application.getPublicId()).get();

    ctx.assertResponseStatus(200, response);
    LicenseThreatGroupCount[] counts = response.getBody(LicenseThreatGroupCount[].class);
    assertThat(counts).isNotNull();
    assertThat(counts).anySatisfy(c -> {
      assertThat(c.getLicenseThreatGroupId()).isEqualTo(ltg.getId());
      assertThat(c.getLicenseThreatGroupName()).isEqualTo("CLM-39702-Counts-Banned");
      assertThat(c.getThreatLevel()).isEqualTo(10);
      assertThat(c.getUnreviewedComponentCount()).isZero();
    });
  }

  @Test
  void testCounts_Organization_200_ReturnsJsonArray() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization();

    HttpResponse response = countsRequest("organization", organization.getId()).get();

    ctx.assertResponseStatus(200, response);
    assertThat(response.getBody(LicenseThreatGroupCount[].class)).isNotNull();
  }

  @Test
  void testCounts_UnknownApplication_404() throws Exception {
    HttpResponse response = countsRequest("application", "does-not-exist").get();

    ctx.assertResponseStatus(404, response);
  }

  @Test
  void testCounts_UnknownOrganization_404() throws Exception {
    HttpResponse response = countsRequest("organization", "does-not-exist").get();

    ctx.assertResponseStatus(404, response);
  }
}
