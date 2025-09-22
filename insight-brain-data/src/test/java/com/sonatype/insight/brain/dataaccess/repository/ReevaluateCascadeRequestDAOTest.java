/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.repository.ReevaluateCascadeRequest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ReevaluateCascadeRequestDAOTest extends AbstractDbDAOTest
{
  private ReevaluateCascadeRequestDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createReevaluateCascadeRequestDAO();
  }

  @Test
  public void testCreateAndGetByComponentHash() {
    // Arrange
    String requestId = "cascade_test_123";
    String componentHash = "abc123def456";
    String username = "testUser";

    tempEntity.newReevaluateCascadeRequest(requestId, componentHash, username);

    // Act - Find by component hash
    List<ReevaluateCascadeRequest> found = dao.getByComponentHash(componentHash);

    // Assert
    assertThat(found).hasSize(1);

    ReevaluateCascadeRequest foundRequest = found.get(0);
    assertThat(foundRequest.getId()).isEqualTo(requestId);
    assertThat(foundRequest.getComponentReferenceHash()).isEqualTo(componentHash);
    assertThat(foundRequest.getCreatedByUsername()).isEqualTo(username);
    assertThat(foundRequest.getCreatedAt()).isNotNull();
  }

  @Test
  public void testGetByComponentHash_MultipleRequests() {
    // Arrange
    String componentHash = "shared_hash_456";
    String username = "testUser";

    tempEntity.newReevaluateCascadeRequest("cascade_1", componentHash, username);
    tempEntity.newReevaluateCascadeRequest("cascade_2", componentHash, username);
    tempEntity.newReevaluateCascadeRequest("cascade_3", "different_hash", username);

    // Act - Find by component hash
    List<ReevaluateCascadeRequest> found = dao.getByComponentHash(componentHash);

    // Assert - Should find only the 2 requests with matching component hash
    assertThat(found).hasSize(2);
    assertThat(found).allMatch(request -> componentHash.equals(request.getComponentReferenceHash()));

    List<String> requestIds = found.stream().map(ReevaluateCascadeRequest::getId).toList();
    assertThat(requestIds).containsExactlyInAnyOrder("cascade_1", "cascade_2");
  }
}
