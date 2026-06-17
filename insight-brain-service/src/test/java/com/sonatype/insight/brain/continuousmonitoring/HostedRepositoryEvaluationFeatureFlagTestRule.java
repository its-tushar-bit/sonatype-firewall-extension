/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.continuousmonitoring;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

/**
 * In-memory shim for {@link SystemConfigurationPropertyFeature}'s static DAO so unit tests can
 * call {@code feature.setEnabled(boolean)} / {@code feature.isEnabled()} without standing up a
 * database. Wires a Mockito-backed {@link SystemConfigurationPropertyDAO} via reflection into
 * the static {@code systemConfigurationPropertyDAO} field set normally by Guice static injection
 * ({@code SystemConfigurationPropertyFeatureTestHelper} pattern). Restore by calling {@link
 * #uninstall} from {@code @AfterClass}.
 */
final class HostedRepositoryEvaluationFeatureFlagTestRule
{
  private HostedRepositoryEvaluationFeatureFlagTestRule() {
  }

  private static SystemConfigurationPropertyDAO previousDao;

  static void install() {
    Map<String, SystemConfigurationProperty> store = new ConcurrentHashMap<>();
    SystemConfigurationPropertyDAO mockDao = Mockito.mock(SystemConfigurationPropertyDAO.class);
    TransactionContext tx = Mockito.mock(TransactionContext.class);
    Mockito.when(mockDao.createTransactionContext()).thenReturn(tx);
    Mockito.when(mockDao.getByName(Mockito.any(TransactionContext.class), Mockito.anyString()))
        .thenAnswer((InvocationOnMock inv) -> store.get(inv.<String>getArgument(1)));
    // Also stub the no-tx overload — SystemConfigurationPropertyFeature.isEnabled() (line 582)
    // calls getByName(String) directly, distinct from the (tx, String) overload used by
    // setEnabled(boolean). Without this stub, isEnabled() always returns
    // enabledWhenAbsent because the default Mockito null leaks through.
    Mockito.when(mockDao.getByName(Mockito.anyString()))
        .thenAnswer((InvocationOnMock inv) -> store.get(inv.<String>getArgument(0)));
    Mockito.doAnswer((InvocationOnMock inv) -> {
      String name = inv.getArgument(1);
      String value = inv.getArgument(2);
      if (value == null) {
        store.remove(name);
      }
      else {
        SystemConfigurationProperty prop = new SystemConfigurationProperty();
        prop.setValue(value);
        store.put(name, prop);
      }
      return null;
    })
        .when(mockDao)
        .set(Mockito.any(TransactionContext.class), Mockito.anyString(),
            Mockito.nullable(String.class));

    previousDao = swapDao(mockDao);
  }

  static void uninstall() {
    swapDao(previousDao);
    previousDao = null;
  }

  private static SystemConfigurationPropertyDAO swapDao(final SystemConfigurationPropertyDAO replacement) {
    try {
      Field field = SystemConfigurationPropertyFeature.class.getDeclaredField("systemConfigurationPropertyDAO");
      field.setAccessible(true);
      Object prior = field.get(null);
      field.set(null, replacement);
      return (SystemConfigurationPropertyDAO) prior;
    }
    catch (ReflectiveOperationException e) {
      throw new IllegalStateException(
          "Failed to swap SystemConfigurationPropertyFeature.systemConfigurationPropertyDAO",
          e);
    }
  }
}
