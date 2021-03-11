/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.api.experimental.dto.ApiFirewallComponentDTO;
import com.sonatype.insight.brain.api.experimental.dto.ApiFirewallQuarantineSummaryDTO;
import com.sonatype.insight.brain.api.experimental.dto.ApiFirewallReleaseQuarantineSummaryDTO;
import com.sonatype.insight.brain.api.experimental.dto.FirewallConfigurationDTO;
import com.sonatype.insight.brain.api.experimental.dto.ApiPageResult;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiPolicyViolationAdapter;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter;
import com.sonatype.insight.brain.dataaccess.repository.FirewallSortableField;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;

import static com.sonatype.insight.brain.model.repository.RepositoryContainer.REPOSITORY_CONTAINER_ID;

/**
 * @since 1.106.0
 */
@Named
public class ApiFirewallService
{
  static final int MIN_PAGE = 1;

  static final int MIN_PAGE_SIZE = 1;

  static final int MAX_PAGE_SIZE = 10000;

  private final InsightConfig insightConfig;

  private final PolicyMonitoringDAO policyMonitoringDAO;

  private final ProductLicense productLicense;

  private final RepositoryComponentDAO repositoryComponentDAO;

  private final RepositoryDAO repositoryDAO;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  private final ApiPolicyViolationAdapter apiPolicyViolationAdapter;

  @Inject
  public ApiFirewallService(
      final InsightConfig insightConfig,
      final PolicyMonitoringDAO policyMonitoringDAO,
      final ProductLicense productLicense,
      final RepositoryComponentDAO repositoryComponentDAO,
      final RepositoryDAO repositoryDAO,
      final ApiPolicyViolationAdapter apiPolicyViolationAdapter,
      final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO
  )
  {
    this.insightConfig = insightConfig;
    this.policyMonitoringDAO = policyMonitoringDAO;
    this.productLicense = productLicense;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.repositoryDAO = repositoryDAO;
    this.apiPolicyViolationAdapter = apiPolicyViolationAdapter;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
  }

  @Authorize(permission = Permission.READ)
  public FirewallConfigurationDTO getFirewallConfiguration() {
    checkExperimentalFeatureFlag();

    checkProductLicense();

    FirewallConfigurationDTO firewallConfigurationDTO = new FirewallConfigurationDTO();
    firewallConfigurationDTO.autoUnquarantineEnabled =
        null != policyMonitoringDAO.getByOwnerId(REPOSITORY_CONTAINER_ID);
    return firewallConfigurationDTO;
  }

  @Authorize(permission = Permission.WRITE)
  public FirewallConfigurationDTO setFirewallConfiguration(final FirewallConfigurationDTO firewallConfigurationDTO) {
    checkExperimentalFeatureFlag();

    checkProductLicense();

    final PolicyMonitoring existingPolicyMonitoring = policyMonitoringDAO.getByOwnerId(REPOSITORY_CONTAINER_ID);
    if (null != existingPolicyMonitoring && !firewallConfigurationDTO.autoUnquarantineEnabled) {
      executeWithAuditSession(() -> policyMonitoringDAO.delete(existingPolicyMonitoring));
    }
    else if (null == existingPolicyMonitoring && firewallConfigurationDTO.autoUnquarantineEnabled) {
      PolicyMonitoring policyMonitoring = new PolicyMonitoring();
      policyMonitoring.setStageTypeId(StageTypes.PROXY.getId());
      policyMonitoring.setOwnerId(REPOSITORY_CONTAINER_ID);
      executeWithAuditSession(() -> policyMonitoringDAO.insert(policyMonitoring));
    }
    return getFirewallConfiguration();
  }

  private void executeWithAuditSession(Runnable runnable) {
    try (AuditSession auditSession = AuditData.get()
        .recordSubEvent(AuditEvent.CONFIGURE_CONTINUOUS_MONITORING, false)) {
      AuditData.get().setOwner(new OwnerDAO().getById(REPOSITORY_CONTAINER_ID)).setStageId(StageTypes.PROXY.getId());
      runnable.run();
    }
  }

  @Authorize(permission = Permission.READ)
  public ApiFirewallQuarantineSummaryDTO getQuarantineSummary() {
    checkExperimentalFeatureFlag();
    checkProductLicense();

    ApiFirewallQuarantineSummaryDTO summary = new ApiFirewallQuarantineSummaryDTO();
    summary.repositoryCount = repositoryDAO.getCount();
    summary.quarantineEnabledRepositoryCount = repositoryDAO.getQuarantineEnabledCount();
    summary.quarantineEnabled = summary.quarantineEnabledRepositoryCount > 0;
    summary.totalComponentCount = repositoryComponentDAO.getCount();
    summary.quarantinedComponentCount = repositoryComponentDAO.getQuarantinedComponentCount();

    return summary;
  }

  private void checkExperimentalFeatureFlag() {
    if (!insightConfig.isExperimentalFeatureEnabled(Feature.FIREWALL_AUTO_UNQUARANTINE)) {
      throw new BadRequestException("Firewall experimental feature is not enabled.");
    }
  }

  private void checkProductLicense() {
    if (!productLicense.hasFeature(LicensedFeature.FIREWALL) ||
        !productLicense.hasFeature(LicensedFeature.RELEASE_INTEGRITY)) {
      throw new InvalidLicenseException();
    }
  }

  @Authorize(permission = Permission.READ)
  public ApiFirewallReleaseQuarantineSummaryDTO getReleaseQuarantineSummary() {
    checkExperimentalFeatureFlag();
    checkProductLicense();

    final Date startOfCurMonth =
        Date.from((LocalDate.now().withDayOfMonth(1)).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    final Date startOfCurYear =
        Date.from((LocalDate.now().withDayOfYear(1)).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

    final ApiFirewallReleaseQuarantineSummaryDTO
        apiFirewallReleaseQuarantineSummaryDTO = new ApiFirewallReleaseQuarantineSummaryDTO();

    apiFirewallReleaseQuarantineSummaryDTO.autoReleaseQuarantineCountMTD =
        repositoryComponentDAO.getAutoReleaseQuarantinedCountByDate(startOfCurMonth);

    apiFirewallReleaseQuarantineSummaryDTO.autoReleaseQuarantineCountYTD =
        repositoryComponentDAO.getAutoReleaseQuarantinedCountByDate(startOfCurYear);

    return apiFirewallReleaseQuarantineSummaryDTO;
  }

  @Authorize(permission = Permission.READ)
  public ApiPageResult<ApiFirewallComponentDTO> getUnquarantineList(FirewallRepositoryComponentFilter filter) {
    if (null == filter.sortableField) {
      filter.sortableField = FirewallSortableField.UNQUARANTINE_TIME.name();
    }

    // validate filter
    this.validateFirewallRepositoryComponentFilter(filter);
    // get a list of all auto-unquarantined repository components
    final List<RepositoryComponent> autoUnquarantinedComponents =
        repositoryComponentDAO.getFirewallRepositoryComponents(filter);

    // get a list of all unique repository ids
    Map<String, Repository> repositoryMap = toRepositoryMap(autoUnquarantinedComponents);

    final long total = repositoryComponentDAO.getTotalFirewallRepositoryComponents(filter);
    ApiPageResult<ApiFirewallComponentDTO> result = new ApiPageResult<>(total, filter.page, filter.pageSize);

    // for each component, attach policy violations for that component
    for (RepositoryComponent component : autoUnquarantinedComponents) {
      final ApiFirewallComponentDTO apiFirewallComponentDTO = new ApiFirewallComponentDTO();
      apiFirewallComponentDTO.displayName =
          ComponentDisplayNameUtil.fromIdentifier(component.getComponentIdentifier()).toString();
      apiFirewallComponentDTO.repository = repositoryMap.get(component.getRepositoryId()).getPublicId();
      apiFirewallComponentDTO.dateCleared = component.getUnquarantineTime();
      apiFirewallComponentDTO.quarantineDate = component.getQuarantineTime();

      // find and add all policy violations for this repository component
      List<RepositoryPolicyViolation> violations = repositoryPolicyViolationDAO
          .getByRepositoryIdAndPathname(component.getRepositoryId(), component.getPathname());
      for (RepositoryPolicyViolation policyViolation : violations) {
        final ApiPolicyViolationDTOV2 policyViolationDTO = apiPolicyViolationAdapter.convert(policyViolation);
        apiFirewallComponentDTO.policyViolations.add(policyViolationDTO);
      }

      result.getResults().add(apiFirewallComponentDTO);
    }

    return result;
  }

  private void validateFirewallRepositoryComponentFilter(final FirewallRepositoryComponentFilter filter) {
    if (filter.page < MIN_PAGE) {
      throw new BadRequestException("Invalid page: " + filter.page);
    }

    if (filter.pageSize < MIN_PAGE_SIZE || filter.pageSize > MAX_PAGE_SIZE) {
      throw new BadRequestException("Invalid page size: " + filter.pageSize);
    }

    if (filter.sortableField == null) {
      throw new BadRequestException("sortBy field is null");
    }

    try {
      FirewallSortableField.valueOf(filter.sortableField);
    }
    catch (IllegalArgumentException exception) {
      throw new BadRequestException("sortBy field is invalid");
    }
  }

  private Map<String, Repository> toRepositoryMap(final List<RepositoryComponent> components) {
    Map<String, Repository> repositoryMap = new HashMap<>();

    final Set<String> repositoryIds = components.stream()
        .map(RepositoryComponent::getRepositoryId).collect(Collectors.toSet());

    for (String repositoryId : repositoryIds) {
      final Repository repository = repositoryDAO.getById(repositoryId);
      repositoryMap.put(repositoryId, repository);
    }
    return repositoryMap;
  }
}
