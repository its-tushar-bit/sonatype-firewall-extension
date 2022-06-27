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

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.QuarantinedComponentAccessDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.hds.ComponentVersionInfoDTO;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.QuarantinedComponentAccess;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreatsAdapter;
import com.sonatype.insight.brain.repository.RepositoryPolicyThreatDTO;
import com.sonatype.insight.brain.repository.RepositoryPolicyViolationDTO;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class QuarantinedComponentService
{
  private final QuarantinedComponentAccessManager quarantinedComponentAccessManager;

  private final ComponentInfoService componentInfoService;

  private final RepositoryDAO repositoryDAO;

  private final RepositoryComponentDAO repositoryComponentDAO;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  private final QuarantinedComponentAccessDAO quarantinedComponentAccessDAO;

  private final TelemetrySender telemetrySender;

  private static final String PATHNAME_SEPARATOR = "/";

  private static final Logger log = LoggerFactory.getLogger(QuarantinedComponentService.class);

  static final String QUARANTINED_COMPONENT_REPORT_OBFUSCATED_COMPONENT_HASH =
      "quarantined_component_report_obfuscated_component_hash";

  static final String QUARANTINED_COMPONENT_REPORT_OBFUSCATED_TOKEN = "quarantined_component_report_obfuscated_token";

  static final String QUARANTINED_COMPONENT_REPORT_GENERATE_TIME = "quarantined_component_report_generate_time";

  static final String QUARANTINED_COMPONENT_REPORT_VIEW_TIME = "quarantined_component_report_view_time";

  static final String QUARANTINED_COMPONENT_REPORT_ANONYMOUS_ACCESS_ENABLED =
      "quarantined_component_report_anonymous_access_enabled";

  @Inject
  public QuarantinedComponentService(
      final DbQuarantinedComponentAccessManager quarantinedComponentAccessManager,
      final ComponentInfoService componentInfoService,
      final RepositoryDAO repositoryDAO,
      final RepositoryComponentDAO repositoryComponentDAO,
      final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO,
      final QuarantinedComponentAccessDAO quarantinedComponentAccessDAO,
      final TelemetrySender telemetrySender)
  {
    this.quarantinedComponentAccessManager = quarantinedComponentAccessManager;
    this.componentInfoService = componentInfoService;
    this.repositoryDAO = repositoryDAO;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
    this.quarantinedComponentAccessDAO = quarantinedComponentAccessDAO;
    this.telemetrySender = telemetrySender;

    componentInfoService.setToolName("ci");
  }

  public QuarantinedComponentDto getQuarantinedComponent(final String token) {
    String repositoryComponentId =
        quarantinedComponentAccessManager.getQuarantinedComponentAccessFromToken(token).getRepositoryComponentId();
    RepositoryComponent repositoryComponent = repositoryComponentDAO.getById(repositoryComponentId);

    checkAccess(repositoryComponent);

    final QuarantinedComponentDto quarantinedComponentDto = new QuarantinedComponentDto();
    quarantinedComponentDto.repositoryComponentId = repositoryComponentId;
    quarantinedComponentDto.success = true;
    return quarantinedComponentDto;
  }

  public QuarantinedComponentOverviewDto getQuarantinedComponentOverview(final String token) {
    QuarantinedComponentAccess quarantinedComponentAccess =
        quarantinedComponentAccessManager.getQuarantinedComponentAccessFromToken(token);
    final String repositoryComponentId = quarantinedComponentAccess.getRepositoryComponentId();
    RepositoryComponent repositoryComponent = repositoryComponentDAO.getById(repositoryComponentId);

    checkAccess(repositoryComponent);

    final QuarantinedComponentOverviewDto quarantinedComponentOverviewDto = new QuarantinedComponentOverviewDto();
    quarantinedComponentOverviewDto.componentDisplayName = getComponentDisplayName(repositoryComponent);
    quarantinedComponentOverviewDto.componentVersion =
        repositoryComponent.getComponentIdentifier().get(ComponentIdentifier.VERSION);
    quarantinedComponentOverviewDto.isQuarantined = repositoryComponent.isQuarantined();
    quarantinedComponentOverviewDto.quarantinedPolicyViolationsCount =
        getQuarantinedPolicyViolationsCount(repositoryComponent);
    quarantinedComponentOverviewDto.repositoryName = getRepositoryName(repositoryComponent);
    quarantinedComponentOverviewDto.quarantinedDate = repositoryComponent.getQuarantineTime();
    quarantinedComponentOverviewDto.cataloguedDate = repositoryComponent.getTime();
    quarantinedComponentOverviewDto.tokenExpiryTime = quarantinedComponentAccessManager
        .getTokenExpiryTime(quarantinedComponentAccess.getGenerateTime());

    sendTelemetry(token, quarantinedComponentAccess.getGenerateTime(), repositoryComponent.getHash());

    return quarantinedComponentOverviewDto;
  }

  public RepositoryPolicyThreatDTO getQuarantinedComponentPolicyViolations(final String token) {
    final String repositoryComponentId =
        quarantinedComponentAccessManager.getQuarantinedComponentAccessFromToken(token).getRepositoryComponentId();
    final RepositoryComponent repositoryComponent = repositoryComponentDAO.getById(repositoryComponentId);

    checkAccess(repositoryComponent);

    final List<RepositoryPolicyViolation> policyViolations = getQuarantinedPolicyViolations(repositoryComponent);

    final RepositoryPolicyThreatDTO repositoryPolicyThreatDTO = new RepositoryPolicyThreatDTO();
    repositoryPolicyThreatDTO.activePolicyViolations =
        policyViolations.stream().sorted(Comparator.comparingInt(RepositoryPolicyViolation::getThreatLevel).reversed())
            .map(this::getRepositoryPolicyViolationDto).collect(Collectors.toList());

    return repositoryPolicyThreatDTO;
  }

  public ComponentVersionInfoDTO getQuarantineComponentVersionRemediation(final String token) {
    final String repositoryComponentId =
        quarantinedComponentAccessManager.getQuarantinedComponentAccessFromToken(token).getRepositoryComponentId();
    final RepositoryComponent repositoryComponent = repositoryComponentDAO.getById(repositoryComponentId);

    checkAccess(repositoryComponent);

    return componentInfoService.getComponentVersionInfoNoAuth(OwnerType.REPOSITORY,
        repositoryComponent.getRepositoryId(), repositoryComponent.getComponentIdentifier(), Stage.ID_PROXY,
        repositoryComponent.getIdentificationSourceId(), null, null);
  }

  NamedComponentDetails getComponentVersionDetails(
      final String token,
      final HttpServletRequest httpRequest,
      final String version) throws IOException
  {
    final String repositoryComponentId =
        quarantinedComponentAccessManager.getQuarantinedComponentAccessFromToken(token).getRepositoryComponentId();
    final RepositoryComponent repositoryComponent = repositoryComponentDAO.getById(repositoryComponentId);

    checkAccess(repositoryComponent);

    final Owner owner = IdUtils.getOwnerNotNull(OwnerType.REPOSITORY, repositoryComponent.getRepositoryId());

    ComponentIdentifier componentIdentifier = repositoryComponent.getComponentIdentifier();
    String hash = repositoryComponent.getHash();

    if (!StringUtils.isBlank(version)
        && !version.equals(repositoryComponent.getComponentIdentifier().get(ComponentIdentifier.VERSION))) {
      // The request is for a different version than the quarantined component's version
      componentIdentifier = repositoryComponent.getComponentIdentifier().createAlternativeVersion(version);
      hash = null;
    }

    return componentInfoService.getComponentDetails(owner, componentIdentifier, MatchState.EXACT.getId(), hash,
        false /* proprietary */, httpRequest);
  }

  public ApiPageResult<String> getQuarantinedComponentOtherVersions(
      final String token, int page, int pageSize, boolean asc)
  {
    if (page <= 0 || pageSize <= 0) {
      throw new BadRequestException("Page and Page size must be greater than 0");
    }

    final String repositoryComponentId =
        quarantinedComponentAccessManager.getQuarantinedComponentAccessFromToken(token).getRepositoryComponentId();
    RepositoryComponent repositoryComponent = repositoryComponentDAO.getById(repositoryComponentId);

    checkAccess(repositoryComponent);

    String repositoryId = repositoryComponent.getRepositoryId();
    String pathname = repositoryComponent.getPathname();
    String pathnamePrefix = getPathnamePrefix(pathname);
    int skipCount = (page - 1) * pageSize;

    List<RepositoryComponent> otherVersionComponents = repositoryComponentDAO
        .getOtherVersionRepositoryComponentsByPathnameFilter(repositoryId, pathnamePrefix, pathname);

    Comparator<RepositoryComponent> comparator = Comparator.comparing(component -> new ComparableVersion(
        component.getComponentIdentifier().get(ComponentIdentifier.VERSION)));

    if (!asc) {
      comparator = comparator.reversed();
    }

    List<String> otherVersionComponentDisplayNames =
        otherVersionComponents.stream()
            .sorted(comparator)
            .map(this::getComponentDisplayName)
            .skip(skipCount).limit(pageSize).collect(Collectors.toList());

    return new ApiPageResult<>(otherVersionComponents.size(), page, pageSize, otherVersionComponentDisplayNames);
  }

  String getPathnamePrefix(String pathname) {

    String[] arr = StringUtils.split(pathname, PATHNAME_SEPARATOR);
    if (arr.length > 2) {
      return StringUtils.join(Arrays.copyOf(arr, arr.length - 2), PATHNAME_SEPARATOR) + PATHNAME_SEPARATOR;
    }
    return pathname;
  }

  private RepositoryPolicyViolationDTO getRepositoryPolicyViolationDto(
      RepositoryPolicyViolation policyViolation)
  {
    final RepositoryPolicyViolationDTO policyViolationDto = new RepositoryPolicyViolationDTO();

    policyViolationDto.policyId = policyViolation.getPolicyId();
    policyViolationDto.policyName = policyViolation.getPolicyName();
    policyViolationDto.policyThreatLevel = policyViolation.getThreatLevel();
    policyViolationDto.constraints =
        PolicyThreatsAdapter.toPolicyThreatsPolicyConstraints(policyViolation.getConstraintFacts());
    policyViolationDto.blocksUnquarantine = Action.ID_FAIL.equals(policyViolation.getActionTypeId());
    policyViolationDto.constraintFactsJson = policyViolation.getConstraintFactsJson();

    return policyViolationDto;
  }

  private String getComponentDisplayName(RepositoryComponent repositoryComponent) {
    ComponentIdentifier componentIdentifier = repositoryComponent.getComponentIdentifier();
    if (componentIdentifier == null) {
      log.error("Component Identifier for the quarantined component with repository component id {} is null",
          repositoryComponent.getId());
      throw new BadRequestException("The component identifier for the requested component does not exist.");
    }
    return ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString();
  }

  private int getQuarantinedPolicyViolationsCount(RepositoryComponent repositoryComponent) {
    String repositoryId = repositoryComponent.getRepositoryId();
    String pathName = repositoryComponent.getPathname();
    return repositoryPolicyViolationDAO.getQuarantinedPolicyViolationsCountByRepositoryIdAndPathname(
        repositoryId,
        pathName);
  }

  private String getRepositoryName(RepositoryComponent repositoryComponent) {
    String repositoryId = repositoryComponent.getRepositoryId();
    Repository repository = repositoryDAO.getById(repositoryId);
    return repository.getPublicId();
  }

  private List<RepositoryPolicyViolation> getQuarantinedPolicyViolations(RepositoryComponent repositoryComponent) {
    String repositoryId = repositoryComponent.getRepositoryId();
    String pathName = repositoryComponent.getPathname();
    return repositoryPolicyViolationDAO.getByRepositoryIdAndPathnameAndActionAndNotWaived(
        repositoryId,
        pathName,
        Action.ID_FAIL);
  }

  private void checkAccess(RepositoryComponent repositoryComponent) {
    if (quarantinedComponentAccessDAO.isAnonymousAccessEnabled()) {
      return;
    }

    checkPermission(repositoryComponent.getRepositoryId());
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
