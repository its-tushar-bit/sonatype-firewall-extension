/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.organization.ApplicationService;

import com.google.common.collect.Collections2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

@Named
public class ComponentSummaryService
{
  private static final Logger log = LoggerFactory.getLogger(ComponentSummaryService.class);

  private final ApplicationComponentDAO applicationComponentDAO;

  private final ApplicationService applicationService;

  private final DashboardUtils dashboardUtils;

  @Inject
  public ComponentSummaryService(ApplicationComponentDAO applicationComponentDAO,
      ApplicationService applicationService, DashboardUtils dashboardUtils)
  {
    this.applicationComponentDAO = applicationComponentDAO;
    this.applicationService = applicationService;
    this.dashboardUtils = dashboardUtils;
  }

  /**
   * Calculates how the non-proprietary components within the matched applications and stages are distributed across the
   * various match states.
   */
  public ComponentSummaryDTO getComponentSummary(Set<String> applicationIds, Set<String> stageIds, Set<String> tagIds) {
    dashboardUtils.validateDashboardLicensed();

    long start = System.currentTimeMillis();

    ComponentSummaryDTO summary = new ComponentSummaryDTO();

    Collection<String> appIds = Collections2.transform(
        applicationService.getApplicationsByIdsAndTagIds(applicationIds, tagIds), DashboardUtils.hasIdIdSelector);
    log.debug("getComponentSummary: Found {} applications filtered by appIds={} and tagIds={} in {} ms.",
        appIds.size(), !isEmpty(applicationIds), !isEmpty(tagIds), System.currentTimeMillis() - start);

    Collection<String> stageTypeIds = dashboardUtils.getStageIds(dashboardUtils.getStageTypes(stageIds));
    List<ApplicationComponent> components = applicationComponentDAO.getNonProprietaryByApplicationIdsAndStageTypeIds(
        appIds, stageTypeIds);
    Map<String, ApplicationComponent> componentsByHash = new HashMap<>();
    for (ApplicationComponent component : components) {
      String hash = component.getHash();
      ApplicationComponent other = componentsByHash.get(hash);
      if (other == null || other.getTime().getTime() < component.getTime().getTime()) {
        componentsByHash.put(hash, component);
      }
    }

    summary.total = componentsByHash.size();
    for (ApplicationComponent component : componentsByHash.values()) {
      String matchState = component.getMatchStateId();
      if (MatchState.EXACT.getId().equals(matchState)) {
        summary.exact++;
      }
      else if (MatchState.SIMILAR.getId().equals(matchState)) {
        summary.similar++;
      }
      else if (MatchState.UNKNOWN.getId().equals(matchState)) {
        summary.unknown++;
      }
      else {
        throw new IllegalStateException("unknown match state: " + matchState);
      }
    }

    log.debug("Calculated component summary in {} ms", System.currentTimeMillis() - start);

    return summary;
  }
}
