/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class ApplicationServiceAuthzTest
    extends AbstractServiceAuthzTest
{

  @Inject
  private ApplicationService applicationService;

  @Test
  public void testGetApplicationsWithReadPermission() {
    grantReadPermission(app.getId());
    Application newApp = tempEntity.newApplication(org.getId());

    List<Application> applications = applicationService.getApplicationsWithReadPermission();

    Assert.assertThat(applications, hasSize(1));
    Assert.assertThat(app.getId(), equalTo(applications.get(0).getId()));

    grantReadPermission(newApp.getId());
    applications = applicationService.getApplicationsWithReadPermission();
    Assert.assertThat(applications, hasSize(2));
  }

}
