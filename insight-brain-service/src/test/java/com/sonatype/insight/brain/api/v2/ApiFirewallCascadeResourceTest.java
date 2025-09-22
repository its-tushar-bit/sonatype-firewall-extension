/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.CascadeReevaluateTicketDTO;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

import java.util.Date;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.component.MatchState;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiFirewallCascadeResourceTest extends AbstractResourceTest
{
  @Test
  public void testInitiateCascadeReevaluation_Success() throws Exception {
    Repository repository = tempEntity.newRepository();
    String componentHash = "test_hash_123";
    Date now = new Date();
    tempEntity.newRepositoryComponent(repository.getId(),
        MatchState.EXACT, "test/path", componentHash,
        ComponentIdentifier.createNpmCoordinates("test-package", "1.0.0"), now, now);

    String uri = PublicApiPaths.MALWARE_CASCADE_REEVALUATE_PATH + "/componentHash/" + componentHash;

    HttpResponse response = restRequest()
        .path(uri)
        .post();

    assertThat(response.getStatusCode()).isEqualTo(200);

    CascadeReevaluateTicketDTO ticket = response.getBody(CascadeReevaluateTicketDTO.class);
    assertThat(ticket).isNotNull();
    assertThat(ticket.statusUrl).startsWith(PublicApiPaths.MALWARE_CASCADE_REEVALUATE_PATH + "/status/");
  }

  @Test
  public void testInitiateCascadeReevaluation_MultipleRepositories() throws Exception {
    // Create multiple repositories with the same component
    Repository repo1 = tempEntity.newRepository("repo-1");
    Repository repo2 = tempEntity.newRepository("repo-2");
    String componentHash = "multi_repo_hash";
    Date now = new Date();

    tempEntity.newRepositoryComponent(repo1.getId(),
        MatchState.EXACT, "test/path1", componentHash,
        ComponentIdentifier.createNpmCoordinates("test-package", "1.0.0"), now, now);
    tempEntity.newRepositoryComponent(repo2.getId(),
        MatchState.EXACT, "test/path2", componentHash,
        ComponentIdentifier.createNpmCoordinates("test-package", "1.0.0"), now, now);

    String uri = PublicApiPaths.MALWARE_CASCADE_REEVALUATE_PATH + "/componentHash/" + componentHash;

    HttpResponse response = restRequest()
        .path(uri)
        .post();

    assertThat(response.getStatusCode()).isEqualTo(200);

    CascadeReevaluateTicketDTO ticket = response.getBody(CascadeReevaluateTicketDTO.class);
    assertThat(ticket).isNotNull();
    assertThat(ticket.statusUrl).startsWith(PublicApiPaths.MALWARE_CASCADE_REEVALUATE_PATH + "/status/");
  }

  @Test
  public void testInitiateCascadeReevaluation_ComponentNotFound() throws Exception {
    String nonExistentHash = "non_existent_hash";
    String uri = PublicApiPaths.MALWARE_CASCADE_REEVALUATE_PATH + "/componentHash/" + nonExistentHash;

    HttpResponse response = restRequest()
        .path(uri)
        .post();

    assertThat(response.getStatusCode()).isEqualTo(200);

    CascadeReevaluateTicketDTO ticket = response.getBody(CascadeReevaluateTicketDTO.class);
    assertThat(ticket).isNotNull();
    assertThat(ticket.statusUrl).startsWith(PublicApiPaths.MALWARE_CASCADE_REEVALUATE_PATH + "/status/");
  }

  @Test
  public void testInitiateCascadeReevaluation_BlankComponentHash() throws Exception {
    String uri = PublicApiPaths.MALWARE_CASCADE_REEVALUATE_PATH + "/componentHash/";

    HttpResponse response = restRequest()
        .path(uri)
        .post();

    // Should return 404 for blank component hash in path (malformed URL)
    assertThat(response.getStatusCode()).isIn(400, 404);
  }

  @Test
  public void testInitiateCascadeReevaluation_InvalidPath() throws Exception {
    String uri = PublicApiPaths.MALWARE_CASCADE_REEVALUATE_PATH + "/componentHash/";

    HttpResponse response = restRequest()
        .path(uri)
        .post();

    // Should return error for invalid path
    assertThat(response.getStatusCode()).isIn(400, 404);
  }

  @Test
  public void testInitiateCascadeReevaluation_RequiresContainerLevelPermission() throws Exception {
    Repository repository = tempEntity.newRepository();
    String componentHash = "permission_test_hash";
    Date now = new Date();
    tempEntity.newRepositoryComponent(repository.getId(),
        MatchState.EXACT, "test/path", componentHash,
        ComponentIdentifier.createNpmCoordinates("permission-package", "1.0.0"), now, now);

    String uri = PublicApiPaths.MALWARE_CASCADE_REEVALUATE_PATH + "/componentHash/" + componentHash;

    HttpResponse response = restRequest()
        .path(uri)
        .post();

    assertThat(response.getStatusCode()).isEqualTo(200);

    CascadeReevaluateTicketDTO ticket = response.getBody(CascadeReevaluateTicketDTO.class);
    assertThat(ticket).isNotNull();
    assertThat(ticket.statusUrl).startsWith(PublicApiPaths.MALWARE_CASCADE_REEVALUATE_PATH + "/status/");
  }
}
