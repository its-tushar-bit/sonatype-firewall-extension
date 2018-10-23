/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.proprietary;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.configuration.ProprietaryConfig;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigResource.FilePathRegex;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Before;
import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class ProprietaryConfigResourceAuditTest
    extends AbstractAuditTest
{
  private Application app;

  private Organization org;

  @Before
  public void before() {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplicationWithParent();
  }

  @Test
  public void testUpsert_Application() throws Exception {
    List<String> packageMatchers = singletonList("a.b.c");
    List<String> regexMatchers = asList("regex11", "regex22");
    upsert(null, app, new ProprietaryConfig(app.getId(), packageMatchers, regexMatchers));

    AuditDTO auditDTO = assertAuditLog(null);
    assertApplicationData(auditDTO, app);
    assertProprietaryConfigCustomData(auditDTO, packageMatchers, regexMatchers);
  }

  @Test
  public void testUpsert_Organization() throws Exception {
    List<String> packageMatchers = singletonList("a.b.c");
    List<String> regexMatchers = asList("regex11", "regex22");
    upsert(null, org, new ProprietaryConfig(org.getId(), packageMatchers, regexMatchers));

    AuditDTO auditDTO = assertAuditLog(null);
    assertOrganizationData(auditDTO, org);
    assertProprietaryConfigCustomData(auditDTO, packageMatchers, regexMatchers);
  }

  @Test
  public void testUpsert_NoMatchers() throws Exception {
    List<String> emptyPackageMatchers = new ArrayList<>();
    List<String> emptyRegexMatchers = new ArrayList<>();
    upsert(null, org, new ProprietaryConfig(org.getId(), emptyPackageMatchers, emptyRegexMatchers));

    AuditDTO auditDTO = assertAuditLog(null);
    assertOrganizationData(auditDTO, org);
    assertProprietaryConfigCustomData(auditDTO, emptyPackageMatchers, emptyRegexMatchers);
  }

  @Test
  public void testUpsert_Unauthorized() throws Exception {
    upsert(unauthorizedUser, org, new ProprietaryConfig(org.getId(), new ArrayList<>(), new ArrayList<>()));

    AuditDTO auditDTO = assertAuditLog("unauthorized");
    assertOrganizationData(auditDTO, org);
  }

  @Test
  public void testAddFilePathRegex_Application() throws Exception {
    FilePathRegex filePathRegex = new FilePathRegex();
    filePathRegex.paths = asList("path1", "path2");
    filePathRegex.regex = "regexp1";
    addFilePathRegex(null, app, filePathRegex);

    AuditDTO auditDTO = assertAuditLog(null);
    assertApplicationData(auditDTO, app);
    assertProprietaryConfigCustomData(auditDTO, new ArrayList<>(),
        asList(Pattern.quote("path1"), Pattern.quote("path2"), "regexp1"));
  }

  @Test
  public void testAddFilePathRegex_Application_withExistingConfig() throws Exception {
    tempEntity.newProprietaryConfig(app.getId(), singletonList("existingPackage"), singletonList("existingRegex"));
    FilePathRegex filePathRegex = new FilePathRegex();
    filePathRegex.paths = asList("path1", "path2");
    filePathRegex.regex = "regexp1";
    addFilePathRegex(null, app, filePathRegex);

    AuditDTO auditDTO = assertAuditLog(null);
    assertApplicationData(auditDTO, app);
    assertProprietaryConfigCustomData(auditDTO, singletonList("existingPackage"),
        asList("existingRegex", Pattern.quote("path1"), Pattern.quote("path2"), "regexp1"));
  }

  @Test
  public void testAddFilePathRegex_Organization() throws Exception {
    FilePathRegex filePathRegex = new FilePathRegex();
    filePathRegex.paths = asList("path1", "path2");
    filePathRegex.regex = "regexp1";
    addFilePathRegex(null, org, filePathRegex);

    AuditDTO auditDTO = assertAuditLog(null);
    assertOrganizationData(auditDTO, org);
    assertProprietaryConfigCustomData(auditDTO, new ArrayList<>(),
        asList(Pattern.quote("path1"), Pattern.quote("path2"), "regexp1"));
  }

  @Test
  public void testAddFilePathRegex_NullData() throws Exception {
    FilePathRegex filePathRegex = new FilePathRegex();
    addFilePathRegex(null, org, filePathRegex);

    AuditDTO auditDTO = assertAuditLog(null);
    assertOrganizationData(auditDTO, org);
    assertProprietaryConfigCustomData(auditDTO, new ArrayList<>(), new ArrayList<>());
  }

  @Test
  public void testAddFilePathRegex_Unauthorized() throws Exception {
    FilePathRegex filePathRegex = new FilePathRegex();
    addFilePathRegex(unauthorizedUser, org, filePathRegex);

    AuditDTO auditDTO = assertAuditLog("unauthorized");
    assertOrganizationData(auditDTO, org);
  }

  private void addFilePathRegex(User user, Owner owner, FilePathRegex filePathRegex)
      throws Exception
  {
    restRequest(user, owner).path(ProprietaryConfigResource.ADD_FILE_PATH_REGEX).body(filePathRegex).post();
  }

  private void upsert(User user, Owner owner, ProprietaryConfig proprietaryConfig)
      throws Exception
  {
    restRequest(user, owner).body(proprietaryConfig).put();
  }

  private HttpRequest restRequest(User user, Owner owner) {
    return (user == null ? restRequest() : restRequest().auth(user.getUsername(), user.getPassword()))
        .path(ProprietaryConfigResource.RESOURCE_PATH).parameter(owner.getType(), owner.getPublicId());
  }

  private AuditDTO assertAuditLog(String error) {
    AuditDTO auditDTO = awaitLogEntries(AuditEvent.CONFIGURE_PROPRIETARY_COMPONENTS, 1).get(0);
    assertStandardData(auditDTO, AuditEvent.CONFIGURE_PROPRIETARY_COMPONENTS, error);
    return auditDTO;
  }

  private void assertProprietaryConfigCustomData(final AuditDTO auditDTO,
                                                 final List<String> packageMatchers,
                                                 final List<String> regexMatchers)
  {
    assertCustomData(auditDTO, "packageMatchers", packageMatchers);
    assertCustomData(auditDTO, "regexMatchers", regexMatchers);
  }
}
