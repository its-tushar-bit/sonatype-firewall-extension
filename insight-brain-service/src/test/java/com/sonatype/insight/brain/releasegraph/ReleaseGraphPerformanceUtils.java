/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.releasegraph;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sonatype.insight.brain.model.GAVPopularity;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;

import org.codehaus.plexus.util.FileUtils;

public class ReleaseGraphPerformanceUtils
{
  public static void main(String[] args) throws Exception {
    InsightWork work = createInsightWork();
    try {
      ReleaseGraphPerformance test = new ReleaseGraphPerformance(10, 5, 6, work);
      // Theoretical worst case - ReleaseGraphPerformance test = new ReleaseGraphPerformance( 170, 1, 6, work );
      long start = System.currentTimeMillis();
      List<Map<GAVPopularity, Long>> results = test.begin();
      System.out.println(System.currentTimeMillis() - start);
      doOutput(args.length > 0 ? args[1] : null, results);
    }
    finally {
      FileUtils.deleteDirectory(work.getWorkDir());
    }
  }

  private static void doOutput(String file, List<Map<GAVPopularity, Long>> results) throws IOException {
    Map<GAVPopularity, List<Long>> data = new HashMap<GAVPopularity, List<Long>>();
    for (Map<GAVPopularity, Long> row : results) {
      for (Entry<GAVPopularity, Long> entry : row.entrySet()) {
        List<Long> d = data.get(entry.getKey());
        if (d == null) {
          d = new LinkedList<Long>();
          data.put(entry.getKey(), d);
        }
        d.add(entry.getValue());
      }
    }

    StringBuilder sb = new StringBuilder();
    for (Entry<GAVPopularity, List<Long>> row : data.entrySet()) {
      GAVPopularity pop = row.getKey();
      sb.append(pop.getGroupId()).append(',').append(pop.getArtifactId()).append(',').append(pop.getVersion());
      for (Long result : row.getValue()) {
        sb.append(',').append(result);
      }
      sb.append('\n');
    }
    if (file != null) {
      FileUtils.fileWrite(new File(file), sb.toString());
    }
    else {
      System.out.println(sb.toString());
    }
  }

  private static InsightWork createInsightWork() throws IOException {
    InsightConfig insightConfig = new InsightConfig();
    File workDir = File.createTempFile("releasegraph", "tmp");
    workDir.delete();
    workDir.mkdirs();
    insightConfig.setSonatypeWork(workDir.getAbsolutePath());
    InsightWork work = new InsightWork(insightConfig);
    return work;
  }
}