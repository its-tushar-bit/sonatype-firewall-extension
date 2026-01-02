/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.ComponentChangeDetectionConfigurationDAO;
import com.sonatype.insight.brain.malware.defense.ApiMalwareComponentEvaluationRequestList.ApiMalwareComponentEvaluationRequest;
import com.sonatype.insight.brain.model.ComponentChangeDetectionEvent;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.stream.Collectors;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.junit.experimental.categories.Category;

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_CONFIG_PATH;
import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_TENANT_CONFIG_FEATURES_PATH;
import static com.sonatype.insight.brain.api.PublicApiPaths.COMPONENT_CHANGE_DETECTION_RESOURCE_PATH;
import static com.sonatype.insight.brain.api.v2.ApiComponentChangeDetectionResource.CONFIGURATION_PATH;
import static com.sonatype.insight.brain.api.v2.ApiComponentChangeDetectionResource.EVENT_PATH;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.COMPONENT_CHANGE_DETECTION_API;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.COMPONENT_CHANGE_DETECTION_MAX_COMPONENTS;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Category(SlowTest.class)
public class MtiqApiComponentChangeDetectionResourceTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  private static final String COMPONENT_CHANGE_CONFIGURATION_PATH =
      COMPONENT_CHANGE_DETECTION_RESOURCE_PATH + "/" + CONFIGURATION_PATH;

  private static final String COMPONENT_CHANGE_EVENT_PATH = COMPONENT_CHANGE_DETECTION_RESOURCE_PATH + "/" + EVENT_PATH;

  private String tenantSlug;

  @Before
  public void setUp() throws Exception {
    tenantSlug = getTestTenant().tenantSlug;
    enableFeatureFlagAndLicense(tenantSlug);
  }

  @Test
  public void testAddComponents() throws Exception {
    HttpResponse response = addComponentsRequest().post();
    assertResponseStatus(204, response);
  }

  @Test
  public void testAddComponents_unauthenticated() throws Exception {
    HttpResponse response = addComponentsRequest().anon().post();
    assertResponseStatus(401, response);
  }

  @Test
  public void testAddComponents_exceedsMaxComponents() throws Exception {
    configureComponentChangeDetectionMaxComponents(tenantSlug, 1);

    HttpResponse response = addComponentsRequest().post();
    List<Map<String, String>> components = response.getBodyList();

    assertResponseStatus(200, response);
    assertThat(components).hasSize(1);
    assertThat(components.get(0)).contains(
        entry("hash", "a13168d8f7c3b9c9a899"),
        entry("packageUrl", "pkg:maven/org.sonatype/maven-policy-demo@1.1.0?type=jar")
    );
  }

  @Test
  public void testGetComponentChangeDetectionEvents_unauthenticated() throws Exception {
    HttpResponse response = componentChangeDetectionEventRequest().anon().get();
    assertResponseStatus(401, response);
  }

  @Test
  public void testGetAndDeleteComponentChangeDetectionEvents() throws Exception {
    Date dateTimeMinusTenDays = Date.from(Instant.now().minus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS));
    Date dateTimeMinusThreeDays = Date.from(Instant.now().minus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS));
    String nowMinusFiveDays = Instant.now().minus(5, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
    String now = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();

    tenantTemporaryEntity.newComponentChangeDetectionEvent("purl1", "demo-1", dateTimeMinusTenDays);
    tenantTemporaryEntity.newComponentChangeDetectionEvent("purl2", "demo-2", dateTimeMinusThreeDays);
    tenantTemporaryEntity.newComponentChangeDetectionEvent("purl3", "demo-3", dateTimeMinusThreeDays);

    HttpResponse response = componentChangeDetectionEventRequest().get();
    List<ComponentChangeDetectionEvent> responseList = getBodyByTypeReference(response.getBodyBytes(),
        new TypeReference<List<ComponentChangeDetectionEvent>>() { });

    assertResponseStatus(200, response);
    assertThat(responseList).hasSize(3);
    assertThat(getPurls(responseList)).contains("purl1", "purl2", "purl3");

    response = componentChangeDetectionEventRequest().query("timestamp", nowMinusFiveDays).post();

    assertResponseStatus(200, response);

    response = componentChangeDetectionEventRequest().get();

    assertResponseStatus(200, response);
    responseList = getBodyByTypeReference(response.getBodyBytes(),
        new TypeReference<List<ComponentChangeDetectionEvent>>() { });
    assertThat(responseList).hasSize(2);
    assertThat(getPurls(responseList)).doesNotContain("purl1");
    assertThat(getPurls(responseList)).contains("purl2", "purl3");

    response = componentChangeDetectionEventRequest().query("timestamp", now).post();

    assertResponseStatus(200, response);

    response = componentChangeDetectionEventRequest().get();

    assertResponseStatus(200, response);
    responseList = getBodyByTypeReference(response.getBodyBytes(),
        new TypeReference<List<ComponentChangeDetectionEvent>>() { });
    assertThat(responseList).isEmpty();
  }

  private <T> T getBodyByTypeReference(final byte[] bodyBytes, final TypeReference<T> typeRef) {
    try {
      return new ObjectMapper().readValue(bodyBytes, typeRef);
    }
    catch (final IOException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private List<String> getPurls(List<ComponentChangeDetectionEvent> results) {
    return results.stream()
        .map(ComponentChangeDetectionEvent::getPurl)
        .collect(Collectors.toList());
  }

  @Test
  public void testDeleteComponentChangeDetectionEvents_unauthenticated() throws Exception {
    HttpResponse response = componentChangeDetectionEventRequest()
        .query("timestamp", DateTime.now().toString("yyyy-MM-dd'T'HH:mm:ss'Z'"))
        .anon()
        .post();
    assertResponseStatus(401, response);
  }

  @Test
  public void testAddComponents_invalidPurlSent() throws Exception {
    ComponentChangeDetectionConfigurationDAO configurationDAO;
    configurationDAO = lookup(ComponentChangeDetectionConfigurationDAO.class);

    List<ApiMalwareComponentEvaluationRequest> components = Arrays.asList(
        new ApiMalwareComponentEvaluationRequest("a13168d8f7c3b9c9a899",
            "pkg:maven/org.sonatype/maven-policy-demo@1.1.0?type=jar"),
        new ApiMalwareComponentEvaluationRequest("b24568d8f7c3b0n9a666",
            "invalidPurl"),
        new ApiMalwareComponentEvaluationRequest("b24568d8f7c3b0n9a667",
            "pkg:pypi/aiobotocore@2.4.0")
    );

    HttpResponse response =
        restRequest().path(COMPONENT_CHANGE_CONFIGURATION_PATH).body(components, MediaType.APPLICATION_JSON).post();
    assertResponseStatus(204, response);
    assertThat(configurationDAO.getCount()).isEqualTo(1);
  }

  private void enableFeatureFlagAndLicense(final String tenantSlug) throws Exception {
    adminRestRequest(ADMIN_TENANT_CONFIG_FEATURES_PATH)
        .parameter(tenantSlug)
        .path(COMPONENT_CHANGE_DETECTION_API)
        .post();

    setFeatures(LicensedFeature.FIREWALL);
  }

  private void configureComponentChangeDetectionMaxComponents(
      final String tenantSlug,
      final Integer maxComponents) throws Exception
  {
    Map<String, Object> propertyConfiguration =
        Collections.singletonMap(COMPONENT_CHANGE_DETECTION_MAX_COMPONENTS, maxComponents);

    adminRestRequest(ADMIN_CONFIG_PATH)
        .parameter(tenantSlug)
        .body(propertyConfiguration)
        .put();
  }

  private HttpRequest addComponentsRequest() {
    List<ApiMalwareComponentEvaluationRequest> components = Arrays.asList(
        new ApiMalwareComponentEvaluationRequest("a13168d8f7c3b9c9a899",
            "pkg:maven/org.sonatype/maven-policy-demo@1.1.0?type=jar"),
        new ApiMalwareComponentEvaluationRequest("b24568d8f7c3b0n9a8n3",
            "pkg:maven/org.sonatype/maven-policy-demo@2.2.0?type=jar")
    );
    return restRequest()
        .path(COMPONENT_CHANGE_CONFIGURATION_PATH)
        .body(components, MediaType.APPLICATION_JSON);
  }

  private HttpRequest componentChangeDetectionEventRequest() {
    return restRequest().path(COMPONENT_CHANGE_EVENT_PATH);
  }
}
