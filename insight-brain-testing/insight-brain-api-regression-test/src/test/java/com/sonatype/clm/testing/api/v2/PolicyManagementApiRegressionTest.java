/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.api.v2;

import com.sonatype.clm.testing.api.AbstractIqApiTest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * API regression coverage for the read-only policy-management endpoints under
 * {@code /api/v2/policies} and {@code /api/v2/policy/{ownerType}/{ownerId}/export}.
 *
 * <p>
 * Both endpoints are {@code @ProductLicenseEnforcementPoint(POLICY_MANAGEMENT)};
 * {@link #enablePolicyManagement()} enables the feature so 402s never mask a 401/404.
 *
 * <p>
 * <b>Read-only surface — no mutating-verb AC.</b> The public {@code /api/v2/} surface
 * for policy management is GET-only; full CRUD lives under the internal
 * {@code /rest/policy/...} resource which is out of scope for this module. Consequently
 * no 400 pins are asserted here — neither endpoint performs upfront body/query
 * validation that produces a stable {@code BadRequestException}, and framework-layer
 * 400s from Jersey coercion aren't part of the contract.
 *
 * <p>
 * <b>404 differs by {@code includeInherited}.</b> With {@code includeInherited=false},
 * the resource DAO-looks up the owner <em>before</em> auth, producing a resource-body
 * 404. With {@code includeInherited=true}, the delegated service method carries
 * {@code @Authorize(READ) @AuthzContext(...)}, so anon callers 401 at the interceptor
 * before the entity lookup — the direct-unauthenticated test uses a seeded app to force
 * this branch rather than the DAO 404.
 */
public class PolicyManagementApiRegressionTest
    extends AbstractIqApiTest
{
  private static final String POLICIES_LIST_PATH = PublicApiPaths.POLICY_RESOURCE_PATH;

  /**
   * Un-templated prefix of {@link PublicApiPaths#POLICY_EXPORT_RESOURCE_PATH} — the raw
   * constant carries a Jersey {@code {ownerType:...}}/{@code {ownerId}} template that
   * leaks through {@code HttpRequest#path()} URL-encoding. Substring at the first
   * {@code /{} gives a compile-time-bound prefix that fails to build if the constant is
   * renamed or loses its templated shape.
   */
  private static final String POLICY_EXPORT_BASE = PublicApiPaths.POLICY_EXPORT_RESOURCE_PATH
      .substring(0, PublicApiPaths.POLICY_EXPORT_RESOURCE_PATH.indexOf("/{"));

  @BeforeEach
  public void enablePolicyManagement() throws Exception {
    setFeatures(LicensedFeature.POLICY_MANAGEMENT);
  }

  private static String exportPath(final String ownerType, final String ownerId) {
    return POLICY_EXPORT_BASE + "/" + ownerType + "/" + ownerId + "/export";
  }

  /**
   * Seeds a uniquely-named root-org policy and verifies the list endpoint returns it. Uses
   * {@code contains} (not {@code containsExactly}) because {@code testCLMServer} is shared
   * across tests within a fork and the list also contains any policies seeded by
   * concurrently-tracked {@code TemporaryEntity} rules on the shared server, plus any
   * pre-seeded schema policies.
   */
  @Test
  public void testGetPolicies_afterSeedingPolicy_containsSeededPolicy() throws Exception {
    String policyName = uniqueName("api-policy-list");
    Policy seeded = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, policyName);

    HttpResponse response = apiGet(POLICIES_LIST_PATH);

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("policies").isArray().isNotEmpty();
    assertThatJson(response.getBodyText())
        .inPath("$.policies[*].id")
        .isArray()
        .contains(seeded.getId());
    assertThatJson(response.getBodyText())
        .inPath("$.policies[*].name")
        .isArray()
        .contains(policyName);
  }

  /**
   * Shiro's anonymous filter rejects the request at the HTTP layer before
   * {@link ApiPolicyResourceV2#getPolicies} runs. The 200-empty-list behavior for
   * <em>authenticated-but-unauthorized</em> callers lives in the service layer
   * ({@code ApiPolicyServiceAuthzTest}) and is out of scope for this HTTP-surface pin —
   * asserting it here would require seeding a permissionless subject.
   */
  @Test
  public void testGetPolicies_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(POLICIES_LIST_PATH);

    assertResponseStatus(401, response);
  }

  @Test
  public void testExportPolicies_organization_direct_containsSeededPolicy() throws Exception {
    Organization org = tempEntity.newOrganization(uniqueName("api-policy-export-org"));
    Policy policy = tempEntity.newPolicy(org.getId(), uniqueName("org-policy"));

    HttpResponse response = apiGet(exportPath("organization", org.getId()));

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("policies").isArray().hasSize(1);
    assertThatJson(response.getBodyText())
        .inPath("$.policies[*].id")
        .isArray()
        .containsExactly(policy.getId());
    assertThatJson(response.getBodyText())
        .inPath("$.policies[*].ownerId")
        .isArray()
        .containsExactly(org.getId());
  }

  @Test
  public void testExportPolicies_application_direct_containsSeededPolicy() throws Exception {
    Organization org = tempEntity.newOrganization(uniqueName("api-policy-export-app-org"));
    Application app = tempEntity.newApplication(uniqueId("api-policy-export-app"), org.getId());
    Policy appPolicy = tempEntity.newPolicy(app.getId(), uniqueName("app-policy"));
    tempEntity.newPolicy(org.getId(), uniqueName("parent-org-policy"));

    HttpResponse response = apiGet(exportPath("application", app.getId()));

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("policies").isArray().hasSize(1);
    assertThatJson(response.getBodyText())
        .inPath("$.policies[*].id")
        .isArray()
        .containsExactly(appPolicy.getId());
    assertThatJson(response.getBodyText())
        .inPath("$.policies[*].ownerId")
        .isArray()
        .containsExactly(app.getId());
  }

  @Test
  public void testExportPolicies_repository_direct_containsSeededPolicy() throws Exception {
    Repository repo = tempEntity.newRepository();
    Policy repoPolicy = tempEntity.newPolicy(repo.getId(), uniqueName("repo-policy"));

    HttpResponse response = apiGet(exportPath("repository", repo.getId()));

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("policies").isArray().hasSize(1);
    assertThatJson(response.getBodyText())
        .inPath("$.policies[*].id")
        .isArray()
        .containsExactly(repoPolicy.getId());
    assertThatJson(response.getBodyText())
        .inPath("$.policies[*].ownerId")
        .isArray()
        .containsExactly(repo.getId());
  }

  /**
   * With {@code includeInherited=true}, the export walks up the owner hierarchy and returns
   * the target's direct policies <em>plus</em> those of every parent. This test seeds an
   * app-level policy and a parent-org-level policy and asserts both appear — a regression
   * that broke hierarchy walking (only direct policies, or duplicated inherited policies)
   * would trip this test.
   *
   * <p>
   * Presence-only assertion via {@code .contains(...)} (not
   * {@code .containsExactlyInAnyOrder(...)}): the walk climbs into the root organization,
   * which carries pre-seeded schema policies and picks up any policies seeded by
   * concurrently-tracked {@code TemporaryEntity} rules on the shared server. An
   * exhaustive assertion would flake whenever another test on the fork holds an active
   * root-org policy — same reason {@code testGetPolicies_afterSeedingPolicy_containsSeededPolicy}
   * uses {@code .contains(...)}.
   */
  @Test
  public void testExportPolicies_application_includeInherited_containsDirectAndInheritedPolicies() throws Exception {
    Organization org = tempEntity.newOrganization(uniqueName("api-policy-inh-org"));
    Application app = tempEntity.newApplication(uniqueId("api-policy-inh-app"), org.getId());
    Policy appPolicy = tempEntity.newPolicy(app.getId(), uniqueName("app-policy-inh"));
    Policy orgPolicy = tempEntity.newPolicy(org.getId(), uniqueName("org-policy-inh"));

    HttpResponse response = apiGet(exportPath("application", app.getId()), "includeInherited", true);

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText())
        .inPath("$.policies[*].id")
        .isArray()
        .contains(appPolicy.getId(), orgPolicy.getId());
  }

  /**
   * On the direct-export path ({@code includeInherited=false}), the delegated service method
   * {@code PolicyImportExport.exportApplication} carries {@code @Authorize(READ)
   * @AuthzContext(Key.APPLICATION)}. For an anonymous caller with a <em>real</em> app id the
   * DAO lookup succeeds and the auth interceptor rejects with 401 — this is the pin. If we
   * used an unknown app id here, the DAO's {@code getByIdNotNull} would throw 404
   * <em>before</em> auth runs, masking the 401 contract.
   */
  @Test
  public void testExportPolicies_direct_unauthenticated_returns401() throws Exception {
    Organization org = tempEntity.newOrganization(uniqueName("api-policy-export-anon-org"));
    Application app = tempEntity.newApplication(uniqueId("api-policy-export-anon-app"), org.getId());

    HttpResponse response = anonApiGet(exportPath("application", app.getId()));

    assertResponseStatus(401, response);
  }

  /**
   * Resource-body 404 via {@code applicationDAO.getByIdNotNull} on the direct-export path.
   * Fragment {@code "Application with ID"} is emitted by
   * {@code AbstractSqlDAO.getByIdNotNull}. Distinct from the interceptor path (inherited
   * export) which produces the same fragment via a different code route — the pin here is
   * on the DAO branch specifically.
   */
  @Test
  public void testExportPolicies_unknownApplication_returns404() throws Exception {
    HttpResponse response = apiGet(exportPath("application", uniqueId("no-app")));

    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .containsIgnoringCase("application with id")
        .containsIgnoringCase("does not exist");
  }

  @Test
  public void testExportPolicies_unknownOrganization_returns404() throws Exception {
    HttpResponse response = apiGet(exportPath("organization", uniqueId("no-org")));

    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .containsIgnoringCase("organization with id")
        .containsIgnoringCase("does not exist");
  }

  @Test
  public void testExportPolicies_unknownRepository_returns404() throws Exception {
    HttpResponse response = apiGet(exportPath("repository", uniqueId("no-repo")));

    assertResponseStatus(404, response);
    assertThat(response.getBodyText())
        .containsIgnoringCase("repository with id")
        .containsIgnoringCase("does not exist");
  }

  /**
   * The {@code @Path} regex on {@code ApiPolicyExportResourceV2} restricts {@code ownerType}
   * to {@code application|organization|repository}; any other value (here
   * {@code repository_manager}, which is a legal owner type on the internal CRUD resource
   * but <em>not</em> on the public export endpoint) fails at the JAX-RS routing layer and
   * produces a framework 404 with no resource body — hence status-only assertion. If a
   * future PR widens the public API to accept {@code repository_manager} /
   * {@code repository_container}, this test will trip and the widening needs an explicit
   * decision.
   */
  @Test
  public void testExportPolicies_invalidOwnerType_returns404() throws Exception {
    HttpResponse response = apiGet(exportPath("repository_manager", uniqueId("any-id")));

    assertResponseStatus(404, response);
  }
}
