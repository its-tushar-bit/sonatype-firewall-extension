/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import static java.util.stream.Collectors.toList;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.dataaccess.TransactionContext;

import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentPathnames;
import com.sonatype.clm.dto.model.repository.RepositoryType;
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
import com.sonatype.insight.brain.repository.RequestSafeComponentsMetricEventService;
import com.sonatype.insight.brain.repository.component.DbQuarantinedComponentAccessManager;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.telemetry.RepositoryComponentTelemetryCreator;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.license.model.LicensedFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.17.0
 */
@Named("integrationRepositoryService")
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

    boolean quarantineEnabled = componentEvaluationDataRequestList != null &&
        componentEvaluationDataRequestList.quarantineEnabled;
    return evaluateComponents(repository, repositoryManagerInstanceId, componentEvaluationDataRequestList,
        quarantineEnabled, false, clientUserAgent);
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
      organizationApplicationManagementEventService.postEventForFirewall();
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

  public HostedRepositoryListDTO getConfiguredRepositories(
      String repositoryManagerInstanceId,
      String searchText,
      String format,
      String sortBy,
      String sortDir,
      Integer page,
      Integer pageSize)
  {
    // Firewall license is required because hosted repositories are a Firewall feature.
    checkLicenseFeature();
    RepositoryManager repositoryManager = getRepositoryManagerDAO().getByInstanceIdNotNull(repositoryManagerInstanceId);
    return getConfiguredRepositories(repositoryManager, searchText, format, sortBy, sortDir, page, pageSize);
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  HostedRepositoryListDTO getConfiguredRepositories(
      @AuthzContext(Key.REPOSITORY_MANAGER) RepositoryManager repositoryManager,
      String searchText,
      String format,
      String sortBy,
      String sortDir,
      Integer page,
      Integer pageSize)
  {
    List<Repository> repositories;
    int totalCount;
    Map<String, Date> lastScanTimes;
    Set<String> repositoriesWithQueuedScans;
    try (TransactionContext tx = repositoryDAO.createTransactionContext()) {
      totalCount = repositoryDAO.countFilteredHostedRepositories(tx, repositoryManager.getId(), searchText, format);
      repositories = repositoryDAO.getFilteredHostedRepositories(
          tx, repositoryManager.getId(), searchText, format, sortBy, sortDir, page, pageSize);
      List<String> repositoryIds = repositories.stream()
          .map(Repository::getId)
          .collect(Collectors.toList());
      lastScanTimes = repositoryComponentDAO.getLastScanTimesByRepositoryIds(tx, repositoryIds);
      repositoriesWithQueuedScans = repositoryComponentDAO.getRepositoryIdsWithQueuedScans(tx, repositoryIds);
    }

    List<HostedRepositoryDTO> dtos = repositories.stream()
        .map(repo -> {
          HostedRepositoryDTO dto = new HostedRepositoryDTO();
          dto.id = repo.getId();
          dto.publicId = repo.getPublicId();
          dto.name = repo.getName();
          dto.format = repo.getFormat();
          dto.type = repo.getRepositoryType();
          dto.auditEnabled = repo.isAuditEnabled();
          dto.quarantineEnabled = repo.isQuarantineEnabled();
          dto.policyCompliantComponentSelectionEnabled = repo.isPolicyCompliantComponentSelectionEnabled();
          dto.namespaceConfusionProtectionEnabled = repo.isNamespaceConfusionProtectionEnabled();
          Date lastScanDate = lastScanTimes.get(repo.getId());
          dto.lastScannedTime = lastScanDate != null ? lastScanDate.getTime() : null;
          dto.hasQueuedScans = repositoriesWithQueuedScans.contains(repo.getId());
          return dto;
        })
        .collect(Collectors.toList());

    // lastScannedTime is derived from REPOSITORY_COMPONENT, not a column on REPOSITORY,
    // so it cannot be sorted at the DB layer. Sort in Java after populating the field.
    if ("lastscannedtime".equalsIgnoreCase(sortBy)) {
      Comparator<HostedRepositoryDTO> byLastScanned;
      if ("desc".equalsIgnoreCase(sortDir)) {
        byLastScanned =
            Comparator.comparing(dto -> dto.lastScannedTime, Comparator.nullsLast(Comparator.reverseOrder()));
      }
      else {
        byLastScanned =
            Comparator.comparing(dto -> dto.lastScannedTime, Comparator.nullsLast(Comparator.naturalOrder()));
      }
      dtos.sort(byLastScanned);
    }

    HostedRepositoryListDTO.ManagerInfo managerInfo = new HostedRepositoryListDTO.ManagerInfo();
    managerInfo.name = repositoryManager.getRawName();
    managerInfo.instanceId = repositoryManager.getInstanceId();
    managerInfo.baseUrl = repositoryManager.getBaseUrl();

    HostedRepositoryListDTO result = new HostedRepositoryListDTO();
    result.manager = managerInfo;
    result.repositories = dtos;
    result.totalCount = totalCount;
    return result;
  }

  /**
   * @since 1.169
   */
  public List<String> getAvailableFormats(String repositoryManagerInstanceId) {
    checkLicenseFeature();
    RepositoryManager repositoryManager = getRepositoryManagerDAO().getByInstanceIdNotNull(repositoryManagerInstanceId);
    return getAvailableFormats(repositoryManager);
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  List<String> getAvailableFormats(@AuthzContext(Key.REPOSITORY_MANAGER) RepositoryManager repositoryManager) {
    List<Repository> repositories =
        repositoryDAO.getByRepositoryManagerIdAndRepositoryType(repositoryManager.getId(), RepositoryType.hosted);
    return repositories.stream()
        .filter(Repository::isMonitoringEnabled)
        .map(Repository::getFormat)
        .filter(Objects::nonNull)
        .distinct()
        .sorted()
        .collect(Collectors.toList());
  }

  public boolean isMalwareWaived(String repositoryManagerInstanceId, String repositoryPublicId, String pathname) {
    checkLicenseFeature();
    if (pathname == null || pathname.isBlank()) {
      return false;
    }
    Repository repository = repositoryDAO.getByRepositoryManagerInstanceIdAndPublicId(
        repositoryManagerInstanceId, repositoryPublicId);
    if (repository == null) {
      return false;
    }
    return isMalwareWaived(repository, pathname);
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  boolean isMalwareWaived(@AuthzContext(Key.REPOSITORY) Repository repository, String pathname) {
    return repositoryPolicyViolationDAO.hasActiveMalwareWaivedViolation(repository.getId(), pathname);
  }
}
