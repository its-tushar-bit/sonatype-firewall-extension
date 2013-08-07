package com.sonatype.insight.brain.releasegraph;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
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
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.GAVPopularity;
import com.sonatype.insight.brain.model.ReportPopularity;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;

public class ReleaseGraphPerformance
{
  private ThreadPoolExecutor pool;

  private List<UserCallable> callables;

  private File srcFile = new File("src/test/resources/report.popularity.json.zip").getAbsoluteFile();

  private Application testApplication;

  private ReleaseGraphResource reportResource;

  private LoadingCache<ReleaseGraphKey, byte[]> cache = CacheBuilder.newBuilder().maximumSize(1000)
      .build(new ReleaseGraphCacheLoader());

  private ReleaseGraphPerformance(int threads, InsightWork work) throws Exception {
    callables = new LinkedList<UserCallable>();
    pool = new ThreadPoolExecutor(threads, threads, 1, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(threads));

    reportResource = new ReleaseGraphResource(cache);

    Field field = ReleaseGraphResource.class.getDeclaredField("work");
    field.setAccessible(true);
    field.set(reportResource, work);

    // trigger db
    testApplication = new Application();
    testApplication.setPublicId("ReleaseGraphPerformance_AppId");
    new ApplicationDAO().insert(testApplication);
  }

  ReleaseGraphPerformance(int reports, int users, InsightWork work) throws Exception {
    this(users * reports, work);

    List<GAVPopularity> gavs = getGAVs();
    for (int r = 0; r < reports; r++) {
      String scanId = createReport(work);
      for (int u = 0; u < users; u++) {
        callables.add(createUser(scanId, gavs));
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
    this(connectionsPerUser * usersPerReport * reports, work);

    List<GAVPopularity> gavs = getGAVs();
    for (int r = 0; r < reports; r++) {
      String scanId = createReport(work);
      for (int u = 0; u < usersPerReport; u++) {
        @SuppressWarnings("unchecked")
        List<GAVPopularity>[] connections = new LinkedList[connectionsPerUser];
        int c = 0;
        for (GAVPopularity gav : gavs) {
          if (connections[c] == null) {
            connections[c] = new LinkedList<GAVPopularity>();
          }
          connections[c].add(gav);
          c = ++c % connectionsPerUser;
        }
        callables.add(createUser(scanId, gavs));
      }
    }
  }

  ReleaseGraphPerformance(int users, boolean preload, InsightWork work) throws Exception {
    this(users, work);

    List<GAVPopularity> gavs = getGAVs();
    List<String> scanIds = new LinkedList<String>();
    int u = (int) Math.ceil(((double) users) / gavs.size());
    for (int i = 0; i < u; i++) {
      String scanId = createReport(work);
      scanIds.add(scanId);
      for (GAVPopularity gav : gavs) {
        callables.add(createUser(scanId, Collections.singletonList(gav)));
        --users;
        if (users == 0) {
          break;
        }
      }
      if (preload) {
        try {
          reportResource.getImage("ReleaseGraphPerformance_AppId", scanId, "fake", "fake", "fake");
        }
        catch (Exception e) {

        }
      }
    }
  }

  void clearCache() {
    cache.invalidateAll();
  }

  List<Map<GAVPopularity, Long>> begin() throws Exception {
    try {
      // pool.prestartAllCoreThreads();
      List<Map<GAVPopularity, Long>> results = new LinkedList<Map<GAVPopularity, Long>>();
      List<Future<Map<GAVPopularity, Long>>> futures = pool.invokeAll(callables);
      for (Future<Map<GAVPopularity, Long>> f : futures) {
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
      int reports = Integer.valueOf(args[0]);
      int usersPerReport = Integer.valueOf(args[1]);

      // ReleaseGraphPerformance test = new ReleaseGraphPerformance( reports * usersPerReport, true, work );
      ReleaseGraphPerformance test = new ReleaseGraphPerformance(reports, usersPerReport, work);
      long start = System.currentTimeMillis();
      System.out.println("Starting");
      List<Map<GAVPopularity, Long>> results = test.begin();
      System.out.println(System.currentTimeMillis() - start);
      doOutput(args.length > 2 ? args[2] : null, results);
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

  private UserCallable createUser(String scanId, List<GAVPopularity> gavs) {
    return new UserCallable(scanId, reportResource, gavs);
  }

  private List<GAVPopularity> getGAVs() throws ZipException, IOException {
    ZipFile zf = null;
    InputStream in = null;
    try {
      zf = new ZipFile(srcFile);
      in = zf.getInputStream(zf.getEntry("popularity.json"));
      return JsonUtils.parse(IOUtil.toByteArray(in), ReportPopularity.class).getPopularity();
    }
    finally {
      IOUtil.close(in);
      zf.close();
    }
  }

  private static class UserCallable
      implements Callable<Map<GAVPopularity, Long>>
  {
    private String scanId;

    private ReleaseGraphResource resource;

    private List<GAVPopularity> gavs;

    private Map<GAVPopularity, Long> results = new HashMap<GAVPopularity, Long>();

    public UserCallable(String scanId, ReleaseGraphResource resource, List<GAVPopularity> gavs) {
      this.scanId = scanId;
      this.resource = resource;
      this.gavs = gavs;
    }

    @Override
    public Map<GAVPopularity, Long> call() throws Exception {
      for (GAVPopularity gav : gavs) {
        long start = System.currentTimeMillis();
        resource.getImage("ReleaseGraphPerformance_AppId", scanId, gav.getGroupId(), gav.getArtifactId(),
            gav.getVersion());
        results.put(gav, System.currentTimeMillis() - start);
      }
      return results;
    }
  }
}