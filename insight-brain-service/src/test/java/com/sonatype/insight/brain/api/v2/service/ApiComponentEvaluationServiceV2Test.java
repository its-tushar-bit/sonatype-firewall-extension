/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDetailsDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationRequestDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationResultDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentEvaluationTicketDTOV2;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public class ApiComponentEvaluationServiceV2Test
    extends AbstractComponentTest
{
  private static final long RETRY_INTERVAL = 50;

  private static final int NUM_TRIES = 1000;

  private static final int CHUNK_SIZE = 5;

  @Inject
  private ApiComponentEvaluationServiceV2 apiComponentEvaluationService;

  @Inject
  private PolicyDAO policyDAO;

  @Mock
  private HdsClient client;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  private ComponentEvaluationV2Helper componentEvaluationV2Helper;

  private Organization org;

  private Application app;

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(client);
    binder.bind(ShutdownHandler.class).toInstance(mockShutdownHandler);
    super.configure(binder);
  }

  @Before
  public void setupApplication() {
    componentEvaluationV2Helper = new ComponentEvaluationV2Helper(policyDAO);

    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());

    apiComponentEvaluationService.setChunkSize(CHUNK_SIZE);
  }

  @Test
  public void testApiComponentEvaluationServiceV2_AddsExecutorToShutdownHandler() {
    verify(mockShutdownHandler).add(apiComponentEvaluationService.getExecutor());
  }

  private void mockHdsRequest(ComponentEvaluationDataRequestList hdsRequest, ComponentEvaluationDataList hdsResult) {
    doReturn(hdsResult).when(client).post(eq(ComponentEvaluationDataList.class),
        eq(ApiComponentDetailsServiceV2.HDS_COMPONENT_DETAILS_PATH), eq(hdsRequest),
        eq(ApiComponentDetailsServiceV2.PURPOSE_EVALUATION));
  }

  @Test
  public void testEvaluateComponents_chunked() throws Exception {
    LinkedHashSet<License> declaredLicenseSet = new LinkedHashSet<>(
        Collections.singletonList(new License("Apache-2.0-LGPL-2.1+-MPL-1.1", "Apache-2.0 or LGPL-2.1+ or MPL-1.1")));
    LinkedHashSet<License> observedLicenseSet = new LinkedHashSet<>(
        Collections.singletonList(new License("Apache-2.0-GPL-2.0+-LGPL-2.0+", "Apache-2.0 or GPL-2.0+ or LGPL-2.0+")));
    List<SecurityVulnerability> securityVulnerabilities = componentEvaluationV2Helper.createSecurityVulnerabilities();

    Map<String, Policy> policies = componentEvaluationV2Helper.createPolicies(org, app);
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();

    int numChunks = 2;
    for (int chunk = 0; chunk < numChunks; chunk++) {
      ApiComponentEvaluationRequestDTOV2 requestChunk = new ApiComponentEvaluationRequestDTOV2();
      ComponentEvaluationDataList componentEvaluationDataList = new ComponentEvaluationDataList();
      componentEvaluationDataList.components = new ArrayList<>();
      for (int i = 0; i < CHUNK_SIZE; i++) {
        int componentIndex = request.components.size();
        ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g" + componentIndex,
            "a" + componentIndex, "v" + componentIndex, "", "e" + componentIndex);
        String packageUrl = PackageUrlIdentifier.toPackageUrl(componentIdentifier);
        ApiComponentDTOV2 component = componentEvaluationV2Helper.createComponent(componentIdentifier,
            "h" + componentIndex, packageUrl);
        request.components.add(component);
        requestChunk.components.add(component);
        componentEvaluationDataList.components.add(componentEvaluationV2Helper.createComponentEvaluationData(
            componentIdentifier, component.hash, MatchState.EXACT, i, declaredLicenseSet, observedLicenseSet,
            securityVulnerabilities, componentIndex /* popularity */));
      }
      mockHdsRequest(componentEvaluationV2Helper.toHdsRequest(requestChunk), componentEvaluationDataList);
    }
    int numComponents = CHUNK_SIZE * 2;

    ApiComponentEvaluationTicketDTOV2 ticket = apiComponentEvaluationService.evaluateComponents(app.getId(), request);
    ApiComponentEvaluationResultDTOV2 details = getComponentEvaluationResult(ticket);

    assertThat(details).isNotNull();
    assertThat(details.errorMessage).isNull();
    assertThat(details.isError).isFalse();
    assertThat(details.applicationId).isEqualTo(app.getId());
    assertThat(details.evaluationDate).isNotNull();
    assertThat(details.submittedDate).isNotNull();
    assertThat(details.results).hasSize(numComponents);
    int i = 0;
    for (ApiComponentDetailsDTOV2 componentDetailsDTOV2 : details.results) {
      componentEvaluationV2Helper.assertComponentDetails(componentDetailsDTOV2, request.components.get(i),
          MatchState.EXACT.getId(), declaredLicenseSet, observedLicenseSet,
          securityVulnerabilities, i /* popularity */, policies);
      i++;
    }
  }

  @Test
  public void testEvaluateComponents_matchByLongHash() throws Exception {
    ApiComponentEvaluationRequestDTOV2 request = new ApiComponentEvaluationRequestDTOV2();

    String hash = "12345678901234567890";
    String longHash = hash + "a";
    // The CLM request uses long hash
    ApiComponentDTOV2 component = componentEvaluationV2Helper.createComponent(null, longHash);
    request.components.add(component);

    // The HDS request uses short hash
    ComponentEvaluationDataRequestList hdsRequest = componentEvaluationV2Helper.toHdsRequest(request);
    hdsRequest.components.get(0).hash = hash;
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    ComponentEvaluationDataList hdsResult = new ComponentEvaluationDataList();
    hdsResult.components = new ArrayList<>();
    hdsResult.components.add(componentEvaluationV2Helper.createComponentEvaluationData(componentIdentifier, hash,
        MatchState.EXACT, 0, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), null));
    mockHdsRequest(hdsRequest, hdsResult);

    ApiComponentEvaluationTicketDTOV2 ticket = apiComponentEvaluationService.evaluateComponents(app.getId(), request);
    ApiComponentEvaluationResultDTOV2 details = getComponentEvaluationResult(ticket);

    assertThat(details).isNotNull();
    assertThat(details.errorMessage).isNull();
    assertThat(details.isError).isFalse();
    assertThat(details.results).hasSize(1);
    assertThat(details.results.get(0).component.hash).isEqualTo(hash);
    assertThat(details.results.get(0).component.componentIdentifier.toComponentIdentifier())
        .isEqualTo(componentIdentifier);
  }

  private ApiComponentEvaluationResultDTOV2 getComponentEvaluationResult(
      final ApiComponentEvaluationTicketDTOV2 evaluationTicket) throws Exception
  {
    boolean done = false;
    int tryCount = 1;
    while (!done) {
      System.out.println("tryCount=" + tryCount);
      try {
        return apiComponentEvaluationService.getComponentEvaluation(evaluationTicket.applicationId,
            evaluationTicket.resultId);
      }
      catch (NotFoundException e) {
        tryCount++;
      }
      if (tryCount >= NUM_TRIES) {
        done = true;
      }
      Thread.sleep(RETRY_INTERVAL);
    }

    return null;
  }
}
