/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.aideveloper;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractAuditTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AiDeveloperOptInResourceTest
    extends AbstractAuditTest
{
  private InsightConfig config;

  private DatabaseConfig originalDatabase;

  @Before
  public void useExternalDatabase() {
    config = lookup(InsightConfig.class);
    originalDatabase = config.getDatabase();
    config.setDatabase(new DatabaseConfig());
  }

  @After
  public void restoreDatabaseConfig() {
    if (config != null) {
      config.setDatabase(originalDatabase);
    }
  }

  @Override
  protected void afterDatabaseReset() {
    // TemporaryEntity has removed the opt-in row; drop the cached copy so the next test reads a fresh install
    SystemConfigurationPropertyDAO.invalidateEntireCache();
  }

  @Test
  public void reportsNotOptedInUntilSomeoneOptsIn() throws Exception {
    AiDeveloperOptInStatus status = getStatus(restRequest());

    assertThat(status.optedIn()).isFalse();
    assertThat(status.optedInBy()).isNull();
    assertThat(status.optedInAt()).isNull();
    assertThat(status.externalDatabaseRequired()).isFalse();
    assertThat(status.message()).isNull();
  }

  @Test
  public void recordsOptingUserAndInstantForEveryLaterReader() throws Exception {
    User user = tempEntity.newUser();
    Instant before = Instant.now();

    AiDeveloperOptInStatus optedIn = optIn(restRequest().auth(user));
    Instant after = Instant.now();

    assertThat(optedIn.optedIn()).isTrue();
    assertThat(optedIn.optedInBy()).isEqualTo(user.getUsername());
    assertThat(Instant.parse(optedIn.optedInAt())).isBetween(before, after);

    // The stored value is what the license check and the GUIDE-3344 telemetry parse
    assertThat(lookup(SystemConfigurationPropertyDAO.class).get(SystemConfigurationProperty.AI_DEVELOPER_OPT_IN))
        .isEqualTo(optedIn.optedInBy() + "," + optedIn.optedInAt());

    assertAuditLog(AuditEvent.CONFIGURE_AI_DEVELOPER_OPT_IN, null, user.getUsername());

    // Reading past the cache is the same read a restarted server performs
    SystemConfigurationPropertyDAO.invalidateEntireCache();
    assertThat(getStatus(restRequest())).isEqualTo(optedIn);
  }

  @Test
  public void keepsOriginalUserAndInstantWhenOptedInAgain() throws Exception {
    AiDeveloperOptInStatus first = optIn(restRequest().auth(tempEntity.newUser()));

    AiDeveloperOptInStatus second = optIn(restRequest().auth(tempEntity.newUser()));

    assertThat(second).isEqualTo(first);
    assertThat(getStatus(restRequest())).isEqualTo(first);
  }

  /**
   * Two users opting in at the same time race on inserting the property. Both calls must report the same record — the
   * one the winner stored.
   */
  @Test
  public void keepsOneRecordWhenTwoUsersOptInAtOnce() throws Exception {
    CyclicBarrier bothReady = new CyclicBarrier(2);
    List<Callable<AiDeveloperOptInStatus>> calls = List.of(
        optInWhenReady(tempEntity.newUser(), bothReady),
        optInWhenReady(tempEntity.newUser(), bothReady));
    ExecutorService threads = Executors.newFixedThreadPool(calls.size());

    try {
      List<Future<AiDeveloperOptInStatus>> results = threads.invokeAll(calls);

      assertThat(results.get(0).get()).isEqualTo(results.get(1).get());
      assertThat(getStatus(restRequest())).isEqualTo(results.get(0).get());
    }
    finally {
      threads.shutdownNow();
    }
  }

  @Test
  public void readsAndRecordsWhileAiDeveloperIsLocked() throws Exception {
    setMissingFeatures(LicensedFeature.AI_DEVELOPER);

    assertThat(getStatus(restRequest()).optedIn()).isFalse();
    assertThat(optIn(restRequest()).optedIn()).isTrue();
  }

  @Test
  public void reportsThatAnExternalDatabaseIsRequiredWhenEmbedded() throws Exception {
    config.setDatabase(null);

    AiDeveloperOptInStatus status = optIn(restRequest());

    assertThat(status.optedIn()).isTrue();
    assertThat(status.externalDatabaseRequired()).isTrue();
    assertThat(status.message()).isEqualTo(AiDeveloperOptInStatus.EMBEDDED_DATABASE_MESSAGE);
  }

  @Test
  public void rejectsUnauthenticatedRequests() throws Exception {
    assertThat(request().anon().get().getStatusCode()).isEqualTo(401);
    assertThat(request().anon().post().getStatusCode()).isEqualTo(401);

    assertThat(getStatus(restRequest()).optedIn()).isFalse();
  }

  private Callable<AiDeveloperOptInStatus> optInWhenReady(User user, CyclicBarrier ready) {
    return () -> {
      ready.await();
      return optIn(restRequest().auth(user));
    };
  }

  private HttpRequest request() {
    return restRequest().path(AiDeveloperOptInResource.RESOURCE_PATH);
  }

  private AiDeveloperOptInStatus getStatus(HttpRequest request) throws Exception {
    return body(request.path(AiDeveloperOptInResource.RESOURCE_PATH).get());
  }

  private AiDeveloperOptInStatus optIn(HttpRequest request) throws Exception {
    return body(request.path(AiDeveloperOptInResource.RESOURCE_PATH).post());
  }

  private AiDeveloperOptInStatus body(HttpResponse response) throws Exception {
    assertThat(response.getStatusCode()).isEqualTo(200);
    return response.getBody(AiDeveloperOptInStatus.class);
  }
}
