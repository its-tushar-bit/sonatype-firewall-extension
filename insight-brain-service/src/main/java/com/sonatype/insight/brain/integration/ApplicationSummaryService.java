/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
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
import com.sonatype.insight.brain.dataaccess.configuration.AutomaticApplicationsConfigurationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.error.exception.BadRequestException;

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

  private final AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO;

  @Inject
  public ApplicationSummaryService(final ApplicationSummaryAdapter applicationAdapter,
                                   final ApplicationDAO applicationDAO,
                                   final AutomaticApplicationsConfigurationDAO automaticApplicationsConfigurationDAO)
  {
    this.applicationAdapter = applicationAdapter;
    this.applicationDAO = applicationDAO;
    this.automaticApplicationsConfigurationDAO = automaticApplicationsConfigurationDAO;
  }

  public ApplicationSummaryList getApplications(Goal goal) {
    return toApplicationSummaryList(getApplicationsForGoal(goal));
  }

  private List<Application> getApplicationsForGoal(Goal goal) {
    if (goal == null) {
      // For back compatibility only
      return getApplicationsForRead();
    }
    switch (goal) {
      case EVALUATE_APPLICATION:
        return getApplicationsForEvaluateApplication();
      case EVALUATE_COMPONENT:
        return getApplicationsForEvaluateComponent();
      default:
        return getApplicationsForRead();
    }
  }

  @AuthzFilter(permission = Permission.READ, context = AuthzFilter.Context.APPLICATION, anonymousAllowed = true)
  protected List<Application> getApplicationsForRead() {
    return applicationDAO.getAll();
  }

  /**
   * @since 1.14.0
   */
  @AuthzFilter(permission = Permission.EVALUATE_APPLICATION, context = AuthzFilter.Context.APPLICATION, anonymousAllowed = true)
  protected List<Application> getApplicationsForEvaluateApplication() {
    return applicationDAO.getAll();
  }

  /**
   * @since 1.14.0
   */
  @AuthzFilter(permission = Permission.EVALUATE_COMPONENT, context = AuthzFilter.Context.APPLICATION)
  protected List<Application> getApplicationsForEvaluateComponent() {
    return applicationDAO.getAll();
  }

  private ApplicationSummaryList toApplicationSummaryList(List<Application> apps) {
    // The input list may be immutable
    apps = new ArrayList<>(apps);
    Collections.sort(apps, APP_COMPARATOR);
    return applicationAdapter.convert(apps);
  }

  /**
   * Verifies if the user can access the application identified by applicationPublicId for the specified goal.
   * If an application with the specified applicationPublicId already exists, then the method checks access for the
   * current user and the specified goal to that application.
   * If such an application does not exist and automatic application creation is enabled, then the method returns true
   * (the application may be created automatically at a later time when a scan is uploaded for that application public
   * ID).
   * 
   * @since 1.45
   */
  boolean isApplicationAllowed(String applicationPublicId, Goal goal) {
    if (goal == null) {
      throw new BadRequestException("A goal must be specified");
    }

    if (applicationDAO.getByPublicId(applicationPublicId) != null) {
      // An application with the specified public ID exists.
      for (Application app : getApplicationsForGoal(goal)) {
        if (app.getPublicId().equals(applicationPublicId)) {
          return true;
        }
      }
      return false;
    }

    // An application with the specified public ID does not exist.
    return automaticApplicationsConfigurationDAO.isEnabled();
  }
}
