/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.tag.ApplicationTagDAO;
import com.sonatype.insight.brain.model.tag.ApplicationTagData;
import com.sonatype.insight.brain.model.tag.ApplicationTagNameDTO;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class ApplicationCategoryTelemetryCollector
    extends PaginatedTelemetryCollectorImpl
{
  private static final Logger log = LoggerFactory.getLogger(ApplicationCategoryTelemetryCollector.class);

  private static final int PAGE_SIZE = 25_000;

  private final ApplicationTagDAO applicationTagDAO;

  @Inject
  public ApplicationCategoryTelemetryCollector(ApplicationTagDAO applicationTagDAO) {
    super(TelemetryPurpose.APPLICATION_CATEGORY, PAGE_SIZE);
    this.applicationTagDAO = applicationTagDAO;
  }

  @Override
  protected List<?> collectData(int pageNumber) {
    return getApplicationTagsForPage(pageNumber);
  }

  private List<ApplicationTagData> getApplicationTagsForPage(int pageNumber) {
    List<ApplicationTagNameDTO> applicationTagPage =
        applicationTagDAO.getPaginatedApplicationIdsWithTags(pageNumber, getPageSize());

    Map<String, List<String>> applicationTagsMap = new HashMap<>();
    addRecordsToMap(applicationTagPage, applicationTagsMap);

    // This mapping makes processing easier on the data lake side
    List<ApplicationTagData> applicationTagData = applicationTagsMap.entrySet()
        .stream()
        .map(entry -> new ApplicationTagData(entry.getKey(), entry.getValue()))
        .toList();

    log.trace("Collected {} application tags for page {} and aggregated them into {} entries",
        applicationTagPage.size(),
        pageNumber,
        applicationTagData.size());

    return applicationTagData;
  }

  private void addRecordsToMap(
      List<ApplicationTagNameDTO> currentPageApplicationsWithTags,
      Map<String, List<String>> applicationTagsMap)
  {
    for (ApplicationTagNameDTO applicationTagNameDTO : currentPageApplicationsWithTags) {
      applicationTagsMap.computeIfAbsent(applicationTagNameDTO.applicationId(), k -> new ArrayList<>())
          .add(applicationTagNameDTO.tagName());
    }
  }

  @Override
  public boolean isClusterTelemetry() {
    return true;
  }
}
