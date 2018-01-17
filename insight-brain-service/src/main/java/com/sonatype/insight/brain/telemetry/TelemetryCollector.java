/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;

import org.apache.commons.math3.stat.descriptive.rank.Percentile;

/**
 * @since 1.43.0
 */
@Named
@Singleton
public class TelemetryCollector
{
  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  static final String NUMBER_OF_ORGS = "number_of_orgs";

  static final String NUMBER_OF_APPS = "number_of_apps";

  static final String MIN_APPS_PER_ORG = "min_apps_per_org";

  static final String MAX_APPS_PER_ORG = "max_apps_per_org";

  static final String P90_APPS_PER_ORG = "p90_apps_per_org";

  @Inject
  public TelemetryCollector(ApplicationDAO applicationDAO, OrganizationDAO organizationDAO) {
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
  }

  public TelemetryData collectAppsAndOrgs() {
    TelemetryData telemetryData = new TelemetryData(System.currentTimeMillis());
    Map<String, String> attributes = telemetryData.getAttributes();

    int numberOfOrgsMinusRoot = organizationDAO.getAll().size() - 1;
    attributes.put(NUMBER_OF_ORGS, String.valueOf(numberOfOrgsMinusRoot));
    List<Application> applications = applicationDAO.getAll();
    attributes.put(NUMBER_OF_APPS, String.valueOf(applications.size()));

    if (applications.size() > 0) {
      long minAppsPerOrg = Long.MAX_VALUE;
      long maxAppsPerOrg = 0;
      Collection<Long> numberOfAppsPerOrg = countAppsPerOrg(applications, numberOfOrgsMinusRoot);
      for (Long appsPerOrg : numberOfAppsPerOrg) {
        minAppsPerOrg = Math.min(minAppsPerOrg, appsPerOrg);
        maxAppsPerOrg = Math.max(maxAppsPerOrg, appsPerOrg);
      }

      attributes.put(MIN_APPS_PER_ORG, String.valueOf(minAppsPerOrg));
      attributes.put(MAX_APPS_PER_ORG, String.valueOf(maxAppsPerOrg));
      Percentile percentile = new Percentile();
      percentile.setData(numberOfAppsPerOrg.stream().mapToDouble(d -> d).toArray());
      attributes.put(P90_APPS_PER_ORG, String.valueOf(percentile.evaluate(90)));
    }
    else {
      attributes.put(MIN_APPS_PER_ORG, "0");
      attributes.put(MAX_APPS_PER_ORG, "0");
      attributes.put(P90_APPS_PER_ORG, "0");
    }
    return telemetryData;
  }

  private Collection<Long> countAppsPerOrg(List<Application> applications, int numberOfOrgsMinusRoot) {
    Collection<Long> appsPerOrg = new ArrayList<>();
    appsPerOrg.addAll(
        applications.stream().collect(Collectors.groupingBy(Application::getOrganizationId, Collectors.counting()))
            .values());
    // Add zeros for Organizations with no Applications
    for (int i = appsPerOrg.size(); i < numberOfOrgsMinusRoot; i++) {
      appsPerOrg.add(0L);
    }
    return appsPerOrg;
  }
}
