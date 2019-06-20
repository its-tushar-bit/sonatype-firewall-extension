/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.actions.ApiComponentChangeActionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.hds.ComponentDetailsDTO;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.purl.PurlIdentifier;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

/**
 * @since 1.64
 */
@Named
public class ApiComponentRemediationService
{
  private static final String OWNER_TYPE_ATTR = "owner_type";

  private static final String OWNER_ID_ATTR = "owner_id";

  private static final String COMPONENT_ATTR = "component";

  private static final String OPTION_NEXT_NO_VIOLATIONS_ATTR = "option_next_no_violations";

  private static final String OPTION_NEXT_NON_FAILING_ATTR = "option_next_non_failing";

  private final ApplicationDAO applicationDAO = new ApplicationDAO();

  private final ComponentInfoService componentInfoService;

  private final TelemetrySender telemetrySender;

  private final HdsClient hdsClient;

  @Inject
  public ApiComponentRemediationService(ComponentInfoService componentInfoService,
                                        HdsClient hdsClient,
                                        TelemetrySender telemetrySender)
  {
    this.componentInfoService = componentInfoService;
    componentInfoService.setToolName("ci");
    this.hdsClient = hdsClient;
    this.telemetrySender = telemetrySender;
  }

  @Authorize(permission = Permission.EVALUATE_COMPONENT)
  public ApiComponentRemediationDTO getSuggestedRemediationForComponent(
      ApiComponentDTOV2 componentDTO,
      @AuthzContext(Key.TYPE) final OwnerType ownerType,
      @AuthzContext(Key.INTERNAL_ID) final String ownerId,
      final String stageId)
  {

    if (stageId != null && StageTypes.getById(stageId) == null) {
      throw new BadRequestException("Invalid stage: " + stageId + ".");
    }

    String publicOwnerId = ownerId;

    ComponentIdentifier componentIdentifier = validateRequest(componentDTO);

    ComponentSummary componentSummary = getComponentSummary(componentIdentifier);

    // Do not allow an empty or invalid version at this time
    if (!componentSummary.isKnown()) {
      throw new BadRequestException("Invalid Component Identifier or packageUrl");
    }

    if (ownerType.equals(OwnerType.APPLICATION)) {
      publicOwnerId = applicationDAO.getByIdNotNull(ownerId).getPublicId();
    }

    List<ComponentDetailsDTO> dtos = componentInfoService
        .getComponentDetailsForAllVersionsNoAuth(ownerType, publicOwnerId, componentIdentifier, stageId);
    ApiComponentRemediationDTO componentRemediationDto = new ApiComponentRemediationDTO();

    int currentIndex =
        IntStream.range(0, dtos.size()).filter(i -> dtos.get(i).componentIdentifier.equals(componentIdentifier))
            .findFirst().orElse(-1);

    Map<String, Object> telemetryAttributes = new HashMap<>();

    if (currentIndex >= 0) { // should always be the case
      findNoViolations(currentIndex, dtos)
          .ifPresent(changeOptionType -> {
            componentRemediationDto.remediation.versionChanges.add(changeOptionType);
            telemetryAttributes.put(OPTION_NEXT_NO_VIOLATIONS_ATTR, String.valueOf(true));
          });

      // if stage is not specified we don't add non-failing remedies
      if (stageId != null) {
        findNonFailing(currentIndex, dtos)
            .ifPresent(changeOptionType -> {
              componentRemediationDto.remediation.versionChanges.add(changeOptionType);
              telemetryAttributes.put(OPTION_NEXT_NON_FAILING_ATTR, String.valueOf(true));
            });
      }
    }

    sendTelemetry(ownerType, ownerId, componentIdentifier, telemetryAttributes);
    return componentRemediationDto;
  }

  private Optional<ApiVersionChangeOptionDTO> findNoViolations(int startingIndex,
                                                               List<ComponentDetailsDTO> dtos)
  {
    return dtos.subList(startingIndex, dtos.size()).stream().filter(dto -> dto.violatedPolicyCount == 0).findFirst()
        .map(dto -> createVersionChangeOption(dto, ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS));

  }

  private Optional<ApiVersionChangeOptionDTO> findNonFailing(int startingIndex,
                                                             List<ComponentDetailsDTO> dtos)
  {
    return dtos.subList(startingIndex, dtos.size()).stream().filter(dto -> !hasFailAction(dto.policyAlerts)).findFirst()
        .map(dto -> createVersionChangeOption(dto, ApiVersionChangeOptionType.NEXT_NON_FAILING));
  }

  private boolean hasFailAction(List<PolicyAlert> alerts) {
    return alerts.stream().anyMatch(
        alert -> Optional.ofNullable(alert.getActions()).map(Collection::stream).orElseGet(Stream::empty)
            .anyMatch(action -> Action.ID_FAIL.equals(action.getActionTypeId())));
  }

  private void sendTelemetry(final OwnerType ownerType,
                             final String ownerId,
                             final ComponentIdentifier componentIdentifier,
                             final Map<String, Object> attributes)
  {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.COMPONENT_REMEDIATION);
    attributes.put(COMPONENT_ATTR, HdsClientAnalytics.obfuscate(JsonUtils.writeUnformatted(componentIdentifier)));
    attributes.put(OWNER_TYPE_ATTR, ownerType.toString());
    attributes.put(OWNER_ID_ATTR, HdsClientAnalytics.obfuscate(ownerId));
    attributes.putIfAbsent(OPTION_NEXT_NO_VIOLATIONS_ATTR, String.valueOf(false));
    attributes.putIfAbsent(OPTION_NEXT_NON_FAILING_ATTR, String.valueOf(false));
    telemetryData.setAttributes(attributes);
    telemetrySender.send(telemetryData);
  }

  private ApiVersionChangeOptionDTO createVersionChangeOption(ComponentDetailsDTO dto,
                                                              ApiVersionChangeOptionType apiVersionChangeOptionType)
  {
    ApiComponentDTOV2 componentDTOV2 = new ApiComponentDTOV2();
    componentDTOV2.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(dto.componentIdentifier);
    componentDTOV2.proprietary = null; // not applicable
    return new ApiVersionChangeOptionDTO(apiVersionChangeOptionType, new ApiComponentChangeActionDTO(componentDTOV2));
  }

  private ComponentIdentifier validateRequest(ApiComponentDTOV2 componentDTO) {
    if (componentDTO == null || (componentDTO.componentIdentifier == null && componentDTO.packageUrl == null)) {
      throw new BadRequestException("One of either componentIdentifier or packageUrl must be supplied.");
    }

    if (componentDTO.componentIdentifier != null) {
      return validateComponentIdentifier(componentDTO);
    }
    else {
      return validatePackageUrl(componentDTO);
    }
  }
  
  private ComponentIdentifier validateComponentIdentifier(ApiComponentDTOV2 componentDTO) {
    try {
      ComponentIdentifier componentIdentifier = new ComponentIdentifier(componentDTO.componentIdentifier.getFormat(),
          componentDTO.componentIdentifier.getCoordinates());
      return validateComponentIdentifier(componentIdentifier);
    }
    catch (InvalidComponentIdentifierException e) {
      throw new BadRequestException(e.getMessage(), e);
    }
  }
  
  private ComponentIdentifier validatePackageUrl(ApiComponentDTOV2 componentDTO) {
    try {
      PurlIdentifier purlIdentifier = new PurlIdentifier(componentDTO.packageUrl);
      ComponentIdentifier componentIdentifier = purlIdentifier.toComponentIdentifier();
      return validateComponentIdentifier(componentIdentifier);
    }
    catch (IllegalArgumentException e) {
      throw new BadRequestException(e.getMessage(), e);
    }
  }
  
  private ComponentIdentifier validateComponentIdentifier(ComponentIdentifier componentIdentifier) {
    try {
      componentIdentifier.ensureComplete();
      return componentIdentifier;
    }
    catch (InvalidComponentIdentifierException e) {
      throw new BadRequestException(e.getMessage(), e);
    }
  }

  private ComponentSummary getComponentSummary(final ComponentIdentifier componentIdentifier) {
    Map<String, String> queryParams = Collections.singletonMap("componentIdentifier",
        ComponentIdentifierAdapter.toJson(componentIdentifier));
    return hdsClient.get(ComponentSummary.class, "rest/component/summary", queryParams);
  }
}
