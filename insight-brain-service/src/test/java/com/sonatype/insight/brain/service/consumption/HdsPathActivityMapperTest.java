/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.model.consumption.ActivityType;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Enclosed.class)
public class HdsPathActivityMapperTest
{
  @RunWith(Parameterized.class)
  public static class AllPathMappingsTest
  {
    @Parameters(name = "{0} -> {1}")
    public static Collection<Object[]> pathMappings() {
      return HdsPathActivityMapper.getPathMappings()
          .entrySet()
          .stream()
          .map(entry -> new Object[]{entry.getKey(), entry.getValue()})
          .collect(Collectors.toList());
    }

    @Parameter(0)
    public String path;

    @Parameter(1)
    public ActivityType expected;

    @Test
    public void resolve_returnsExpectedActivityType() {
      assertThat(HdsPathActivityMapper.resolve(path)).isEqualTo(expected);
    }
  }

  public static class BoundaryTests
  {
    @Test
    public void resolve_pathWithPrefix_resolvesCorrectly() {
      // Paths may have tenant prefix or other prefixes
      ActivityType result = HdsPathActivityMapper.resolve("/api/v1/rest/component/details/evaluation");
      assertThat(result).isEqualTo(ActivityType.COMPONENT_DETAILS);
    }

    @Test
    public void resolve_firewallPath_returnsNull() {
      ActivityType result = HdsPathActivityMapper.resolve("rest/component/details/firewall");
      assertThat(result).isNull();
    }

    @Test
    public void resolve_unknownPath_returnsNull() {
      ActivityType result = HdsPathActivityMapper.resolve("rest/unknown/endpoint");
      assertThat(result).isNull();
    }

    @Test
    public void resolve_nullPath_returnsNull() {
      ActivityType result = HdsPathActivityMapper.resolve(null);
      assertThat(result).isNull();
    }

    @Test
    public void pathMappings_haveNoSubstringCollisions() {
      Map<String, ActivityType> mappings = HdsPathActivityMapper.getPathMappings();
      for (String a : mappings.keySet()) {
        for (String b : mappings.keySet()) {
          if (!a.equals(b)) {
            assertThat(a)
                .as("Substring collision in PATH_MAPPINGS: key '%s' is contained in key '%s'."
                    + " Because resolve() iterates in insertion order, iteration order would silently"
                    + " pick one activity type over the other depending on map ordering. Either rename"
                    + " the keys or document the priority explicitly.", b, a)
                .doesNotContain(b);
          }
        }
      }
    }
  }
}
