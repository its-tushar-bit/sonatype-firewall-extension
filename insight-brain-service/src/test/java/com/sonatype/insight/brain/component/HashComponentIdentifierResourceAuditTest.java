/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;
import com.sonatype.insight.brain.common.test.SlowTest;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class HashComponentIdentifierResourceAuditTest
    extends AbstractAuditTest
{
  private static final String COMPONENT_HASH = "componentHash";

  private static final ComponentIdentifier COMPONENT_IDENTIFIER = ComponentIdentifier
      .createMavenCoordinates("groupId", "artifactId", "version", "classifier", "extension");

  private static final String COMMENT = "comment";

  private HashComponentIdentifierDAO hashComponentIdentifierDAO;

  @Before
  public void before() throws Exception {
    hashComponentIdentifierDAO = lookup(HashComponentIdentifierDAO.class);

    mockComponentSummary(COMPONENT_IDENTIFIER, ComponentSummary.create(false));
  }

  @After
  public void after() {
    hashComponentIdentifierDAO.delete(hashComponentIdentifierDAO.getByHash(COMPONENT_HASH));
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(HashComponentIdentifierResource.RESOURCE_PATH);
  }

  @Test
  public void testSet() throws Exception {
    HashComponentIdentifier hashComponentIdentifier = hashComponentIdentifier(COMPONENT_HASH, COMPONENT_IDENTIFIER,
        COMMENT);

    restRequest().body(hashComponentIdentifier).post();

    assertHashComponentIdentifierData(assertAuditLog(AuditEvent.SET_COMPONENT_IDENTITY, null), hashComponentIdentifier);
  }

  @Test
  public void testSet_NullComment() throws Exception {
    HashComponentIdentifier hashComponentIdentifier = hashComponentIdentifier(COMPONENT_HASH, COMPONENT_IDENTIFIER,
        null);

    restRequest().body(hashComponentIdentifier).post();

    assertHashComponentIdentifierData(assertAuditLog(AuditEvent.SET_COMPONENT_IDENTITY, null), hashComponentIdentifier);
  }

  @Test
  public void testSet_Unauthorized() throws Exception {
    HashComponentIdentifier hashComponentIdentifier = hashComponentIdentifier(COMPONENT_HASH, COMPONENT_IDENTIFIER,
        COMMENT);

    restRequest().with(unauthorizedUser()).body(hashComponentIdentifier).post();

    assertAuditLog(AuditEvent.SET_COMPONENT_IDENTITY, "unauthorized");
  }

  @Test
  public void testDelete() throws Exception {
    HashComponentIdentifier hashComponentIdentifier = hashComponentIdentifier(COMPONENT_HASH, COMPONENT_IDENTIFIER,
        COMMENT);
    hashComponentIdentifierDAO.insert(hashComponentIdentifier);

    restRequest().path(COMPONENT_HASH).delete();

    assertHashComponentIdentifierData(assertAuditLog(AuditEvent.UNSET_COMPONENT_IDENTITY, null),
        hashComponentIdentifier);
  }

  @Test
  public void testUpdate() throws Exception {
    HashComponentIdentifier hashComponentIdentifier = hashComponentIdentifier(COMPONENT_HASH, COMPONENT_IDENTIFIER,
        COMMENT);
    hashComponentIdentifierDAO.insert(hashComponentIdentifier);
    hashComponentIdentifier.setId("new-id");
    restRequest().body(hashComponentIdentifier).put();

    assertHashComponentIdentifierData(assertAuditLog(AuditEvent.SET_COMPONENT_IDENTITY, null), hashComponentIdentifier);
  }

  private HashComponentIdentifier hashComponentIdentifier(String componentHash,
                                                          ComponentIdentifier componentIdentifier,
                                                          String comment)
  {
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(componentHash, componentIdentifier);
    hashComponentIdentifier.setComment(comment);
    return hashComponentIdentifier;
  }

  private void assertHashComponentIdentifierData(AuditDTO auditDTO, HashComponentIdentifier hashComponentIdentifier) {
    assertCustomData(auditDTO, "componentHash", hashComponentIdentifier.getHash());
    assertCustomObject(auditDTO, "componentIdentifier", hashComponentIdentifier.getComponentIdentifier());
    assertCustomData(auditDTO, "comment", hashComponentIdentifier.getComment());
  }
}
