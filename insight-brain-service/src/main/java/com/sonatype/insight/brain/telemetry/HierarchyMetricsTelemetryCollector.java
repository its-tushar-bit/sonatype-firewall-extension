/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
import com.sonatype.insight.brain.db.H2DatabaseUtil;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.52
 */
@Named
@Singleton
public class HierarchyMetricsTelemetryCollector
    implements TelemetryCollector
{
  private static final Logger log = LoggerFactory.getLogger(HierarchyMetricsTelemetryCollector.class);

  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  public static final String NUMBER_OF_ORGS = "number_of_orgs";

  public static final String NUMBER_OF_APPS = "number_of_apps";

  public static final String MIN_APPS_PER_ORG = "min_apps_per_org";

  public static final String MAX_APPS_PER_ORG = "max_apps_per_org";

  public static final String P90_APPS_PER_ORG = "p90_apps_per_org";

  public static final String ODS_SIZE_BYTES = "ods_size_bytes";

  @Inject
  public HierarchyMetricsTelemetryCollector(ApplicationDAO applicationDAO,
                                            OrganizationDAO organizationDAO)
  {
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
  }

  @Override
  public TelemetryData collectData() {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.HIERARCHY_METRICS);
    Map<String, Object> attributes = telemetryData.getAttributes();

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
    attributes.put(ODS_SIZE_BYTES, getOdsSizeBytes());
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

  private String getOdsSizeBytes() {
    try {
      if (!OperationalDataStoreProvider.isDatabaseInMemory()) {
        return String.valueOf(Files.size(
            Paths.get(H2DatabaseUtil.getDatabasePath(OperationalDataStoreProvider.getDatabaseConfig()) + ".h2.db")));
      }
    }
    catch (IOException e) {
      log.warn(e.getMessage(), e);
    }
    return null;
  }
}
