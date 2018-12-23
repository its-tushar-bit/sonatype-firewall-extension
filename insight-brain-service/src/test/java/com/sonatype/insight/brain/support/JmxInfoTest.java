/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.util.SortedMap;

import javax.inject.Inject;

import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JmxInfoTest
    extends InjectedTest
{
  @Inject
  private JmxInfo jmxInfo;

  @Test
  public void testGetJmxInfo() throws Exception {
    final SortedMap<String, Object> entries = jmxInfo.getJmxInfo();
    assertThat(entries.size()).isGreaterThan(1);

    @SuppressWarnings("unchecked")
    final SortedMap<String, Object> mapOS = (SortedMap<String, Object>) entries.get("java.lang:type=OperatingSystem");
    assertThat(mapOS.get("TotalPhysicalMemorySize")).isNotNull();
    assertThat(mapOS.get("FreePhysicalMemorySize")).isNotNull();
  }
}
