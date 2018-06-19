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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.db.H2DatabaseUtil;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.Application;

import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.43.0
 */
@Named
@Singleton
public class TelemetryCollector
{
  private static final Logger log = LoggerFactory.getLogger(TelemetryCollector.class);
  
  private static final String UNKNOWN = "unknown";

  private static final Set<String> ALL_FORMATS = new HashSet<>(Arrays
      .asList(UNKNOWN, ComponentIdentifier.FORMAT_MAVEN, ComponentIdentifier.FORMAT_NPM,
          ComponentIdentifier.FORMAT_NUGET, ComponentIdentifier.FORMAT_ANAME, ComponentIdentifier.FORMAT_PYPI,
          ComponentIdentifier.FORMAT_RPM, ComponentIdentifier.FORMAT_RUBYGEMS));

  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final ApplicationComponentDAO applicationComponentDAO;

  public static final String NUMBER_OF_ORGS = "number_of_orgs";

  public static final String NUMBER_OF_APPS = "number_of_apps";

  public static final String MIN_APPS_PER_ORG = "min_apps_per_org";

  public static final String MAX_APPS_PER_ORG = "max_apps_per_org";

  public static final String P90_APPS_PER_ORG = "p90_apps_per_org";

  public static final String ODS_SIZE_BYTES = "ods_size_bytes";

  public static final String NUMBER_OF_COMPONENTS = "number_of_components";

  @Inject
  public TelemetryCollector(ApplicationDAO applicationDAO,
                            OrganizationDAO organizationDAO,
                            ApplicationComponentDAO applicationComponentDAO)
  {
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
    this.applicationComponentDAO = applicationComponentDAO;
  }

  public TelemetryData collectData() {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.HIERARCHY_METRICS, System.currentTimeMillis());
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

  public TelemetryData collectComponentCountsData() {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.COMPONENT_COUNTS, System.currentTimeMillis());
    Map<String, Object> attributes = telemetryData.getAttributes();
    int totalCount = 0;
    for (String format : ALL_FORMATS) {
      int count = applicationComponentDAO.getCountByComponentIdFormat(format.equals(UNKNOWN) ? null : format);
      totalCount += count;
      attributes.put("number_of_" + format.replace("-", "") + "_components", String.valueOf(count));
    }
    attributes.put(NUMBER_OF_COMPONENTS, String.valueOf(totalCount));
    return telemetryData;
  }
}
