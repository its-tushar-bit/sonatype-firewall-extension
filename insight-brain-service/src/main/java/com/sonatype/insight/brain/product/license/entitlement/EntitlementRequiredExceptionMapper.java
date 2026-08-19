/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license.entitlement;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

/**
 * Maps {@link EntitlementRequiredException} to a structured JSON 402 response
 * with upsell metadata for tier-gated features.
 */
@Named
@Singleton
public class EntitlementRequiredExceptionMapper
    implements ExceptionMapper<EntitlementRequiredException>
{
  @Override
  public Response toResponse(EntitlementRequiredException exception) {
    UpsellInfo upsell = exception.getUpsellInfo();

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("error", "feature_not_available");
    body.put("code", "ENTITLEMENT_REQUIRED");
    body.put("feature", exception.getFeature().name());
    body.put("message", upsell.getMessage());
    body.put("upgrade_hint", upsell.getUpgradeHint());
    if (upsell.getDocsUrl() != null) {
      body.put("docs_url", upsell.getDocsUrl());
    }
    body.put("cta_url", upsell.getCtaUrl());

    return Response.status(402)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .entity(body)
        .build();
  }
}
