/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class ApiAuditLogsResourceTest
    extends AbstractResourceTest
{
  private static final String LOG_DIR = "./log";

  @After
  public void after() throws IOException {
    deleteAuditLogs();
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.AUDIT_LOGS_RESOURCE_PATH);
  }

  @Test
  public void testGetAuditLogs() throws Exception {
    copyTestResource("audit-2024-02-07.log.gz");
    copyTestResource("audit-2024-02-08.log.gz");

    HttpResponse response = restRequest().auth(getUser())
        .query("startUtcDate", "2024-02-04")
        .query("endUtcDate", "2024-02-07")
        .get();

    assertResponseStatus(HttpStatus.SC_OK, response);
    assertThat(response.getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
    String expectedContent =
        "{\"timestamp\":\"2024-02-07T17:56:48.007-03:00\",\"username\":\"*SYSTEM\",\"domain\":\"server\","
            + "\"type\":\"start\",\"data\":{\"serverInstanceId\":\"e3a2d628-48fb-4be1-8b7e-861bf64b9224\","
            + "\"serverConfigurationFile\":\"/home/config.yml\",\"serverRelease\":\"173.0-SNAPSHOT\","
            + "\"serverBuild\":\"build-number\",\"processOwner\":\"obarra\"}}\n";
    assertThat(response.getBodyText()).isEqualTo(expectedContent);
  }

  private void copyTestResource(String filename) throws IOException {
    String filepath = getClass().getClassLoader().getResource(getClass().getSimpleName() + "/" + filename).getFile();
    Files.copy(new File(filepath).toPath(), Paths.get(LOG_DIR, filename));
  }

  private void deleteAuditLogs() throws IOException {
    Files.list(Paths.get(LOG_DIR))
        .filter(
            file -> file.getFileName().toString().startsWith("audit") && file.getFileName().toString().endsWith(".gz"))
        .forEach(file -> {
          try {
            Files.delete(file);
          }
          catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        });
  }

  private User getUser() {
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false /* global */, Permission.ACCESS_AUDIT_LOG);
    tempEntity.newMembershipMapping(Organization.ROOT_ORGANIZATION_ID, role.getId(), user.getUsername());
    return user;
  }
}
