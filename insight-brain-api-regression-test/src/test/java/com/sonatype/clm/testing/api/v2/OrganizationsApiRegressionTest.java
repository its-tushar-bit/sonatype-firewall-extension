/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.api.v2;

import com.sonatype.clm.testing.api.AbstractIqApiTest;
import com.sonatype.clm.testing.api.categories.ApiRegressionTest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationDTO;
import com.sonatype.insight.brain.model.Organization;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * API regression suite for {@code api/v2/organizations} — covers create (root and child), get
 * (single + list), name filter, byid batch lookup, move (reparent), delete, and the auth
 * contract. Note: there is no plain PUT rename on this resource — parent changes go through the
 * dedicated {@code /{id}/move/destination/{destinationId}} sub-path.
 */
@Category(ApiRegressionTest.class)
public class OrganizationsApiRegressionTest
    extends AbstractIqApiTest
{
  private static final String ORGS_BASE = PublicApiPaths.ORG_RESOURCE_PATH;

  @Test
  public void testCreateOrganization_underRoot() throws Exception {
    ApiOrganizationDTO body = new ApiOrganizationDTO();
    body.name = uniqueName("Api Org Root");

    HttpResponse response = apiPostJson(ORGS_BASE, body);
    assertResponseStatus(200, response);

    String responseBody = response.getBodyText();
    assertThatJson(responseBody).node("name").isEqualTo(body.name);
    assertThatJson(responseBody).node("id").isString().isNotEmpty();
    assertThatJson(responseBody).node("parentOrganizationId").isEqualTo(Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testCreateOrganization_underParent() throws Exception {
    Organization parent = tempEntity.newOrganization(uniqueName("Api Org Parent"));

    ApiOrganizationDTO body = new ApiOrganizationDTO();
    body.name = uniqueName("Api Org Child");
    body.parentOrganizationId = parent.getId();

    HttpResponse response = apiPostJson(ORGS_BASE, body);
    assertResponseStatus(200, response);

    String responseBody = response.getBodyText();
    assertThatJson(responseBody).node("name").isEqualTo(body.name);
    assertThatJson(responseBody).node("parentOrganizationId").isEqualTo(parent.getId());
  }

  @Test
  public void testGetOrganizations_returnsSeededOrg() throws Exception {
    Organization org = tempEntity.newOrganization(uniqueName("Api Org List"));

    HttpResponse response = apiGet(ORGS_BASE, "organizationName", org.getName());
    assertResponseStatus(200, response);

    String responseBody = response.getBodyText();
    assertThatJson(responseBody).node("organizations").isArray().hasSize(1);
    assertThatJson(responseBody).node("organizations[0].id").isEqualTo(org.getId());
  }

  @Test
  public void testGetOrganizations_filterByUnknownName_returnsEmpty() throws Exception {
    HttpResponse response = apiGet(ORGS_BASE, "organizationName", uniqueName("No Such Org"));
    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText())
        .node("organizations")
        .isArray()
        .isEmpty();
  }

  @Test
  public void testGetOrganizationsByIds_returnsSeededOrgs() throws Exception {
    Organization org1 = tempEntity.newOrganization(uniqueName("Api Org ByIds 1"));
    Organization org2 = tempEntity.newOrganization(uniqueName("Api Org ByIds 2"));

    HttpResponse response = apiGet(ORGS_BASE + "/byid", "id", org1.getId(), org2.getId());
    assertResponseStatus(200, response);

    String responseBody = response.getBodyText();
    assertThatJson(responseBody).node("organizations").isArray().hasSize(2);
    assertThatJson(responseBody)
        .inPath("$.organizations[*].id")
        .isArray()
        .contains(org1.getId(), org2.getId());
  }

  @Test
  public void testGetOrganizationById_success() throws Exception {
    Organization org = tempEntity.newOrganization(uniqueName("Api Org By Id"));

    HttpResponse response = apiGet(orgPath(org));
    assertResponseStatus(200, response);

    String responseBody = response.getBodyText();
    assertThatJson(responseBody).node("id").isEqualTo(org.getId());
    assertThatJson(responseBody).node("name").isEqualTo(org.getName());
  }

  @Test
  public void testGetOrganizationById_notFound() throws Exception {
    HttpResponse response = apiGet(orgPath(uniqueId("nonexistent-org")));
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).containsIgnoringCase("does not exist");
  }

  /** Move an org from one parent to another via {@code PUT /{id}/move/destination/{destinationId}}. */
  @Test
  public void testMoveOrganization_success() throws Exception {
    Organization parentA = tempEntity.newOrganization(uniqueName("Api Org Parent A"));
    Organization parentB = tempEntity.newOrganization(uniqueName("Api Org Parent B"));
    Organization child = tempEntity.newOrganization(uniqueName("Api Org Child"), parentA);

    HttpResponse response = apiPut(orgPath(child) + "/move/destination/" + parentB.getId());
    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("errors").isArray().isEmpty();

    HttpResponse get = apiGet(orgPath(child));
    assertResponseStatus(200, get);
    assertThatJson(get.getBodyText()).node("parentOrganizationId").isEqualTo(parentB.getId());
  }

  @Test
  public void testDeleteOrganization_success() throws Exception {
    Organization org = tempEntity.newOrganization(uniqueName("Api Org Delete"));

    assertResponseStatus(204, apiDelete(orgPath(org)));
    assertResponseStatus(404, apiGet(orgPath(org)));
    // Org is deleted via API above; TemporaryEntity cleanup at teardown is a no-op today.
  }

  /**
   * Deleting an org that has a child organization cascade-deletes both — the server does not
   * refuse the operation. Verified against {@code OrganizationDAO.delete} (line 395:
   * {@code ownerDAOProvider.get().cascadeDelete(tx, organization)}); the only refusal paths are
   * ROOT org (line 354) and the auto-application-parent org when auto-app creation is enabled
   * (line 360). Test pins the cascade contract so a future change to a refusal contract would
   * fail here.
   *
   * <p>
   * <b>Note on the AC discrepancy:</b> the Jira AC for CLM-42135 lists "refuses delete when
   * children exist" for this endpoint, but that behavior was never actually shipped for the
   * general has-children case (see the code-path summary above). This test intentionally
   * documents the real contract, not the AC — a well-intentioned future change that "fixes"
   * the behavior to match the AC would need to update this test alongside the DAO change.
   */
  @Test
  public void testDeleteOrganization_withChildren_cascades() throws Exception {
    Organization parent = tempEntity.newOrganization(uniqueName("Api Org Parent With Child"));
    Organization child = tempEntity.newOrganization(uniqueName("Api Org Child Of"), parent);

    assertResponseStatus(204, apiDelete(orgPath(parent)));
    assertResponseStatus(404, apiGet(orgPath(parent)));
    assertResponseStatus(404, apiGet(orgPath(child)));
  }

  @Test
  public void testDeleteOrganization_notFound() throws Exception {
    HttpResponse response = apiDelete(orgPath(uniqueId("nonexistent-org")));
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).containsIgnoringCase("does not exist");
  }

  /** Auth contract: unauthenticated callers get 401, not 200/403/404. */
  @Test
  public void testGetOrganizations_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(ORGS_BASE);
    assertResponseStatus(401, response);
  }

  /** Auth contract on POST: unauthenticated create fails with 401 before the body is parsed. */
  @Test
  public void testCreateOrganization_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiPostJson(ORGS_BASE, new ApiOrganizationDTO());
    assertResponseStatus(401, response);
  }

  /** Auth contract on PUT (move): unauthenticated move fails with 401. */
  @Test
  public void testMoveOrganization_unauthenticated_returns401() throws Exception {
    String movePath = orgPath(uniqueId("any-org")) + "/move/destination/" + uniqueId("dest-org");
    HttpResponse response = anonApiPut(movePath);
    assertResponseStatus(401, response);
  }

  /** Auth contract on DELETE: unauthenticated delete fails with 401. */
  @Test
  public void testDeleteOrganization_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiDelete(orgPath(uniqueId("any-org")));
    assertResponseStatus(401, response);
  }

  private static String orgPath(final Organization org) {
    return orgPath(org.getId());
  }

  private static String orgPath(final String organizationId) {
    return ORGS_BASE + "/" + organizationId;
  }
}
