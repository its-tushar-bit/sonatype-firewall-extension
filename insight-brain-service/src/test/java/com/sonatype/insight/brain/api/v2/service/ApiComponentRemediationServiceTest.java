/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.hds.ComponentDetailsDTO;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public class ApiComponentRemediationServiceTest
    extends AbstractComponentTest
{
  public static final String MISSING_COORDINATES = "The following coordinates are missing for given format: ";

  private static final ComponentIdentifier MAVEN_COORDINATES_V1 = ComponentIdentifier.createMavenCoordinates("g1", "a1",
      "v1", "", "jar");

  private static final ComponentIdentifier MAVEN_COORDINATES_V2 = ComponentIdentifier.createMavenCoordinates("g1", "a1",
      "v2", "", "jar");

  private static final ComponentIdentifier MAVEN_COORDINATES_V3 = ComponentIdentifier.createMavenCoordinates("g1", "a1",
      "v3", "", "jar");

  private Application app;

  @Inject
  private ApiComponentRemediationService service;

  @Mock
  private ComponentInfoService componentInfoServiceMock;

  @Mock
  private TelemetrySender telemetrySenderMock;

  @Override
  public void configure(Binder binder) {
    binder.bind(ComponentInfoService.class).toInstance(componentInfoServiceMock);
    binder.bind(TelemetrySender.class).toInstance(telemetrySenderMock);
    super.configure(binder);
  }

  @Before
  public void setupApplication() {
    app = tempEntity.newApplicationWithParent();
  }

  @Test
  public void testGetSuggestedRemediationForComponent_VersionUpgrade_NoComponentIdentifier() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      service.getSuggestedRemediationForComponent(new ApiComponentDTOV2(), OwnerType.APPLICATION, app.getId());
    }).withMessage("ComponentIdentifier must be supplied.");
  }

  @Test
  public void testGetSuggestedRemediationForComponent_invalidComponentIdentifier_NoCoordinates() throws Exception {
    String jsonRequest =
        "{\"hash\":\"h1\",\"componentIdentifier\":{\"format\":\"maven\"},\"proprietary\":false}";
    ApiComponentDTOV2 request = JsonUtils.parse(jsonRequest, ApiComponentDTOV2.class);
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      service.getSuggestedRemediationForComponent(request, OwnerType.APPLICATION, app.getId());
    }).withMessage("A component identifier must have at least one coordinate.");
  }

  @Test
  public void testGetSuggestedRemediationForComponent_InvalidComponentIdentifier_NoExtension() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ApiComponentDTOV2 component = createComponent(componentIdentifier);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      service.getSuggestedRemediationForComponent(component, OwnerType.APPLICATION, app.getId());
    }).withMessage(MISSING_COORDINATES + "[extension]");
  }

  @Test
  public void testGetSuggestedRemediationForComponent_BadOwnerId() throws Exception {
    doCallRealMethod().when(componentInfoServiceMock)
        .getComponentDetailsForAllVersionsNoAuth(any(OwnerType.class), any(String.class),
            any(ComponentIdentifier.class));
    testGetSuggestedRemediationForComponent_BadOwnerId(OwnerType.APPLICATION, "Could not find an application with ID ");
    testGetSuggestedRemediationForComponent_BadOwnerId(OwnerType.ORGANIZATION, "Cannot find organization with ID ");
  }

  private void testGetSuggestedRemediationForComponent_BadOwnerId(final OwnerType ownerType,
                                                                  final String expectedErrMsgPrefix)
  {
    ApiComponentDTOV2 dto = new ApiComponentDTOV2();
    dto.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(MAVEN_COORDINATES_V1);
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      service.getSuggestedRemediationForComponent(dto, ownerType, "bogusOwnerId");
    }).withMessage(expectedErrMsgPrefix + "bogusOwnerId.");
  }

  @Test
  public void testGetSuggestedRemediationForComponent_VersionUpgrade_AllVersionsWithViolations() {
    ComponentDetailsDTO dto1 = new ComponentDetailsDTO();
    dto1.componentIdentifier = MAVEN_COORDINATES_V1;
    dto1.violatedPolicyCount = 1;
    ComponentDetailsDTO dto2 = new ComponentDetailsDTO();
    dto2.componentIdentifier = MAVEN_COORDINATES_V2;
    dto2.violatedPolicyCount = 1;
    ComponentDetailsDTO dto3 = new ComponentDetailsDTO();
    dto3.componentIdentifier = MAVEN_COORDINATES_V3;
    dto3.violatedPolicyCount = 1;

    List<ComponentDetailsDTO> list = Stream.of(dto1, dto2, dto3).collect(Collectors.toList());
    mockHdsGetComponentDetailsList(list);

    ApiComponentDTOV2 dto = new ApiComponentDTOV2();
    dto.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(dto1.componentIdentifier);

    ApiComponentRemediationDTO retVal = service
        .getSuggestedRemediationForComponent(dto, OwnerType.APPLICATION, app.getId());
    assertRemediationZeroCounts(retVal.remediation);
    assertTelemetry("application", app.getId(), dto1.componentIdentifier);
  }

  @Test
  public void testGetSuggestedRemediationForComponent_VersionUpgrade_PreviousNonVulnerableVersion() {
    ComponentDetailsDTO v1 = new ComponentDetailsDTO();
    v1.componentIdentifier = MAVEN_COORDINATES_V1;
    v1.violatedPolicyCount = 0;
    ComponentDetailsDTO v2 = new ComponentDetailsDTO();
    v2.componentIdentifier = MAVEN_COORDINATES_V2;
    v2.violatedPolicyCount = 1;
    ComponentDetailsDTO v3 = new ComponentDetailsDTO();
    v3.componentIdentifier = MAVEN_COORDINATES_V3;
    v3.violatedPolicyCount = 1;

    List<ComponentDetailsDTO> list = Stream.of(v1, v2, v3).collect(Collectors.toList());
    mockHdsGetComponentDetailsList(list);

    ApiComponentDTOV2 dto = new ApiComponentDTOV2();
    dto.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(v2.componentIdentifier);

    ApiComponentRemediationDTO retVal = service
        .getSuggestedRemediationForComponent(dto, OwnerType.APPLICATION, app.getId());
    // we only look forward so we shouldn't downgrade
    assertRemediationZeroCounts(retVal.remediation);
  }

  @Test
  public void testGetSuggestedRemediationForComponent_VersionUpgrade_LastVersion() {
    ComponentDetailsDTO v1 = new ComponentDetailsDTO();
    v1.componentIdentifier = MAVEN_COORDINATES_V1;
    v1.violatedPolicyCount = 1;
    ComponentDetailsDTO v2 = new ComponentDetailsDTO();
    v2.componentIdentifier = MAVEN_COORDINATES_V2;
    v2.violatedPolicyCount = 1;
    ComponentDetailsDTO v3 = new ComponentDetailsDTO();
    v3.componentIdentifier = MAVEN_COORDINATES_V3;
    v3.violatedPolicyCount = 1;

    List<ComponentDetailsDTO> list = Stream.of(v1, v2, v3).collect(Collectors.toList());
    mockHdsGetComponentDetailsList(list);

    ApiComponentDTOV2 dto = new ApiComponentDTOV2();
    dto.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(v3.componentIdentifier);

    ApiComponentRemediationDTO retVal = service
        .getSuggestedRemediationForComponent(dto, OwnerType.APPLICATION, app.getId());
    assertRemediationZeroCounts(retVal.remediation);
  }

  @Test
  public void testGetSuggestedRemediationForComponent_VersionUpgrade_NextNoViolations() {
    ComponentDetailsDTO v1 = new ComponentDetailsDTO();
    v1.componentIdentifier = MAVEN_COORDINATES_V1;
    v1.violatedPolicyCount = 1;
    ComponentDetailsDTO v2 = new ComponentDetailsDTO();
    v2.componentIdentifier = MAVEN_COORDINATES_V2;
    v2.violatedPolicyCount = 0;
    ComponentDetailsDTO v3 = new ComponentDetailsDTO();
    v3.componentIdentifier = MAVEN_COORDINATES_V3;
    v3.violatedPolicyCount = 0;

    List<ComponentDetailsDTO> list = Stream.of(v1, v2, v3).collect(Collectors.toList());
    mockHdsGetComponentDetailsList(list);

    ApiComponentDTOV2 dto = new ApiComponentDTOV2();
    dto.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(v1.componentIdentifier);

    ApiComponentRemediationDTO retVal = service
        .getSuggestedRemediationForComponent(dto, OwnerType.APPLICATION, app.getId());
    assertRemediation(retVal.remediation,
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(v2.componentIdentifier),
        ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS);
    assertTelemetry("application", app.getId(), v1.componentIdentifier, "option_next_no_violations");
  }

  @Test
  public void testGetSuggestedRemediationForComponent_VersionUpgrade_Current() {
    ComponentDetailsDTO v1 = new ComponentDetailsDTO();
    v1.componentIdentifier = MAVEN_COORDINATES_V1;
    v1.violatedPolicyCount = 0;
    ComponentDetailsDTO v2 = new ComponentDetailsDTO();
    v2.componentIdentifier = MAVEN_COORDINATES_V2;
    v2.violatedPolicyCount = 0;
    ComponentDetailsDTO v3 = new ComponentDetailsDTO();
    v3.componentIdentifier = MAVEN_COORDINATES_V3;
    v3.violatedPolicyCount = 0;

    List<ComponentDetailsDTO> list = Stream.of(v1, v2, v3).collect(Collectors.toList());
    mockHdsGetComponentDetailsList(list);

    ApiComponentDTOV2 dto = new ApiComponentDTOV2();
    dto.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(v1.componentIdentifier);

    ApiComponentRemediationDTO retVal = service
        .getSuggestedRemediationForComponent(dto, OwnerType.APPLICATION, app.getId());
    assertRemediation(retVal.remediation,
        ApiComponentIdentifierDTOV2.fromComponentIdentifier(v1.componentIdentifier),
        ApiVersionChangeOptionType.CURRENT);
    assertTelemetry("application", app.getId(), v1.componentIdentifier, "option_current");
  }

  private void assertTelemetry(final String ownerType,
                               final String ownerId,
                               final ComponentIdentifier componentIdentifier,
                               final String... expectedTrueAttributes)
  {
    ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor = ArgumentCaptor.forClass(TelemetryData.class);
    verify(telemetrySenderMock).send(telemetryDataArgumentCaptor.capture());
    TelemetryData telemetryData = telemetryDataArgumentCaptor.getValue();
    Map<String, Object> expectedAttributes = new HashMap<>();
    expectedAttributes.put("owner_type", ownerType);
    expectedAttributes.put("owner_id", HdsClientAnalytics.obfuscate(ownerId));
    expectedAttributes.put("component", HdsClientAnalytics.obfuscate(JsonUtils.writeUnformatted(componentIdentifier)));
    expectedAttributes.put("option_next_no_violations", "false");
    expectedAttributes.put("option_current", "false");
    for (String attribute : expectedTrueAttributes) {
      expectedAttributes.put(attribute, "true");
    }
    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData.getPurpose()).isEqualTo(TelemetryPurpose.COMPONENT_REMEDIATION);
    assertThat(telemetryData.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    assertThat(telemetryData.getAttributes()).isEqualTo(expectedAttributes);
  }

  private void mockHdsGetComponentDetailsList(List<ComponentDetailsDTO> list) {
    doReturn(list).when(componentInfoServiceMock)
        .getComponentDetailsForAllVersionsNoAuth(eq(OwnerType.APPLICATION), eq(app.getPublicId()),
            any(ComponentIdentifier.class));
  }

  private void assertRemediationZeroCounts(ApiComponentRemediationValueDTO apiComponentRemediationValueDTO) {
    assertThat(apiComponentRemediationValueDTO.componentOverrides).hasSize(0);
    assertThat(apiComponentRemediationValueDTO.policyWaivers).hasSize(0);
    assertThat(apiComponentRemediationValueDTO.versionChanges).hasSize(0);
  }

  private void assertRemediation(ApiComponentRemediationValueDTO apiComponentRemediationValueDTO,
                                 ApiComponentIdentifierDTOV2 expectedComponentIdentifier,
                                 ApiVersionChangeOptionType expectedVersionChangeOptionType)
  {
    assertThat(apiComponentRemediationValueDTO.componentOverrides).hasSize(0);
    assertThat(apiComponentRemediationValueDTO.policyWaivers).hasSize(0);

    assertThat(apiComponentRemediationValueDTO).isNotNull();
    assertThat(apiComponentRemediationValueDTO.versionChanges).hasSize(1);
    ApiVersionChangeOptionDTO apiVersionChangeOptionDTO = apiComponentRemediationValueDTO.versionChanges.get(0);
    assertThat(apiVersionChangeOptionDTO.getType()).isEqualTo(expectedVersionChangeOptionType);
    ApiComponentDTOV2 apiComponentDTOV2 = apiVersionChangeOptionDTO.getData().getComponent();

    assertThat(apiComponentDTOV2.componentIdentifier).isNotNull();
    assertThat(apiComponentDTOV2.componentIdentifier.getFormat()).isEqualTo(expectedComponentIdentifier.getFormat());
    assertThat(apiComponentDTOV2.componentIdentifier.getCoordinates())
        .isEqualTo(expectedComponentIdentifier.getCoordinates());
    assertThat(apiComponentDTOV2.hash).isNull();
    assertThat(apiComponentDTOV2.proprietary).isNull();
  }

  private ApiComponentDTOV2 createComponent(final ComponentIdentifier componentIdentifier) {
    ApiComponentDTOV2 component = new ApiComponentDTOV2();
    component.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);
    return component;
  }
}
