/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.brain.model.SchemaInfo;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SchemaInfoDAOTest
    extends AbstractDbDAOTest
{
  private SchemaInfoDAO dao = new SchemaInfoDAO();

  @Test
  public void testCRUD() throws Exception {
    SchemaInfo schemaInfo = dao.get();
    assertThat(schemaInfo).isNotNull();

    schemaInfo.setDroolsCodeVersion(-13);
    dao.update(schemaInfo);
    schemaInfo = dao.get();
    assertThat(schemaInfo.getDroolsCodeVersion()).isEqualTo(-13);

    SchemaInfo schemaInfoToDelete = schemaInfo;
    assertThatThrownBy(() -> {
      dao.delete(schemaInfoToDelete);
    }).isInstanceOf(UnsupportedOperationException.class);

    assertThatThrownBy(() -> {
      dao.insert(new SchemaInfo());
    }).isInstanceOf(UnsupportedOperationException.class);
  }
}
