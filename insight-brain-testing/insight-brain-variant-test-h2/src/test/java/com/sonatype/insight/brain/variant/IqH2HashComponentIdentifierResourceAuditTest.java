/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLEncoder;

import jakarta.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.component.HashComponentIdentifierResource;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * IQ Server on H2 — {@code HashComponentIdentifierResourceAuditTest} converted to the reused-server
 * {@link IqH2Test} pattern. No base class; wiring is via the injected {@link IqTestContext}.
 */
@IqH2Test
class IqH2HashComponentIdentifierResourceAuditTest
    implements AuditTestSupport
{
  private static final String COMPONENT_HASH = "componentHash";

  private static final ComponentIdentifier COMPONENT_IDENTIFIER = ComponentIdentifier
      .createMavenCoordinates("groupId", "artifactId", "version", "classifier", "extension");

  private static final String COMMENT = "comment";

  private IqTestContext ctx;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private HashComponentIdentifierDAO hashComponentIdentifierDAO;

  private User unauthorizedUserEntity;

  private final TestLogOutput logOutput =
      new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void before() throws Exception {
    logOutput.before();
    logOutput.clear();
    unauthorizedUserEntity = ctx.tempEntity().newUser();
    hashComponentIdentifierDAO = ctx.lookup(HashComponentIdentifierDAO.class);

    mockComponentSummary(COMPONENT_IDENTIFIER, ComponentSummary.create(false));
  }

  @AfterEach
  void after() {
    logOutput.tearDown();
  }

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @Override
  public String getUnauthorizedUsername() {
    return unauthorizedUserEntity.getUsername();
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private java.util.function.Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUserEntity);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(HashComponentIdentifierResource.RESOURCE_PATH);
  }

  private void mockComponentSummary(
      ComponentIdentifier componentIdentifier,
      ComponentSummary componentSummary) throws Exception
  {
    ctx.hdsRespondWith(componentSummary)
        .atUri(UriBuilder.fromPath("rest/component/summary")
            .queryParam("componentIdentifier", URLEncoder.encode(toJson(componentIdentifier), "UTF-8"))
            .build());
  }

  private String toJson(Object object) {
    try {
      return objectMapper.writeValueAsString(object);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Test
  void testSet() throws Exception {
    HashComponentIdentifier hashComponentIdentifier = hashComponentIdentifier(COMPONENT_HASH, COMPONENT_IDENTIFIER,
        COMMENT);

    restRequest().body(hashComponentIdentifier).post();

    assertHashComponentIdentifierData(assertAuditLog(AuditEvent.SET_COMPONENT_IDENTITY, null), hashComponentIdentifier);
  }

  @Test
  void testSet_NullComment() throws Exception {
    HashComponentIdentifier hashComponentIdentifier = hashComponentIdentifier(COMPONENT_HASH, COMPONENT_IDENTIFIER,
        null);

    restRequest().body(hashComponentIdentifier).post();

    assertHashComponentIdentifierData(assertAuditLog(AuditEvent.SET_COMPONENT_IDENTITY, null), hashComponentIdentifier);
  }

  @Test
  void testSet_Unauthorized() throws Exception {
    HashComponentIdentifier hashComponentIdentifier = hashComponentIdentifier(COMPONENT_HASH, COMPONENT_IDENTIFIER,
        COMMENT);

    restRequest().with(unauthorizedUser()).body(hashComponentIdentifier).post();

    assertAuditLog(AuditEvent.SET_COMPONENT_IDENTITY, "unauthorized");
  }

  @Test
  void testDelete() throws Exception {
    HashComponentIdentifier hashComponentIdentifier = hashComponentIdentifier(COMPONENT_HASH, COMPONENT_IDENTIFIER,
        COMMENT);
    hashComponentIdentifierDAO.insert(hashComponentIdentifier);

    restRequest().path(COMPONENT_HASH).delete();

    assertHashComponentIdentifierData(assertAuditLog(AuditEvent.UNSET_COMPONENT_IDENTITY, null),
        hashComponentIdentifier);
  }

  @Test
  void testUpdate() throws Exception {
    HashComponentIdentifier hashComponentIdentifier = hashComponentIdentifier(COMPONENT_HASH, COMPONENT_IDENTIFIER,
        COMMENT);
    hashComponentIdentifierDAO.insert(hashComponentIdentifier);
    hashComponentIdentifier.setId("new-id");
    restRequest().body(hashComponentIdentifier).put();

    assertHashComponentIdentifierData(assertAuditLog(AuditEvent.SET_COMPONENT_IDENTITY, null), hashComponentIdentifier);
  }

  private HashComponentIdentifier hashComponentIdentifier(
      String componentHash,
      ComponentIdentifier componentIdentifier,
      String comment)
  {
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(componentHash, componentIdentifier);
    hashComponentIdentifier.setComment(comment);
    return hashComponentIdentifier;
  }

  private void assertHashComponentIdentifierData(AuditDTO auditDTO, HashComponentIdentifier hashComponentIdentifier) {
    assertCustomData(auditDTO, "componentHash", hashComponentIdentifier.getHash());
    assertCustomObject(auditDTO, "componentIdentifier", hashComponentIdentifier.getComponentIdentifier());
    assertCustomData(auditDTO, "comment", hashComponentIdentifier.getComment());
  }

  private static final class TestLogOutput
      extends LogOutput
  {
    TestLogOutput(String... loggerNames) {
      super(loggerNames);
    }

    void tearDown() {
      after();
    }
  }
}
