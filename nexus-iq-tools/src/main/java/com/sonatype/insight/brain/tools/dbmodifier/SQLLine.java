/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.dbmodifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.text.MessageFormat.format;

class SQLLine
{
  public final String table;

  final List<String> cols;

  final List<String> vals;

  SQLLine(String table, List<String> cols, List<String> vals) {
    this.table = table;
    this.cols = cols;
    this.vals = vals;
  }

  private String buildStatement() {
    List<String> quotedCols = cols.stream().map(s -> format("\"{0}\"", s)).collect(Collectors.toList());
    if (cols.size() > 0) {
      String template = "INSERT INTO {0}({1}) VALUES({2});";
      return format(template, table, String.join(", ", quotedCols), String.join(", ", vals));
    }
    else {
      String template = "INSERT INTO {0} VALUES({1});";
      return format(template, table, String.join(", ", vals));
    }
  }

  @Override
  public String toString() {
    return buildStatement();
  }

  public String columnValue(String colName) {
    if (!cols.contains(colName)) {
      return null;
    }
    return vals.get(cols.indexOf(colName));
  }

  static Builder builder() {
    return new Builder();
  }

  public static class Builder
  {
    private String lineTable = "INVALID_placeholder_table";

    private List<String> lineCols = new ArrayList<>();

    private List<String> lineVals = new ArrayList<>();

    private Builder() {

    }

    Builder setLineVals(String... lineVals) {
      this.lineVals = Arrays.asList(lineVals);
      return this;
    }

    Builder setLineCols(String... lineCols) {
      this.lineCols = Arrays.asList(lineCols);
      return this;
    }

    Builder setLineTable(final String lineTable) {
      this.lineTable = lineTable;
      return this;
    }

    public SQLLine build() {
      return new SQLLine(lineTable, lineCols, lineVals);
    }
  }
}
