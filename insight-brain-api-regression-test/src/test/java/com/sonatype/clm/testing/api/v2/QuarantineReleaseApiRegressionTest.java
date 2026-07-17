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
import com.sonatype.insight.brain.api.v2.ApiComponentReleaseQuarantineResource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import java.util.Date;

import jakarta.ws.rs.core.MediaType;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * API regression suite for {@code POST /api/v2/repositories/quarantine/{quarantineId}/release}
 * ({@link ApiComponentReleaseQuarantineResource}). The endpoint releases a quarantined
 * repository component without re-evaluating policy — the {@code quarantineId} path segment
 * is the internal {@code RepositoryComponent.id}, and the request body is a plain-text
 * comment (not JSON), so tests use the raw {@link #apiRequest} builder rather than
 * {@code apiPostJson}. This is the only JAX-RS method on the resource; there is no
 * intentionally-deferred coverage.
 *
 * <p>
 * Requires {@link LicensedFeature#FIREWALL} (class-level
 * {@code @ProductLicenseEnforcementPoint}); the 401 contract holds even without the license
 * because Shiro's anonymous-filter runs before the license enforcement point.
 *
 * <p>
 * Error contracts guarded here match the messages emitted by
 * {@code ApiComponentReleaseQuarantineService}:
 * <ul>
 * <li>Unknown quarantineId → 404 {@code "Cannot find a component with quarantineId ..."}
 * (resource-body path: {@code RepositoryComponentDAO} lookup — the resource has no
 * {@code @AuthzContext} interceptor, so this fragment reliably pins the DAO branch,
 * not an auth-layer 404.)
 * <li>Component exists but not quarantined → 400 {@code "... is not quarantined."}
 * <li>Blank comment → 400 {@code "Comment has not been specified."}
 * </ul>
 *
 * <p>
 * <b>Fake ids in 401 tests.</b> {@code _unauthenticated_returns401} passes a fabricated
 * {@code uniqueId("any-quarantine")} because Shiro's anonymous-filter 401s before the
 * resource body's DAO lookup. If a future refactor moves the quarantine-id validation ahead
 * of the auth check, this test would surface a 404 instead — a legitimate signal to update
 * alongside the refactor, not a bug in the test.
 */
@Category(ApiRegressionTest.class)
public class QuarantineReleaseApiRegressionTest
    extends AbstractIqApiTest
{
  @Before
  public void enableFirewall() throws Exception {
    setFeatures(LicensedFeature.FIREWALL);
  }

  private static String releasePath(final String quarantineId) {
    return PublicApiPaths.COMPONENT_QUARANTINE_RELEASE_PATH_V2.replace("{quarantineId}", quarantineId);
  }

  /**
   * Seed a repository component in a fresh repository. Each call gets its own
   * {@code RepositoryManager} / {@code Repository} pair and a unique maven artifactId, so
   * concurrent invocations in the same suite run never collide on the
   * {@code (repository_id, purl)} uniqueness constraint.
   *
   * @param quarantineTime {@code non-null} to seed a quarantined component; {@code null} to
   *          seed a component that was ingested but never quarantined
   */
  private RepositoryComponent seedComponent(final Date quarantineTime) throws Exception {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, uniqueId("api-quar-rel-repo"));
    Date ingestedTime = quarantineTime != null ? quarantineTime : new Date();
    String purl = "pkg:maven/g/" + uniqueId("art") + "@v1?type=jar";
    return tempEntity.newRepositoryComponent(
        repository.getId(), MatchState.EXACT, "pathname", tempEntity.newRandomHash(),
        new PackageUrlIdentifier(purl).ensureCompleteIdentifier(),
        ingestedTime,
        quarantineTime);
  }

  /**
   * Seed a quarantined component with a quarantine time deliberately set 1 second in the
   * past. The offset guarantees {@code quarantineReleaseTime} (assigned by the resource at
   * request time) is strictly greater than {@code quarantineTime}, so the happy-path
   * assertion on {@code quarantineReleaseTime} being non-null is not competing with any
   * clock-skew or same-millisecond edge case on fast machines.
   */
  private RepositoryComponent seedQuarantinedComponent() throws Exception {
    return seedComponent(new Date(System.currentTimeMillis() - 1000));
  }

  private HttpResponse postRelease(final String quarantineId, final String comment) throws Exception {
    return apiRequest().path(releasePath(quarantineId)).body(comment, MediaType.TEXT_PLAIN).post();
  }

  private HttpResponse anonPostRelease(final String quarantineId, final String comment) throws Exception {
    return anonApiRequest().path(releasePath(quarantineId)).body(comment, MediaType.TEXT_PLAIN).post();
  }

  @Test
  public void testReleaseQuarantine_happyPath_returns200() throws Exception {
    RepositoryComponent quarantined = seedQuarantinedComponent();

    HttpResponse response = postRelease(quarantined.getId(), "released via regression");
    assertResponseStatus(200, response);

    assertThatJson(response.getBodyText()).node("componentReleasedFromQuarantine").isPresent();
    assertThatJson(response.getBodyText())
        .node("componentReleasedFromQuarantine.component.quarantineReleaseTime")
        .isNotNull();
  }

  @Test
  public void testReleaseQuarantine_unknownQuarantineId_returns404() throws Exception {
    HttpResponse response = postRelease(uniqueId("no-such-quarantine"), "n/a");
    assertResponseStatus(404, response);
    assertThat(response.getBodyText()).containsIgnoringCase("cannot find a component with quarantineid");
  }

  @Test
  public void testReleaseQuarantine_componentNotQuarantined_returns400() throws Exception {
    RepositoryComponent notQuarantined = seedComponent(null);

    HttpResponse response = postRelease(notQuarantined.getId(), "release attempt");
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).containsIgnoringCase("is not quarantined");
  }

  @Test
  public void testReleaseQuarantine_missingComment_returns400() throws Exception {
    RepositoryComponent quarantined = seedQuarantinedComponent();

    HttpResponse response = postRelease(quarantined.getId(), "");
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).containsIgnoringCase("comment has not been specified");
  }

  @Test
  public void testReleaseQuarantine_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonPostRelease(uniqueId("any-quarantine"), "n/a");
    assertResponseStatus(401, response);
  }
}
