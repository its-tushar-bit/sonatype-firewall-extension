/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.hds.AbstractHdsClientTest;
import com.sonatype.insight.brain.hds.AffectedComponentDTO;
import com.sonatype.insight.brain.hds.AffectedComponentList;
import com.sonatype.insight.brain.model.consumption.ActivityType;
import com.sonatype.insight.brain.model.consumption.ConsumptionEvent;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.utils.Retry;
import com.sonatype.insight.brain.version.DefaultVersionService;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Covers the wiring branches in {@code HdsClient.recordConsumption} that are not exercised by
 * {@link com.sonatype.insight.brain.hds.HdsClientTest} or the integration suite:
 *
 * <ol>
 * <li>{@code DEVELOPER_PRIORITIES} fan-out: one {@code record()} call per
 * {@code (refId, coordinates)} entity in the {@code AffectedComponentList} payload.
 * <li>Empty-entities silent no-op: zero entities → {@code record()} is never called.
 * <li>Direct-API bypass: {@code ConsumptionContext.isDirectApiRequest()==true} routes through
 * the count-based path with {@code ActivityType.API}, not the fan-out path.
 * </ol>
 *
 * <p>
 * Tests call through the public API ({@link HdsClient#getWithMultimap}) backed by a real
 * in-process Jetty server (inherited from {@link AbstractHdsClientTest}) so that the
 * {@code execute → recordConsumption} wiring is exercised without any reflective access to
 * private methods.
 *
 * @since 1.205 (CLM-40771)
 */
@RunWith(MockitoJUnitRunner.class)
@Category(SlowTest.class)
public class HdsClientRecordConsumptionTest
    extends AbstractHdsClientTest
{
  private static final String AFFECTED_PATH = "/rest/vulnerability/affected";

  private static final String ORG_ID = "test-org";

  private static final String TIER = "pro";

  private static final String SOURCE = "ui";

  private final ObjectMapper mapper = new ObjectMapper();

  @Inject
  private Configuration configuration;

  private ConsumptionRecorder mockRecorder;

  private CurrentUser mockCurrentUser;

  @Override
  protected void initClient() {
    ProductLicense mockProductLicense = mock(ProductLicense.class);
    // lenient: these stubs are needed for most tests but not Test B (no-op / no emitEvent call).
    // Using lenient() for shared setup that is not triggered in every test path avoids
    // UnnecessaryStubbingException from MockitoJUnitRunner's strict mode.
    Mockito.lenient().when(mockProductLicense.isValid()).thenReturn(true);
    Mockito.lenient().when(mockProductLicense.getFingerprint()).thenReturn("license-fingerprint");
    mockCurrentUser = mock(CurrentUser.class);
    Mockito.lenient().when(mockCurrentUser.getUsernameOrSystem()).thenReturn("testuser");
    mockRecorder = mock(ConsumptionRecorder.class);
    InsightProxy proxy = new InsightProxy(configuration, passwordHandler);
    client = newHdsClient(
        proxy,
        mockProductLicense,
        configuration,
        new DefaultVersionService(),
        telemetryId,
        mockCurrentUser,
        20,
        name -> new Retry(name, 0, null, e -> false, i -> Duration.ZERO),
        mockRecorder);
  }

  @After
  public void clearConsumptionContext() {
    ConsumptionContext.clear();
  }

  // -------------------------------------------------------------------------
  // Test A: DEVELOPER_PRIORITIES fan-out emits N record() calls for N entities
  // -------------------------------------------------------------------------

  @Test
  public void recordConsumption_developerPriorities_fanOutOncePerEntity() throws Exception {
    // Given: two components, each with one refId — produces 2 (refId, coordinates) entities
    AffectedComponentDTO comp1 =
        new AffectedComponentDTO("maven", "com.example", "alpha", "1.0.0", List.of("CVE-2025-001"));
    AffectedComponentDTO comp2 =
        new AffectedComponentDTO("maven", "com.example", "beta", "2.0.0", List.of("CVE-2025-002"));
    AffectedComponentList payload = new AffectedComponentList(List.of(comp1, comp2), null, false);
    serveJson(payload);

    // appId must be set for DEVELOPER_PRIORITIES fan-out — cross-app surfaces (no appId)
    // are skipped by design (see 1fd5411dd1).
    ConsumptionContext.set(ORG_ID, TIER, SOURCE);
    ConsumptionContext.get().setAppId("test-app");

    // When
    Multimap<String, String> queryParams = HashMultimap.create();
    queryParams.put("refId", "CVE-2025-001");
    queryParams.put("refId", "CVE-2025-002");
    client.getWithMultimap(AffectedComponentList.class, AFFECTED_PATH, queryParams);

    // Then: one record() call per entity (2 total, one per CVE-component pair)
    ArgumentCaptor<ConsumptionEvent> captor = ArgumentCaptor.forClass(ConsumptionEvent.class);
    verify(mockRecorder, times(2)).record(captor.capture());
    List<ConsumptionEvent> events = captor.getAllValues();
    assertThat(events).extracting(ConsumptionEvent::getActivityType)
        .containsOnly(ActivityType.DEVELOPER_PRIORITIES);
  }

  // -------------------------------------------------------------------------
  // Test B: Empty entities → record() is never called
  // -------------------------------------------------------------------------

  @Test
  public void recordConsumption_developerPriorities_noopWhenAllRefIdsNull() throws Exception {
    // Given: one component with null refIds — DeveloperPrioritiesPayloadExtractor yields 0 entities
    AffectedComponentDTO compNoRefs =
        new AffectedComponentDTO("maven", "com.example", "gamma", "3.0.0", null);
    AffectedComponentList payload = new AffectedComponentList(List.of(compNoRefs), null, false);
    serveJson(payload);

    ConsumptionContext.set(ORG_ID, TIER, SOURCE);

    // When
    Multimap<String, String> queryParams = HashMultimap.create();
    queryParams.put("refId", "CVE-2025-999");
    client.getWithMultimap(AffectedComponentList.class, AFFECTED_PATH, queryParams);

    // Then: zero record() calls — the payload had no valid (refId, coordinates) pairs
    verifyNoInteractions(mockRecorder);
  }

  // -------------------------------------------------------------------------
  // Test C: Direct API request bypasses fan-out → single record() with ActivityType.API
  // -------------------------------------------------------------------------

  @Test
  public void recordConsumption_directApiRequest_bypassesFanOut_emitsSingleApiEvent() throws Exception {
    // Given: same two-component payload as Test A, but directApiRequest=true
    AffectedComponentDTO comp1 =
        new AffectedComponentDTO("maven", "com.example", "alpha", "1.0.0", List.of("CVE-2025-001"));
    AffectedComponentDTO comp2 =
        new AffectedComponentDTO("maven", "com.example", "beta", "2.0.0", List.of("CVE-2025-002"));
    AffectedComponentList payload = new AffectedComponentList(List.of(comp1, comp2), null, false);
    serveJson(payload);

    // directApiRequest=true: overrides activityType to API, skips the fan-out branch entirely.
    // KnownCountExtractor.extractCount(AffectedComponentList) returns 1 (single-object fallback),
    // so the count-based path emits exactly one record() call with ActivityType.API.
    ConsumptionContext.set(ORG_ID, TIER, SOURCE, true);

    // When
    Multimap<String, String> queryParams = HashMultimap.create();
    queryParams.put("refId", "CVE-2025-001");
    queryParams.put("refId", "CVE-2025-002");
    client.getWithMultimap(AffectedComponentList.class, AFFECTED_PATH, queryParams);

    // Then: exactly ONE record() call (not 2), and the activity type is API
    ArgumentCaptor<ConsumptionEvent> captor = ArgumentCaptor.forClass(ConsumptionEvent.class);
    verify(mockRecorder, times(1)).record(captor.capture());
    ConsumptionEvent event = captor.getValue();
    assertThat(event.getActivityType()).isEqualTo(ActivityType.API);
  }

  // -------------------------------------------------------------------------
  // Test D: /rest/component/dependencies POST body extraction → keyed VR event
  // -------------------------------------------------------------------------

  @Test
  public void recordConsumption_componentDependencies_post_emitsKeyedVersionRecommendationEvent() throws Exception {
    PackageUrlIdentifier purl1 = new PackageUrlIdentifier("pkg:maven/com.example/foo@1.0.0");
    PackageUrlIdentifier purl2 = new PackageUrlIdentifier("pkg:maven/com.example/bar@2.0.0");
    List<PackageUrlIdentifier> requestPurls = List.of(purl1, purl2);

    // Serve a minimal ComponentDependenciesDTO response (empty maps are sufficient —
    // VR events use KnownCountExtractor which returns 1 for any non-null object).
    Map<PackageUrlIdentifier, Collection<PackageUrlIdentifier>> depsMap = new HashMap<>();
    Map<PackageUrlIdentifier, ComponentDetails> detailsMap = new HashMap<>();
    ComponentDependenciesDTO response = new ComponentDependenciesDTO(depsMap, detailsMap);
    serveJson(response);

    ConsumptionContext.set(ORG_ID, TIER, SOURCE);
    ConsumptionContext.get().setScanId("scan-vr-test");
    ConsumptionContext.get().setSessionId("session-vr-test");

    // Execute the POST
    client.post(ComponentDependenciesDTO.class, "rest/component/dependencies", requestPurls);

    // Assert: exactly one record() call, activity type is VERSION_RECOMMENDATION, key is non-null
    ArgumentCaptor<ConsumptionEvent> captor = ArgumentCaptor.forClass(ConsumptionEvent.class);
    verify(mockRecorder, times(1)).record(captor.capture());
    ConsumptionEvent event = captor.getValue();
    assertThat(event.getActivityType()).isEqualTo(ActivityType.VERSION_RECOMMENDATION);
    assertThat(event.getIdempotencyKey()).isNotNull();

    // Same PURL set → same key; order should not matter
    serveJson(response);
    ConsumptionContext.clear();
    ConsumptionContext.set(ORG_ID, TIER, SOURCE);
    ConsumptionContext.get().setScanId("scan-vr-test");
    ConsumptionContext.get().setSessionId("session-vr-test");
    client.post(ComponentDependenciesDTO.class, "rest/component/dependencies", List.of(purl2, purl1));
    ArgumentCaptor<ConsumptionEvent> captor2 = ArgumentCaptor.forClass(ConsumptionEvent.class);
    verify(mockRecorder, times(2)).record(captor2.capture());
    String firstKey = captor2.getAllValues().get(0).getIdempotencyKey();
    String secondKey = captor2.getAllValues().get(1).getIdempotencyKey();
    assertThat(firstKey).isEqualTo(secondKey);

    // Different PURL set → different key
    PackageUrlIdentifier purl3 = new PackageUrlIdentifier("pkg:maven/com.example/baz@3.0.0");
    serveJson(response);
    ConsumptionContext.clear();
    ConsumptionContext.set(ORG_ID, TIER, SOURCE);
    ConsumptionContext.get().setScanId("scan-vr-test");
    ConsumptionContext.get().setSessionId("session-vr-test");
    client.post(ComponentDependenciesDTO.class, "rest/component/dependencies", List.of(purl3));
    ArgumentCaptor<ConsumptionEvent> captor3 = ArgumentCaptor.forClass(ConsumptionEvent.class);
    verify(mockRecorder, times(3)).record(captor3.capture());
    String differentKey = captor3.getAllValues().get(2).getIdempotencyKey();
    assertThat(differentKey).isNotEqualTo(firstKey);
  }

  // -------------------------------------------------------------------------
  // Test E: Recorder throws → HDS RPC still completes (exception-swallow invariant)
  // -------------------------------------------------------------------------

  @Test
  public void hdsCall_succeeds_evenWhenRecorderThrows() throws Exception {
    // This test pins the load-bearing invariant: "consumption failures must NEVER
    // break HDS RPCs". recordConsumption wraps everything in catch(Exception e){log.warn}.
    doThrow(new RuntimeException("recorder boom")).when(mockRecorder).record(any());

    AffectedComponentDTO comp =
        new AffectedComponentDTO("maven", "com.example", "alpha", "1.0.0", List.of("CVE-2025-001"));
    AffectedComponentList payload = new AffectedComponentList(List.of(comp), null, false);
    serveJson(payload);

    ConsumptionContext.set(ORG_ID, TIER, SOURCE);
    ConsumptionContext.get().setAppId("test-app");

    Multimap<String, String> queryParams = HashMultimap.create();
    queryParams.put("refId", "CVE-2025-001");

    // HDS call must return cleanly — no exception may propagate out
    AffectedComponentList result =
        client.getWithMultimap(AffectedComponentList.class, AFFECTED_PATH, queryParams);
    assertThat(result).isNotNull();

    // record() was attempted (the catch-warn path fired), confirmed by the doThrow stub
    verify(mockRecorder, times(1)).record(any());
  }

  // -------------------------------------------------------------------------
  // Test F: DEVELOPER_PRIORITIES skips fan-out when appId is null (cross-app surface)
  // -------------------------------------------------------------------------

  @Test
  public void recordConsumption_developerPriorities_skipsFanOut_whenAppIdNull() throws Exception {
    // Cross-app surface: ConsumptionContext is set but appId is NOT set.
    // This is the 461k-row Advanced Search regression vector: iterating every affected
    // package for popular CVEs produces no usable idempotency key because the
    // DEVELOPER_PRIORITIES shape requires appId. Emitting those events would flood
    // consumption_events with NULL-keyed rows that bypass dedup and overcount billing.
    AffectedComponentDTO comp =
        new AffectedComponentDTO("maven", "com.example", "alpha", "1.0.0", List.of("CVE-2025-001"));
    AffectedComponentList payload = new AffectedComponentList(List.of(comp), null, false);
    serveJson(payload);

    // Set context without appId — simulates Advanced Search / cross-app surface
    ConsumptionContext.set(ORG_ID, TIER, SOURCE);
    // ctx.setAppId NOT called intentionally

    Multimap<String, String> queryParams = HashMultimap.create();
    queryParams.put("refId", "CVE-2025-001");
    client.getWithMultimap(AffectedComponentList.class, AFFECTED_PATH, queryParams);

    // No record() call must be made — the fan-out is silently skipped
    verifyNoInteractions(mockRecorder);
  }

  @Test
  public void recordConsumption_versionRecommendation_suppressedForComponentDetailsPageLoad() throws Exception {
    PackageUrlIdentifier purl = new PackageUrlIdentifier("pkg:maven/com.example/foo@1.0.0");
    ComponentDependenciesDTO response = new ComponentDependenciesDTO(new HashMap<>(), new HashMap<>());
    serveJson(response);

    ConsumptionContext.set(ORG_ID, TIER, SOURCE);
    ConsumptionContext.get().setScanId("scan-vr-suppress");
    ConsumptionContext.get().setSessionId("session-vr-suppress");

    try (var ignored = ConsumptionContext.suppressVrCascadeScope()) {
      client.post(ComponentDependenciesDTO.class, "rest/component/dependencies", List.of(purl));
    }

    verifyNoInteractions(mockRecorder);
  }

  @Test
  public void recordConsumption_versionRecommendation_emittedForRemediationFlowsOutsidePageLoad() throws Exception {
    PackageUrlIdentifier purl = new PackageUrlIdentifier("pkg:maven/com.example/foo@1.0.0");
    ComponentDependenciesDTO response = new ComponentDependenciesDTO(new HashMap<>(), new HashMap<>());
    serveJson(response);

    ConsumptionContext.set(ORG_ID, TIER, SOURCE);
    ConsumptionContext.get().setScanId("scan-vr-emit");
    ConsumptionContext.get().setSessionId("session-vr-emit");

    client.post(ComponentDependenciesDTO.class, "rest/component/dependencies", List.of(purl));

    ArgumentCaptor<ConsumptionEvent> captor = ArgumentCaptor.forClass(ConsumptionEvent.class);
    verify(mockRecorder, times(1)).record(captor.capture());
    assertThat(captor.getValue().getActivityType()).isEqualTo(ActivityType.VERSION_RECOMMENDATION);
  }

  @Test
  public void recordConsumption_directApiRequest_precedesSuppressVrCascade() throws Exception {
    // Direct-API calls are remapped to ActivityType.API before the suppression check, so an API
    // event MUST still fire for per-call billing even when suppressVrCascade=true is set.
    PackageUrlIdentifier purl = new PackageUrlIdentifier("pkg:maven/com.example/foo@1.0.0");
    ComponentDependenciesDTO response = new ComponentDependenciesDTO(new HashMap<>(), new HashMap<>());
    serveJson(response);

    ConsumptionContext.set(ORG_ID, TIER, SOURCE, /* directApiRequest */ true);
    ConsumptionContext.get().setScanId("scan-api-precedence");
    ConsumptionContext.get().setSessionId("session-api-precedence");
    ConsumptionContext.get().setSuppressVrCascade(true);

    client.post(ComponentDependenciesDTO.class, "rest/component/dependencies", List.of(purl));

    ArgumentCaptor<ConsumptionEvent> captor = ArgumentCaptor.forClass(ConsumptionEvent.class);
    verify(mockRecorder, times(1)).record(captor.capture());
    assertThat(captor.getValue().getActivityType()).isEqualTo(ActivityType.API);
  }

  // -------------------------------------------------------------------------
  // Helper: configure the Jetty handler to return a JSON-serialised payload
  // -------------------------------------------------------------------------

  private void serveJson(Object payload) throws Exception {
    String json = mapper.writeValueAsString(payload);
    handler = new HttpServlet()
    {
      @Override
      protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.OK_200);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(json);
      }
    };
  }
}
