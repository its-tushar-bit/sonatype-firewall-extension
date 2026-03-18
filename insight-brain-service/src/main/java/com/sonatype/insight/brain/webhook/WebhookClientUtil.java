/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.security.FIPSConfig;
import com.sonatype.insight.brain.security.FIPSModeDetector;
import com.sonatype.insight.brain.service.InsightProxy;
import com.sonatype.insight.brain.webhook.dto.WebhookPayload;
import com.sonatype.insight.client.utils.AbstractClient;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.Result;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.io.BaseEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.25.0
 */
@Named
@Singleton
public class WebhookClientUtil
{
  public static final String WEBHOOK_ID_HEADER = "X-Nexus-Webhook-ID";

  public static final String WEBHOOK_DELIVERY_HEADER = "X-Nexus-Webhook-Delivery";

  public static final String WEBHOOK_SIGNATURE_ALGORITHM_HEADER = "X-Nexus-Webhook-Signature-Algorithm";

  public static final String WEBHOOK_SIGNATURE_HEADER = "X-Nexus-Webhook-Signature";

  private static final Logger log = LoggerFactory.getLogger(WebhookClientUtil.class);

  private final InsightProxy insightProxy;

  @Inject
  public WebhookClientUtil(final InsightProxy insightProxy) {
    this.insightProxy = insightProxy;
  }

  public void post(final Webhook decryptedWebhook, final String webhookId, final WebhookPayload payload) {
    String deliveryId = UUID.randomUUID().toString();
    try {
      Configuration configuration = new Configuration();
      insightProxy.contextualize(configuration, decryptedWebhook.getUrl());

      WebhookClient webhookClient = new WebhookClient(configuration, deliveryId, decryptedWebhook, webhookId, payload);

      AuditData.get().setData("webhookDeliveryId", deliveryId);
      log.debug("Sending Webhook {} with delivery ID {}", webhookId, deliveryId);
      Result result = webhookClient.post();
      if (result.status() >= 300) {
        AuditData.get().setHttpStatus(result.status());
        String msg = result.message();
        log.error(
            "Unable to perform HTTP request for Webhook {} with delivery ID {} due to Status Code: {} Message: {}",
            webhookId, deliveryId, result.status(), msg);
      }
    }
    catch (JsonProcessingException ex) {
      AuditData.get().setException(ex);
      log.error("Unable to marshall Webhook {}", webhookId, ex);
    }
    catch (SocketTimeoutException ex) {
      AuditData.get().setException(ex);
      log.error("Timeout waiting for response from peer for Webhook {} with delivery ID {} Message: {}",
          webhookId, deliveryId, ex.getMessage());
    }
    catch (IOException | RuntimeException ex) {
      log.error("Unable to perform HTTP request for Webhook {} with delivery ID {}", webhookId, deliveryId, ex);
      if (ex instanceof RuntimeException) {
        throw (RuntimeException) ex;
      }
      AuditData.get().setException(ex);
    }
  }

  /**
   * @since 1.25.0
   */
  private class WebhookClient
      extends AbstractClient
  {
    private static final String HMAC_SHA1 = "HmacSHA1";

    private static final int REQUEST_TIMEOUT = 10000;

    private final BaseEncoding hex = BaseEncoding.base16().lowerCase();

    private final String deliveryId;

    private final Webhook decryptedWebhook;

    private final String webhookId;

    private final String json;

    private final String hmacAlgorithm;

    private WebhookClient(
        final Configuration config,
        final String deliveryId,
        final Webhook decryptedWebhook,
        final String webhookId,
        final WebhookPayload payload) throws JsonProcessingException
    {
      super(config);

      this.deliveryId = deliveryId;
      this.decryptedWebhook = decryptedWebhook;
      this.webhookId = webhookId;

      ObjectMapper objectMapper = new ObjectMapper()
          .setVisibility(PropertyAccessor.FIELD, Visibility.PUBLIC_ONLY)
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .setSerializationInclusion(JsonInclude.Include.NON_NULL);
      this.json = objectMapper.writeValueAsString(payload);

      this.hmacAlgorithm = FIPSModeDetector.isEnabled() ? FIPSConfig.getFipsHmacAlgorithm() : HMAC_SHA1;
    }

    public Result post() throws IOException {
      return path().timeout(REQUEST_TIMEOUT).post(new StringEntity(json, ContentType.APPLICATION_JSON));
    }

    @Override
    protected HttpRequest prepare(final HttpRequest request) {
      try {
        request.setHeader(WEBHOOK_ID_HEADER, webhookId);
        request.setHeader(WEBHOOK_DELIVERY_HEADER, deliveryId);
        if (StringUtils.isNotBlank(decryptedWebhook.getSecretKey())) {
          request.setHeader(WEBHOOK_SIGNATURE_ALGORITHM_HEADER, hmacAlgorithm);
          request.setHeader(WEBHOOK_SIGNATURE_HEADER, sign(json, decryptedWebhook.getSecretKey()));
        }

        return request;
      }
      catch (NoSuchAlgorithmException | InvalidKeyException ex) {
        log.error("Unable to generate Webhook secret key.", ex);
        throw new RuntimeException(ex);
      }
    }

    /**
     * Generate HMAC signature (HEX encoded) of given body using secret as key.
     */
    private String sign(
        final String json,
        final String secretKey) throws NoSuchAlgorithmException, InvalidKeyException
    {
      SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), hmacAlgorithm);
      Mac mac = Mac.getInstance(hmacAlgorithm);
      mac.init(key);
      byte[] bytes = mac.doFinal(json.getBytes(StandardCharsets.UTF_8));
      return hex.encode(bytes);
    }
  }
}
