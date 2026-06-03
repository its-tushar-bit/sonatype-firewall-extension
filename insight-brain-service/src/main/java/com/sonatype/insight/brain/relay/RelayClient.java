/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;

import com.sonatype.insight.brain.relay.dto.RelayGitHubAppSecretRequest;
import com.sonatype.insight.brain.relay.dto.RelayRegisterRequest;
import com.sonatype.insight.brain.relay.dto.RelayRegisterResponse;
import com.sonatype.insight.brain.relay.dto.RelayRotateKeyResponse;
import com.sonatype.insight.brain.relay.dto.RelayRotateWebhookSecretResponse;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.utils.Retry;
import com.sonatype.insight.client.utils.HttpClientUtils;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.GatewayTimeoutException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.annotations.VisibleForTesting;
import com.sonatype.insight.brain.lifecycle.Managed;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP client for the Sonatype SCM webhook relay. Handles register, GitHub App registration, and
 * the GitHub App webhook-secret rotation endpoint; status mapping and retries follow the same
 * conventions as {@code HdsClient}.
 */
@Named
@Singleton
public class RelayClient
    implements Managed
{
  private static final Logger log = LoggerFactory.getLogger(RelayClient.class);

  static final String REGISTER_PATH = "api/register";

  static final String INSTALLATION_PATH_PREFIX = "api/installations/";

  static final String GITHUB_APP_WEBHOOK_SECRET_PATH = "api/github-app/webhook-secret";

  static final String ROTATE_KEY_PATH = "api/rotate-key";

  static final String ROTATE_WEBHOOK_SECRET_PATH = "api/rotate-webhook-secret";

  static final String RELAY_KEY_HEADER = "X-Relay-Key";

  static final String WEBHOOK_TOKEN_HEADER = "x-relay-webhook-token";

  static final int CONNECTION_POOL_SIZE = 5;

  private static final int MILLIS_PER_SECOND = 1000;

  private static final long IDLE_CONNECTION_EVICTION_SECONDS = 30;

  private static final long DEFERRED_CLOSE_DELAY_MINUTES = 15;

  static final java.util.function.Function<String, Retry> DEFAULT_RETRY_CREATOR =
      name -> new Retry(name, 4, null, BadGatewayException.class::isInstance, i -> Duration.ofSeconds(1));

  private final InsightProxy proxy;

  private final Configuration configuration;

  private final java.util.function.Function<String, Retry> retryCreator;

  private volatile HttpClientUtils.Configuration config;

  private volatile CloseableHttpClient client;

  /** Test-only flag: skips the 15-minute defer when shutting down a previous client. */
  @VisibleForTesting
  boolean waitToCloseOldClients = true;

  @Inject
  public RelayClient(InsightProxy proxy, Configuration configuration) {
    this(proxy, configuration, DEFAULT_RETRY_CREATOR);
  }

  @VisibleForTesting
  public RelayClient(
      InsightProxy proxy,
      Configuration configuration,
      java.util.function.Function<String, Retry> retryCreator)
  {
    this.proxy = proxy;
    this.configuration = configuration;
    this.retryCreator = retryCreator;
    updateClient();
  }

  /**
   * POSTs the binary license body to {@code /api/register} as PAT registration and returns the
   * credentials assigned by the relay. The relay treats the license bytes as the registration
   * credential, so this call needs no separate auth header.
   */
  public RelayRegisterResponse register(byte[] licenseBytes) {
    if (licenseBytes == null || licenseBytes.length == 0) {
      throw new BadRequestException("License bytes are required to register with the relay.");
    }
    HttpPost req = new HttpPost(buildUri(REGISTER_PATH));
    req.setEntity(new ByteArrayEntity(licenseBytes, ContentType.APPLICATION_OCTET_STREAM));
    req.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
    return execute(retryCreator.apply(REGISTER_PATH), req, RelayRegisterResponse.class);
  }

  /**
   * Re-registers using the caller's existing api key as proof-of-possession (X-Relay-Key
   * header). The relay treats this as an in-place license change: the license fingerprint may
   * differ from the previous registration, but customer_id and webhook_url stay stable and
   * credentials are rotated.
   */
  public RelayRegisterResponse reRegisterWithApiKey(byte[] licenseBytes, String apiKey) {
    if (licenseBytes == null || licenseBytes.length == 0) {
      throw new BadRequestException("License bytes are required to register with the relay.");
    }
    if (StringUtils.isBlank(apiKey)) {
      throw new BadRequestException("API key is required for in-place re-registration.");
    }
    HttpPost req = new HttpPost(buildUri(REGISTER_PATH));
    req.setEntity(new ByteArrayEntity(licenseBytes, ContentType.APPLICATION_OCTET_STREAM));
    req.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
    req.setHeader(RELAY_KEY_HEADER, apiKey);
    return execute(retryCreator.apply(REGISTER_PATH), req, RelayRegisterResponse.class);
  }

  /**
   * Recovers a registration whose api key has been lost or rotated, by sending the existing
   * webhook token as proof-of-possession. The relay re-issues an api key and webhook signing
   * secret while keeping customer_id and webhook_url stable.
   */
  public RelayRegisterResponse recoverWithWebhookToken(byte[] licenseBytes, String webhookToken) {
    if (licenseBytes == null || licenseBytes.length == 0) {
      throw new BadRequestException("License bytes are required to register with the relay.");
    }
    if (StringUtils.isBlank(webhookToken)) {
      throw new BadRequestException("Webhook token is required for relay recovery.");
    }
    HttpPost req = new HttpPost(buildUri(REGISTER_PATH));
    req.setEntity(new ByteArrayEntity(licenseBytes, ContentType.APPLICATION_OCTET_STREAM));
    req.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
    req.setHeader(WEBHOOK_TOKEN_HEADER, webhookToken);
    return execute(retryCreator.apply(REGISTER_PATH), req, RelayRegisterResponse.class);
  }

  /**
   * Registers (or routes by) a GitHub App installation. The relay's {@code /api/register}
   * endpoint dispatches PAT vs. GitHub App on the body's {@code installationIds[]} field, so we
   * send a JSON body with the base64-encoded license and the installation id. The App-level
   * webhook secret is uploaded separately via {@link #setGitHubAppWebhookSecret}.
   */
  public RelayRegisterResponse registerGitHubApp(byte[] licenseBytes, String installationId) {
    return registerGitHubApp(licenseBytes, installationId, null);
  }

  /**
   * Registers a GitHub App installation with the relay, optionally authenticating as an existing
   * customer via {@code existingApiKey}. The relay treats a same-license, same-mode call with
   * {@code X-Relay-Key} as an in-place re-registration: customer_id + queue + already-registered
   * installations are preserved and the new installation_id is added to the index.
   *
   * <p>
   * When the caller has no api key (first ever registration, or a recovery path), pass
   * {@code null} and the relay accepts a fresh anonymous register if the license fingerprint
   * is also new. Cross-mode (PAT ↔ App) re-registration is still rejected with 409 even with
   * {@code X-Relay-Key}; callers must explicitly deregister the prior mode first.
   */
  public RelayRegisterResponse registerGitHubApp(byte[] licenseBytes, String installationId, String existingApiKey) {
    if (licenseBytes == null || licenseBytes.length == 0) {
      throw new BadRequestException("License bytes are required to register with the relay.");
    }
    if (StringUtils.isBlank(installationId)) {
      throw new BadRequestException("installationId is required.");
    }
    HttpPost req = new HttpPost(buildUri(REGISTER_PATH));
    String body = JsonUtils.format(new RelayRegisterRequest(
        java.util.Base64.getEncoder().encodeToString(licenseBytes),
        java.util.Collections.singletonList(installationId)));
    req.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
    req.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
    if (StringUtils.isNotBlank(existingApiKey)) {
      req.setHeader(RELAY_KEY_HEADER, existingApiKey);
    }
    return execute(retryCreator.apply(REGISTER_PATH), req, RelayRegisterResponse.class);
  }

  /**
   * Stores or rotates the App-level HMAC webhook secret on the relay. Authenticates with the
   * {@code apiKey} returned from {@link #registerGitHubApp}; the relay applies a rotation grace
   * window so deliveries signed with the previous secret remain valid for a few minutes.
   */
  public void setGitHubAppWebhookSecret(String apiKey, String webhookSecret) {
    if (StringUtils.isBlank(apiKey)) {
      throw new BadRequestException("Relay API key is required.");
    }
    if (StringUtils.isBlank(webhookSecret)) {
      throw new BadRequestException("webhookSecret is required.");
    }
    HttpPost req = new HttpPost(buildUri(GITHUB_APP_WEBHOOK_SECRET_PATH));
    String body = JsonUtils.format(new RelayGitHubAppSecretRequest(webhookSecret));
    req.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
    req.setHeader(RELAY_KEY_HEADER, apiKey);
    req.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
    execute(retryCreator.apply(GITHUB_APP_WEBHOOK_SECRET_PATH), req, null);
  }

  /**
   * Deletes the customer's relay registration. Used during admin-triggered deregistration and
   * cross-mode migration (PAT ↔ GitHub App). Status codes map through {@link #throwForStatus}
   * as for the other endpoints (404 surfaces as {@link NotFoundException} so callers can treat
   * "already gone" as success when desired).
   */
  public void deregister(String apiKey) {
    requireApiKey(apiKey);
    HttpDelete req = new HttpDelete(buildUri(REGISTER_PATH));
    req.setHeader(RELAY_KEY_HEADER, apiKey);
    req.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
    execute(retryCreator.apply(REGISTER_PATH), req, null);
  }

  /**
   * Removes a single GitHub App installation from the relay's installation index without
   * touching the customer record. Used when the IQ admin deletes one App on a tenant that
   * still has others registered: the customer-wide deregister would tear down everything,
   * and IQ-side cleanup alone leaves an orphan index entry that keeps routing webhooks for
   * the deleted installation into the customer's queue.
   *
   * <p>
   * The relay returns 204 on success and on idempotent re-call (already-deleted), 401 when
   * the api key is invalid, and 403 when the installation belongs to a different customer.
   * 4xx responses surface as {@link jakarta.ws.rs.NotAuthorizedException} /
   * {@link jakarta.ws.rs.BadRequestException} per the standard {@link #execute} mapping.
   */
  public void deleteInstallation(String apiKey, String installationId) {
    requireApiKey(apiKey);
    if (StringUtils.isBlank(installationId)) {
      throw new BadRequestException("installationId is required.");
    }
    String path = INSTALLATION_PATH_PREFIX + installationId;
    HttpDelete req = new HttpDelete(buildUri(path));
    req.setHeader(RELAY_KEY_HEADER, apiKey);
    req.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
    execute(retryCreator.apply(INSTALLATION_PATH_PREFIX), req, null);
  }

  /**
   * Rotates the IQ→relay api key. The relay generates a fresh api key, returns it once, and
   * keeps the previous key valid for a 5-minute grace window so in-flight polls do not fail.
   * Authenticates with the caller's current api key via the {@code X-Relay-Key} header; the
   * body is empty.
   */
  public RelayRotateKeyResponse rotateApiKey(String currentApiKey) {
    requireApiKey(currentApiKey);
    HttpPost req = new HttpPost(buildUri(ROTATE_KEY_PATH));
    req.setHeader(RELAY_KEY_HEADER, currentApiKey);
    req.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
    return execute(retryCreator.apply(ROTATE_KEY_PATH), req, RelayRotateKeyResponse.class);
  }

  /**
   * Rotates the per-customer PAT webhook signing secret. The relay generates a fresh secret,
   * returns it once, and keeps the previous secret valid for a 5-minute grace window so the
   * SCM-side reconfiguration can lag without dropping deliveries. Authenticates with the
   * caller's current api key via the {@code X-Relay-Key} header; the body is empty.
   */
  public RelayRotateWebhookSecretResponse rotateWebhookSecret(String currentApiKey) {
    requireApiKey(currentApiKey);
    HttpPost req = new HttpPost(buildUri(ROTATE_WEBHOOK_SECRET_PATH));
    req.setHeader(RELAY_KEY_HEADER, currentApiKey);
    req.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
    return execute(
        retryCreator.apply(ROTATE_WEBHOOK_SECRET_PATH), req, RelayRotateWebhookSecretResponse.class);
  }

  private static void requireApiKey(String apiKey) {
    if (StringUtils.isBlank(apiKey)) {
      throw new BadRequestException("Relay API key is required.");
    }
  }

  public synchronized void serverConfigurationChanged() {
    updateClient();
  }

  @Override
  public void start() {
  }

  @Override
  public void stop() throws IOException {
    if (client != null) {
      client.close();
    }
  }

  private synchronized void updateClient() {
    HttpClientUtils.Configuration newConfig = new HttpClientUtils.Configuration();
    newConfig.setConnectTimeout(configuration.getConnectTimeoutInSeconds() * MILLIS_PER_SECOND);
    newConfig.setSocketTimeout(configuration.getSocketTimeoutInSeconds() * MILLIS_PER_SECOND);
    proxy.contextualize(newConfig, configuration.getRelayUrl());
    this.config = newConfig;

    CloseableHttpClient oldClient = client;
    client = HttpClientUtils.create(newConfig)
        .setMaxConnTotal(CONNECTION_POOL_SIZE)
        .setMaxConnPerRoute(CONNECTION_POOL_SIZE)
        .evictIdleConnections(IDLE_CONNECTION_EVICTION_SECONDS, TimeUnit.SECONDS)
        .build();
    if (oldClient != null) {
      scheduleDeferredClose(oldClient);
    }
  }

  /**
   * Closes the old HTTP client after a delay so concurrent in-flight requests on it can
   * drain. Mirrors {@code HdsClient.updateClient}'s pattern.
   */
  private void scheduleDeferredClose(CloseableHttpClient oldClient) {
    Thread closer = new Thread(() -> {
      try {
        if (waitToCloseOldClients) {
          Thread.sleep(TimeUnit.MINUTES.toMillis(DEFERRED_CLOSE_DELAY_MINUTES));
        }
        oldClient.close();
      }
      catch (Exception e) {
        log.error("Failed to cleanly shutdown obsolete relay HTTP client", e);
      }
    }, "RelayHttpClientCloser");
    closer.start();
  }

  private String buildUri(String path) {
    return UriBuilder.fromUri(config.getServerUrl()).path(path).build().toString();
  }

  private <T> T execute(Retry retry, HttpUriRequest request, Class<T> clazz) {
    return retry.executeSupplier(() -> doExecute(request, clazz));
  }

  /**
   * Executes the request and parses (or discards) the body inside a single try-with-resources
   * so the underlying connection is always released back to the pool. Earlier shape returned
   * the open response to the caller, which leaked the leased connection on every error path
   * that didn't subsequently consume the entity (e.g. response-parse failures, retry handler
   * partial reads, surface-throws from {@link #throwForStatus}). With
   * {@code maxConnPerRoute = 5} the pool is exhausted after five such leaks and every
   * subsequent register/poll call hangs indefinitely on {@code getPoolEntryBlocking}.
   */
  private <T> T doExecute(HttpUriRequest request, Class<T> clazz) {
    log.debug("Relay request: {} {}", request.getMethod(), request.getURI());
    long start = System.currentTimeMillis();
    try (CloseableHttpResponse response = client.execute(request)) {
      throwForStatus(response);
      return parseResponseBody(response, clazz);
    }
    catch (HttpHostConnectException | ConnectTimeoutException e) {
      throw new GatewayTimeoutException(e.getMessage(), e);
    }
    catch (UnknownHostException e) {
      throw new BadGatewayException(
          "The hostname for the SCM webhook relay could not be resolved; verify the relay URL and DNS.", e);
    }
    catch (SSLException e) {
      throw new BadGatewayException("The SSL/TLS connection to the SCM webhook relay could not be established.", e);
    }
    catch (IOException e) {
      log.error("Relay request failed: {}", e.getMessage(), e);
      throw new BadGatewayException("The request to the SCM webhook relay failed, please retry.", e);
    }
    finally {
      log.debug("Relay request completed in {} ms", System.currentTimeMillis() - start);
    }
  }

  private void throwForStatus(HttpResponse response) {
    int status = response.getStatusLine().getStatusCode();
    if (status >= 200 && status < 300) {
      return;
    }
    String message = readErrorMessage(response);
    EntityUtils.consumeQuietly(response.getEntity());
    switch (status) {
      case 400:
        throw new BadRequestException(message);
      case 401:
      case 403:
        throw new NotAuthorizedException(
            "The SCM webhook relay rejected the registration credential (HTTP " + status + ").");
      case 404:
        throw new NotFoundException("The SCM webhook relay endpoint was not found (HTTP 404).");
      case 500:
        throw new InternalServerErrorException("The SCM webhook relay returned an internal error, please retry.");
      case 502:
      case 503:
      case 504:
        throw new BadGatewayException("The SCM webhook relay is unavailable (HTTP " + status + "), please retry.");
      default:
        log.error("Unexpected relay response status {}: {}", status, message);
        throw new BadGatewayException(
            "The SCM webhook relay returned an unexpected status " + status + ", please retry.");
    }
  }

  @VisibleForTesting
  String readErrorMessage(HttpResponse response) {
    try {
      HttpEntity entity = response.getEntity();
      if (entity == null) {
        return response.getStatusLine().getReasonPhrase();
      }
      return EntityUtils.toString(entity, StandardCharsets.UTF_8);
    }
    catch (IOException e) {
      return response.getStatusLine().getReasonPhrase();
    }
  }

  private <T> T parseResponseBody(HttpResponse response, Class<T> clazz) {
    try {
      HttpEntity entity = response.getEntity();
      if (entity == null || clazz == null) {
        return null;
      }
      byte[] bytes = EntityUtils.toByteArray(entity);
      if (bytes.length == 0) {
        return null;
      }
      return JsonUtils.parse(bytes, clazz);
    }
    catch (IOException e) {
      log.error("Failed to read relay response: {}", e.getMessage(), e);
      throw new BadGatewayException("Failed to read response from the SCM webhook relay, please retry.");
    }
  }
}
