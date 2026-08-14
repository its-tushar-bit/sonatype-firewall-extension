/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FirewallIgnorePatternsTest
{
  @Test
  public void testGetSet() {
    FirewallIgnorePatterns firewallIgnorePatterns = new FirewallIgnorePatterns();
    assertThat(firewallIgnorePatterns.getFirewallIgnorePatterns()).isNull();

    com.sonatype.clm.dto.model.component.FirewallIgnorePatterns ignorePatterns =
        new com.sonatype.clm.dto.model.component.FirewallIgnorePatterns();
    ignorePatterns.regexpsByRepositoryFormat.put("format1", Arrays.asList("a", "b"));
    ignorePatterns.regexpsByRepositoryFormat.put("format2", Collections.singletonList("c"));

    firewallIgnorePatterns.setFirewallIgnorePatterns(ignorePatterns);

    assertThat(firewallIgnorePatterns.getFirewallIgnorePatterns()).usingRecursiveComparison().isEqualTo(ignorePatterns);

    firewallIgnorePatterns.setFirewallIgnorePatterns(null);

    assertThat(firewallIgnorePatterns.getFirewallIgnorePatterns()).isNull();
  }
}
