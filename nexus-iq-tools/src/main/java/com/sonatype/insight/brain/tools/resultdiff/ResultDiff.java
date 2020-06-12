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
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.validators.PositiveInteger;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultDiff
{
  private static final Logger log = LoggerFactory.getLogger(ResultDiff.class);

  static final String ERROR_MIN_FILES = "A minimum of 2 files are required to compare.";

  static final String ERROR_INVALID_FILE_PREFIX = "Invalid file: ";

  @Parameter(description = "<file 1> <file 2> ... [file n]")
  private List<File> files = new ArrayList<>();

  @Parameter(names = "-minDiff", validateWith = PositiveInteger.class,
             description = "only report result with a minimum difference greater than this number of milliseconds")
  private int minDiff = 2000;

  private static class Result
  {
    String source;

    String url;

    @SuppressWarnings("unused")
    String payload;

    Long ms;

    @SuppressWarnings("unused")
    String httpCode;

    @SuppressWarnings("unused")
    Long size;

    @SuppressWarnings("unused")
    String md5;

    @SuppressWarnings("unused")
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
      rd.validate();
      rd.run();
    }
    catch (Exception e) {
      if (e instanceof ParameterException) {
        log.info("\nERROR: {}\n", e.getMessage());
      }
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

  private void validate() throws Exception {
    validateFiles(files);
  }

  @VisibleForTesting
  static void validateFiles(List<File> files) {
    String error;
    if (files.size() < 2) {
      error = ERROR_MIN_FILES;
    }
    else {
      Optional<String> invalid = files.stream().filter(f -> !f.exists()).map(File::getAbsolutePath).findFirst();
      error = invalid.isPresent() ? ERROR_INVALID_FILE_PREFIX + invalid.get() : null;
    }

    if (error != null) {
      throw new ParameterException(error);
    }
  }

  private Map<String, List<Result>> loadResults(List<File> files) {
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

        Integer skipTo = allLines.stream()
            .filter(line -> line.startsWith("---------------"))
            .findFirst().map(line -> allLines.indexOf(line))
            .orElseGet(() -> 0);

        for (String line : allLines.subList(skipTo, allLines.size())) {
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
      int colonAt = line.indexOf(":");
      if (colonAt < 0) {
        continue;
      }
      String key = line.substring(0, colonAt).trim().toLowerCase(Locale.ENGLISH);
      String value = line.substring(colonAt + 1).trim();

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
  void setMinDiff(int minDiff) {
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
    if (diffs.isEmpty()) {
      log.info("\nNo differences found greater than {}ms\n", minDiff);
    }
    diffs.forEach(diff -> {
      log.info("url: {}", diff.url);
      log.info("  min: {}ms  max: {}ms", diff.min, diff.max);
      diff.results.forEach(res -> log.info("  {}: {}\t+{}%", res.source, res.ms, res.over));
    });
  }
}
