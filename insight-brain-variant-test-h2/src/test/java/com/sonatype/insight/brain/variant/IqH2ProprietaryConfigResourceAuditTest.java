/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.configuration.ProprietaryConfig;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigResource;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigResource.FilePathRegex;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

@IqH2Test
class IqH2ProprietaryConfigResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private Application app;

  private Organization org;

  private User unauthorizedUser;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
    org = ctx.tempEntity().newOrganization();
    app = ctx.tempEntity().newApplicationWithParent();
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
    return unauthorizedUser.getUsername();
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  @Test
  void testUpsert_Application() throws Exception {
    List<String> packageMatchers = singletonList("a.b.c");
    List<String> regexMatchers = asList("regex11", "regex22");
    upsert(null, app, new ProprietaryConfig(app.getId(), packageMatchers, regexMatchers));

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_PROPRIETARY_COMPONENTS, null);
    assertApplicationData(auditDTO, app);
    assertProprietaryConfigCustomData(auditDTO, packageMatchers, regexMatchers);
  }

  @Test
  void testUpsert_Organization() throws Exception {
    List<String> packageMatchers = singletonList("a.b.c");
    List<String> regexMatchers = asList("regex11", "regex22");
    upsert(null, org, new ProprietaryConfig(org.getId(), packageMatchers, regexMatchers));

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_PROPRIETARY_COMPONENTS, null);
    assertOrganizationData(auditDTO, org);
    assertProprietaryConfigCustomData(auditDTO, packageMatchers, regexMatchers);
  }

  @Test
  void testUpsert_NoMatchers() throws Exception {
    List<String> emptyPackageMatchers = new ArrayList<>();
    List<String> emptyRegexMatchers = new ArrayList<>();
    upsert(null, org, new ProprietaryConfig(org.getId(), emptyPackageMatchers, emptyRegexMatchers));

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_PROPRIETARY_COMPONENTS, null);
    assertOrganizationData(auditDTO, org);
    assertProprietaryConfigCustomData(auditDTO, emptyPackageMatchers, emptyRegexMatchers);
  }

  @Test
  void testUpsert_Unauthorized() throws Exception {
    upsert(unauthorizedUser(), org, new ProprietaryConfig(org.getId(), new ArrayList<>(), new ArrayList<>()));

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_PROPRIETARY_COMPONENTS, "unauthorized");
    assertOrganizationData(auditDTO, org);
  }

  @Test
  void testAddFilePathRegex_Application() throws Exception {
    FilePathRegex filePathRegex = new FilePathRegex();
    filePathRegex.paths = asList("path1", "path2");
    filePathRegex.regex = "regexp1";
    addFilePathRegex(null, app, filePathRegex);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_PROPRIETARY_COMPONENTS, null);
    assertApplicationData(auditDTO, app);
    assertProprietaryConfigCustomData(auditDTO, new ArrayList<>(),
        asList(Pattern.quote("path1"), Pattern.quote("path2"), "regexp1"));
  }

  @Test
  void testAddFilePathRegex_Application_withExistingConfig() throws Exception {
    ctx.tempEntity()
        .newProprietaryConfig(app.getId(), singletonList("existingPackage"), singletonList("existingRegex"));
    FilePathRegex filePathRegex = new FilePathRegex();
    filePathRegex.paths = asList("path1", "path2");
    filePathRegex.regex = "regexp1";
    addFilePathRegex(null, app, filePathRegex);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_PROPRIETARY_COMPONENTS, null);
    assertApplicationData(auditDTO, app);
    assertProprietaryConfigCustomData(auditDTO, singletonList("existingPackage"),
        asList("existingRegex", Pattern.quote("path1"), Pattern.quote("path2"), "regexp1"));
  }

  @Test
  void testAddFilePathRegex_Organization() throws Exception {
    FilePathRegex filePathRegex = new FilePathRegex();
    filePathRegex.paths = asList("path1", "path2");
    filePathRegex.regex = "regexp1";
    addFilePathRegex(null, org, filePathRegex);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_PROPRIETARY_COMPONENTS, null);
    assertOrganizationData(auditDTO, org);
    assertProprietaryConfigCustomData(auditDTO, new ArrayList<>(),
        asList(Pattern.quote("path1"), Pattern.quote("path2"), "regexp1"));
  }

  @Test
  void testAddFilePathRegex_NullData() throws Exception {
    FilePathRegex filePathRegex = new FilePathRegex();
    addFilePathRegex(null, org, filePathRegex);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_PROPRIETARY_COMPONENTS, null);
    assertOrganizationData(auditDTO, org);
    assertProprietaryConfigCustomData(auditDTO, new ArrayList<>(), new ArrayList<>());
  }

  @Test
  void testAddFilePathRegex_Unauthorized() throws Exception {
    FilePathRegex filePathRegex = new FilePathRegex();
    addFilePathRegex(unauthorizedUser(), org, filePathRegex);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_PROPRIETARY_COMPONENTS, "unauthorized");
    assertOrganizationData(auditDTO, org);
  }

  private void addFilePathRegex(Consumer<HttpRequest> user, Owner owner, FilePathRegex filePathRegex) throws Exception {
    restRequest(user, owner).path(ProprietaryConfigResource.ADD_FILE_PATH_REGEX).body(filePathRegex).post();
  }

  private void upsert(Consumer<HttpRequest> user, Owner owner, ProprietaryConfig proprietaryConfig) throws Exception {
    restRequest(user, owner).body(proprietaryConfig).put();
  }

  private HttpRequest restRequest(Consumer<HttpRequest> user, Owner owner) {
    return ctx.restRequest()
        .with(user)
        .path(ProprietaryConfigResource.RESOURCE_PATH)
        .parameter(owner.getType(),
            owner.getPublicId());
  }

  private void assertProprietaryConfigCustomData(
      final AuditDTO auditDTO,
      final List<String> packageMatchers,
      final List<String> regexMatchers)
  {
    assertCustomData(auditDTO, "packageMatchers", packageMatchers);
    assertCustomData(auditDTO, "regexMatchers", regexMatchers);
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
