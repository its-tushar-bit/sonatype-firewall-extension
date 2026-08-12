/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import static com.sonatype.insight.brain.repository.RepositoryService.buildProxyUrl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.repository.RepositoryDTO;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallQuarantineSummaryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallQuarantinedComponentDto;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallReleaseQuarantineConfigDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallReleaseQuarantineSummaryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentEvaluationRequestList;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentEvaluationRequestList.ApiRepositoryComponentEvaluationRequest;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentEvaluationResultList;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryContainerDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryManagerDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryManagerListDTO;
import com.sonatype.insight.brain.api.v2.service.ApiComponentDetailsAdapter;
import com.sonatype.insight.brain.api.v2.service.ApiPolicyViolationAdapter;
import com.sonatype.insight.brain.api.v2.service.ComponentFormatConstants;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoUnquarantinePolicyConditionTypeDAO;
import com.sonatype.insight.brain.dataaccess.policy.ProxyRepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.VirtualRepositoryConfigDAO;
import com.sonatype.insight.brain.dataaccess.repository.FirewallQuarantinedComponentDetails;
import com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter;
import com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter.FirewallComponentFilterState;
import com.sonatype.insight.brain.dataaccess.repository.FirewallSortableField;
import com.sonatype.insight.brain.dataaccess.repository.InvalidRepositoryException;
import com.sonatype.insight.brain.dataaccess.repository.QuarantinedComponentAccessDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.integration.repository.AbstractRepositoryService;
import com.sonatype.insight.brain.integration.repository.RepositoryService;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.AutoUnquarantinePolicyConditionType;
import com.sonatype.insight.brain.model.policy.ConditionType;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.repository.VirtualRepositoryConfig;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.repository.ManagerType;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.organization.OrganizationService;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.security.AuthzFilter;
import com.sonatype.insight.brain.security.AuthzFilter.Context;
import com.sonatype.insight.brain.telemetry.AutoReleaseQuarantineTelemetry;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.lqa.LqaComponentIdentifier;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.common.base.Strings;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.repository.RepositoryContainer.REPOSITORY_CONTAINER_ID;

/**
 * @since 1.106.0
 */
@Named
public class ApiFirewallService
{
  private static final Logger log = LoggerFactory.getLogger(ApiFirewallService.class);

  static final int MIN_PAGE = 1;

  static final int MIN_PAGE_SIZE = 1;

  static final int MAX_PAGE_SIZE = 10000;

  static final int MAX_COMPONENTS_TO_EVALUATE = 100;

  static final String AUTO_RELEASE_QUARANTINE_CONFIG_TELEMETRY = "auto_release_quarantine_config";

  private final ProductLicense productLicense;

  private final ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  private final RepositoryDAO repositoryDAO;

  private final ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO;

  private final AutoUnquarantinePolicyConditionTypeDAO autoUnquarantinePolicyConditionTypeDAO;

  private final QuarantinedComponentAccessDAO quarantinedComponentAccessDAO;

  private final TelemetrySender telemetrySender;

  private final RepositoryManagerDAO repositoryManagerDAO;

  private final VirtualRepositoryConfigDAO virtualRepositoryConfigDAO;

  private final RepositoryService repositoryService;

  private final ApiComponentDetailsAdapter apiComponentDetailsAdapter;

  private final OwnerDAO ownerDAO;

  private final OrganizationService organizationService;

  private final com.sonatype.insight.brain.repository.RepositoryService mainRepositoryService;

  private final BaseUrl baseUrl;

  private final FirewallPermissionGate firewallPermissionGate;

  private static final Set<String> PROXY_FORMATS = Set.of("maven2", "npm", "pypi", "nuget");

  private static final Set<String> ALLOWED_UPSTREAM_SCHEMES = Set.of("http", "https");

  @Inject
  public ApiFirewallService(
      final ProductLicense productLicense,
      final ProxyRepositoryComponentDAO proxyRepositoryComponentDAO,
      final RepositoryDAO repositoryDAO,
      final ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO,
      final AutoUnquarantinePolicyConditionTypeDAO autoUnquarantinePolicyConditionTypeDAO,
      final QuarantinedComponentAccessDAO quarantinedComponentAccessDAO,
      final TelemetrySender telemetrySender,
      final RepositoryManagerDAO repositoryManagerDAO,
      final VirtualRepositoryConfigDAO virtualRepositoryConfigDAO,
      final RepositoryService repositoryService,
      final ApiComponentDetailsAdapter apiComponentDetailsAdapter,
      final OwnerDAO ownerDAO,
      final OrganizationService organizationService,
      final com.sonatype.insight.brain.repository.RepositoryService mainRepositoryService,
      final BaseUrl baseUrl,
      final FirewallPermissionGate firewallPermissionGate)
  {
    this.productLicense = productLicense;
    this.proxyRepositoryComponentDAO = proxyRepositoryComponentDAO;
    this.repositoryDAO = repositoryDAO;
    this.proxyRepositoryPolicyViolationDAO = proxyRepositoryPolicyViolationDAO;
    this.autoUnquarantinePolicyConditionTypeDAO = autoUnquarantinePolicyConditionTypeDAO;
    this.quarantinedComponentAccessDAO = quarantinedComponentAccessDAO;
    this.telemetrySender = telemetrySender;
    this.repositoryManagerDAO = repositoryManagerDAO;
    this.virtualRepositoryConfigDAO = virtualRepositoryConfigDAO;
    this.repositoryService = repositoryService;
    this.apiComponentDetailsAdapter = apiComponentDetailsAdapter;
    this.ownerDAO = ownerDAO;
    this.organizationService = organizationService;
    this.mainRepositoryService = mainRepositoryService;
    this.baseUrl = baseUrl;
    this.firewallPermissionGate = firewallPermissionGate;
  }

  private void executeWithAuditSession(Runnable runnable) {
    try (AuditSession auditSession = AuditData.get()
        .recordSubEvent(AuditEvent.CONFIGURE_CONTINUOUS_MONITORING, false))
    {
      AuditData.get().setOwner(ownerDAO.getById(REPOSITORY_CONTAINER_ID)).setStageId(StageTypes.PROXY.getId());
      runnable.run();
    }
  }

  @Authorize(permission = Permission.READ)
  void checkReadPermission(@SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.OWNER) Owner owner) {
  }

  @Authorize(permission = Permission.WRITE)
  void checkWritePermission(@SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.OWNER) Owner owner) {
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  void checkEvaluateComponentPermission(@SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.OWNER) Owner owner) {
  }

  ApiFirewallQuarantineSummaryDTO getQuarantineSummary() {
    checkProductLicense();
    firewallPermissionGate.resolvePermittedRepositoryIds(); // gate check - counts remain global

    ApiFirewallQuarantineSummaryDTO summary = new ApiFirewallQuarantineSummaryDTO();
    summary.repositoryCount = repositoryDAO.getCountByRepositoryType(RepositoryType.proxy);
    summary.quarantineEnabledRepositoryCount = repositoryDAO.getQuarantineEnabledCount();
    summary.quarantineEnabled = summary.quarantineEnabledRepositoryCount > 0;
    summary.totalComponentCount = proxyRepositoryComponentDAO.getCount();
    summary.quarantinedComponentCount = proxyRepositoryComponentDAO.getQuarantinedComponentCount();

    return summary;
  }

  List<ApiFirewallReleaseQuarantineConfigDTO> getReleaseQuarantineConfig() {
    checkProductLicense();
    firewallPermissionGate.resolvePermittedRepositoryIds();

    return getReleaseQuarantineConfig_NoChecks();
  }

  private List<ApiFirewallReleaseQuarantineConfigDTO> getReleaseQuarantineConfig_NoChecks() {
    final Set<String> enabledPolicyConditionTypes = getAutoUnquarantineEnabledPolicyConditionTypesIds();

    return ConditionTypes.getAllWithAutoUnquarantineSupported()
        .stream()
        .map(conditionType -> generateReleaseQuarantineConfigDto(enabledPolicyConditionTypes, conditionType))
        .collect(Collectors.toList());
  }

  List<ApiFirewallReleaseQuarantineConfigDTO> setReleaseQuarantineConfig(
      final List<ApiFirewallReleaseQuarantineConfigDTO> apiFirewallReleaseQuarantineConfigDTOS)
  {
    checkProductLicense();
    checkWritePermission(RepositoryContainer.SINGLETON);

    if (apiFirewallReleaseQuarantineConfigDTOS == null) {
      throw new BadRequestException("No policy condition types were specified for update.");
    }

    validateInputConfigurationDtos(apiFirewallReleaseQuarantineConfigDTOS);

    if (!apiFirewallReleaseQuarantineConfigDTOS.isEmpty()) {
      executeWithAuditSession(() -> {
        disablePolicyConditionTypesForMonitoring(apiFirewallReleaseQuarantineConfigDTOS.stream()
            .filter(conditionType -> !conditionType.autoReleaseQuarantineEnabled)
            .collect(
                Collectors.toList()));

        enablePolicyConditionTypesForMonitoring(apiFirewallReleaseQuarantineConfigDTOS.stream()
            .filter(conditionType -> conditionType.autoReleaseQuarantineEnabled)
            .collect(
                Collectors.toList()));
      });
    }

    final List<ApiFirewallReleaseQuarantineConfigDTO> result = getReleaseQuarantineConfig_NoChecks();
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
    return autoUnquarantinePolicyConditionTypeDAO.getAll()
        .stream()
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
        !productLicense.hasFeature(LicensedFeature.RELEASE_INTEGRITY))
    {
      throw new InvalidLicenseException();
    }
  }

  ApiFirewallReleaseQuarantineSummaryDTO getReleaseQuarantineSummary() {
    checkProductLicense();
    firewallPermissionGate.resolvePermittedRepositoryIds(); // gate check - counts remain global

    final Date startOfCurMonth =
        Date.from((LocalDate.now().withDayOfMonth(1)).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    final Date startOfCurYear =
        Date.from((LocalDate.now().withDayOfYear(1)).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

    final ApiFirewallReleaseQuarantineSummaryDTO apiFirewallReleaseQuarantineSummaryDTO =
        new ApiFirewallReleaseQuarantineSummaryDTO();

    apiFirewallReleaseQuarantineSummaryDTO.autoReleaseQuarantineCountMTD =
        proxyRepositoryComponentDAO.getAutoReleaseQuarantinedCountByDate(startOfCurMonth);

    apiFirewallReleaseQuarantineSummaryDTO.autoReleaseQuarantineCountYTD =
        proxyRepositoryComponentDAO.getAutoReleaseQuarantinedCountByDate(startOfCurYear);

    return apiFirewallReleaseQuarantineSummaryDTO;
  }

  ApiPageResult<ApiFirewallComponentDTO> getComponents(FirewallRepositoryComponentFilter filter) {
    Set<String> permittedRepositoryIds = firewallPermissionGate.resolvePermittedRepositoryIds();
    filter.permittedRepositoryIds = permittedRepositoryIds;

    if (null == filter.sortableField) {
      filter.sortableField = FirewallSortableField.RELEASE_QUARANTINE_TIME;
    }

    // validate filter
    this.validateFirewallRepositoryComponentFilter(filter);

    // get a list of all auto-unquarantined repository components
    final List<ProxyRepositoryComponent> autoUnquarantinedComponents =
        proxyRepositoryComponentDAO.getFirewallRepositoryComponents(filter);

    // get a list of all unique repository ids
    Map<String, Repository> repositoryMap = toRepositoryMap(autoUnquarantinedComponents);

    final long total = proxyRepositoryComponentDAO.getTotalFirewallRepositoryComponents(filter);
    ApiPageResult<ApiFirewallComponentDTO> result = new ApiPageResult<>(total, filter.page, filter.pageSize);

    // for each component, attach policy violations for that component
    for (ProxyRepositoryComponent component : autoUnquarantinedComponents) {
      final ApiFirewallComponentDTO apiFirewallComponentDTO = new ApiFirewallComponentDTO();
      apiFirewallComponentDTO.displayName = component.getDisplayName();
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
      List<ProxyRepositoryPolicyViolation> violations = proxyRepositoryPolicyViolationDAO
          .getByRepositoryIdAndPathnameAndActionAndNotWaived(component.getRepositoryId(), component.getPathname(),
              Action.ID_FAIL);
      proxyRepositoryPolicyViolationDAO.loadConstraintFacts(violations);
      for (ProxyRepositoryPolicyViolation policyViolation : violations) {
        final ApiPolicyViolationDTOV2 policyViolationDTO = ApiPolicyViolationAdapter.convert(policyViolation);
        apiFirewallComponentDTO.quarantinePolicyViolations.add(policyViolationDTO);
      }

      result.getResults().add(apiFirewallComponentDTO);
    }

    return result;
  }

  ApiPageResult<ApiFirewallQuarantinedComponentDto> getQuarantinedComponents(
      FirewallRepositoryComponentFilter firewallFilter)
  {
    Set<String> permittedRepositoryIds = firewallPermissionGate.resolvePermittedRepositoryIds();
    firewallFilter.permittedRepositoryIds = permittedRepositoryIds;

    this.validateGetQuarantinedComponentsFilter(firewallFilter);

    List<FirewallQuarantinedComponentDetails> detailsList =
        proxyRepositoryComponentDAO.getQuarantinedComponentsDetails(firewallFilter);

    List<ApiFirewallQuarantinedComponentDto> responseDtos =
        detailsList.stream().map(ApiFirewallQuarantinedComponentDto::new).collect(Collectors.toList());

    final long totalSize = proxyRepositoryComponentDAO.getTotalFirewallRepositoryComponents(firewallFilter);

    return new ApiPageResult<>(totalSize, firewallFilter.page, firewallFilter.pageSize, responseDtos);
  }

  private void validateGetQuarantinedComponentsFilter(final FirewallRepositoryComponentFilter filter) {
    this.validateFirewallRepositoryComponentFilter(filter);

    if (!filter.firewallComponentFilterState.equals(FirewallComponentFilterState.QUARANTINE)) {
      throw new BadRequestException(String.format(
          "FilterState %s is not applicable to get Firewall Quarantined components.",
          filter.firewallComponentFilterState.name()));
    }

    EnumSet<FirewallSortableField> validSortFields =
        EnumSet.of(FirewallSortableField.QUARANTINE_TIME, FirewallSortableField.REPOSITORY_PUBLIC_ID,
            FirewallSortableField.POLICY_NAME, FirewallSortableField.COMPONENT_DISPLAY_NAME);

    if (!validSortFields.contains(filter.sortableField)) {
      throw new BadRequestException(String.format(
          "SortableField %s is not applicable to get Firewall Quarantined components.",
          filter.sortableField.getLabel()));
    }
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

  private Map<String, Repository> toRepositoryMap(final List<ProxyRepositoryComponent> components) {
    Map<String, Repository> repositoryMap = new HashMap<>();

    final Set<String> repositoryIds = components.stream()
        .map(ProxyRepositoryComponent::getRepositoryId)
        .collect(Collectors.toSet());

    for (String repositoryId : repositoryIds) {
      final Repository repository = repositoryDAO.getById(repositoryId);
      repositoryMap.put(repositoryId, repository);
    }
    return repositoryMap;
  }

  void setQuarantinedComponentViewAnonymousAccess(boolean enabled) {
    checkWritePermission(RepositoryContainer.SINGLETON);

    AuditData auditData = AuditData.get();
    auditData.setData("enabled", enabled);

    quarantinedComponentAccessDAO.setAnonymousAccess(enabled);

    log.info("Quarantined Component View anonymous access was " + (enabled ? "enabled" : "disabled") + ".");
  }

  String getQuarantinedComponentViewAnonymousAccess() {
    return Boolean.toString(quarantinedComponentAccessDAO.isAnonymousAccessEnabled());
  }

  ApiRepositoryManagerListDTO getRepositoryManagers() {
    productLicense.validateFeature(LicensedFeature.FIREWALL);

    List<RepositoryManager> repositoryManagersWithReadPermission =
        filterRepositoryManagersWithReadPermission(repositoryManagerDAO.getAll());
    List<ApiRepositoryManagerDTO> apiRepositoryManagerDTOS = repositoryManagersWithReadPermission.stream()
        .map(ApiFirewallService::fromRepositoryManager)
        .collect(Collectors.toList());

    return new ApiRepositoryManagerListDTO(apiRepositoryManagerDTOS);
  }

  @AuthzFilter(permission = Permission.READ, context = Context.REPOSITORY_MANAGER)
  List<RepositoryManager> filterRepositoryManagersWithReadPermission(List<RepositoryManager> repositoryManagers) {
    return repositoryManagers;
  }

  @Authorize(permission = Permission.READ)
  ApiRepositoryListDTO getConfiguredRepositories(
      @AuthzContext(Key.REPOSITORY_MANAGER_ID) String repositoryManagerId,
      Long sinceUtcTimestamp)
  {
    productLicense.validateFeature(LicensedFeature.FIREWALL);

    RepositoryManager repositoryManager = repositoryManagerDAO.getByIdNotNull(repositoryManagerId);

    List<Repository> repositories = repositoryService
        .getConfiguredRepositoriesNoAuthz(repositoryManager, sinceUtcTimestamp,
            null);

    // upstreamUrl is deliberately not attached here — the satellite table is Firewall
    // Enterprise-only and joining it here would (a) run an extra read for ordinary Firewall
    // customers whose rows never exist in it, and (b) put a service-class change into this
    // data-layer story. Stories 3.2 / 3.4a own the Firewall-Enterprise-scoped endpoint that
    // surfaces upstreamUrl.
    List<ApiRepositoryDTO> apiRepositories = repositories.stream()
        .map(ApiRepositoryDTO::fromRepository)
        .collect(Collectors.toList());

    ApiRepositoryListDTO apiRepositoryListDTO = new ApiRepositoryListDTO();
    apiRepositoryListDTO.repositories = apiRepositories;

    return apiRepositoryListDTO;
  }

  @Authorize(permission = Permission.WRITE)
  void configureRepositories(
      @AuthzContext(Key.REPOSITORY_MANAGER_ID) String repositoryManagerId,
      ApiRepositoryListDTO dto)
  {
    AuditData.get().setRepositoryManagerId(repositoryManagerId);
    productLicense.validateFeature(LicensedFeature.FIREWALL);

    validate(repositoryManagerId, dto);
    List<RepositoryDTO> repositoryDTOs = dto.repositories.stream()
        .map(ApiRepositoryDTO::toRepositoryDTO)
        .collect(Collectors.toList());
    repositoryService.configureRepositoriesNoAuthz(repositoryManagerDAO.getByIdNotNull(repositoryManagerId),
        repositoryDTOs, true);
  }

  private void validate(String repositoryManagerId, ApiRepositoryListDTO dto) {
    if (dto == null || CollectionUtils.isEmpty(dto.repositories)) {
      throw new BadRequestException("No repository configurations specified.");
    }
    dto.repositories.forEach(repository -> validate(repositoryManagerId, repository));
  }

  private void validate(String repositoryManagerId, ApiRepositoryDTO dto) {
    repositoryDAO.validateNotEmptyPublicId(dto.publicId);
    if (ApiRepositoryDTO.toRepositoryType(dto.type) == null) {
      throw new BadRequestException("The repository type must be proxy or hosted.");
    }
    if (dto.format != null &&
        !getSupportedFormats().contains(AbstractRepositoryService.translateRepositoryFormat(dto.format)))
    {
      throw new BadRequestException(String.format("Unrecognized format '%s'.", dto.format));
    }
    Repository repository = ApiRepositoryDTO.toRepository(dto);
    Repository existingRepository =
        repositoryDAO.getByRepositoryManagerIdAndPublicId(repositoryManagerId, dto.publicId);
    if (existingRepository != null) {
      repository.setId(existingRepository.getId());
      repositoryDAO.validateUpdate(existingRepository, repository);
    }
    if (repository.getRepositoryType() == RepositoryType.proxy && repository.isQuarantineEnabled() &&
        !repository.isAuditEnabled())
    {
      throw new InvalidRepositoryException("Quarantine requires Audit to be enabled.");
    }
    repositoryDAO.validateEnabledFeatures(repository);
  }

  private Set<String> getSupportedFormats() {
    Set<String> supportedFormats = new HashSet<>(ComponentIdentifier.getFormatsSupportedByHds());
    supportedFormats.add(LqaComponentIdentifier.FORMAT_ALPINE);
    supportedFormats.add(LqaComponentIdentifier.FORMAT_BOWER);
    supportedFormats.add(LqaComponentIdentifier.FORMAT_DEBIAN);
    supportedFormats.add(LqaComponentIdentifier.FORMAT_DRUPAL);
    return supportedFormats;
  }

  ApiRepositoryComponentEvaluationResultList evaluateComponents(
      final String repositoryManagerId,
      final String repositoryId,
      final ApiRepositoryComponentEvaluationRequestList apiRepositoryComponentEvaluationRequestList)
  {
    productLicense.validateFeature(LicensedFeature.FIREWALL);
    RepositoryManager repositoryManager = repositoryManagerDAO.getByIdNotNull(repositoryManagerId);
    Repository repository = repositoryDAO.getByIdNotNull(repositoryId);
    validateApiRepositoryComponentEvaluationRequest(repository, repositoryManager,
        apiRepositoryComponentEvaluationRequestList);

    AuditData.get().setData("componentCount", apiRepositoryComponentEvaluationRequestList.components.size());

    log.debug("Evaluating components for repository {}:{} ({})", repositoryManager.getInstanceId(),
        repository.getPublicId(), repository.getId());

    RepositoryComponentEvaluationDataRequestList requestList =
        apiComponentDetailsAdapter.convertFromDTO(apiRepositoryComponentEvaluationRequestList);

    RepositoryComponentEvaluationDataList result = repositoryService.evaluateComponents(
        repository,
        repositoryManager.getInstanceId(),
        requestList,
        repository.isQuarantineEnabled(),
        true,
        null /* clientUserAgent */
    );

    return apiComponentDetailsAdapter.convertToDTO(repositoryManager, repository,
        apiRepositoryComponentEvaluationRequestList, result);
  }

  private void validateApiRepositoryComponentEvaluationRequest(
      final Repository repository,
      final RepositoryManager repositoryManager,
      final ApiRepositoryComponentEvaluationRequestList dto)
  {
    if (!repository.getRepositoryManagerId().equals(repositoryManager.getId())) {
      throw new NotFoundException(
          String.format("Repository '%s' not found in repository manager '%s'.", repository.getId(),
              repositoryManager.getId()));
    }
    if (dto == null || dto.components == null || dto.components.isEmpty()) {
      throw new BadRequestException("There should be at least 1 component to evaluate.");
    }
    if (dto.components.size() > MAX_COMPONENTS_TO_EVALUATE) {
      throw new BadRequestException(
          String.format("Max amount of components to evaluate is '%d'.", MAX_COMPONENTS_TO_EVALUATE));
    }
    if (dto.format == null || dto.format.isEmpty()) {
      throw new BadRequestException("The format must be specified.");
    }
    for (ApiRepositoryComponentEvaluationRequest component : dto.components) {
      if (component.pathname == null && component.packageUrl == null) {
        throw new BadRequestException("One of pathname or packageUrl must be specified.");
      }

      // Coordinate-based formats identify components by coordinates (name+version).
      // Hash-based formats require hash for exact file identification.
      if (component.hash == null) {
        if (component.packageUrl == null) {
          throw new BadRequestException(
              "The hash must be specified when packageUrl is not provided.");
        }
        else if (!ComponentFormatConstants.isCoordinateBasedFormat(dto.format)) {
          throw new BadRequestException(
              "The hash must be specified for '" + dto.format + "' format. " +
                  "Hash is only optional for coordinate-based formats: " +
                  String.join(", ", ComponentFormatConstants.COORDINATE_BASED_FORMATS) + ".");
        }
      }

      if (component.pathname == null) {
        ComponentIdentifier componentIdentifier =
            new PackageUrlIdentifier(component.packageUrl).ensureCompleteIdentifier();
        if (!dto.format.equals(componentIdentifier.getFormat())) {
          throw new BadRequestException("Component format must match that of the request.");
        }
      }
    }
  }

  @Authorize(permission = Permission.READ)
  public ApiRepositoryManagerDTO getRepositoryManager(
      @AuthzContext(Key.REPOSITORY_MANAGER_ID) String repositoryManagerId)
  {
    return fromRepositoryManager(repositoryManagerDAO.getById(repositoryManagerId));
  }

  @Authorize(permission = Permission.WRITE)
  public void deleteRepositoryManager(@AuthzContext(Key.REPOSITORY_MANAGER_ID) String repositoryManagerId) {
    RepositoryManager repoManager = repositoryManagerDAO.getById(repositoryManagerId);
    // Delete related organization
    if (repoManager.getRelatedOrganizationId() != null) {
      try {
        organizationService.deleteOrganizationNoAuthz(repoManager.getRelatedOrganizationId());
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    // Cascade delete repository organizations
    repositoryDAO.getByRepositoryManagerId(repoManager.getId()).forEach(mainRepositoryService::delete);
    repositoryManagerDAO.delete(repoManager);
    AuditData.get().setRepositoryManager(repoManager);
  }

  @Authorize(permission = Permission.WRITE)
  public ApiRepositoryDTO addRepository(
      @AuthzContext(Key.REPOSITORY_MANAGER_ID) String repositoryManagerId,
      ApiRepositoryDTO apiRepositoryDTO)
  {
    productLicense.validateFeature(LicensedFeature.FIREWALL);
    RepositoryManager repositoryManager = repositoryManagerDAO.getByIdNotNull(repositoryManagerId);
    if (repositoryManager.getManagerType() != ManagerType.VIRTUAL) {
      throw new BadRequestException("Repository Manager type must be virtual.");
    }

    if (apiRepositoryDTO.repositoryId != null) {
      throw new BadRequestException("The repository ID must be null.");
    }

    if (Strings.isNullOrEmpty(apiRepositoryDTO.publicId)) {
      throw new BadRequestException("Repository public ID is required.");
    }

    if (Strings.isNullOrEmpty(apiRepositoryDTO.format)) {
      throw new BadRequestException("Repository format is required.");
    }

    if (!PROXY_FORMATS.contains(apiRepositoryDTO.format)) {
      throw new BadRequestException("Repository format not supported.");
    }

    if (Strings.isNullOrEmpty(apiRepositoryDTO.upstreamUrl)) {
      throw new BadRequestException("Repository upstream URL is required.");
    }

    // Persistence-time SSRF guard on the upstream URL. FIRE-664 will replace this with an
    // outbound-boundary check that pins the resolved connection (defeats DNS rebinding);
    // until then this write-time check is the only barrier against loopback / link-local /
    // RFC1918 / cloud-metadata addresses reaching the outbound path. Removing it before
    // FIRE-664 lands would strictly weaken a live endpoint.
    validateUpstreamUrl(apiRepositoryDTO.upstreamUrl);

    Repository repository = ApiRepositoryDTO.toRepository(apiRepositoryDTO);
    repository.setRepositoryManagerId(repositoryManagerId);
    repository.setRepositoryType(RepositoryType.proxy);
    repository.setQuarantineEnabled(true);
    repository.setAuditEnabled(true);

    VirtualRepositoryConfig config = new VirtualRepositoryConfig();
    config.setUpstreamUrl(apiRepositoryDTO.upstreamUrl);

    // Repository row and its satellite config are a 1:1 pair — commit them atomically so a
    // satellite-insert failure cannot leave the repository row orphaned (no upstreamUrl on
    // subsequent reads, no easy cleanup).
    try (TransactionContext tx = repositoryDAO.createTransactionContext()) {
      tx.begin();
      // repository.getId() is populated by AbstractDAO.insert before the DB write, so the
      // satellite can reference it here.
      repositoryDAO.insert(tx, repository);
      config.setRepositoryId(repository.getId());
      virtualRepositoryConfigDAO.insert(tx, config);
      tx.commit();
    }

    AuditData.get().setRepository(repository);

    ApiRepositoryDTO result = ApiRepositoryDTO.fromRepository(repository);
    result.proxyUrl = buildProxyUrl(baseUrl.get(), repositoryManager.getInstanceId(), repository.getPublicId());
    return result;
  }

  public ApiRepositoryManagerDTO addRepositoryManager(ApiRepositoryManagerDTO apiRepositoryManagerDTO) {
    productLicense.validateFeature(LicensedFeature.FIREWALL);
    checkWritePermission(RepositoryContainer.SINGLETON);

    if (apiRepositoryManagerDTO.id != null) {
      throw new BadRequestException("The repository manager ID must be null.");
    }

    RepositoryManager repositoryManager = toRepositoryManager(apiRepositoryManagerDTO);
    repositoryManagerDAO.insert(repositoryManager);

    AuditData.get().setRepositoryManager(repositoryManager);

    return fromRepositoryManager(repositoryManager);
  }

  public ApiRepositoryManagerDTO addVirtualRepositoryManager(ApiRepositoryManagerDTO apiRepositoryManagerDTO) {
    productLicense.validateFeature(LicensedFeature.FIREWALL);
    checkWritePermission(RepositoryContainer.SINGLETON);

    if (apiRepositoryManagerDTO.id != null) {
      throw new BadRequestException("The repository manager ID must be null.");
    }

    apiRepositoryManagerDTO.instanceId = UUID.randomUUID().toString();
    apiRepositoryManagerDTO.managerType = ManagerType.VIRTUAL;
    RepositoryManager repositoryManager = toRepositoryManager(apiRepositoryManagerDTO);
    repositoryManagerDAO.insert(repositoryManager);

    AuditData.get().setRepositoryManager(repositoryManager);

    return fromRepositoryManager(repositoryManager);
  }

  public ApiRepositoryContainerDTO getRepositoryContainer() {
    checkReadPermission(RepositoryContainer.SINGLETON);

    ApiRepositoryContainerDTO apiRepositoryContainerDTO = new ApiRepositoryContainerDTO();
    apiRepositoryContainerDTO.id = RepositoryContainer.REPOSITORY_CONTAINER_ID;
    apiRepositoryContainerDTO.name = RepositoryContainer.SINGLETON.getName();
    apiRepositoryContainerDTO.relatedOrganizationId = RepositoryContainer.SINGLETON.getRelatedOrganizationId();
    return apiRepositoryContainerDTO;
  }

  /**
   * Write-time SSRF guard for {@code /firewall/{id}/repositories}: rejects non-http(s) schemes and
   * hosts that resolve to loopback, any-local, link-local (covers cloud IMDS at 169.254/16),
   * site-local (RFC1918), or multicast addresses. Defeatable by a DNS record flip between this
   * call and the outbound request — an outbound-boundary check that pins the resolved connection
   * (FIRE-664) is the durable fix. Retained here until FIRE-664 lands so this live endpoint is not
   * strictly weaker than it is on {@code main}.
   */
  static void validateUpstreamUrl(String url) {
    URI parsed;
    try {
      parsed = new URI(url);
    }
    catch (URISyntaxException e) {
      throw new BadRequestException("Invalid upstream URL: " + url, e);
    }
    String scheme = parsed.getScheme();
    if (scheme == null || !ALLOWED_UPSTREAM_SCHEMES.contains(scheme.toLowerCase())) {
      throw new BadRequestException("Upstream URL must use http or https.");
    }
    String host = parsed.getHost();
    if (Strings.isNullOrEmpty(host)) {
      throw new BadRequestException("Upstream URL must include a host.");
    }
    InetAddress[] addresses;
    try {
      addresses = InetAddress.getAllByName(host);
    }
    catch (UnknownHostException e) {
      throw new BadRequestException("Upstream URL host could not be resolved: " + host, e);
    }
    for (InetAddress address : addresses) {
      if (address.isLoopbackAddress() || address.isAnyLocalAddress() || address.isLinkLocalAddress()
          || address.isSiteLocalAddress() || address.isMulticastAddress())
      {
        throw new BadRequestException("Upstream URL must not point to an internal or restricted address.");
      }
    }
  }

  private static ApiRepositoryManagerDTO fromRepositoryManager(RepositoryManager repositoryManager) {
    ApiRepositoryManagerDTO dto = new ApiRepositoryManagerDTO();
    dto.id = repositoryManager.getId();
    dto.name = repositoryManager.getName();
    dto.instanceId = repositoryManager.getInstanceId();
    dto.productName = repositoryManager.getProductName();
    dto.productVersion = repositoryManager.getProductVersion();
    dto.managerType = repositoryManager.getManagerType();
    return dto;
  }

  private static RepositoryManager toRepositoryManager(ApiRepositoryManagerDTO apiRepositoryManagerDTO) {
    RepositoryManager repositoryManager = new RepositoryManager();
    repositoryManager.setId(apiRepositoryManagerDTO.id);
    repositoryManager.setName(apiRepositoryManagerDTO.name);
    repositoryManager.setInstanceId(apiRepositoryManagerDTO.instanceId);
    repositoryManager.setProductName(apiRepositoryManagerDTO.productName);
    repositoryManager.setProductVersion(apiRepositoryManagerDTO.productVersion);
    // A client that omits managerType (older NXRM predating the field, direct-JSON scripts) should
    // inherit the entity's TRADITIONAL default (RepositoryManager.managerType field initializer),
    // not clobber it to null and force DAO-level normalization on every write. Only propagate an
    // explicitly-set DTO value.
    if (apiRepositoryManagerDTO.managerType != null) {
      repositoryManager.setManagerType(apiRepositoryManagerDTO.managerType);
    }
    return repositoryManager;
  }
}
