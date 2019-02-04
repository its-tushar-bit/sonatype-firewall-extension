/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.dbmodifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.regex.Pattern;

public class InputParser
{
  private static final String EDGE_CASE_00 = "STRINGDECODE(', ";

  private static final String EDGE_CASE_REPLACEMENT_00 = "-¦¦¦00¦¦¦-";

  private static Pattern value_pattern = Pattern.compile("', ");

  private static Pattern column_pattern = Pattern.compile("\\s*,\\s*");

  private static void trimPush(Stack<String> target, String val) {
    if (!val.trim().isEmpty()) {
      target.push(val);
    }
  }

  private static List<String> parseCols(String cols) {
    if (cols == null || cols.trim().isEmpty()) {
      return new ArrayList<>();
    }
    return Arrays.asList(column_pattern.split(cols.replace("\"", "")));
  }

  private static List<String> parseVals(String values) {
    List<String> vals = new ArrayList<>();
    Stack<String> bits = new Stack<>();

    String[] parts = value_pattern.split(values.replace(EDGE_CASE_00, EDGE_CASE_REPLACEMENT_00));

    for (int j = parts.length - 1; j >= 0; j--) {
      bits.push(parts[j].replace(EDGE_CASE_REPLACEMENT_00, EDGE_CASE_00));
    }

    while (!bits.empty()) {
      String nextVal = extractValue(bits);
      if (nextVal != null) {
        vals.add(nextVal);
      }
    }
    return vals;
  }

  private static String extractValue(final Stack<String> bits) {
    String valueSegment = bits.pop();

    if (valueSegment.startsWith("'")) {
      return valueSegment + (bits.empty() ? "" : "'");
    }
    else { //special handling
      if (valueSegment.startsWith("STRINGDECODE('")) {
        while (!valueSegment.contains("'), ") && !bits.empty()) {
          valueSegment = valueSegment + "'" + bits.pop();
        }
      }

      if (valueSegment.contains("'), ")) {
        if (!valueSegment.startsWith("STRINGDECODE")) {
          trimPush(bits, valueSegment.substring(valueSegment.indexOf("STRINGDECODE")));
          trimPush(bits, valueSegment.substring(0, valueSegment.indexOf("STRINGDECODE")));
        }
        else {
          trimPush(bits, valueSegment.substring(valueSegment.indexOf("'), ") + 4));
          return valueSegment.substring(0, valueSegment.indexOf("'), ") + 2);
        }
      }
      else if (valueSegment.startsWith("STRINGDECODE")) {
        return valueSegment;
      }
      else if (valueSegment.startsWith("TIMESTAMP")) {
        return valueSegment + (bits.empty() ? "" : "'");
      }
      else {
        if (valueSegment.contains(", ")) {
          trimPush(bits, valueSegment.substring(valueSegment.indexOf(", ") + 2));
          return valueSegment.substring(0, valueSegment.indexOf(", "));
        }
        else {
          return valueSegment;
        }
      }
    }
    return null;
  }

  static SQLLine parseInput(String query) {
    String tabColVal = query.substring(query.indexOf("INTO ") + 5);

    String tabCol = tabColVal.substring(0, tabColVal.indexOf("VALUES("));
    String tab = tabCol.indexOf("(") > 0 ? tabCol.substring(0, tabCol.indexOf("(")) : tabCol.trim();
    String col = tabCol.indexOf("(") > 0 ? tabCol.substring(tabCol.indexOf("(") + 1, tabCol.lastIndexOf(")")) : "";
    String val = tabColVal.substring(tabColVal.indexOf("VALUES(") + 7);
    val = val.substring(0, val.lastIndexOf(")"));

    List<String> pCols = parseCols(col);
    List<String> pVals = parseVals(val);

    if (pCols.size() == 0 || pCols.size() == pVals.size()) {
      return new SQLLine(tab, pCols, pVals);
    }

    return new SQLLine("ERROR", new ArrayList<>(), new ArrayList<>());
  }
}
