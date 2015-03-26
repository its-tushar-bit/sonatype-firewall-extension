/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
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
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.AuthzFilter;

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

  private final ApplicationDAO applicationDAO;

  @Inject
  public ApplicationSummaryService(final ApplicationSummaryAdapter applicationAdapter,
      final ApplicationDAO applicationDAO)
  {
    this.applicationAdapter = applicationAdapter;
    this.applicationDAO = applicationDAO;
  }

  public ApplicationSummaryList getApplications(Goal goal) {
    if (goal == null) {
      goal = Goal.EVALUATE_COMPONENT;
    }

    List<Application> result;
    switch (goal) {
      case EVALUATE_APPLICATION:
        result = getApplicationsForEvaluateApplication();
        break;
      default:
        result = getApplicationsForRead();
        break;
    }
    return toApplicationSummaryList(result);
  }

  @AuthzFilter(permission = Permission.READ, context = AuthzFilter.Context.APPLICATION, anonymousAllowed = true)
  protected List<Application> getApplicationsForRead() {
    return applicationDAO.getAll();
  }

  @AuthzFilter(permission = Permission.EVALUATE_APPLICATION, context = AuthzFilter.Context.APPLICATION, anonymousAllowed = true)
  protected List<Application> getApplicationsForEvaluateApplication() {
    return applicationDAO.getAll();
  }

  private ApplicationSummaryList toApplicationSummaryList(List<Application> apps) {
    // The input list may be immutable
    apps = new ArrayList<>(apps);
    Collections.sort(apps, APP_COMPARATOR);
    return applicationAdapter.convert(apps);
  }
}
