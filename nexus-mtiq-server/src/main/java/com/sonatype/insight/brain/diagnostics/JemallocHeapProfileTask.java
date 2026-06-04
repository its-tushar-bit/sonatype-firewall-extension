/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.diagnostics;

import com.sonatype.insight.brain.service.AdminTask;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class JemallocHeapProfileTask
    extends AdminTask
{
  private static final Logger log = LoggerFactory.getLogger(JemallocHeapProfileTask.class);

  public static final String PATH = "jemallocHeapProfile";

  private final Path jemallocProfileDir;

  @Inject
  public JemallocHeapProfileTask(MultiTenantInsightConfig config) {
    super(PATH);
    jemallocProfileDir = config.getJemallocProfileDir();
  }

  @Override
  public void execute() {
    var currentISODate = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
    var basename = String.format("jemalloc-%s.heap", currentISODate);
    Path profilePath = jemallocProfileDir.resolve(basename);
    boolean profilingAlreadyActive;

    try {
      profilingAlreadyActive = JemallocProfileDumper.dumpProfile(profilePath.toString());
    }
    catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
      throw new IllegalStateException(
          "jemalloc mallctl symbol not available, unable to create heap profile. Is jemalloc LD_PRELOAD'd?", e);
    }

    log.info("Created jemalloc heap profile: {}", basename);
    if (!profilingAlreadyActive) {
      log.info("Jemalloc profiling was not previously active. It has now been activated just prior to recording " +
          "this heap profile and has been left active. The recorded profile is not likely to be useful on its own " +
          "but can be used as a diff baseline for comparison against future profiles of this same JVM instance.");
    }

    log.info("To generate an SVG showing the difference between two profiles within an MTIQ container, use: " +
        "jeprof --svg --base=profile1.heap /opt/sonatype/nexus-iq-server/bin/nexus-mtiq-server profile2.heap > " +
        "heap-diff.svg. Required packages: libjemalloc-dev, binutils, graphviz");
  }
}
