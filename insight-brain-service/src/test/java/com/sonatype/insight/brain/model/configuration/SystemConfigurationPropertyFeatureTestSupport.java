/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.dataaccess.TransactionContext;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

/**
 * In-memory shim for {@link SystemConfigurationPropertyFeature}'s static DAO. Install in
 * {@code @Before}, uninstall in {@code @After}. Swaps a single static DAO, so it assumes
 * single-threaded, non-parallel test execution and one live install at a time.
 */
public final class SystemConfigurationPropertyFeatureTestSupport
{
  private SystemConfigurationPropertyFeatureTestSupport() {
  }

  private static SystemConfigurationPropertyDAO previousDao;

  private static boolean installed;

  public static void install() {
    if (installed) {
      throw new IllegalStateException("SystemConfigurationPropertyFeatureTestSupport already installed; "
          + "call uninstall() before installing again (not safe for parallel/nested use)");
    }
    ConcurrentMap<String, SystemConfigurationProperty> store = new ConcurrentHashMap<>();
    SystemConfigurationPropertyDAO mockDao = Mockito.mock(SystemConfigurationPropertyDAO.class);
    TransactionContext tx = Mockito.mock(TransactionContext.class);
    Mockito.when(mockDao.createTransactionContext()).thenReturn(tx);
    Mockito.when(mockDao.getByName(Mockito.any(TransactionContext.class), Mockito.anyString()))
        .thenAnswer((InvocationOnMock inv) -> store.get(inv.<String>getArgument(1)));
    // isEnabled() calls the getByName(String) overload, distinct from getByName(tx, String); both
    // must be stubbed or isEnabled() always returns enabledWhenAbsent via Mockito's default null.
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
        .set(Mockito.any(TransactionContext.class), Mockito.anyString(), Mockito.nullable(String.class));
    previousDao = swapDao(mockDao);
    installed = true;
  }

  public static void uninstall() {
    swapDao(previousDao);
    previousDao = null;
    installed = false;
  }

  private static SystemConfigurationPropertyDAO swapDao(final SystemConfigurationPropertyDAO replacement) {
    // Install via the sanctioned public injection point (also used by prod wiring); capturing the
    // prior DAO for restore still needs a read, and there is no public getter, so the read reflects.
    SystemConfigurationPropertyDAO prior = currentDao();
    SystemConfigurationPropertyFeature.injectDependencies(replacement);
    return prior;
  }

  private static SystemConfigurationPropertyDAO currentDao() {
    try {
      Field field = SystemConfigurationPropertyFeature.class.getDeclaredField("systemConfigurationPropertyDAO");
      field.setAccessible(true);
      return (SystemConfigurationPropertyDAO) field.get(null);
    }
    catch (ReflectiveOperationException e) {
      throw new IllegalStateException(
          "Failed to read SystemConfigurationPropertyFeature.systemConfigurationPropertyDAO", e);
    }
  }
}
