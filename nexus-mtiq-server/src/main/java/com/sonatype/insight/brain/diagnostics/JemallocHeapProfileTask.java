/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.diagnostics;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import com.sonatype.jemalloc.JemallocProfileDumper;

import io.dropwizard.servlets.tasks.Task;

/**
 * Dump a heap profile using jemalloc. The profile is saved on the server and the filename is included in the response
 */
@Named
public class JemallocHeapProfileTask
    extends Task
{
  public static final String PATH = "jemallocHeapProfile";

  private final Path jemallocProfileDir;

  @Inject
  public JemallocHeapProfileTask(MultiTenantInsightConfig config) {
    super(PATH);
    jemallocProfileDir = config.getJemallocProfileDir();
  }

  @Override
  public void execute(final Map<String, List<String>> parameters, final PrintWriter output) {
    var currentISODate = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
    var basename = String.format("jemalloc-%s.heap", currentISODate);
    Path profilePath = jemallocProfileDir.resolve(basename);
    boolean profilingAlreadyActive;

    try {
      profilingAlreadyActive = JemallocProfileDumper.dumpProfile(profilePath.toString());
    }
    catch (UnsatisfiedLinkError e) {
      throw new IllegalStateException("jemalloc-jni library not loaded, unable to create heap profile");
    }

    output.println("Created jemalloc heap profile: " + basename);
    if (!profilingAlreadyActive) {
      output.println("""

          Jemalloc profiling was not previously active. It has now been activated just prior to recording this heap
          profile and has been left active. The recorded profile is not likely to be useful on its own but can be used
          as a diff baseline for comparison against future profiles of this same JVM instance.
          """);
    }

    output.println("""

        To generate an SVG showing the difference between two profiles within an MTIQ container, use the following
        command with the appropriate file names:
        `jeprof --svg --base=profile1.heap /opt/sonatype/nexus-iq-server/bin/nexus-mtiq-server profile2.heap > \
        heap-diff.svg`

        The following packages must be installed with apt in order to run the above command:
        * libjemalloc-dev
        * binutils
        * graphviz
        """);
  }
}
