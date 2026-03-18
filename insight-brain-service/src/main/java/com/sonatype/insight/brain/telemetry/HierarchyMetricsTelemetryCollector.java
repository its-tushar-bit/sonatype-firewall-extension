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

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.apache.commons.math3.stat.descriptive.rank.Percentile;

/**
 * @since 1.52
 */
@Named
@Singleton
public class HierarchyMetricsTelemetryCollector
    implements TelemetryCollector
{
  private final ApplicationDAO applicationDAO;

  private final OrganizationDAO organizationDAO;

  private final RepositoryDAO repositoryDAO;

  private final RepositoryComponentDAO repositoryComponentDAO;

  private final ApplicationComponentDAO applicationComponentDAO;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  public static final String NUMBER_OF_ORGS = "number_of_orgs";

  public static final String NUMBER_OF_APPS = "number_of_apps";

  public static final String NUMBER_OF_APP_COMPONENTS = "number_of_app_components";

  public static final String NUMBER_OF_APP_COMPONENT_VIOLATIONS = "number_of_app_component_violations";

  public static final String NUMBER_OF_APP_EVALUATIONS = "number_of_app_evaluations";

  public static final String NUMBER_OF_REPOS = "number_of_repos";

  public static final String NUMBER_OF_REPO_COMPONENTS = "number_of_repo_components";

  public static final String NUMBER_OF_REPO_COMPONENT_VIOLATIONS = "number_of_repo_component_violations";

  public static final String MIN_APPS_PER_ORG = "min_apps_per_org";

  public static final String MAX_APPS_PER_ORG = "max_apps_per_org";

  public static final String P90_APPS_PER_ORG = "p90_apps_per_org";

  @Inject
  public HierarchyMetricsTelemetryCollector(
      ApplicationDAO applicationDAO,
      OrganizationDAO organizationDAO,
      RepositoryDAO repositoryDAO,
      RepositoryComponentDAO repositoryComponentDAO,
      ApplicationComponentDAO applicationComponentDAO,
      RepositoryPolicyViolationDAO repositoryPolicyViolationDAO,
      PolicyViolationDAO policyViolationDAO,
      PolicyEvaluationDAO policyEvaluationDAO)
  {
    this.applicationDAO = applicationDAO;
    this.organizationDAO = organizationDAO;
    this.repositoryDAO = repositoryDAO;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.applicationComponentDAO = applicationComponentDAO;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
  }

  @Override
  public boolean isClusterTelemetry() {
    return true;
  }

  @Override
  public TelemetryData collectData() {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.HIERARCHY_METRICS);
    Map<String, Object> attributes = telemetryData.getAttributes();

    int numberOfOrgsMinusRoot = organizationDAO.getAll().size() - 1;
    attributes.put(NUMBER_OF_ORGS, String.valueOf(numberOfOrgsMinusRoot));

    List<Application> applications = applicationDAO.getAll();
    attributes.put(NUMBER_OF_APPS, String.valueOf(applications.size()));

    // Row count of various tables
    long appComponentsCount = applicationComponentDAO.getCount();
    long appViolationsCount = policyViolationDAO.getCount();
    long appEvaluationsCount = policyEvaluationDAO.getCount();
    long repoCount = repositoryDAO.getCount();
    long repoComponentsCount = repositoryComponentDAO.getCount();
    long repoViolationsCount = repositoryPolicyViolationDAO.getCount();

    attributes.put(NUMBER_OF_APP_COMPONENTS, appComponentsCount);
    attributes.put(NUMBER_OF_APP_COMPONENT_VIOLATIONS, appViolationsCount);
    attributes.put(NUMBER_OF_APP_EVALUATIONS, appEvaluationsCount);
    attributes.put(NUMBER_OF_REPOS, repoCount);
    attributes.put(NUMBER_OF_REPO_COMPONENTS, repoComponentsCount);
    attributes.put(NUMBER_OF_REPO_COMPONENT_VIOLATIONS, repoViolationsCount);

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
        applications.stream()
            .collect(Collectors.groupingBy(Application::getOrganizationId, Collectors.counting()))
            .values());
    // Add zeros for Organizations with no Applications
    for (int i = appsPerOrg.size(); i < numberOfOrgsMinusRoot; i++) {
      appsPerOrg.add(0L);
    }
    return appsPerOrg;
  }
}
