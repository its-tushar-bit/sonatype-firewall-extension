/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.brain.model.SchemaInfo;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.fail;

public class SchemaInfoDAOTest
    extends AbstractDbDAOTest
{
  private SchemaInfoDAO dao = new SchemaInfoDAO();

  @Test
  public void testCRUD() throws Exception {
    SchemaInfo schemaInfo = dao.get();
    assertThat(schemaInfo, is(notNullValue()));

    schemaInfo.setDroolsCodeVersion(-13);
    dao.update(schemaInfo);
    schemaInfo = dao.get();
    assertThat(schemaInfo.getDroolsCodeVersion(), is(-13));

    try {
      dao.delete(schemaInfo);
      fail("Expected exception");
    }
    catch (UnsupportedOperationException e) {
      // good boy
    }

    try {
      dao.insert(new SchemaInfo());
      fail("Expected exception");
    }
    catch (UnsupportedOperationException e) {
      // good boy
    }
  }
}
