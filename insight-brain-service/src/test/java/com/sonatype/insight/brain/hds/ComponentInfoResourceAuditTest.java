/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;

import org.junit.Before;
import org.junit.Test;

public class ComponentInfoResourceAuditTest
    extends AbstractComponentInfoResourceAuditTest
{
  private static final String LICENSES_SUBPATH = "/licenses";

  private static final String MULTI_LICENSES_SUBPATH = "/multiLicenses";

  private static final String MULTI_LICENSES_LEGAL_REVIEWER_SUBPATH = MULTI_LICENSES_SUBPATH + "/legalReviewer";

  private static final String ALL_VERSIONS_SUBPATH = "/allVersions";

  private static final String LIST_SUBPATH = "/list";

  private static final String VULNERABILITIES_SUBPATH = "/vulnerabilities";

  private Repository repository;

  @Before
  public void createRepository() {
    repository = tempEntity.newRepository();
  }

  @Test
  public void testGetComponentDetails_Application_CoordinatesOnly() throws Exception {
    detailsRequest(application, COMPONENT_IDENTIFIER, null).get();

    assertAuditComponentInfo(application, COMPONENT_IDENTIFIER, null);
  }

  @Test
  public void testGetComponentDetails_Application_HashOnly() throws Exception {
    detailsRequest(application, null, COMPONENT_HASH).get();

    assertAuditComponentInfo(application, null, COMPONENT_HASH);
  }

  @Test
  public void testGetComponentDetails_Application_CoordinatesAndHash() throws Exception {
    detailsRequest(application, COMPONENT_IDENTIFIER, COMPONENT_HASH).get();

    assertAuditComponentInfo(application, COMPONENT_IDENTIFIER, COMPONENT_HASH);
  }

  @Test
  public void testGetComponentDetails_Repository() throws Exception {
    detailsRequest(repository, COMPONENT_IDENTIFIER, COMPONENT_HASH).get();

    assertAuditComponentInfo(repository, COMPONENT_IDENTIFIER, COMPONENT_HASH);
  }

  @Test
  public void testGetComponentDetails_Unauthorized() throws Exception {
    detailsRequest(repository, COMPONENT_IDENTIFIER, COMPONENT_HASH).with(unauthorizedUser()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  public void testGetComponentDetailsList_Application() throws Exception {
    HttpRequest detailsListRequest = detailsRequestForSubpath(LIST_SUBPATH, application, COMPONENT_IDENTIFIER);
    setupHdsResponseForComponent(detailsListRequest);

    detailsListRequest.get();

    assertAuditComponentInfo(application, COMPONENT_IDENTIFIER);
  }

  @Test
  public void testGetComponentDetailsList_Repository() throws Exception {
    HttpRequest request = detailsRequestForSubpath(LIST_SUBPATH, repository, COMPONENT_IDENTIFIER);
    setupHdsResponseForComponent(request);

    request.get();

    assertAuditComponentInfo(repository, COMPONENT_IDENTIFIER);
  }

  @Test
  public void testGetComponentDetailsList_Unauthorized() throws Exception {
    detailsRequestForSubpath(LIST_SUBPATH, application, COMPONENT_IDENTIFIER).with(unauthorizedUser()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  public void testGetComponentDetailsForAllVersions_Application() throws Exception {
    HttpRequest request = detailsRequestForSubpath(ALL_VERSIONS_SUBPATH, application, COMPONENT_IDENTIFIER);
    setupHdsResponseForComponent(request);

    request.get();

    assertAuditComponentInfo(application, COMPONENT_IDENTIFIER);
  }

  @Test
  public void testGetComponentDetailsForAllVersions_Repository() throws Exception {
    HttpRequest request = detailsRequestForSubpath(ALL_VERSIONS_SUBPATH, repository, COMPONENT_IDENTIFIER);
    setupHdsResponseForComponent(request);

    request.get();

    assertAuditComponentInfo(repository, COMPONENT_IDENTIFIER);
  }

  @Test
  public void testGetComponentDetailsForAllVersions_Unauthorized() throws Exception {
    detailsRequestForSubpath(ALL_VERSIONS_SUBPATH, application, COMPONENT_IDENTIFIER).with(unauthorizedUser()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  public void testGetLicenses_Application() throws Exception {
    detailsRequestForSubpath(LICENSES_SUBPATH, application, COMPONENT_IDENTIFIER).get();

    assertAuditComponentInfo(application, COMPONENT_IDENTIFIER);
  }

  @Test
  public void testGetLicenses_Repository() throws Exception {
    detailsRequestForSubpath(LICENSES_SUBPATH, repository, COMPONENT_IDENTIFIER).get();

    assertAuditComponentInfo(repository, COMPONENT_IDENTIFIER);
  }

  @Test
  public void testGetLicenses_Unauthorized() throws Exception {
    detailsRequestForSubpath(LICENSES_SUBPATH, repository, COMPONENT_IDENTIFIER).with(unauthorizedUser()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  public void testGetMultiLicenses_Application() throws Exception {
    detailsRequestForSubpath(MULTI_LICENSES_SUBPATH, application, COMPONENT_IDENTIFIER).get();

    assertAuditComponentInfo(application, COMPONENT_IDENTIFIER);
  }

  @Test
  public void testGetMultiLicenses_Repository() throws Exception {
    detailsRequestForSubpath(MULTI_LICENSES_SUBPATH, repository, COMPONENT_IDENTIFIER).get();

    assertAuditComponentInfo(repository, COMPONENT_IDENTIFIER);
  }

  @Test
  public void testGetMultiLicenses_Unauthorized() throws Exception {
    detailsRequestForSubpath(MULTI_LICENSES_SUBPATH, repository, COMPONENT_IDENTIFIER).with(unauthorizedUser()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  public void testGetMultiLicensesForLegalReviewer_Application() throws Exception {
    detailsRequestForSubpath(MULTI_LICENSES_LEGAL_REVIEWER_SUBPATH, application, COMPONENT_IDENTIFIER).get();

    assertAuditComponentInfo(application, COMPONENT_IDENTIFIER);
  }

  @Test
  public void testGetMultiLicensesForLegalReviewer_Repository() throws Exception {
    detailsRequestForSubpath(MULTI_LICENSES_LEGAL_REVIEWER_SUBPATH, repository, COMPONENT_IDENTIFIER).get();

    assertAuditComponentInfo(repository, COMPONENT_IDENTIFIER);
  }

  @Test
  public void testGetMultiLicensesForLegalReviewer_Unauthorized() throws Exception {
    detailsRequestForSubpath(MULTI_LICENSES_LEGAL_REVIEWER_SUBPATH, repository, COMPONENT_IDENTIFIER)
        .with(unauthorizedUser())
        .get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  public void testGetSecurityVulnerabilities_Application_CoordinatesOnly() throws Exception {
    detailsRequestForSubpath(VULNERABILITIES_SUBPATH, application, COMPONENT_IDENTIFIER, null).get();

    assertAuditComponentInfo(application, COMPONENT_IDENTIFIER, null);
  }

  @Test
  public void testGetSecurityVulnerabilities_Application_CoordinatesAndHash() throws Exception {
    detailsRequestForSubpath(VULNERABILITIES_SUBPATH, application, COMPONENT_IDENTIFIER, COMPONENT_HASH).get();

    assertAuditComponentInfo(application, COMPONENT_IDENTIFIER, COMPONENT_HASH);
  }

  @Test
  public void testGetSecurityVulnerabilities_Repository() throws Exception {
    detailsRequestForSubpath(VULNERABILITIES_SUBPATH, repository, COMPONENT_IDENTIFIER, COMPONENT_HASH).get();

    assertAuditComponentInfo(repository, COMPONENT_IDENTIFIER, COMPONENT_HASH);
  }

  @Test
  public void testGetSecurityVulnerabilities_Unauthorized() throws Exception {
    detailsRequestForSubpath(VULNERABILITIES_SUBPATH, repository, COMPONENT_IDENTIFIER, COMPONENT_HASH)
        .with(unauthorizedUser())
        .get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  private HttpRequest detailsRequestForSubpath(
      String subpath,
      Owner owner,
      ComponentIdentifier componentIdentifier,
      String hash)
  {
    return detailsRequest(owner, componentIdentifier, hash).path(subpath);
  }

  private HttpRequest detailsRequestForSubpath(String subpath, Owner owner, ComponentIdentifier componentIdentifier) {
    return detailsRequest(owner, componentIdentifier, null).path(subpath);
  }

  private HttpRequest detailsRequest(Owner owner, ComponentIdentifier componentIdentifier, String hash) {
    return restRequest().path(ComponentInfoResource.RESOURCE_PATH, ComponentInfoResource.COMPONENT_DETAILS_PATH)
        .parameter(owner.getType(), owner.getType().equals(OwnerType.APPLICATION) ? owner.getPublicId() : owner.getId())
        .query("componentIdentifier", componentIdentifier)
        .query("hash", hash);
  }
}
