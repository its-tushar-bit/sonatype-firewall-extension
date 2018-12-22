/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.resultdiff;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.tools.urlrunner.Stats;
import com.sonatype.insight.brain.tools.urlrunner.UrlRunnerCli;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import com.beust.jcommander.ParameterException;
import org.apache.http.message.BasicStatusLine;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.LoggerFactory;

import static org.apache.http.HttpVersion.HTTP_1_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ResultDiffTest
{

  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  private File logStats(String fileName, List<Stats> stats) throws Exception {

    File output = tempDir.newFile(fileName);
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

    FileAppender<ILoggingEvent> file = new FileAppender<>();
    file.setName("FileLogger");
    file.setFile(output.getAbsolutePath());
    file.setContext(context);
    file.setAppend(true);

    // Filter out anything < INFO
    ThresholdFilter infoFilter = new ThresholdFilter();
    infoFilter.setLevel("INFO");
    infoFilter.setContext(context);
    infoFilter.start();
    file.addFilter(infoFilter);

    // Message Encoder
    PatternLayoutEncoder ple = new PatternLayoutEncoder();
    ple.setContext(context);
    ple.setPattern("%m%n");
    ple.start();
    file.setEncoder(ple);

    file.start();

    // Get ROOT logger, and add appender to it
    Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
    root.setLevel(Level.DEBUG);
    root.addAppender(file);

    UrlRunnerCli test = new UrlRunnerCli();

    stats.forEach(s -> test.printStats(s, root));
    file.stop();

    return output;
  }

  private Stats makeStats(String url, long ms) {
    Stats s = new Stats();
    s.setUrl(url);
    s.setResponseTime(ms);
    s.setStatusLine(new BasicStatusLine(HTTP_1_1, 200, "no reason"));
    return s;
  }

  @Test
  public void testCompareResults_TwoFiles() throws Exception {
    List<Stats> file_1_Stats = new ArrayList<>();
    List<Stats> file_2_Stats = new ArrayList<>();

    file_1_Stats.add(makeStats("/xyz/test_01/", 1000L));
    file_2_Stats.add(makeStats("/xyz/test_01/", 4000L));

    file_1_Stats.add(makeStats("/xyz/test_02/", 1000L));
    file_2_Stats.add(makeStats("/xyz/test_02/", 2000L));

    File out1 = logStats("stats_001.log", file_1_Stats);
    File out2 = logStats("stats_002.log", file_2_Stats);

    ResultDiff resultDiff = new ResultDiff();
    List<ResultDiff.DiffData> diffDefault2000 = resultDiff.compareResults(out1, out2);

    assertThat(diffDefault2000).hasSize(1);
    assertThat(diffDefault2000.get(0).min).isEqualTo(1000);
    assertThat(diffDefault2000.get(0).max).isEqualTo(4000);
    assertThat(diffDefault2000.get(0).results).hasSize(2);

    resultDiff.setMinDiff(500);
    List<ResultDiff.DiffData> diff500 = resultDiff.compareResults(out1, out2);

    assertThat(diff500).hasSize(2);
    assertThat(diff500.get(0).min).isEqualTo(1000);
    assertThat(diff500.get(0).max).isEqualTo(4000);
    assertThat(diff500.get(0).results).hasSize(2);
    assertThat(diff500.get(1).min).isEqualTo(1000);
    assertThat(diff500.get(1).max).isEqualTo(2000);
    assertThat(diff500.get(1).results).hasSize(2);
  }

  @Test
  public void testCompareResults_ThreeFiles() throws Exception {
    List<Stats> file_1_Stats = new ArrayList<>();
    List<Stats> file_2_Stats = new ArrayList<>();
    List<Stats> file_3_Stats = new ArrayList<>();

    file_1_Stats.add(makeStats("/xyz/test_01/", 1000L));
    file_2_Stats.add(makeStats("/xyz/test_01/", 4000L));
    file_3_Stats.add(makeStats("/xyz/test_01/", 7000L));

    file_1_Stats.add(makeStats("/xyz/test_02/", 1000L));
    file_2_Stats.add(makeStats("/xyz/test_02/", 2000L));
    file_3_Stats.add(makeStats("/xyz/test_02/", 500L));

    File out1 = logStats("stats_001.log", file_1_Stats);
    File out2 = logStats("stats_002.log", file_2_Stats);
    File out3 = logStats("stats_003.log", file_3_Stats);

    ResultDiff resultDiff = new ResultDiff();
    List<ResultDiff.DiffData> diffDefault2000 = resultDiff.compareResults(out1, out2, out3);

    assertThat(diffDefault2000).hasSize(1);
    assertThat(diffDefault2000.get(0).min).isEqualTo(1000);
    assertThat(diffDefault2000.get(0).max).isEqualTo(7000);
    assertThat(diffDefault2000.get(0).results).hasSize(3);

    resultDiff.setMinDiff(500);
    List<ResultDiff.DiffData> diff500 = resultDiff.compareResults(out1, out2, out3);

    assertThat(diff500).hasSize(2);
    assertThat(diff500.get(0).min).isEqualTo(1000);
    assertThat(diff500.get(0).max).isEqualTo(7000);
    assertThat(diff500.get(0).results).hasSize(3);
    assertThat(diff500.get(1).min).isEqualTo(500);
    assertThat(diff500.get(1).max).isEqualTo(2000);
    assertThat(diff500.get(1).results).hasSize(3);
  }

  @Test
  public void testValidateFiles_MinimumFiles() throws Exception {
    List<File> list_0 = new ArrayList<>();

    List<File> list_1 = new ArrayList<>();
    list_1.add(tempDir.newFile());

    List<File> list_2 = new ArrayList<>();
    list_2.add(tempDir.newFile());
    list_2.add(tempDir.newFile());

    assertThatThrownBy(() -> {
      ResultDiff.validateFiles(list_0);
    }).isInstanceOf(ParameterException.class).hasMessage(ResultDiff.ERROR_MIN_FILES);

    assertThatThrownBy(() -> {
      ResultDiff.validateFiles(list_1);
    }).isInstanceOf(ParameterException.class).hasMessage(ResultDiff.ERROR_MIN_FILES);

    ResultDiff.validateFiles(list_2);
  }

  @Test
  public void testValidateFiles_InvalidFile() throws Exception {
    List<File> list_2 = new ArrayList<>();

    File valid = tempDir.newFile();
    File invalid = new File(valid.getAbsolutePath() + "-invalid");
    list_2.add(valid);
    list_2.add(invalid);

    assertThat(valid).exists();
    assertThat(invalid).doesNotExist();

    assertThatThrownBy(() -> {
      ResultDiff.validateFiles(list_2);
    }).isInstanceOf(ParameterException.class).hasMessageStartingWith(ResultDiff.ERROR_INVALID_FILE_PREFIX);
  }

}
