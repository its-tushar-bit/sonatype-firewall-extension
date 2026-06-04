/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay;

import java.time.Duration;

import com.sonatype.insight.brain.relay.dto.RelayAckResponse;
import com.sonatype.insight.brain.relay.dto.RelayEventsResponse;
import com.sonatype.insight.brain.relay.dto.RelayRegisterResponse;
import com.sonatype.insight.brain.relay.dto.RelayRotateKeyResponse;
import com.sonatype.insight.brain.relay.dto.RelayRotateWebhookSecretResponse;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.utils.Retry;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.error.exception.BadRequestException;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.sonatype.insight.error.exception.NotFoundException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotAuthorizedException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RelayClientTest
{
  @Rule
  public WireMockRule relayServer = new WireMockRule(wireMockConfig().dynamicPort());

  private RelayClient client;

  @Before
  public void before() {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getRelayUrl()).thenReturn(relayServer.baseUrl() + "/");
    when(configuration.getConnectTimeoutInSeconds()).thenReturn(2);
    when(configuration.getSocketTimeoutInSeconds()).thenReturn(2);
    PasswordHandler passwordHandler = mock(PasswordHandler.class);
    InsightProxy proxy = new InsightProxy(configuration, passwordHandler);
    // Disable retries so error-path tests don't have to wait or reseed stubs.
    client = new RelayClient(proxy, configuration,
        name -> new Retry(name, 0, null, e -> false, i -> Duration.ZERO));
  }

  @Test
  public void register_success_postsRawBytesToApiRegister() {
    relayServer.stubFor(post(urlPathEqualTo("/api/register"))
        .withHeader("Content-Type", equalTo("application/octet-stream"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"apiKey\":\"k\",\"webhookUrl\":\"https://relay.example/webhook/abc/github\","
                + "\"webhookSecret\":\"s\",\"customerId\":\"cust-1\"}")));

    RelayRegisterResponse response = client.register("license-bytes".getBytes());

    assertThat(response.getApiKey()).isEqualTo("k");
    assertThat(response.getWebhookUrl()).isEqualTo("https://relay.example/webhook/abc/github");
    assertThat(response.getWebhookSecret()).isEqualTo("s");
    assertThat(response.getCustomerId()).isEqualTo("cust-1");
    relayServer.verify(com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor(urlPathEqualTo("/api/register"))
        .withRequestBody(com.github.tomakehurst.wiremock.client.WireMock.binaryEqualTo("license-bytes".getBytes())));
  }

  @Test
  public void register_missingLicenseBytes_isBadRequest() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> client.register(null));
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> client.register(new byte[0]));
  }

  @Test
  public void register_401_isNotAuthorized() {
    relayServer.stubFor(post(urlPathEqualTo("/api/register"))
        .willReturn(aResponse().withStatus(401).withBody("nope")));

    assertThatExceptionOfType(NotAuthorizedException.class)
        .isThrownBy(() -> client.register("license-bytes".getBytes()));
  }

  @Test
  public void register_500_mapsTo500() {
    relayServer.stubFor(post(urlPathEqualTo("/api/register"))
        .willReturn(aResponse().withStatus(500).withBody("boom")));

    assertThatExceptionOfType(InternalServerErrorException.class)
        .isThrownBy(() -> client.register("license-bytes".getBytes()));
  }

  @Test
  public void register_503_mapsToBadGateway() {
    relayServer.stubFor(post(urlPathEqualTo("/api/register"))
        .willReturn(aResponse().withStatus(503)));

    assertThatExceptionOfType(BadGatewayException.class)
        .isThrownBy(() -> client.register("license-bytes".getBytes()));
  }

  @Test
  public void register_malformedJson_failsAsBadGateway() {
    relayServer.stubFor(post(urlPathEqualTo("/api/register"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("not json")));

    assertThatExceptionOfType(BadGatewayException.class)
        .isThrownBy(() -> client.register("license-bytes".getBytes()));
  }

  @Test
  public void registerGitHubApp_success_sendsJsonBodyToApiRegister() {
    String licenseB64 = java.util.Base64.getEncoder().encodeToString("license-bytes".getBytes());
    relayServer.stubFor(post(urlPathEqualTo("/api/register"))
        .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
        .withRequestBody(equalToJson("{\"license\":\"" + licenseB64 + "\",\"installationIds\":[\"42\"]}"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"apiKey\":\"k\",\"webhookUrl\":null,\"webhookSecret\":null,\"customerId\":\"cust-1\"}")));

    RelayRegisterResponse response = client.registerGitHubApp("license-bytes".getBytes(), "42");

    assertThat(response.getApiKey()).isEqualTo("k");
    assertThat(response.getCustomerId()).isEqualTo("cust-1");
  }

  @Test
  public void registerGitHubApp_missingInstallationId_isBadRequest() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> client.registerGitHubApp("license-bytes".getBytes(), null));
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> client.registerGitHubApp("license-bytes".getBytes(), "  "));
  }

  @Test
  public void registerGitHubApp_existingApiKey_setsRelayKeyHeader() {
    // Same-mode (App → second App) re-register: relay treats the call as in-place when
    // X-Relay-Key matches an active customer. The new installation_id is added to the
    // existing customer's installation-index instead of starting a competing customer.
    String licenseB64 = java.util.Base64.getEncoder().encodeToString("license-bytes".getBytes());
    relayServer.stubFor(post(urlPathEqualTo("/api/register"))
        .withHeader("X-Relay-Key", equalTo("existing-api-key"))
        .withRequestBody(equalToJson("{\"license\":\"" + licenseB64 + "\",\"installationIds\":[\"99\"]}"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(
                "{\"apiKey\":\"k\",\"webhookUrl\":null,\"webhookSecret\":null,\"customerId\":\"cust-existing\"}")));

    RelayRegisterResponse response = client.registerGitHubApp("license-bytes".getBytes(), "99", "existing-api-key");

    assertThat(response.getCustomerId()).isEqualTo("cust-existing");
    relayServer.verify(com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor(urlPathEqualTo("/api/register"))
        .withHeader("X-Relay-Key", equalTo("existing-api-key")));
  }

  @Test
  public void registerGitHubApp_nullApiKey_omitsRelayKeyHeader() {
    // Fresh / first-time register: no api key available; relay accepts as anonymous when the
    // license fingerprint is also new.
    relayServer.stubFor(post(urlPathEqualTo("/api/register"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"apiKey\":\"k\",\"webhookUrl\":null,\"webhookSecret\":null,\"customerId\":\"cust-fresh\"}")));

    client.registerGitHubApp("license-bytes".getBytes(), "42", null);

    relayServer.verify(com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor(urlPathEqualTo("/api/register"))
        .withoutHeader("X-Relay-Key"));
  }

  @Test
  public void registerGitHubApp_missingLicenseBytes_isBadRequest() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> client.registerGitHubApp(null, "42"));
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> client.registerGitHubApp(new byte[0], "42"));
  }

  @Test
  public void setGitHubAppWebhookSecret_success_postsApiKeyAndJsonBody() {
    relayServer.stubFor(post(urlPathEqualTo("/api/github-app/webhook-secret"))
        .withHeader("X-Relay-Key", equalTo("k"))
        .withRequestBody(equalToJson("{\"webhookSecret\":\"hmac\"}"))
        .willReturn(aResponse().withStatus(200).withBody("{\"ok\":true}")));

    client.setGitHubAppWebhookSecret("k", "hmac");

    relayServer.verify(com.github.tomakehurst.wiremock.client.WireMock
        .postRequestedFor(urlPathEqualTo("/api/github-app/webhook-secret"))
        .withHeader("X-Relay-Key", equalTo("k")));
  }

  @Test
  public void setGitHubAppWebhookSecret_blankApiKey_isBadRequest() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> client.setGitHubAppWebhookSecret(null, "hmac"));
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> client.setGitHubAppWebhookSecret("", "hmac"));
  }

  @Test
  public void setGitHubAppWebhookSecret_blankSecret_isBadRequest() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> client.setGitHubAppWebhookSecret("k", null));
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> client.setGitHubAppWebhookSecret("k", ""));
  }

  @Test
  public void setGitHubAppWebhookSecret_401_isNotAuthorized() {
    relayServer.stubFor(post(urlPathEqualTo("/api/github-app/webhook-secret"))
        .willReturn(aResponse().withStatus(401)));

    assertThatExceptionOfType(NotAuthorizedException.class)
        .isThrownBy(() -> client.setGitHubAppWebhookSecret("k", "hmac"));
  }

  @Test
  public void reRegisterWithApiKey_postsXRelayKeyHeader() {
    relayServer.stubFor(post(urlPathEqualTo("/api/register"))
        .withHeader("X-Relay-Key", equalTo("existing-key"))
        .withHeader("Content-Type", equalTo("application/octet-stream"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"apiKey\":\"k2\",\"webhookUrl\":\"https://relay.example/webhook/abc/github\","
                + "\"webhookSecret\":\"s2\",\"customerId\":\"cust-1\"}")));

    RelayRegisterResponse response = client.reRegisterWithApiKey("license-bytes".getBytes(), "existing-key");

    assertThat(response.getApiKey()).isEqualTo("k2");
    assertThat(response.getCustomerId()).isEqualTo("cust-1");
    relayServer.verify(com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor(urlPathEqualTo("/api/register"))
        .withHeader("X-Relay-Key", equalTo("existing-key"))
        .withRequestBody(com.github.tomakehurst.wiremock.client.WireMock.binaryEqualTo("license-bytes".getBytes())));
  }

  @Test
  public void reRegisterWithApiKey_blankApiKey_throwsBadRequest() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> client.reRegisterWithApiKey("license-bytes".getBytes(), null));
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> client.reRegisterWithApiKey("license-bytes".getBytes(), "  "));
  }

  @Test
  public void reRegisterWithApiKey_missingLicenseBytes_isBadRequest() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> client.reRegisterWithApiKey(null, "k"));
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> client.reRegisterWithApiKey(new byte[0], "k"));
  }

  @Test
  public void recoverWithWebhookToken_postsXRelayWebhookTokenHeader() {
    relayServer.stubFor(post(urlPathEqualTo("/api/register"))
        .withHeader("x-relay-webhook-token", equalTo("existing-token"))
        .withHeader("Content-Type", equalTo("application/octet-stream"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"apiKey\":\"k3\",\"webhookUrl\":\"https://relay.example/webhook/abc/github\","
                + "\"webhookSecret\":\"s3\",\"customerId\":\"cust-1\"}")));

    RelayRegisterResponse response = client.recoverWithWebhookToken("license-bytes".getBytes(), "existing-token");

    assertThat(response.getApiKey()).isEqualTo("k3");
    relayServer.verify(com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor(urlPathEqualTo("/api/register"))
        .withHeader("x-relay-webhook-token", equalTo("existing-token"))
        .withRequestBody(com.github.tomakehurst.wiremock.client.WireMock.binaryEqualTo("license-bytes".getBytes())));
  }

  @Test
  public void recoverWithWebhookToken_blankToken_throwsBadRequest() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> client.recoverWithWebhookToken("license-bytes".getBytes(), null));
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> client.recoverWithWebhookToken("license-bytes".getBytes(), ""));
  }

  @Test
  public void recoverWithWebhookToken_missingLicenseBytes_isBadRequest() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> client.recoverWithWebhookToken(null, "t"));
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> client.recoverWithWebhookToken(new byte[0], "t"));
  }

  @Test
  public void rotateApiKey_success_returnsNewKeyAndExpiry() {
    relayServer.stubFor(post(urlPathEqualTo("/api/rotate-key"))
        .withHeader("X-Relay-Key", equalTo("old"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"apiKey\":\"new\",\"previousKeyExpiresAt\":\"2026-01-01T00:05:00Z\"}")));

    RelayRotateKeyResponse response = client.rotateApiKey("old");

    assertThat(response.getApiKey()).isEqualTo("new");
    assertThat(response.getPreviousKeyExpiresAt()).isEqualTo("2026-01-01T00:05:00Z");
    relayServer.verify(
        com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor(urlPathEqualTo("/api/rotate-key"))
            .withHeader("X-Relay-Key", equalTo("old")));
  }

  @Test
  public void rotateApiKey_blankApiKey_isBadRequest() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> client.rotateApiKey(null));
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> client.rotateApiKey(""));
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> client.rotateApiKey("  "));
  }

  @Test
  public void rotateApiKey_401_isNotAuthorized() {
    relayServer.stubFor(post(urlPathEqualTo("/api/rotate-key"))
        .willReturn(aResponse().withStatus(401)));

    assertThatExceptionOfType(NotAuthorizedException.class)
        .isThrownBy(() -> client.rotateApiKey("old"));
  }

  @Test
  public void rotateApiKey_503_mapsToBadGateway() {
    relayServer.stubFor(post(urlPathEqualTo("/api/rotate-key"))
        .willReturn(aResponse().withStatus(503)));

    assertThatExceptionOfType(BadGatewayException.class)
        .isThrownBy(() -> client.rotateApiKey("old"));
  }

  @Test
  public void rotateWebhookSecret_success_returnsNewSecretAndExpiry() {
    relayServer.stubFor(post(urlPathEqualTo("/api/rotate-webhook-secret"))
        .withHeader("X-Relay-Key", equalTo("key"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"webhookSecret\":\"newSecret\","
                + "\"previousSecretExpiresAt\":\"2026-01-01T00:05:00Z\"}")));

    RelayRotateWebhookSecretResponse response = client.rotateWebhookSecret("key");

    assertThat(response.getWebhookSecret()).isEqualTo("newSecret");
    assertThat(response.getPreviousSecretExpiresAt()).isEqualTo("2026-01-01T00:05:00Z");
    relayServer.verify(
        com.github.tomakehurst.wiremock.client.WireMock
            .postRequestedFor(urlPathEqualTo("/api/rotate-webhook-secret"))
            .withHeader("X-Relay-Key", equalTo("key")));
  }

  @Test
  public void rotateWebhookSecret_blankApiKey_isBadRequest() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> client.rotateWebhookSecret(null));
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> client.rotateWebhookSecret(""));
  }

  @Test
  public void rotateWebhookSecret_401_isNotAuthorized() {
    relayServer.stubFor(post(urlPathEqualTo("/api/rotate-webhook-secret"))
        .willReturn(aResponse().withStatus(401)));

    assertThatExceptionOfType(NotAuthorizedException.class)
        .isThrownBy(() -> client.rotateWebhookSecret("key"));
  }

  @Test
  public void rotateWebhookSecret_503_mapsToBadGateway() {
    relayServer.stubFor(post(urlPathEqualTo("/api/rotate-webhook-secret"))
        .willReturn(aResponse().withStatus(503)));

    assertThatExceptionOfType(BadGatewayException.class)
        .isThrownBy(() -> client.rotateWebhookSecret("key"));
  }

  @Test
  public void deleteInstallation_success_sendsDeleteWithRelayKey() {
    relayServer.stubFor(delete(urlPathEqualTo("/api/installations/12345"))
        .withHeader("X-Relay-Key", equalTo("key"))
        .willReturn(aResponse().withStatus(204)));

    client.deleteInstallation("key", "12345");

    relayServer.verify(
        com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor(urlPathEqualTo("/api/installations/12345"))
            .withHeader("X-Relay-Key", equalTo("key")));
  }

  @Test
  public void deleteInstallation_blankApiKey_isBadRequest() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> client.deleteInstallation(null, "12345"));
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> client.deleteInstallation("", "12345"));
  }

  @Test
  public void deleteInstallation_blankInstallationId_isBadRequest() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> client.deleteInstallation("key", null));
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> client.deleteInstallation("key", "  "));
  }

  @Test
  public void deleteInstallation_401_isNotAuthorized() {
    relayServer.stubFor(delete(urlPathEqualTo("/api/installations/12345"))
        .willReturn(aResponse().withStatus(401)));

    assertThatExceptionOfType(NotAuthorizedException.class)
        .isThrownBy(() -> client.deleteInstallation("key", "12345"));
  }

  @Test
  public void deleteInstallation_403_mapsToNotAuthorized() {
    // Cross-customer attempt at the relay surfaces as 403; the client maps both 401 and 403
    // to NotAuthorizedException since both indicate the supplied credential cannot perform
    // the requested operation.
    relayServer.stubFor(delete(urlPathEqualTo("/api/installations/12345"))
        .willReturn(aResponse().withStatus(403)));

    assertThatExceptionOfType(NotAuthorizedException.class)
        .isThrownBy(() -> client.deleteInstallation("key", "12345"));
  }

  @Test
  public void pollEvents_success_returnsEvents() {
    relayServer.stubFor(get(urlPathEqualTo("/api/events"))
        .withHeader("X-Relay-Key", equalTo("k"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"events\":[{\"eventId\":\"e1\",\"provider\":\"github\",\"eventType\":\"pull_request_opened\","
                + "\"repositoryUrl\":\"https://github.com/o/r\",\"timestamp\":\"2026-01-01T00:00:00Z\","
                + "\"receiptHandle\":\"r-1\",\"payload\":{\"number\":1}}]}")));

    RelayEventsResponse response = client.pollEvents("k", 50);

    assertThat(response.getEvents()).hasSize(1);
    assertThat(response.getEvents().get(0).getEventId()).isEqualTo("e1");
    assertThat(response.getEvents().get(0).getReceiptHandle()).isEqualTo("r-1");
    assertThat(response.getEvents().get(0).getPayload()).containsEntry("number", 1);
  }

  @Test
  public void pollEvents_empty_returnsEmptyList() {
    relayServer.stubFor(get(urlPathEqualTo("/api/events"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"events\":[]}")));

    RelayEventsResponse response = client.pollEvents("k", 50);
    assertThat(response.getEvents()).isEmpty();
  }

  @Test
  public void pollEvents_401_isNotAuthorized() {
    relayServer.stubFor(get(urlPathEqualTo("/api/events"))
        .willReturn(aResponse().withStatus(401)));

    assertThatExceptionOfType(NotAuthorizedException.class)
        .isThrownBy(() -> client.pollEvents("k", 50));
  }

  @Test
  public void pollEvents_500_mapsTo500() {
    relayServer.stubFor(get(urlPathEqualTo("/api/events"))
        .willReturn(aResponse().withStatus(500)));

    assertThatExceptionOfType(InternalServerErrorException.class)
        .isThrownBy(() -> client.pollEvents("k", 50));
  }

  @Test
  public void pollEvents_503_mapsToBadGateway() {
    relayServer.stubFor(get(urlPathEqualTo("/api/events"))
        .willReturn(aResponse().withStatus(503)));

    assertThatExceptionOfType(BadGatewayException.class)
        .isThrownBy(() -> client.pollEvents("k", 50));
  }

  @Test
  public void pollEvents_malformedJson_isBadGateway() {
    relayServer.stubFor(get(urlPathEqualTo("/api/events"))
        .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("not json")));

    assertThatExceptionOfType(BadGatewayException.class)
        .isThrownBy(() -> client.pollEvents("k", 50));
  }

  @Test
  public void pollEvents_blankApiKey_isBadRequest() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> client.pollEvents(null, 50));
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> client.pollEvents("", 50));
  }

  @Test
  public void pollEvents_nonPositiveMax_isBadRequest() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> client.pollEvents("k", 0));
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> client.pollEvents("k", -1));
  }

  @Test
  public void ack_success() {
    relayServer.stubFor(post(urlPathEqualTo("/api/events/ack"))
        .withHeader("X-Relay-Key", equalTo("k"))
        .withRequestBody(equalToJson("{\"receiptHandles\":[\"a\",\"b\"]}"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"acknowledged\":[\"a\",\"b\"],\"failed\":[]}")));

    RelayAckResponse response = client.ack("k", java.util.List.of("a", "b"));

    assertThat(response.getAcknowledged()).containsExactly("a", "b");
    assertThat(response.getFailed()).isEmpty();
  }

  @Test
  public void ack_partialFailure_reportsFailedHandles() {
    relayServer.stubFor(post(urlPathEqualTo("/api/events/ack"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"acknowledged\":[\"a\"],\"failed\":[\"b\"]}")));

    RelayAckResponse response = client.ack("k", java.util.List.of("a", "b"));

    assertThat(response.getAcknowledged()).containsExactly("a");
    assertThat(response.getFailed()).containsExactly("b");
  }

  @Test
  public void ack_emptyHandles_returnsEmptyResponseWithoutHttpCall() {
    RelayAckResponse response = client.ack("k", java.util.Collections.emptyList());
    assertThat(response.getAcknowledged()).isEmpty();
    assertThat(response.getFailed()).isEmpty();
    relayServer.verify(0,
        com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor(urlPathEqualTo("/api/events/ack")));
  }

  @Test
  public void ack_503_mapsToBadGateway() {
    relayServer.stubFor(post(urlPathEqualTo("/api/events/ack"))
        .willReturn(aResponse().withStatus(503)));

    assertThatExceptionOfType(BadGatewayException.class)
        .isThrownBy(() -> client.ack("k", java.util.List.of("a")));
  }

  @Test
  public void deregister_success() {
    relayServer.stubFor(delete(urlPathEqualTo("/api/register"))
        .withHeader("X-Relay-Key", equalTo("k"))
        .willReturn(aResponse().withStatus(204)));

    client.deregister("k");

    relayServer
        .verify(com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor(urlPathEqualTo("/api/register")));
  }

  @Test
  public void deregister_blankApiKey_isBadRequest() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> client.deregister(null));
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> client.deregister(""));
  }

  @Test
  public void deregister_401_isNotAuthorized() {
    relayServer.stubFor(delete(urlPathEqualTo("/api/register"))
        .willReturn(aResponse().withStatus(401)));

    assertThatExceptionOfType(NotAuthorizedException.class)
        .isThrownBy(() -> client.deregister("k"));
  }

  @Test
  public void deregister_404_isNotFound() {
    relayServer.stubFor(delete(urlPathEqualTo("/api/register"))
        .willReturn(aResponse().withStatus(404)));

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> client.deregister("k"));
  }

  @Test
  public void deregister_500_mapsTo500() {
    relayServer.stubFor(delete(urlPathEqualTo("/api/register"))
        .willReturn(aResponse().withStatus(500).withBody("boom")));

    assertThatExceptionOfType(InternalServerErrorException.class)
        .isThrownBy(() -> client.deregister("k"));
  }

  @Test
  public void deregister_503_mapsToBadGateway() {
    relayServer.stubFor(delete(urlPathEqualTo("/api/register"))
        .willReturn(aResponse().withStatus(503)));

    assertThatExceptionOfType(BadGatewayException.class)
        .isThrownBy(() -> client.deregister("k"));
  }
}
