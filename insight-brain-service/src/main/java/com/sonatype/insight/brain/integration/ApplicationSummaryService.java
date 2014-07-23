/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.application.ApplicationSummaryList;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.organization.ApplicationService;

@Named
public class ApplicationSummaryService
{
  private static final Comparator<Application> APP_COMPARATOR = new Comparator<Application>()
  {
    @Override
    public int compare(Application a1, Application a2) {
      return a1.getName().compareToIgnoreCase(a2.getName());
    }
  };

  private final ApplicationSummaryAdapter applicationAdapter;

  private final ApplicationService applicationService;

  @Inject
  public ApplicationSummaryService(final ApplicationSummaryAdapter applicationAdapter,
      final ApplicationService applicationService)
  {
    this.applicationAdapter = applicationAdapter;
    this.applicationService = applicationService;
  }

  public ApplicationSummaryList getApplications() {
    List<Application> apps = new ArrayList<>(applicationService.getApplications());
    Collections.sort(apps, APP_COMPARATOR);
    return applicationAdapter.convert(apps);
  }
}
