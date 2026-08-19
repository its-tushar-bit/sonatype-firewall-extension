/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.dataaccess.policy.ProxyRepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.QuarantinedComponentAccessDAO;
import com.sonatype.insight.brain.dataaccess.repository.ProxyRepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.hds.ComponentVersionInfoDTO;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.ProxyRepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.QuarantinedComponentAccess;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.repository.ProxyRepositoryPolicyViolationDTO;
import com.sonatype.insight.brain.repository.RepositoryService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.telemetry.NonBreakingRecommendationTelemetryStats.SourceEndpoint;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;

@Named
public class QuarantinedComponentService
{
  private final QuarantinedComponentAccessManager quarantinedComponentAccessManager;

  private final ComponentInfoService componentInfoService;

  private final RepositoryDAO repositoryDAO;

  private final ProxyRepositoryComponentDAO proxyRepositoryComponentDAO;

  private final ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO;

  private final QuarantinedComponentAccessDAO quarantinedComponentAccessDAO;

  private final TelemetrySender telemetrySender;

  private final RepositoryService repositoryService;

  private static final String PATHNAME_SEPARATOR = "/";

  static final String QUARANTINED_COMPONENT_REPORT_OBFUSCATED_COMPONENT_HASH =
      "quarantined_component_report_obfuscated_component_hash";

  static final String QUARANTINED_COMPONENT_REPORT_OBFUSCATED_TOKEN = "quarantined_component_report_obfuscated_token";

  static final String QUARANTINED_COMPONENT_REPORT_GENERATE_TIME = "quarantined_component_report_generate_time";

  static final String QUARANTINED_COMPONENT_REPORT_VIEW_TIME = "quarantined_component_report_view_time";

  static final String QUARANTINED_COMPONENT_REPORT_ANONYMOUS_ACCESS_ENABLED =
      "quarantined_component_report_anonymous_access_enabled";

  private final IdUtils idUtils;

  @Inject
  public QuarantinedComponentService(
      final DbQuarantinedComponentAccessManager quarantinedComponentAccessManager,
      final ComponentInfoService componentInfoService,
      final RepositoryDAO repositoryDAO,
      final ProxyRepositoryComponentDAO proxyRepositoryComponentDAO,
      final ProxyRepositoryPolicyViolationDAO proxyRepositoryPolicyViolationDAO,
      final QuarantinedComponentAccessDAO quarantinedComponentAccessDAO,
      final TelemetrySender telemetrySender,
      final RepositoryService repositoryService,
      final IdUtils idUtils)
  {
    this.quarantinedComponentAccessManager = quarantinedComponentAccessManager;
    this.componentInfoService = componentInfoService;
    this.repositoryDAO = repositoryDAO;
    this.proxyRepositoryComponentDAO = proxyRepositoryComponentDAO;
    this.proxyRepositoryPolicyViolationDAO = proxyRepositoryPolicyViolationDAO;
    this.quarantinedComponentAccessDAO = quarantinedComponentAccessDAO;
    this.telemetrySender = telemetrySender;
    this.repositoryService = repositoryService;
    this.idUtils = idUtils;

    componentInfoService.setToolName("ci");
  }

  public QuarantinedComponentDto getQuarantinedComponent(final String token) {
    String repositoryComponentId =
        quarantinedComponentAccessManager.getQuarantinedComponentAccessFromToken(token).getProxyRepositoryComponentId();
    ProxyRepositoryComponent proxyRepositoryComponent = proxyRepositoryComponentDAO.getById(repositoryComponentId);

    checkAccess(proxyRepositoryComponent);

    final QuarantinedComponentDto quarantinedComponentDto = new QuarantinedComponentDto();
    quarantinedComponentDto.repositoryComponentId = repositoryComponentId;
    quarantinedComponentDto.success = true;
    return quarantinedComponentDto;
  }

  public QuarantinedComponentOverviewDto getQuarantinedComponentOverview(final String token) {
    QuarantinedComponentAccess quarantinedComponentAccess =
        quarantinedComponentAccessManager.getQuarantinedComponentAccessFromToken(token);
    final String repositoryComponentId = quarantinedComponentAccess.getProxyRepositoryComponentId();
    ProxyRepositoryComponent proxyRepositoryComponent = proxyRepositoryComponentDAO.getById(repositoryComponentId);

    checkAccess(proxyRepositoryComponent);

    final QuarantinedComponentOverviewDto quarantinedComponentOverviewDto = new QuarantinedComponentOverviewDto();
    quarantinedComponentOverviewDto.componentIdentifier =
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(proxyRepositoryComponent.getComponentIdentifier());
    quarantinedComponentOverviewDto.componentHash = proxyRepositoryComponent.getHash();
    quarantinedComponentOverviewDto.matchState = proxyRepositoryComponent.getMatchStateId();
    quarantinedComponentOverviewDto.pathname = proxyRepositoryComponent.getPathname();
    quarantinedComponentOverviewDto.componentDisplayName = proxyRepositoryComponent.getDisplayName();
    if (proxyRepositoryComponent.getComponentIdentifier() != null) {
      quarantinedComponentOverviewDto.componentVersion =
          proxyRepositoryComponent.getComponentIdentifier().get(ComponentIdentifier.VERSION);
    }
    quarantinedComponentOverviewDto.isQuarantined = proxyRepositoryComponent.isQuarantined();
    quarantinedComponentOverviewDto.quarantinedPolicyViolationsCount =
        getQuarantinedPolicyViolationsCount(proxyRepositoryComponent);
    quarantinedComponentOverviewDto.repositoryId = proxyRepositoryComponent.getRepositoryId();
    quarantinedComponentOverviewDto.repositoryName = getRepositoryName(proxyRepositoryComponent);
    quarantinedComponentOverviewDto.quarantinedDate = proxyRepositoryComponent.getQuarantineTime();
    quarantinedComponentOverviewDto.tokenExpiryTime = quarantinedComponentAccessManager
        .getTokenExpiryTime(quarantinedComponentAccess.getGenerateTime());

    sendTelemetry(token, quarantinedComponentAccess.getGenerateTime(), proxyRepositoryComponent.getHash());

    return quarantinedComponentOverviewDto;
  }

  public List<ProxyRepositoryPolicyViolationDTO> getQuarantinedComponentPolicyViolations(final String token) {
    final String repositoryComponentId =
        quarantinedComponentAccessManager.getQuarantinedComponentAccessFromToken(token).getProxyRepositoryComponentId();
    final ProxyRepositoryComponent proxyRepositoryComponent =
        proxyRepositoryComponentDAO.getById(repositoryComponentId);

    checkAccess(proxyRepositoryComponent);

    final List<ProxyRepositoryPolicyViolation> policyViolations =
        getQuarantinedPolicyViolations(proxyRepositoryComponent);
    proxyRepositoryPolicyViolationDAO.loadConstraintFacts(policyViolations);

    List<ProxyRepositoryPolicyViolationDTO> repositoryPolicyViolationDTOs =
        policyViolations.stream()
            .sorted(Comparator.comparingInt(ProxyRepositoryPolicyViolation::getThreatLevel).reversed())
            .map(repositoryService::toRepositoryPolicyViolationDTO)
            .collect(Collectors.toList());

    return repositoryPolicyViolationDTOs;
  }

  public ComponentVersionInfoDTO getQuarantineComponentVersionRemediation(final String token) {
    final String repositoryComponentId =
        quarantinedComponentAccessManager.getQuarantinedComponentAccessFromToken(token).getProxyRepositoryComponentId();
    final ProxyRepositoryComponent proxyRepositoryComponent =
        proxyRepositoryComponentDAO.getById(repositoryComponentId);

    checkAccess(proxyRepositoryComponent);

    return componentInfoService.getComponentVersionInfoNoAuth(OwnerType.REPOSITORY,
        proxyRepositoryComponent.getRepositoryId(), proxyRepositoryComponent.getComponentIdentifier(), Stage.ID_PROXY,
        proxyRepositoryComponent.getIdentificationSourceId(), null, null, SourceEndpoint.QUARANTINED_COMPONENT,
        false);
  }

  NamedComponentDetails getQuarantinedComponentVersionDetails(
      final String token,
      final HttpServletRequest httpRequest,
      final String version) throws IOException
  {
    final String repositoryComponentId =
        quarantinedComponentAccessManager.getQuarantinedComponentAccessFromToken(token).getProxyRepositoryComponentId();
    final ProxyRepositoryComponent proxyRepositoryComponent =
        proxyRepositoryComponentDAO.getById(repositoryComponentId);

    checkAccess(proxyRepositoryComponent);

    final Owner owner = idUtils.getOwnerNotNull(OwnerType.REPOSITORY, proxyRepositoryComponent.getRepositoryId());

    ComponentIdentifier componentIdentifier = proxyRepositoryComponent.getComponentIdentifier();
    String hash = proxyRepositoryComponent.getHash();

    if (!StringUtils.isBlank(version)
        && !version.equals(proxyRepositoryComponent.getComponentIdentifier().get(ComponentIdentifier.VERSION)))
    {
      // The request is for a different version than the quarantined component's version
      componentIdentifier = proxyRepositoryComponent.getComponentIdentifier().createAlternativeVersion(version);
      hash = null;
    }

    return componentInfoService.getComponentDetails(owner, componentIdentifier, MatchState.EXACT.getId(), hash,
        false /* proprietary */, httpRequest);
  }

  public ApiPageResult<String> getQuarantinedComponentOtherVersions(
      final String token,
      int page,
      int pageSize,
      boolean asc)
  {
    if (page <= 0 || pageSize <= 0) {
      throw new BadRequestException("Page and Page size must be greater than 0");
    }

    final String repositoryComponentId =
        quarantinedComponentAccessManager.getQuarantinedComponentAccessFromToken(token).getProxyRepositoryComponentId();
    ProxyRepositoryComponent proxyRepositoryComponent = proxyRepositoryComponentDAO.getById(repositoryComponentId);

    checkAccess(proxyRepositoryComponent);

    String repositoryId = proxyRepositoryComponent.getRepositoryId();
    String pathname = proxyRepositoryComponent.getPathname();
    String pathnamePrefix = getPathnamePrefix(pathname);
    int skipCount = (page - 1) * pageSize;

    List<ProxyRepositoryComponent> otherVersionComponents = proxyRepositoryComponentDAO
        .getOtherVersionRepositoryComponentsByPathnameFilter(repositoryId, pathnamePrefix, pathname)
        .stream()
        .filter(component -> filterAllowedVersions(proxyRepositoryComponent.getComponentIdentifier(),
            component.getComponentIdentifier()))
        .collect(Collectors.toList());

    Comparator<ProxyRepositoryComponent> comparator = Comparator.comparing(component -> new ComparableVersion(
        component.getComponentIdentifier().get(ComponentIdentifier.VERSION)));

    if (!asc) {
      comparator = comparator.reversed();
    }

    List<String> otherVersionComponentDisplayNames =
        otherVersionComponents.stream()
            .sorted(comparator)
            .map(ProxyRepositoryComponent::getDisplayName)
            .skip(skipCount)
            .limit(pageSize)
            .collect(Collectors.toList());

    return new ApiPageResult<>(otherVersionComponents.size(), page, pageSize, otherVersionComponentDisplayNames);
  }

  private boolean filterAllowedVersions(
      ComponentIdentifier componentIdentifier,
      ComponentIdentifier otherComponentIdentifier)
  {
    ComponentIdentifier otherAlternativeVersion = otherComponentIdentifier
        .createAlternativeVersion(componentIdentifier.get(ComponentIdentifier.VERSION));

    return componentIdentifier.equals(otherAlternativeVersion);
  }

  String getPathnamePrefix(String pathname) {

    String[] arr = StringUtils.split(pathname, PATHNAME_SEPARATOR);
    if (arr.length > 2) {
      return StringUtils.join(Arrays.copyOf(arr, arr.length - 2), PATHNAME_SEPARATOR) + PATHNAME_SEPARATOR;
    }
    return pathname;
  }

  private int getQuarantinedPolicyViolationsCount(ProxyRepositoryComponent proxyRepositoryComponent) {
    String repositoryId = proxyRepositoryComponent.getRepositoryId();
    String pathName = proxyRepositoryComponent.getPathname();
    return proxyRepositoryPolicyViolationDAO.getQuarantinedPolicyViolationsCountByRepositoryIdAndPathname(
        repositoryId,
        pathName);
  }

  private String getRepositoryName(ProxyRepositoryComponent proxyRepositoryComponent) {
    String repositoryId = proxyRepositoryComponent.getRepositoryId();
    Repository repository = repositoryDAO.getById(repositoryId);
    return repository.getPublicId();
  }

  private List<ProxyRepositoryPolicyViolation> getQuarantinedPolicyViolations(
      ProxyRepositoryComponent proxyRepositoryComponent)
  {
    String repositoryId = proxyRepositoryComponent.getRepositoryId();
    String pathName = proxyRepositoryComponent.getPathname();
    return proxyRepositoryPolicyViolationDAO.getByRepositoryIdAndPathnameAndActionAndNotWaived(
        repositoryId,
        pathName,
        Action.ID_FAIL);
  }

  private void checkAccess(ProxyRepositoryComponent proxyRepositoryComponent) {
    if (quarantinedComponentAccessDAO.isAnonymousAccessEnabled()) {
      return;
    }

    checkPermission(proxyRepositoryComponent.getRepositoryId());
  }

  // Must have at least package visibility for the authz annotations to take effect.
  @Authorize(permission = Permission.READ)
  void checkPermission(@SuppressWarnings("unused") @AuthzContext(Key.REPOSITORY_ID) String repositoryId) {
    // The permission check is handled by the authz annotations
  }

  private void sendTelemetry(
      final String token,
      final Date tokenGenerateTime,
      final String componentHash)
  {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.QUARANTINED_COMPONENT_REPORT_USAGE);

    Map<String, Object> attributes = telemetryData.getAttributes();
    attributes.put(QUARANTINED_COMPONENT_REPORT_OBFUSCATED_COMPONENT_HASH, HdsClientAnalytics.obfuscate(componentHash));
    attributes.put(QUARANTINED_COMPONENT_REPORT_OBFUSCATED_TOKEN, HdsClientAnalytics.obfuscate(token));
    attributes.put(QUARANTINED_COMPONENT_REPORT_GENERATE_TIME, tokenGenerateTime.getTime());
    attributes.put(QUARANTINED_COMPONENT_REPORT_VIEW_TIME, new Date().getTime());
    attributes.put(QUARANTINED_COMPONENT_REPORT_ANONYMOUS_ACCESS_ENABLED,
        quarantinedComponentAccessDAO.isAnonymousAccessEnabled());

    telemetrySender.send(telemetryData);
  }
}
