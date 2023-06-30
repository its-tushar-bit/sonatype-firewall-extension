/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.organization;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;

@Named
public class ApplicationSourceControlService
{
  private final ApplicationDAO applicationDAO;

  private final SourceControlDAO sourceControlDAO;

  private final SourceControlUtils sourceControlUtils;

  @Inject
  public ApplicationSourceControlService(
      final ApplicationDAO applicationDAO,
      final SourceControlDAO sourceControlDAO,
      final SourceControlUtils sourceControlUtils)
  {
    this.applicationDAO = applicationDAO;
    this.sourceControlDAO = sourceControlDAO;
    this.sourceControlUtils = sourceControlUtils;
  }

  public List<Application> getApplicationsWithAutomatedSourceControlFeedbackDisabled() {
    final List<Application> scmEnabledApps = new ArrayList<>();
    final List<Application> scmDisabledApps = new ArrayList<>();
    final List<Application> applications = applicationDAO.getAll();
    applications.forEach(app -> {
      final boolean isScmEnabled = sourceControlUtils.isScmEnabled(app.getId());
      if (isScmEnabled) {
        scmEnabledApps.add(app);
      }
      else {
        scmDisabledApps.add(app);
      }
    });

    if (applications.size() == scmDisabledApps.size()) {
      return scmDisabledApps;
    }

    final List<Application> appsWithAutomatedSourceControlFeedbackDisabled =
        sourceControlDAO.getApplicationsWithAutomatedSourceControlFeedbackDisabled(scmEnabledApps);
    appsWithAutomatedSourceControlFeedbackDisabled.addAll(scmDisabledApps);
    return appsWithAutomatedSourceControlFeedbackDisabled;
  }
}
