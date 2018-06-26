/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.resultdiff;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.validators.PositiveInteger;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultDiff
{
  private static final Logger log = LoggerFactory.getLogger(ResultDiff.class);

  @Parameter(description = "<file 1> <file 2> ... [file n]")
  private List<File> files = new ArrayList<>();

  @Parameter(names = "-minDiff", validateWith = PositiveInteger.class, description = "only report result with a minimum difference greater than this number of milliseconds")
  private Integer minDiff = 2000;

  private static class Result
  {
    String source;

    String url;

    String payload;

    Long ms;

    String httpCode;

    Long size;

    String md5;

    String record;

    Integer over;
  }

  static class DiffData
  {
    long min;

    long max;

    String url;

    List<Result> results = new ArrayList<>();
  }

  public static void main(String[] args) {
    try {
      ResultDiff rd = new ResultDiff();
      JCommander.newBuilder() //
          .addObject(rd) //
          .build() //
          .parse(args);
      rd.run();

    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
      printUsage();
      System.exit(1);
    }
  }

  private static void printUsage() {
    JCommander.newBuilder() //
        .addObject(new ResultDiff()) //
        .programName("java -jar nexus-iq-tools.jar resultdiff") //
        .build() //
        .usage();
  }

  private Map<String, List<Result>> loadResults(List<File> files) throws Exception {
    return files.stream().collect(Collectors.toMap(File::getName, this::parseResults));
  }

  private void run() throws Exception {
    printDiffs(compareResults(loadResults(files)));
  }

  private List<Result> parseResults(File resultFile) {
    try {
      String source = resultFile.getName();
      List<Result> results = new ArrayList<>();
      List<String> current = new ArrayList<>();
      try (BufferedReader br = new BufferedReader(new FileReader(resultFile))) {
        List<String> allLines = br.lines().collect(Collectors.toList());
        for (String line : allLines) {
          if (line.startsWith("-----")) {
            if (!current.isEmpty()) {
              results.add(makeResult(current, source));
              current.clear();
            }
          }
          else {
            current.add(line);
          }
        }

        if (!current.isEmpty()) {
          results.add(makeResult(current, source));
        }
      }
      return results;
    }
    catch (IOException ioex) {
      throw new UncheckedIOException(ioex);
    }
  }

  private Result makeResult(List<String> lines, String source) {
    Result result = new Result();
    result.source = source;
    for (String line : lines) {
      String key = line.substring(0, line.indexOf(":")).trim().toLowerCase(Locale.ENGLISH);
      String value = line.substring(line.indexOf(":") + 1).trim();

      if (key.equals("url")) {
        result.url = value;
      }
      else if (key.endsWith("payload")) {
        result.payload = value;
      }
      else if (key.endsWith("status")) {
        result.httpCode = value;
      }
      else if (key.endsWith("time")) {
        result.ms = Long.valueOf(value);
      }
      else if (key.endsWith("size")) {
        result.size = Long.valueOf(value);
      }
      else if (key.startsWith("md5")) {
        result.md5 = value;
      }
    }
    return result;
  }

  @VisibleForTesting
  void setMinDiff(Integer minDiff) {
    this.minDiff = minDiff;
  }

  @VisibleForTesting
  List<DiffData> compareResults(File... files) throws Exception {
    return compareResults(loadResults(Arrays.asList(files)));
  }

  private List<DiffData> compareResults(Map<String, List<Result>> allResults) {
    List<DiffData> foundDiffs = new ArrayList<>();

    SortedSet<String> keys = new TreeSet<>(allResults.keySet());
    int length = allResults.get(keys.first()).size();

    IntStream.range(0, length).forEach(index -> {
      Long minMs = allResults.values().stream().map(result -> result.get(index).ms).min(Comparator.naturalOrder())
          .get();
      Long maxMs = allResults.values().stream().map(result -> result.get(index).ms).max(Comparator.naturalOrder())
          .get();
      if ((maxMs - minMs) > minDiff) {
        DiffData diff = new DiffData();
        diff.url = allResults.get(keys.first()).get(index).url;
        diff.min = minMs;
        diff.max = maxMs;
        keys.forEach(key -> {
          Result res = allResults.get(key).get(index);
          res.over = (int) (100 * ((res.ms - minMs) / minMs.doubleValue()));
          diff.results.add(res);
        });
        foundDiffs.add(diff);
      }
    });

    return foundDiffs;
  }

  private void printDiffs(List<DiffData> diffs) {
    diffs.forEach(diff -> {
      log.info("url: {}", diff.url);
      log.info("  min: {}ms  max: {}ms", diff.min, diff.max);
      diff.results.forEach(res -> log.info("  {}: {}\t+{}%", res.source, res.ms, res.over));
    });
  }

}
