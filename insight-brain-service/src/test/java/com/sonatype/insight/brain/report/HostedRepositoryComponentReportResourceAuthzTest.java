/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.sonatype.insight.brain.common.test.SlowTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Authorization coverage for {@link HostedRepositoryComponentReportResource}.
 * <p>
 * Re-evaluation is a write operation on an individual component, so it is gated on
 * {@link Permission#EVALUATE_COMPONENT} — the same permission the hosted-scan branch of
 * {@link ReportResource#reevaluatePolicy} requires. {@link Permission#WRITE} ("Edit IQ Elements")
 * is a separately assignable permission that must not by itself authorize an evaluation.
 */
@Category(SlowTest.class)
public class HostedRepositoryComponentReportResourceAuthzTest
    extends AbstractResourceAuthzTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(HostedRepositoryComponentReportResource.RESOURCE_PATH);
  }

  @Test
  public void testReevaluatePolicy_requiresEvaluateComponentPermission() throws Exception {
    Repository repository = tempEntity.newRepository();
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);

    grantPermission(hrc.getId(), Permission.EVALUATE_COMPONENT);

    // The granted user reaches the handler; the unauthorized user is rejected with 403 before it.
    // A 404 from the granted user is an acceptable success status here: authorization passed and the
    // handler then found no policy_evaluation for this scanId. What matters is that it is not 403.
    HttpRequest request = restRequest().path(HostedRepositoryComponentReportResource.REEVALUATE_PATH)
        .parameter(hrc.getId(), "no-such-scan");
    testAuthzPost(request, 404);
  }

  @Test
  public void testReevaluatePolicy_writePermissionAloneIsNotSufficient() throws Exception {
    Repository repository = tempEntity.newRepository();
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);

    // WRITE is what the read handlers on this resource use. Granting only WRITE must not authorize
    // triggering an evaluation — that is the distinction this endpoint's @Authorize encodes.
    grantPermission(hrc.getId(), Permission.WRITE);

    HttpRequest request = restRequest().path(HostedRepositoryComponentReportResource.REEVALUATE_PATH)
        .parameter(hrc.getId(), "no-such-scan");
    assertThat(request.auth(authorized).post().getStatusCode()).isEqualTo(403);
  }
}
