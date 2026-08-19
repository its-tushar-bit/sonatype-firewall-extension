/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.releasegraph;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.model.ComponentPopularity;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;

import org.apache.commons.io.FileUtils;

public class ReleaseGraphPerformanceUtils
{
  public static void main(String[] args) throws Exception {
    InsightWork work = createInsightWork();
    try {
      ReleaseGraphPerformance test = new ReleaseGraphPerformance(10, 5, 6, work);
      // Theoretical worst case - ReleaseGraphPerformance test = new ReleaseGraphPerformance( 170, 1, 6, work );
      long start = System.currentTimeMillis();
      List<Map<ComponentPopularity, Long>> results = test.begin();
      System.out.println(System.currentTimeMillis() - start);
      doOutput(args.length > 0 ? args[1] : null, results);
    }
    finally {
      new FileCleaner().delete(work.getWorkDir());
    }
  }

  private static void doOutput(String file, List<Map<ComponentPopularity, Long>> results) throws IOException {
    Map<ComponentPopularity, List<Long>> data = new HashMap<>();
    for (Map<ComponentPopularity, Long> row : results) {
      for (Entry<ComponentPopularity, Long> entry : row.entrySet()) {
        List<Long> d = data.computeIfAbsent(entry.getKey(), k -> new LinkedList<>());
        d.add(entry.getValue());
      }
    }

    StringBuilder sb = new StringBuilder();
    for (Entry<ComponentPopularity, List<Long>> row : data.entrySet()) {
      ComponentPopularity pop = row.getKey();
      sb.append(pop.getComponentIdentifier());
      for (Long result : row.getValue()) {
        sb.append(',').append(result);
      }
      sb.append('\n');
    }
    if (file != null) {
      FileUtils.writeStringToFile(new File(file), sb.toString(), StandardCharsets.UTF_8);
    }
    else {
      System.out.println(sb);
    }
  }

  private static InsightWork createInsightWork() throws IOException {
    InsightConfig insightConfig = new InsightConfig();
    File workDir = Files.createTempDirectory("releasegraph").toFile();
    insightConfig.setSonatypeWork(workDir.getAbsolutePath());
    InsightWork work = new InsightWork(insightConfig);
    return work;
  }
}
