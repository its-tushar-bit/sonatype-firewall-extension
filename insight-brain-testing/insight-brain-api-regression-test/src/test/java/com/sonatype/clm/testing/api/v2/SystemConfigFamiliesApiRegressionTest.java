/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.api.v2;

import com.sonatype.clm.testing.api.AbstractIqApiTest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.model.Organization;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

/**
 * Wide-and-shallow regression coverage for the {@code /api/v2/config/*} subfamilies —
 * one contract pin per system-configuration resource. Every family lands its path at a
 * {@link PublicApiPaths} constant so a rename fails to compile here; per-endpoint depth
 * lives at the resource tier (e.g. {@code ApiMailConfigurationResourceTest},
 * {@code ApiZScalerConfigurationResourceTest}).
 *
 * <p>
 * <b>Auth ordering.</b> Every family rejects unauthenticated access with 401 upstream
 * of any resource-body validation, license check, or feature-flag gate. That ordering is
 * what the 401 tests pin — a change to Shiro filter registration or resource-tier auth
 * annotations would surface here regardless of the family-specific state.
 *
 * <p>
 * <b>AC deviations.</b>
 * <ul>
 * <li>Happy-path GETs on most families return 200 with a default DTO or 404 when the
 * config row is absent — the branch depends on seeded state and isn't part of the wire
 * contract. These families receive 401-anon-only pins; happy-path lives at the resource
 * tier.</li>
 * <li>{@code /api/v2/config/features/{feature}} is POST/DELETE only (401 pin uses POST);
 * {@code /api/v2/config/integrationVersions/cache} is DELETE-only.</li>
 * </ul>
 */
public class SystemConfigFamiliesApiRegressionTest
    extends AbstractIqApiTest
{

  private static final String CONFIG_FEATURES_BASE = PublicApiPaths.CONFIG_FEATURES_PATH;

  /**
   * Un-templated prefix of {@link PublicApiPaths#SCAN_HEALTH_CONFIG_PATH_V2} — the raw
   * constant carries a Jersey {@code {ownerType:...}}/{@code {ownerId}} template that
   * leaks through {@code HttpRequest#path()} URL-encoding. Substring at the first
   * {@code /{} gives a compile-time-bound prefix that fails to build if the constant
   * loses its templated shape or is renamed.
   */
  private static final String SCAN_HEALTH_CONFIG_BASE = PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2
      .substring(0, PublicApiPaths.SCAN_HEALTH_CONFIG_PATH_V2.indexOf("/{"));

  @Test
  public void testEnableFeature_unauthenticated_returns401() throws Exception {
    HttpResponse response =
        anonApiPostJson(CONFIG_FEATURES_BASE + "/" + uniqueId("no-such-feature"), Map.of());

    assertResponseStatus(401, response);
  }

  @Test
  public void testDisableFeature_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiDelete(CONFIG_FEATURES_BASE + "/" + uniqueId("no-such-feature"));

    assertResponseStatus(401, response);
  }

  @Test
  public void testGetMailConfig_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(PublicApiPaths.MAIL_CONFIG_RESOURCE_PATH_V2);

    assertResponseStatus(401, response);
  }

  /**
   * <b>Happy-path 200 omitted.</b> The proxy-config resource returns
   * {@code 404 "Proxy server not configured."} on a fresh IQ Server rather than a default
   * DTO — an initial happy-path pin would require seeding the proxy config row (either
   * via {@code TemporaryEntity} plumbing not currently exposed here, or via a PUT that
   * would then need explicit cleanup because {@code TemporaryEntity.after()} does not
   * clear the system-scoped proxy config). Owner-tier tests
   * ({@code ApiProxyServerConfigurationResourceTest}) exercise both the 200 and 404
   * branches — this class pins only the 401 anon contract.
   */
  @Test
  public void testGetHttpProxyServerConfig_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(PublicApiPaths.PROXY_SERVER_CONFIG_PATH_V2);

    assertResponseStatus(401, response);
  }

  @Test
  public void testGetSamlConfig_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(PublicApiPaths.SAML_CONFIG_RESOURCE_PATH_V2);

    assertResponseStatus(401, response);
  }

  @Test
  public void testGetOidcConfig_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(PublicApiPaths.OIDC_CONFIG_RESOURCE_PATH_V2);

    assertResponseStatus(401, response);
  }

  @Test
  public void testGetCrowdConfig_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(PublicApiPaths.CROWD_CONFIG_RESOURCE_PATH_V2);

    assertResponseStatus(401, response);
  }

  @Test
  public void testGetJiraConfig_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(PublicApiPaths.JIRA_CONFIG_RESOURCE_PATH_V2);

    assertResponseStatus(401, response);
  }

  @Test
  public void testGetScanHealthConfig_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(
        SCAN_HEALTH_CONFIG_BASE + "/organization/" + Organization.ROOT_ORGANIZATION_ID);

    assertResponseStatus(401, response);
  }

  @Test
  public void testGetReverseProxyAuthConfig_unauthenticated_returns401() throws Exception {
    HttpResponse response =
        anonApiGet(PublicApiPaths.REVERSE_PROXY_AUTHENTICATION_CONFIG_RESOURCE_PATH_V2);

    assertResponseStatus(401, response);
  }

  @Test
  public void testGetRepositoryConnectionConfig_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(
        PublicApiPaths.REPOSITORY_CONNECTION_CONFIG_PATH_V2 + "/organization/"
            + Organization.ROOT_ORGANIZATION_ID);

    assertResponseStatus(401, response);
  }

  // /api/v2/config/artifactoryConnection

  @Test
  public void testGetArtifactoryConnectionConfig_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(
        PublicApiPaths.ARTIFACTORY_CONNECTION_CONFIG_PATH_V2 + "/organization/"
            + Organization.ROOT_ORGANIZATION_ID);

    assertResponseStatus(401, response);
  }

  @Test
  public void testGetZScalerConfig_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(PublicApiPaths.ZSCALER_CONFIG_RESOURCE_PATH_V2);

    assertResponseStatus(401, response);
  }

  private static final String INTEGRATION_VERSIONS_CACHE_PATH =
      PublicApiPaths.CONFIG_RESOURCE_PATH_V2 + "/integrationVersions/cache";

  /**
   * Happy-path DELETE — the cache invalidator returns 200 (not 204) with a
   * {@code {"entriesInvalidated": <n>}} body. The value depends on live cache state so
   * only its type is pinned. This is the one DELETE happy path in this class because it
   * has no side-effects on config rows: it just clears an in-memory cache.
   */
  @Test
  public void testInvalidateIntegrationVersionsCache_happyPath_returns200() throws Exception {
    HttpResponse response = apiDelete(INTEGRATION_VERSIONS_CACHE_PATH);

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("entriesInvalidated").isNumber();
  }

  @Test
  public void testInvalidateIntegrationVersionsCache_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiDelete(INTEGRATION_VERSIONS_CACHE_PATH);

    assertResponseStatus(401, response);
  }
}
