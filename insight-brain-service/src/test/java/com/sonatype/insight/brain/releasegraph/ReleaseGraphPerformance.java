/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.releasegraph;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipFile;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ComponentPopularity;
import com.sonatype.insight.brain.model.ReportPopularity;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReleaseGraphPerformance
{
  private final ThreadPoolExecutor pool;

  private final List<UserCallable> callables;

  private final File srcFile = new File("src/test/resources/report.popularity.json.zip").getAbsoluteFile();

  private final Application testApplication;

  private final ReleaseGraphResource reportResource;

  private final LoadingCache<ReleaseGraphKey, byte[]> cache;

  private ReleaseGraphPerformance(int threads) {
    callables = new LinkedList<>();
    pool = new ThreadPoolExecutor(threads, threads, 1, TimeUnit.SECONDS, new ArrayBlockingQueue<>(threads));
    cache = CacheBuilder.newBuilder().maximumSize(1000)
        .build(new ReleaseGraphCacheLoader(new ReportItemCacheLoader(null, new ApplicationDAO())));
    ReleaseGraphCacheProvider mockReleaseGraphCacheProvider = mock(ReleaseGraphCacheProvider.class);
    when(mockReleaseGraphCacheProvider.get()).thenReturn(cache);
    reportResource = new ReleaseGraphResource(new ReleaseGraphService(mockReleaseGraphCacheProvider));

    // trigger db
    testApplication = new Application();
    testApplication.setPublicId("ReleaseGraphPerformance_AppId");
    testApplication.setName("perf-test");
    new ApplicationDAO().insert(testApplication);
  }

  ReleaseGraphPerformance(int reports, int users, InsightWork work) throws Exception {
    this(users * reports);

    List<ComponentPopularity> components = getComponents();
    for (int r = 0; r < reports; r++) {
      String scanId = createReport(work);
      for (int u = 0; u < users; u++) {
        callables.add(createUser(scanId, components));
      }
    }
  }

  /**
   * Simulates browser test which
   * 
   * @param reports the number of reports
   * @param usersPerReport the number of users per report
   * @param connectionsPerUser the number of connections each user uses (FF uses 6)
   * @param work
   * @throws Exception
   */
  ReleaseGraphPerformance(int reports, int usersPerReport, int connectionsPerUser, InsightWork work) throws Exception {
    this(connectionsPerUser * usersPerReport * reports);

    List<ComponentPopularity> components = getComponents();
    for (int r = 0; r < reports; r++) {
      String scanId = createReport(work);
      for (int u = 0; u < usersPerReport; u++) {
        @SuppressWarnings("unchecked")
        List<ComponentPopularity>[] connections = new LinkedList[connectionsPerUser];
        int c = 0;
        for (ComponentPopularity component : components) {
          if (connections[c] == null) {
            connections[c] = new LinkedList<>();
          }
          connections[c].add(component);
          c = ++c % connectionsPerUser;
        }
        callables.add(createUser(scanId, components));
      }
    }
  }

  ReleaseGraphPerformance(int users, boolean preload, InsightWork work) throws Exception {
    this(users);

    List<ComponentPopularity> components = getComponents();
    List<String> scanIds = new LinkedList<>();
    int u = (int) Math.ceil(((double) users) / components.size());
    for (int i = 0; i < u; i++) {
      String scanId = createReport(work);
      scanIds.add(scanId);
      for (ComponentPopularity component : components) {
        callables.add(createUser(scanId, Collections.singletonList(component)));
        --users;
        if (users == 0) {
          break;
        }
      }
      if (preload) {
        try {
          reportResource.getImage("ReleaseGraphPerformance_AppId", scanId, "fake", "fake", "fake", null);
        }
        catch (Exception e) {
          // ignored
        }
      }
    }
  }

  void clearCache() {
    cache.invalidateAll();
  }

  List<Map<ComponentPopularity, Long>> begin() throws Exception {
    try {
      // pool.prestartAllCoreThreads();
      List<Map<ComponentPopularity, Long>> results = new LinkedList<>();
      List<Future<Map<ComponentPopularity, Long>>> futures = pool.invokeAll(callables);
      for (Future<Map<ComponentPopularity, Long>> f : futures) {
        results.add(f.get());
      }
      pool.shutdown();

      return results;
    }
    finally {
      pool.shutdownNow();
    }
  }

  public static void main(String[] args) throws Exception {
    InsightWork work = createInsightWork();
    try {
      int reports = Integer.parseInt(args[0]);
      int usersPerReport = Integer.parseInt(args[1]);

      // ReleaseGraphPerformance test = new ReleaseGraphPerformance( reports * usersPerReport, true, work );
      ReleaseGraphPerformance test = new ReleaseGraphPerformance(reports, usersPerReport, work);
      long start = System.currentTimeMillis();
      System.out.println("Starting");
      List<Map<ComponentPopularity, Long>> results = test.begin();
      System.out.println(System.currentTimeMillis() - start);
      doOutput(args.length > 2 ? args[2] : null, results);
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
      FileUtils.fileWrite(new File(file), sb.toString());
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

  private String createReport(InsightWork work) throws IOException {
    String scanId = UUID.randomUUID().toString().replace("-", "");
    // create report structure
    File reportDir = new File(work.getWorkDir(), "report/" + testApplication.getId() + "/" + scanId);
    if (!reportDir.mkdirs()) {
      throw new IllegalStateException("Failed to create Report directory");
    }
    // copy zip
    FileUtils.copyFile(srcFile, new File(reportDir, "report.zip"));
    return scanId;
  }

  private UserCallable createUser(String scanId, List<ComponentPopularity> components) {
    return new UserCallable(scanId, reportResource, components);
  }

  private List<ComponentPopularity> getComponents() throws IOException {
    try (ZipFile zf = new ZipFile(srcFile); InputStream in = zf.getInputStream(zf.getEntry("popularity.json"))) {
      return JsonUtils.parse(IOUtil.toByteArray(in), ReportPopularity.class).getPopularity();
    }
  }

  private static class UserCallable
      implements Callable<Map<ComponentPopularity, Long>>
  {
    private final String scanId;

    private final ReleaseGraphResource resource;

    private final List<ComponentPopularity> components;

    private final Map<ComponentPopularity, Long> results = new HashMap<>();

    public UserCallable(String scanId, ReleaseGraphResource resource, List<ComponentPopularity> components) {
      this.scanId = scanId;
      this.resource = resource;
      this.components = components;
    }

    @Override
    public Map<ComponentPopularity, Long> call() {
      for (ComponentPopularity component : components) {
        if (ComponentIdentifier.FORMAT_MAVEN.equals(component.getComponentIdentifier().getFormat())) {
          long start = System.currentTimeMillis();
          resource.getImage("ReleaseGraphPerformance_AppId", scanId, null, null, null,
              component.getComponentIdentifier());
          results.put(component, System.currentTimeMillis() - start);
        }
      }
      return results;
    }
  }
}
