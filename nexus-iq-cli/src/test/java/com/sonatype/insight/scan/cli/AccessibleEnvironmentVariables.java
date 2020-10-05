/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

/**
 * The JUnit rule {@link EnvironmentVariables} does not provide access to the map of variables and we need them to pass
 * into the native image config gen tests. So this is simply an extension of {@link EnvironmentVariables} which provides
 * access to the map of variables
 */
public class AccessibleEnvironmentVariables
    extends EnvironmentVariables
{
  private final Map<String, String> copyOfEnvironmentVariables = new HashMap<>();

  @Override
  public EnvironmentVariables set(final String name, final String value) {
    copyOfEnvironmentVariables.put(name, value);
    return super.set(name, value);
  }

  public Map<String, String> get() {
    return ImmutableMap.copyOf(copyOfEnvironmentVariables);
  }
}
