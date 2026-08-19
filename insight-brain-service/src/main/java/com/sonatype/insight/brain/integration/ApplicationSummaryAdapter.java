/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.sonatype.clm.dto.model.application.ApplicationSummary;
import com.sonatype.clm.dto.model.application.ApplicationSummaryList;
import com.sonatype.insight.brain.model.Application;

/**
 * Adapter class to translate between Application entity objects and ApplicationSummary objects
 *
 * @since 1.11.0
 */
class ApplicationSummaryAdapter
{
  static ApplicationSummaryList convert(Collection<Application> applications) {
    ApplicationSummaryList applicationSummaryList = new ApplicationSummaryList();
    List<ApplicationSummary> applicationSummaries = new ArrayList<>();
    applicationSummaryList.setApplicationSummaries(applicationSummaries);

    if (applications != null) {
      for (Application application : applications) {
        ApplicationSummary applicationSummary = convert(application);
        applicationSummaries.add(applicationSummary);
      }
    }

    return applicationSummaryList;
  }

  static ApplicationSummary convert(Application application) {
    if (application == null) {
      return null;
    }
    ApplicationSummary applicationDTO = new ApplicationSummary();
    applicationDTO.setId(application.getId());
    applicationDTO.setPublicId(application.getPublicId());
    applicationDTO.setName(application.getName());
    return applicationDTO;
  }
}
