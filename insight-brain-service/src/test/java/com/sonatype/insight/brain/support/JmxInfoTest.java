/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.inject.Inject;

import com.sonatype.insight.brain.testing.BrainInjectedTest;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

public class JmxInfoTest
    extends BrainInjectedTest
{
  @Inject
  private JmxInfo jmxInfo;

  @Test
  public void testGetJmxInfo() {
    try {
      System.setProperty("dw.some.passWord", "pass1");
      System.setProperty("dw.some.passPhrase", "pass2");
      System.setProperty("dw.some.other", "other");

      final SortedMap<String, SortedMap<String, Object>> entries = jmxInfo.getJmxInfo();

      assertThat(entries.size()).isGreaterThan(1);
      final SortedMap<String, Object> mapOS = entries.get("java.lang:type=OperatingSystem");
      assertThat(mapOS.get("TotalPhysicalMemorySize")).isNotNull();
      assertThat(mapOS.get("FreePhysicalMemorySize")).isNotNull();
      Map<String, Object> mapRuntime = entries.get("java.lang:type=Runtime");
      Set<Map<String, Object>> systemProperties = (Set<Map<String, Object>>) mapRuntime.get("SystemProperties");
      assertThat(systemProperties)
          .extracting(systemProperty -> systemProperty.get("key") + "=" + systemProperty.get("value"))
          .contains("dw.some.passWord=****", "dw.some.passPhrase=****", "dw.some.other=other");
    }
    finally {
      System.clearProperty("dw.some.passWord");
      System.clearProperty("dw.some.passPhrase");
      System.clearProperty("dw.some.other");
    }
  }

  @Test
  public void testObfuscatePasswords() {
    SortedMap<String, Object> runtime = new TreeMap<>();
    runtime.put("InputArguments",
        Arrays.asList("-Ddw.some.passWord=pass1", "-Ddw.some.passPhrase=pass2", "-Ddw.some.other=other"));
    runtime.put("SystemProperties", new HashSet<>(Arrays.asList(
        new HashMap<>(ImmutableMap.of("key", "dw.some.passWord", "value", "pass1")),
        new HashMap<>(ImmutableMap.of("key", "dw.some.passPhrase", "value", "pass2")),
        new HashMap<>(ImmutableMap.of("key", "dw.some.other", "value", "other"))
    )));
    Map<String, SortedMap<String, Object>> entries = new HashMap<>();
    entries.put("java.lang:type=Runtime", runtime);

    jmxInfo.obfuscatePasswords(entries);

    assertThat((List<String>) runtime.get("InputArguments"))
        .containsExactly("-Ddw.some.passWord=****", "-Ddw.some.passPhrase=****", "-Ddw.some.other=other");
    assertThat((Set<Map<String, Object>>) runtime.get("SystemProperties"))
        .extracting(systemProperty -> systemProperty.get("key") + "=" + systemProperty.get("value"))
        .containsExactlyInAnyOrder("dw.some.passWord=****", "dw.some.passPhrase=****", "dw.some.other=other");
  }
}
