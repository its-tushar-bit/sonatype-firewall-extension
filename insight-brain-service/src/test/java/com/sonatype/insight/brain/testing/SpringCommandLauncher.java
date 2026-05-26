/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing;

import com.sonatype.insight.brain.spring.InsightBrainCommandDispatcher;
import com.sonatype.insight.brain.spring.InsightBrainSpringApplication;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class SpringCommandLauncher
{
  private SpringCommandLauncher() {
    // utility class
  }

  public static void launch(String commandName, String configPath, String... commandArgs) throws Exception {
    List<String> arguments = new ArrayList<>();
    arguments.add(commandName);
    if (configPath != null) {
      arguments.add(configPath);
    }
    arguments.addAll(Arrays.asList(commandArgs));

    new InsightBrainCommandDispatcher().dispatch(InsightBrainSpringApplication.class,
        arguments.toArray(String[]::new));
  }
}
