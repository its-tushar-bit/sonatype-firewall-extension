/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.net.InetAddress;

import com.sonatype.insight.db.H2DatabaseEngine;
import com.sonatype.insight.db.PostgresDatabaseEngine;

import org.junit.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class DatabaseUtilTest
{
  @Test
  public void testGetDatabaseEngine_H2() {
    assertThat(DatabaseUtil.getDatabaseEngineFromName("h2")).isEqualTo(H2DatabaseEngine.INSTANCE);
  }

  @Test
  public void testGetDatabaseEngine_PostgreSQL() {
    assertThat(DatabaseUtil.getDatabaseEngineFromName("PostgreSQL")).isEqualTo(PostgresDatabaseEngine.INSTANCE);
  }

  @Test
  public void testGenerateApplicationName() {
    try (MockedStatic<InetAddress> inetAddressMockedStatic = mockStatic(InetAddress.class)) {
      InetAddress mockAddress = mock(InetAddress.class);
      when(mockAddress.getHostName()).thenReturn("somehost");
      inetAddressMockedStatic.when(InetAddress::getLocalHost).thenReturn(mockAddress);

      String name = DatabaseUtil.generateApplicationNameWithHost("bar");
      assertThat(name).containsPattern("bar-somehost-[a-zA-Z0-9]{5}");
    }
  }
}
