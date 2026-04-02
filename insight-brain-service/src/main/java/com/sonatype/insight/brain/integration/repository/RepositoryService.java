/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentPathnames;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.container.images.ContainerImageReportService;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.hds.FirewallQuarantineHdsClient;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.policy.violation.PolicyViolationLoggerFactory;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.repository.ProprietaryComponentNameDetector;
import com.sonatype.insight.brain.repository.RepositoryComponentDeleteService;
import com.sonatype.insight.brain.repository.RepositoryPolicyEvaluator;
import com.sonatype.insight.brain.repository.component.DbQuarantinedComponentAccessManager;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetryCreator;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.repository.RequestSafeComponentsMetricEventService;
import com.sonatype.insight.license.model.LicensedFeature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;

/**
 * @since 1.17.0
 */
@Named
@Singleton
public class RepositoryService
    extends AbstractRepositoryService
{
  private static final Logger log = LoggerFactory.getLogger(RepositoryService.class);

  private final RepositoryComponentDeleteService repositoryComponentDeleteService;

  private final com.sonatype.insight.brain.webhook.OrganizationApplicationManagementEventService organizationApplicationManagementEventService;

  @Inject
  public RepositoryService(
      RepositoryPolicyEvaluator repositoryPolicyEvaluator,
      ProprietaryComponentNameDetector proprietaryComponentNameDetector,
      ProductLicense productLicense,
      PolicyViolationLoggerFactory policyViolationLoggerFactory,
      RepositoryComponentTelemetryCreator repositoryComponentTelemetryCreator,
      DbQuarantinedComponentAccessManager quarantinedComponentAccessManager,
      FirewallQuarantineHdsClient quarantineHdsClient,
      ApplicationDAO applicationDAO,
      ApplicationService applicationService,
      RepositoryComponentDeleteService repositoryComponentDeleteService,
      TelemetrySender telemetrySender,
      RepositoryManagerDAO repositoryManagerDAO,
      RepositoryDAO repositoryDAO,
      RepositoryComponentDAO repositoryComponentDAO,
      RepositoryPolicyViolationDAO repositoryPolicyViolationDAO,
      FirewallIgnorePatternService firewallIgnorePatternService,
      RequestSafeComponentsMetricEventService requestSafeComponentsMetricEventService,
      com.sonatype.insight.brain.repository.RepositoryService mainRepositoryService,
      ContainerImageReportService containerImageReportService,
      final com.sonatype.insight.brain.webhook.OrganizationApplicationManagementEventService organizationApplicationManagementEventService)
  {
    super(repositoryPolicyEvaluator, proprietaryComponentNameDetector, productLicense, policyViolationLoggerFactory,
        LicensedFeature.FIREWALL, repositoryComponentTelemetryCreator, quarantinedComponentAccessManager,
        quarantineHdsClient, applicationDAO, applicationService, telemetrySender, repositoryManagerDAO, repositoryDAO,
        repositoryComponentDAO, repositoryPolicyViolationDAO, firewallIgnorePatternService,
        requestSafeComponentsMetricEventService, mainRepositoryService, containerImageReportService);
    this.repositoryComponentDeleteService = repositoryComponentDeleteService;
    this.organizationApplicationManagementEventService = organizationApplicationManagementEventService;
  }

  /**
   * Called from NXRM for npm audit. Maybe other usages?
   *
   * @since 1.89
   */
  public RepositoryComponentEvaluationDataList evaluateComponentsAdhoc(
      String repositoryManagerInstanceId,
      String repositoryPublicId,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
      String clientUserAgent)
  {
    checkLicenseFeature();
    auditRepoComponentEvalList(componentEvaluationDataRequestList);
    Repository repository = getOrCreateRepository(repositoryManagerInstanceId, repositoryPublicId);

    log.debug("Evaluating components for repository {}:{} ({})", repositoryManagerInstanceId,
        repositoryPublicId, repository.getId());

    return evaluateComponents(repository, repositoryManagerInstanceId, componentEvaluationDataRequestList,
        false, false, clientUserAgent);
  }

  private Repository getOrCreateRepository(String repositoryManagerInstanceId, String repositoryPublicId) {
    Repository repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(
        repositoryManagerInstanceId, repositoryPublicId);

    if (repository == null) {
      repository = new Repository(null, repositoryPublicId);
      RepositoryManager repositoryManager = getOrCreateRepositoryManager(repositoryManagerInstanceId);
      repository.setRepositoryManagerId(repositoryManager.getId());
      repository.setAuditEnabled(false);
      repositoryDAO.insert(repository);
      organizationApplicationManagementEventService.postEvent();
    }
    AuditData.get().setRepository(repository);

    return repository;
  }

  /**
   * Removes all components from the given repository that have paths not in the given pathname list and with timestamp
   * before or equal to the given timestamp.
   *
   * @param repositoryComponentPathnames the pathname list and timestamp used to filter the components to be deleted.
   *
   * @since 1.137
   */
  void removeExtraComponents(
      String repositoryManagerInstanceId,
      String repositoryPublicId,
      RepositoryComponentPathnames repositoryComponentPathnames)
  {
    checkLicenseFeature();

    log.debug("Removing extra components for repository {}:{}.", repositoryManagerInstanceId, repositoryPublicId);

    Repository repository = repositoryDAO
        .getByRepositoryManagerInstanceIdAndPublicIdNotNull(repositoryManagerInstanceId, repositoryPublicId);

    long start = System.currentTimeMillis();
    int removedComponentCount = removeExtraComponents(repository, repositoryComponentPathnames);

    log.info("Removed {} extra components for repository {}:{} ({}) in {} ms.", removedComponentCount,
        repositoryManagerInstanceId, repositoryPublicId, repository.getId(), System.currentTimeMillis() - start);
  }

  /**
   * Removes all components from the given repository that have paths not in the given pathname list and with timestamp
   * before or equal to the given timestamp.
   *
   * @param repositoryComponentPathnames the pathname list and timestamp used to filter the components to be deleted.
   *
   * @since 1.137
   */
  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  int removeExtraComponents(
      @AuthzContext(Key.REPOSITORY) Repository repository,
      RepositoryComponentPathnames repositoryComponentPathnames)
  {
    long start = System.currentTimeMillis();
    List<RepositoryComponent> allComponents = repositoryComponentDAO.getByRepositoryId(repository.getId());

    // Find all components that are not in the input list of pathnames
    // and were added before the input timestamp.
    List<RepositoryComponent> extraComponents = allComponents.stream() //
        .filter(component -> !repositoryComponentPathnames.pathnames.contains(component.getPathname())) //
        .filter(component -> component.getTime().getTime() <= repositoryComponentPathnames.time.getTime()) //
        .collect(toList());
    log.debug("Retrieved {} components to be deleted in {} ms.", extraComponents.size(),
        System.currentTimeMillis() - start);

    extraComponents.forEach(repositoryComponentDeleteService::deleteComponent);

    return extraComponents.size();
  }
}
