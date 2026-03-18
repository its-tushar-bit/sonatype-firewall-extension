/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Collection;
import java.util.Collections;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.uri.UriTemplate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TelemetryContainerRequestFilterTest
    extends AbstractComponentTest
{
  @Inject
  private TelemetryContainerRequestFilter telemetryContainerRequestFilter;

  @Before
  @After
  public void clearRestEndpointInvocations() {
    TelemetryContainerRequestFilter.REST_ENDPOINT_INVOCATIONS.get().clear();
  }

  @Test
  public void testCollectAllData_NoRequests() {
    assertRestEndpointTelemetry(telemetryContainerRequestFilter.collectAllData());
  }

  @Test
  public void testCollectAllData_MatchingPaths() {
    telemetryContainerRequestFilter
        .filter(mockContainerRequestContext("GET", PublicApiPaths.BASE_PATH + "/something"));
    telemetryContainerRequestFilter
        .filter(mockContainerRequestContext("GET", UserInterfaceLinksHelper.RESOURCE_PATH + "/something"));

    assertRestEndpointTelemetry(telemetryContainerRequestFilter.collectAllData(),
        new RestEndpointTelemetry("GET", PublicApiPaths.BASE_PATH + "/something", 1),
        new RestEndpointTelemetry("GET", UserInterfaceLinksHelper.RESOURCE_PATH + "/something", 1));
  }

  @Test
  public void testCollectAllData_NonMatchingPath() {
    telemetryContainerRequestFilter.filter(mockContainerRequestContext("GET", "apibutnotreally"));

    assertThat(telemetryContainerRequestFilter.collectAllData()).isEmpty();
  }

  @Test
  public void testCollectAllData_SameMethodSamePath() {
    telemetryContainerRequestFilter.filter(mockContainerRequestContext("GET", createMatchingPath("path")));
    telemetryContainerRequestFilter.filter(mockContainerRequestContext("GET", createMatchingPath("path")));

    Collection<TelemetryData> telemetryData = telemetryContainerRequestFilter.collectAllData();

    assertRestEndpointTelemetry(telemetryData, new RestEndpointTelemetry("GET", createMatchingPath("path"), 2));
  }

  @Test
  public void testCollectAllData_DifferentMethodSamePath() {
    telemetryContainerRequestFilter.filter(mockContainerRequestContext("GET", createMatchingPath("path")));
    telemetryContainerRequestFilter.filter(mockContainerRequestContext("PUT", createMatchingPath("path")));

    Collection<TelemetryData> telemetryData = telemetryContainerRequestFilter.collectAllData();

    assertRestEndpointTelemetry(telemetryData,
        new RestEndpointTelemetry("GET", createMatchingPath("path"), 1),
        new RestEndpointTelemetry("PUT", createMatchingPath("path"), 1));
  }

  @Test
  public void testCollectAllData_SameMethodDifferentPath() {
    telemetryContainerRequestFilter.filter(mockContainerRequestContext("GET", createMatchingPath("path")));
    telemetryContainerRequestFilter.filter(mockContainerRequestContext("GET", createMatchingPath("other", "path")));

    Collection<TelemetryData> telemetryData = telemetryContainerRequestFilter.collectAllData();

    assertRestEndpointTelemetry(telemetryData,
        new RestEndpointTelemetry("GET", createMatchingPath("path"), 1),
        new RestEndpointTelemetry("GET", createMatchingPath("other", "path"), 1));
  }

  @Test
  public void testCollectAllData_DifferentMethodDifferentPath() {
    telemetryContainerRequestFilter.filter(mockContainerRequestContext("GET", createMatchingPath("path")));
    telemetryContainerRequestFilter.filter(mockContainerRequestContext("PUT", createMatchingPath("other", "path")));

    Collection<TelemetryData> telemetryData = telemetryContainerRequestFilter.collectAllData();

    assertRestEndpointTelemetry(telemetryData,
        new RestEndpointTelemetry("GET", createMatchingPath("path"), 1),
        new RestEndpointTelemetry("PUT", createMatchingPath("other", "path"), 1));
  }

  @Test
  public void testIsClusterTelemetry() {
    assertThat(telemetryContainerRequestFilter.isClusterTelemetry()).isFalse();
  }

  @Test
  public void testShouldNotLeakDataBetweenTenants_whenMultiTenantMode() {
    Tenant tenant1 = testAsNewTenant(testName, t1 -> {
      telemetryContainerRequestFilter
          .filter(mockContainerRequestContext("GET", PublicApiPaths.BASE_PATH + "/something"));
      telemetryContainerRequestFilter
          .filter(mockContainerRequestContext("GET", UserInterfaceLinksHelper.RESOURCE_PATH + "/something"));
    });

    Tenant tenant2 = testAsNewTenant(testName, t2 -> {
      telemetryContainerRequestFilter
          .filter(mockContainerRequestContext("GET", PublicApiPaths.BASE_PATH + "/something"));
      telemetryContainerRequestFilter
          .filter(mockContainerRequestContext("GET", UserInterfaceLinksHelper.RESOURCE_PATH + "/something"));
      telemetryContainerRequestFilter
          .filter(mockContainerRequestContext("GET", PublicApiPaths.BASE_PATH + "/something"));
    });

    testAsTenant(tenant1, t1 -> {
      assertRestEndpointTelemetry(telemetryContainerRequestFilter.collectAllData(),
          new RestEndpointTelemetry("GET", PublicApiPaths.BASE_PATH + "/something", 1),
          new RestEndpointTelemetry("GET", UserInterfaceLinksHelper.RESOURCE_PATH + "/something", 1));
    });

    testAsTenant(tenant2, t2 -> {
      assertRestEndpointTelemetry(telemetryContainerRequestFilter.collectAllData(),
          new RestEndpointTelemetry("GET", PublicApiPaths.BASE_PATH + "/something", 2),
          new RestEndpointTelemetry("GET", UserInterfaceLinksHelper.RESOURCE_PATH + "/something", 1));
    });
  }

  private String createMatchingPath(String... pathSegments) {
    return "api/" + String.join("/", pathSegments);
  }

  private ContainerRequestContext mockContainerRequestContext(String method, String path) {
    ContainerRequestContext mockContainerRequestContext = mock(ContainerRequestContext.class);
    lenient().when(mockContainerRequestContext.getMethod()).thenReturn(method);
    ExtendedUriInfo mockExtendedUriInfo = mock(ExtendedUriInfo.class);
    when(mockExtendedUriInfo.getPath()).thenReturn(path);
    lenient().when(mockExtendedUriInfo.getMatchedTemplates())
        .thenReturn(Collections.singletonList(new UriTemplate(path)));
    lenient().when(mockContainerRequestContext.getUriInfo()).thenReturn(mockExtendedUriInfo);
    return mockContainerRequestContext;
  }

  private void assertRestEndpointTelemetry(
      Collection<TelemetryData> telemetryData,
      RestEndpointTelemetry... restEndpointTelemetry)
  {
    assertThat(telemetryData).isNotNull();
    assertThat(telemetryData).extracting(TelemetryData::getPurpose)
        .allMatch(purpose -> purpose.equals(TelemetryPurpose.REST_ENDPOINT_USAGE));
    assertThat(telemetryData)
        .extracting(
            t -> (RestEndpointTelemetry) t.getAttributes().get(TelemetryContainerRequestFilter.REST_ENDPOINT_TELEMETRY))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(restEndpointTelemetry);
  }
}
