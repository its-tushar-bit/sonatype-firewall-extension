/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.api.v2;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.api.AbstractIqApiTest;
import com.sonatype.clm.testing.api.categories.ApiRegressionTest;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.ApiComponentsInQuarantineReportingResource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;

import java.util.Date;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

/**
 * API regression suite for {@code GET /api/v2/reports/components/quarantined}
 * ({@link ApiComponentsInQuarantineReportingResource}).
 *
 * <p>
 * A {@link RepositoryComponent} enters "quarantined" state when
 * {@code setQuarantineTime(...)} is non-null and {@code setUnquarantineTimeForManualRelease}
 * is null — released components must not surface in the response.
 *
 * <p>
 * Note: the response DTO ({@code ApiRepositoryComponentDTO}) exposes {@code packageUrl},
 * {@code displayName}, {@code hash}, {@code componentIdentifier}, {@code quarantineId} and
 * {@code quarantineTime} — but NOT {@code pathname}. Positive-path assertions are made
 * against the {@code hash} field, which the fixture derives uniquely per test.
 */
@Category(ApiRegressionTest.class)
public class ComponentsInQuarantineReportApiRegressionTest
    extends AbstractIqApiTest
{
  private static final String QUARANTINED_PATH =
      PublicApiPaths.REPORTS_RESOURCE_PATH_V2 + ApiComponentsInQuarantineReportingResource.PATH;

  /**
   * Positive-path + filter guard. A component released from quarantine (has both
   * {@code quarantineTime} and {@code unquarantineTimeForManualRelease}) must NOT surface
   * in the response. A still-quarantined component in the same test must appear — proving
   * the filter is active, not just returning "everything". The {@code .contains(...)}
   * assertion also implicitly proves the endpoint returns non-empty results.
   */
  @Test
  public void testGetComponentsInQuarantine_releasedComponentIsExcluded() throws Exception {
    Repository repo = tempEntity.newRepository(uniqueId("api-quarantine-rel"));
    String stillQuarantinedHash = tempEntity.newRandomHash();
    String releasedHash = tempEntity.newRandomHash();
    createQuarantinedComponent(repo, uniqueId("api-quarantine-still-path"), stillQuarantinedHash);
    createReleasedComponent(repo, uniqueId("api-quarantine-released-path"), releasedHash);

    HttpResponse response = apiGet(QUARANTINED_PATH);
    assertResponseStatus(200, response);

    assertThatJson(response.getBodyText())
        .inPath("$.componentsInQuarantine[*].components[*].component.hash")
        .isArray()
        .contains(stillQuarantinedHash)
        .doesNotContain(releasedHash);
  }

  @Test
  public void testGetComponentsInQuarantine_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(QUARANTINED_PATH);
    assertResponseStatus(401, response);
  }

  private void createQuarantinedComponent(final Repository repo, final String pathname, final String hash) {
    RepositoryComponent component = baseComponent(repo, pathname, hash);
    component.setQuarantineTime(new Date());
    tempEntity.newRepositoryComponent(component);
  }

  private void createReleasedComponent(final Repository repo, final String pathname, final String hash) {
    RepositoryComponent component = baseComponent(repo, pathname, hash);
    component.setQuarantineTime(new Date());
    component.setUnquarantineTimeForManualRelease(new Date());
    tempEntity.newRepositoryComponent(component);
  }

  private static RepositoryComponent baseComponent(final Repository repo, final String pathname, final String hash) {
    RepositoryComponent component = new RepositoryComponent();
    component.setRepositoryId(repo.getId());
    component.setPathname(pathname);
    component.setTime(new Date());
    component.setHash(hash);
    component.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    component.setMatchStateId(MatchState.EXACT.getId());
    component.setIdentificationSourceId(IdentificationSource.SONATYPE.getId());
    component.setLastEvaluationTime(new Date());
    return component;
  }
}
