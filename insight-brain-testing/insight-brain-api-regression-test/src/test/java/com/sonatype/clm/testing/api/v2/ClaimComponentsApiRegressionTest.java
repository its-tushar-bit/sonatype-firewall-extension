/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.api.v2;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.api.AbstractIqApiTest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiHashComponentIdentifierDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * API regression suite for {@code api/v2/claim/components}.
 *
 * <p>
 * Covers: claim (POST), get by hash (GET/{hash}), get all (GET), delete (DELETE/{hash});
 * not-found cases; and the unauthenticated auth contract (401) per HTTP verb.
 *
 * <p>
 * <b>Cleanup:</b> POST to claim creates an entity that is NOT tracked by {@code tempEntity}.
 * Every test that claims a hash must {@code apiDelete(claimPath(hash))} in a {@code finally}
 * block — claimed entities are not tracked by {@code tempEntity}, so each test must delete its own hash.
 *
 * <p>
 * <b>HDS stub:</b> the claim endpoint calls {@code rest/component/summary} to verify the
 * component is unknown before allowing it to be claimed. The {@code @Before} method stubs
 * this for the fixed packageUrl used in {@link #buildDto}.
 */
public class ClaimComponentsApiRegressionTest
    extends AbstractIqApiTest
{
  private static final String CLAIM_BASE = PublicApiPaths.CLAIM_PATH_V2;

  // Maven coordinates derived from "pkg:maven/test/test@1.0.0?type=jar" (used in buildDto)
  private static final ComponentIdentifier CLAIM_COMPONENT_ID =
      ComponentIdentifier.createMavenCoordinates("test", "test", "1.0.0", "", "jar");

  @BeforeEach
  public void stubHds() throws Exception {
    // Claim endpoint calls rest/component/summary to confirm the component is unknown;
    // ComponentSummary.create(false) means isKnown() == false — required to allow claiming.
    mockComponentSummary(CLAIM_COMPONENT_ID, ComponentSummary.create(false));
  }

  @Test
  public void testClaimComponent_returns200WithClaimerId() throws Exception {
    String hash = tempEntity.newRandomHash();
    ApiHashComponentIdentifierDTO dto = buildDto(hash);

    HttpResponse createResponse = apiPostJson(CLAIM_BASE, dto);
    try {
      assertResponseStatus(200, createResponse);
      assertThatJson(createResponse.getBodyText()).node("hash").isEqualTo(hash);
      assertThatJson(createResponse.getBodyText()).node("claimerId").isString().isNotEmpty();
    }
    finally {
      apiDelete(claimPath(hash));
    }
  }

  @Test
  public void testGetClaimedComponent_returns200() throws Exception {
    String hash = tempEntity.newRandomHash();
    HttpResponse postResponse = apiPostJson(CLAIM_BASE, buildDto(hash));
    try {
      assertResponseStatus(200, postResponse);
      HttpResponse getResponse = apiGet(claimPath(hash));
      assertResponseStatus(200, getResponse);
      assertThatJson(getResponse.getBodyText()).node("hash").isEqualTo(hash);
    }
    finally {
      apiDelete(claimPath(hash));
    }
  }

  @Test
  public void testGetAllClaimedComponents_returns200() throws Exception {
    String hash = tempEntity.newRandomHash();
    HttpResponse postResponse = apiPostJson(CLAIM_BASE, buildDto(hash));
    try {
      assertResponseStatus(200, postResponse);
      HttpResponse getAllResponse = apiGet(CLAIM_BASE);
      assertResponseStatus(200, getAllResponse);
      assertThatJson(getAllResponse.getBodyText()).node("componentClaims").isArray();
      assertThatJson(getAllResponse.getBodyText())
          .inPath("$.componentClaims[*].hash")
          .isArray()
          .contains(hash);
    }
    finally {
      apiDelete(claimPath(hash));
    }
  }

  @Test
  public void testDeleteClaimedComponent_returns204() throws Exception {
    String hash = tempEntity.newRandomHash();
    HttpResponse postResponse = apiPostJson(CLAIM_BASE, buildDto(hash));
    try {
      assertResponseStatus(200, postResponse);

      HttpResponse deleteResponse = apiDelete(claimPath(hash));
      assertResponseStatus(204, deleteResponse);

      HttpResponse getAfterDelete = apiGet(claimPath(hash));
      assertResponseStatus(404, getAfterDelete);
    }
    finally {
      // idempotent: if delete already succeeded this is a no-op 404
      apiDelete(claimPath(hash));
    }
  }

  @Test
  public void testClaimComponent_claimAgain_returns200() throws Exception {
    // POST is an upsert: claiming an already-claimed hash returns 200 (not 409)
    String hash = tempEntity.newRandomHash();
    HttpResponse firstResponse = apiPostJson(CLAIM_BASE, buildDto(hash));
    try {
      assertResponseStatus(200, firstResponse);
      HttpResponse secondResponse = apiPostJson(CLAIM_BASE, buildDto(hash));
      assertResponseStatus(200, secondResponse);
      assertThatJson(secondResponse.getBodyText()).node("hash").isEqualTo(hash);
    }
    finally {
      apiDelete(claimPath(hash));
    }
  }

  @Test
  public void testGetClaimedComponent_unknownHash_returns404() throws Exception {
    // Real endpoint logic runs — no AuthzContext guard on this path
    HttpResponse response = apiGet(claimPath(uniqueId("no-such-hash")));
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).containsIgnoringCase("Cannot find component claim");
  }

  @Test
  public void testDeleteClaimedComponent_unknownHash_returns404() throws Exception {
    // Real endpoint logic runs — no AuthzContext guard on this path
    HttpResponse response = apiDelete(claimPath(uniqueId("no-such-hash")));
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).containsIgnoringCase("Cannot find component claim");
  }

  /** Auth contract: unauthenticated callers get 401, not 200/403/404. */
  @Test
  public void testGetClaimedComponent_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(claimPath(uniqueId("anon-hash")));
    assertResponseStatus(401, response);
  }

  /** Auth contract: unauthenticated callers get 401, not 200/403/404. */
  @Test
  public void testGetAllClaimedComponents_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(CLAIM_BASE);
    assertResponseStatus(401, response);
  }

  /** Auth contract on POST: unauthenticated callers get 401 before the body is parsed. */
  @Test
  public void testClaimComponent_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiPostJson(CLAIM_BASE, new ApiHashComponentIdentifierDTO());
    assertResponseStatus(401, response);
  }

  /** Auth contract on DELETE: unauthenticated callers get 401. */
  @Test
  public void testDeleteClaimedComponent_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiDelete(claimPath(uniqueId("anon-del")));
    assertResponseStatus(401, response);
  }

  @Test
  public void testClaimComponent_missingPackageUrl_returns400() throws Exception {
    // hash present but identifier/packageUrl absent — both are required by the endpoint
    ApiHashComponentIdentifierDTO dto = new ApiHashComponentIdentifierDTO();
    dto.hash = tempEntity.newRandomHash();
    HttpResponse response = apiPostJson(CLAIM_BASE, dto);
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).containsIgnoringCase("identifier/package url");
  }

  private static String claimPath(String hash) {
    return CLAIM_BASE + "/" + hash;
  }

  private ApiHashComponentIdentifierDTO buildDto(String hash) {
    ApiHashComponentIdentifierDTO dto = new ApiHashComponentIdentifierDTO();
    dto.hash = hash;
    dto.packageUrl = "pkg:maven/test/test@1.0.0?type=jar";
    dto.comment = "claimed by regression test";
    return dto;
  }
}
