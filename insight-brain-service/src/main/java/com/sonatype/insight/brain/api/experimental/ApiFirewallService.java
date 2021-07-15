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
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.api.experimental.dto.ApiFirewallComponentDTO;
import com.sonatype.insight.brain.api.experimental.dto.ApiFirewallQuarantineSummaryDTO;
import com.sonatype.insight.brain.api.experimental.dto.ApiFirewallReleaseQuarantineConfigDTO;
import com.sonatype.insight.brain.api.experimental.dto.ApiFirewallReleaseQuarantineSummaryDTO;
import com.sonatype.insight.brain.api.experimental.dto.ApiPageResult;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiPolicyViolationAdapter;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoUnquarantinePolicyConditionTypeDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter;
import com.sonatype.insight.brain.dataaccess.repository.FirewallSortableField;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.policy.AutoUnquarantinePolicyConditionType;
import com.sonatype.insight.brain.model.policy.ConditionType;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.telemetry.AutoReleaseQuarantineTelemetry;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

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

  static final String AUTO_RELEASE_QUARANTINE_CONFIG_TELEMETRY = "auto_release_quarantine_config";

  private final ProductLicense productLicense;

  private final RepositoryComponentDAO repositoryComponentDAO;

  private final RepositoryDAO repositoryDAO;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  private final AutoUnquarantinePolicyConditionTypeDAO autoUnquarantinePolicyConditionTypeDAO;

  private final ApiPolicyViolationAdapter apiPolicyViolationAdapter;

  private final TelemetrySender telemetrySender;

  @Inject
  public ApiFirewallService(
      final ProductLicense productLicense,
      final RepositoryComponentDAO repositoryComponentDAO,
      final RepositoryDAO repositoryDAO,
      final ApiPolicyViolationAdapter apiPolicyViolationAdapter,
      final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO,
      final AutoUnquarantinePolicyConditionTypeDAO autoUnquarantinePolicyConditionTypeDAO,
      final TelemetrySender telemetrySender)
  {
    this.productLicense = productLicense;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.repositoryDAO = repositoryDAO;
    this.apiPolicyViolationAdapter = apiPolicyViolationAdapter;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
    this.autoUnquarantinePolicyConditionTypeDAO = autoUnquarantinePolicyConditionTypeDAO;
    this.telemetrySender = telemetrySender;
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
    checkProductLicense();

    ApiFirewallQuarantineSummaryDTO summary = new ApiFirewallQuarantineSummaryDTO();
    summary.repositoryCount = repositoryDAO.getCount();
    summary.quarantineEnabledRepositoryCount = repositoryDAO.getQuarantineEnabledCount();
    summary.quarantineEnabled = summary.quarantineEnabledRepositoryCount > 0;
    summary.totalComponentCount = repositoryComponentDAO.getCount();
    summary.quarantinedComponentCount = repositoryComponentDAO.getQuarantinedComponentCount();

    return summary;
  }

  @Authorize(permission = Permission.READ)
  public List<ApiFirewallReleaseQuarantineConfigDTO> getReleaseQuarantineConfig() {
    checkProductLicense();

    final Set<String> enabledPolicyConditionTypes = getAutoUnquarantineEnabledPolicyConditionTypesIds();

    return ConditionTypes.getAllWithAutoUnquarantineSupported().stream()
        .map(conditionType -> generateReleaseQuarantineConfigDto(enabledPolicyConditionTypes, conditionType))
        .collect(Collectors.toList());
  }

  @Authorize(permission = Permission.WRITE)
  public List<ApiFirewallReleaseQuarantineConfigDTO> setReleaseQuarantineConfig(
      final List<ApiFirewallReleaseQuarantineConfigDTO> apiFirewallReleaseQuarantineConfigDTOS)
  {
    checkProductLicense();

    if (apiFirewallReleaseQuarantineConfigDTOS == null) {
      throw new BadRequestException("No policy condition types were specified for update.");
    }

    validateInputConfigurationDtos(apiFirewallReleaseQuarantineConfigDTOS);

    if (!apiFirewallReleaseQuarantineConfigDTOS.isEmpty()) {
      executeWithAuditSession(() -> {
        disablePolicyConditionTypesForMonitoring(apiFirewallReleaseQuarantineConfigDTOS.stream()
            .filter(conditionType -> !conditionType.autoReleaseQuarantineEnabled).collect(
                Collectors.toList()));

        enablePolicyConditionTypesForMonitoring(apiFirewallReleaseQuarantineConfigDTOS.stream()
            .filter(conditionType -> conditionType.autoReleaseQuarantineEnabled).collect(
                Collectors.toList()));
      });
    }

    final List<ApiFirewallReleaseQuarantineConfigDTO> result = getReleaseQuarantineConfig();
    sendAutoReleaseQuarantineConfigTelemetry(result);
    return result;
  }

  private void sendAutoReleaseQuarantineConfigTelemetry(
      final List<ApiFirewallReleaseQuarantineConfigDTO> releaseQuarantineConfigDTOS)
  {
    final AutoReleaseQuarantineTelemetry autoReleaseQuarantineTelemetry = new AutoReleaseQuarantineTelemetry();
    final TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.AUTO_RELEASE_FROM_QUARANTINE_CONFIGURATION);
    telemetryData.getAttributes().put(AUTO_RELEASE_QUARANTINE_CONFIG_TELEMETRY, autoReleaseQuarantineTelemetry);

    releaseQuarantineConfigDTOS.forEach(apiFirewallReleaseQuarantineConfigDTO -> {
      if (apiFirewallReleaseQuarantineConfigDTO.autoReleaseQuarantineEnabled) {
        autoReleaseQuarantineTelemetry.enabledConditionTypes.add(apiFirewallReleaseQuarantineConfigDTO.id);
      }
      else {
        autoReleaseQuarantineTelemetry.disabledConditionTypes.add(apiFirewallReleaseQuarantineConfigDTO.id);
      }
    });

    telemetrySender.send(telemetryData);
  }

  private void validateInputConfigurationDtos(
      final List<ApiFirewallReleaseQuarantineConfigDTO> apiFirewallReleaseQuarantineConfigDTOS)
  {
    apiFirewallReleaseQuarantineConfigDTOS.forEach(conditionType -> {
      if (null == conditionType.id) {
        throw new BadRequestException("Some Policy Condition Types do not have ID's specified.");
      }

      if (null == conditionType.autoReleaseQuarantineEnabled) {
        throw new BadRequestException(String
            .format("Policy Condition Type with id '%s' does not have the enabled flag specified.", conditionType.id));
      }
    });
  }

  private void disablePolicyConditionTypesForMonitoring(
      final List<ApiFirewallReleaseQuarantineConfigDTO> apiFirewallReleaseQuarantineConfigDTOS)
  {
    apiFirewallReleaseQuarantineConfigDTOS.forEach(conditionType -> {
      final AutoUnquarantinePolicyConditionType autoUnquarantinePolicyConditionType =
          autoUnquarantinePolicyConditionTypeDAO.getById(conditionType.id);
      if (autoUnquarantinePolicyConditionType != null) {
        autoUnquarantinePolicyConditionTypeDAO.delete(autoUnquarantinePolicyConditionType);
      }
    });
  }

  private void enablePolicyConditionTypesForMonitoring(
      final List<ApiFirewallReleaseQuarantineConfigDTO> apiFirewallReleaseQuarantineConfigDTOS)
  {
    apiFirewallReleaseQuarantineConfigDTOS.forEach(conditionType -> {
      if (autoUnquarantinePolicyConditionTypeDAO.getById(conditionType.id) == null) {
        AutoUnquarantinePolicyConditionType autoUnquarantinePolicyConditionType =
            new AutoUnquarantinePolicyConditionType(conditionType.id);
        autoUnquarantinePolicyConditionTypeDAO.insert(autoUnquarantinePolicyConditionType);
      }
    });
  }

  public Set<String> getAutoUnquarantineEnabledPolicyConditionTypesIds() {
    return autoUnquarantinePolicyConditionTypeDAO.getAll().stream()
        .map(AutoUnquarantinePolicyConditionType::getId)
        .collect(Collectors.toSet());
  }

  private ApiFirewallReleaseQuarantineConfigDTO generateReleaseQuarantineConfigDto(
      final Set<String> enabledPolicyConditionTypes,
      final ConditionType conditionType)
  {
    ApiFirewallReleaseQuarantineConfigDTO apiFirewallReleaseQuarantineConfigDTO =
        new ApiFirewallReleaseQuarantineConfigDTO();
    apiFirewallReleaseQuarantineConfigDTO.autoReleaseQuarantineEnabled =
        enabledPolicyConditionTypes.contains(conditionType.getId());
    apiFirewallReleaseQuarantineConfigDTO.id = conditionType.getId();
    apiFirewallReleaseQuarantineConfigDTO.name = conditionType.getName();

    return apiFirewallReleaseQuarantineConfigDTO;
  }

  private void checkProductLicense() {
    if (!productLicense.hasFeature(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE) ||
        !productLicense.hasFeature(LicensedFeature.RELEASE_INTEGRITY)) {
      throw new InvalidLicenseException();
    }
  }

  @Authorize(permission = Permission.READ)
  public ApiFirewallReleaseQuarantineSummaryDTO getReleaseQuarantineSummary() {
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
  public ApiPageResult<ApiFirewallComponentDTO> getComponents(FirewallRepositoryComponentFilter filter) {
    if (null == filter.sortableField) {
      filter.sortableField = FirewallSortableField.RELEASE_QUARANTINE_TIME;
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
      apiFirewallComponentDTO.componentIdentifier = component.getComponentIdentifier();
      apiFirewallComponentDTO.hash = component.getHash();
      apiFirewallComponentDTO.matchState = component.getMatchStateId();
      apiFirewallComponentDTO.pathname = component.getPathname();
      apiFirewallComponentDTO.repositoryId = component.getRepositoryId();
      apiFirewallComponentDTO.quarantined = component.isQuarantined();

      // find and add all policy violations for this repository component
      List<RepositoryPolicyViolation> violations = repositoryPolicyViolationDAO
          .getByRepositoryIdAndPathnameAndActionAndNotWaived(component.getRepositoryId(), component.getPathname(),
              Action.ID_FAIL);
      for (RepositoryPolicyViolation policyViolation : violations) {
        final ApiPolicyViolationDTOV2 policyViolationDTO = apiPolicyViolationAdapter.convert(policyViolation);
        apiFirewallComponentDTO.quarantinePolicyViolations.add(policyViolationDTO);
      }

      result.getResults().add(apiFirewallComponentDTO);
    }

    return result;
  }

  private void validateFirewallRepositoryComponentFilter(final FirewallRepositoryComponentFilter filter) {
    if (filter.page < MIN_PAGE) {
      throw new BadRequestException("Invalid page: " + filter.page + ". Page shouldn't be lower than " + MIN_PAGE);
    }

    if (filter.pageSize < MIN_PAGE_SIZE || filter.pageSize > MAX_PAGE_SIZE) {
      throw new BadRequestException(
          "Invalid page size: " + filter.pageSize + ". Page size should be between " + MIN_PAGE_SIZE + " and " +
              MAX_PAGE_SIZE);
    }

    if (filter.sortableField == null) {
      throw new BadRequestException("sortBy field is null");
    }

    if (null == filter.firewallComponentFilterState) {
      throw new BadRequestException("firewallComponentFilterState is required and cannot be null.");
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
