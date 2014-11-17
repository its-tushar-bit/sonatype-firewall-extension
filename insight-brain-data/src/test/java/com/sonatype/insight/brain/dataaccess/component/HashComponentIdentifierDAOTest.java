/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.component;

import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HashComponentIdentifierDAOTest
    extends AbstractDbDAOTest
{
  @Test
  public void testCRUD() throws Exception {
    HashComponentIdentifierDAO dao = new HashComponentIdentifierDAO();

    String hash = "123456789012345678901";
    assertTrue(hash.length() > 20);
    String truncatedHash = hash.substring(0, 20);
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "e1", "c1");
    Date createTime = new Date();

    // Create
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(hash, componentIdentifier);
    hashComponentIdentifier.setCreateTime(createTime);
    assertNull(hashComponentIdentifier.getId());
    dao.insert(hashComponentIdentifier);
    assertNotNull(hashComponentIdentifier.getId());

    // Read
    hashComponentIdentifier = dao.getById(hashComponentIdentifier.getId());
    assertNotNull(hashComponentIdentifier);
    assertHashComponentIdentifier(truncatedHash, componentIdentifier, createTime, hashComponentIdentifier);
    assertThat(hashComponentIdentifier.getComment(), isEmptyOrNullString());

    // Update
    String comment = "Comment for update";
    hashComponentIdentifier.setComment(comment);
    dao.update(hashComponentIdentifier);
    hashComponentIdentifier = dao.getById(hashComponentIdentifier.getId());
    assertNotNull(hashComponentIdentifier);
    assertHashComponentIdentifier(truncatedHash, componentIdentifier, createTime, hashComponentIdentifier);
    assertThat(hashComponentIdentifier.getComment(), is(comment));

    // Delete
    dao.delete(hashComponentIdentifier);

    hashComponentIdentifier = dao.getById(hashComponentIdentifier.getId());
    assertNull(hashComponentIdentifier);
  }

  @Test
  public void testGetByComponentIdentifier() throws Exception {
    HashComponentIdentifierDAO dao = new HashComponentIdentifierDAO();

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");

    dao.insert(new HashComponentIdentifier("11111111111111111111", componentIdentifier));

    HashComponentIdentifier hashComponentIdentifier = dao.getByComponentIdentifier(componentIdentifier);
    assertNotNull(hashComponentIdentifier);

    dao.delete(hashComponentIdentifier);
  }

  private void assertHashComponentIdentifier(String hash, ComponentIdentifier componentIdentifier, Date createTime,
      HashComponentIdentifier hashComponentIdentifier)
  {
    assertEquals(hash, hashComponentIdentifier.getHash());
    assertEquals(componentIdentifier, hashComponentIdentifier.getComponentIdentifier());
    assertEquals(createTime, hashComponentIdentifier.getCreateTime());
  }

  @Test
  public void testAddDuplicateByHash() throws Exception {
    HashComponentIdentifierDAO dao = new HashComponentIdentifierDAO();

    String hash = "ab1234ab1234ab";
    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2");

    HashComponentIdentifier hashComponentIdentifier1 = new HashComponentIdentifier(hash, componentIdentifier1);
    dao.insert(hashComponentIdentifier1);

    HashComponentIdentifier hashComponentIdentifier2 = new HashComponentIdentifier(hash, componentIdentifier2);
    try {
      dao.insert(hashComponentIdentifier2);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException expected) {
      assertEquals("This component is already mapped to 'g1 : a1 : v1'.",
          expected.getMessage());
    }

    dao.delete(hashComponentIdentifier1);
  }

  @Test
  public void testAddDuplicateByComponentIdentifier() throws Exception {
    HashComponentIdentifierDAO dao = new HashComponentIdentifierDAO();

    String hash = "ab1234ab1234ab";
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1");

    HashComponentIdentifier hashComponentIdentifier1 = new HashComponentIdentifier(hash, componentIdentifier);
    dao.insert(hashComponentIdentifier1);

    HashComponentIdentifier hashComponentIdentifier2 = new HashComponentIdentifier(hash + "1", componentIdentifier);
    try {
      dao.insert(hashComponentIdentifier2);
      fail("Expected BadRequestException");
    }
    catch (BadRequestException expected) {
      assertEquals("Another component is already mapped to 'g1 : a1 : v1'.",
          expected.getMessage());
    }

    dao.delete(hashComponentIdentifier1);
  }
}
