/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.dbmodifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomStringUtils;

import static java.text.MessageFormat.format;
import static org.h2.util.StringUtils.javaDecode;
import static org.h2.util.StringUtils.javaEncode;

class ScrubberInsertMods
{
  // set a fixed seed for consistent reproducible randomization
  private static final long seed = 20180507L;

  private static Random RANDOM = new Random(seed);

  // visible for testing
  static final String NAME_OPEN_JSON = "\"name\" : \"";

  private static final String NOTIFICATIONS_OPEN_JSON = "  \"notifications\" : {\n";

  // visible for testing
  static final String DEFAULT_PASS =
      "$shiro1$SHA-256$10$7PC5QqeewnJK3iBQLPoq+Q==$5G44CC6HIYL8113tbp9lL0lNDP5CQJzbar0mWWkKbIM=";

  // visible for testing
  static final String NOTIFICATIONS_EMPTY_CLOSING_JSON =
      "  \"notifications\" : {\n" +
          "    \"userNotifications\" : [ ],\n" +
          "    \"roleNotifications\" : [ ],\n" +
          "    \"jiraNotifications\" : [ ]\n" +
          "  }\n" +
          "}";

  private static final Set<String> EXCLUDED_TERMS = new HashSet<>(Arrays.asList("Root Organization"));

  private static String defaultRandomizer(String src) {
    return consistentRandomString(src);
  }

  private static String pathRandomizer(String src) {
    StringTokenizer st = new StringTokenizer(src, "[\\W\n\r\t\\/;:() ]", true);
    StringBuilder builder = new StringBuilder();
    while (st.hasMoreTokens()) {
      String tok = st.nextToken();
      if (Pattern.matches("[\\w\\d-+_.]+", tok)) {
        builder.append(consistentRandomString(tok));
      }
      else {
        builder.append(tok);
      }
    }
    return builder.toString();
  }

  private static String stripNotifications(String json) {
    if (json.contains(NOTIFICATIONS_OPEN_JSON)) {
      return json.substring(0, json.indexOf(NOTIFICATIONS_OPEN_JSON)) + NOTIFICATIONS_EMPTY_CLOSING_JSON;
    }
    return json;
  }

  private static String replaceJsonNames(String json) {
    if (json.contains(NAME_OPEN_JSON)) {
      StringBuffer modified = new StringBuffer(json.length());
      int position = 0;
      int next = json.indexOf(NAME_OPEN_JSON);
      while (next > 0) {
        int eol = json.indexOf('\n', next);
        int closing = json.lastIndexOf('"', eol);
        String target = json.substring(next + NAME_OPEN_JSON.length(), closing);
        modified.append(json, position, next + NAME_OPEN_JSON.length());
        modified.append(consistentRandomString(target));
        position = closing;
        next = json.indexOf(NAME_OPEN_JSON, position);
      }
      modified.append(json.substring(position));

      return modified.toString();
    }
    return json;
  }

  // visible for testing
  static String jsonRandomizer(String src) {
    return replaceJsonNames(stripNotifications(src));
  }

  private static Function<String, String> stringValueModifier(Function<String, String> mutator) {
    Function<String, String> valueMod = src -> {
      if (src.startsWith("SYSTEM_COMBINE_CLOB")) {
        return src;
      }
      else if (!src.startsWith("'")) {
        return src;
      }
      String unesc = stripQuotes(src);
      return wrapQuotes(mutator.apply(unesc));
    };

    Function<String, String> wrappedValueMod = src -> {
      if (src.startsWith("STRINGDECODE(") && src.endsWith(")")) {
        return wrapStringDecode(valueMod.apply(stripStringDecode(src)));
      }
      return valueMod.apply(src);
    };

    return wrappedValueMod;
  }

  // visible for testing
  static void resetCache(Long randomSeed) {
    randomStringCache = new HashMap<>();
    if (randomSeed != null) {
      RANDOM = new Random(randomSeed);
    }
    else {
      RANDOM = new Random(seed);
    }
  }

  private static Map<String, String> randomStringCache = new HashMap<>();

  private static String consistentRandomString(String unesc) {
    if (EXCLUDED_TERMS.contains(unesc)) {
      return unesc;
    }
    return randomStringCache
        .computeIfAbsent(unesc, s -> RandomStringUtils.random(unesc.length(), 0, 0, true, true, null, RANDOM));
  }

  private static Map<String, SortedMap<Integer, SQLLine>> clobCache = new HashMap<>();

  private static List<SQLLine> consumeClobChunk(SQLLine line) {
    SortedMap<Integer, SQLLine> clobs = clobCache.getOrDefault(line.vals.get(0), new TreeMap<>());
    clobs.put(Integer.parseInt(line.vals.get(1)), line);
    clobCache.put(line.vals.get(0), clobs);
    return Collections.emptyList();
  }

  private static String stripStringDecode(String src) {
    return javaDecode(src.substring(13, src.length() - 1));
  }

  private static String wrapStringDecode(String src) {
    return "STRINGDECODE(" + javaEncode(src) + ")";
  }

  private static String stripQuotes(String src) {
    return src.substring(1, src.length() - 1);
  }

  private static String wrapQuotes(String src) {
    return "'" + src + "'";
  }

  private static List<SQLLine> randomizeClob(String combineClob, Function<String, String> mutator) {
    String targetClob = combineClob.substring(combineClob.indexOf('(') + 1, combineClob.indexOf(')'));
    List<SQLLine> cloblines = new ArrayList<>(clobCache.get(targetClob).values());
    List<String> parts = cloblines.stream()
        .map(cl -> cl.vals.get(2))
        .map(ScrubberInsertMods::stripStringDecode)
        .map(ScrubberInsertMods::stripQuotes)
        .collect(Collectors.toList());
    List<Integer> lengths = parts.stream().map(String::length).collect(Collectors.toList());
    String combined = wrapQuotes(String.join("", parts));
    String randomized = stripQuotes(mutator.apply(combined));

    for (int i = 0; i < cloblines.size(); i++) {
      int offset = lengths.subList(0, i).stream().mapToInt(Integer::intValue).sum();
      String randomizedPart = wrapStringDecode(wrapQuotes(randomized.substring(offset, offset + lengths.get(i))));
      cloblines.get(i).vals.set(2, randomizedPart);
    }

    return cloblines;
  }

  private static Function<String, String> userRandomizer(final String targetCol) {
    List<String> preserveAdmin = Arrays.asList("username", "user", "member_name", "contact_internal_name");

    return src -> {
      if (targetCol.equals("password")) {
        return DEFAULT_PASS;
      }
      else if (preserveAdmin.contains(targetCol)) {
        if (src.toLowerCase(Locale.ENGLISH).equals("admin")) {
          return src;
        }
      }

      return defaultRandomizer(src);
    };
  }

  private static Function<SQLLine, List<SQLLine>> tableModFiltered(String filterCol,
                                                                   String filterVal,
                                                                   final String... targetCols)
  {
    Function<SQLLine, List<SQLLine>> conditionalOp = tableMod(targetCols);
    return line -> {
      if (line.cols.contains(filterCol)) {
        int colIndex = line.cols.indexOf(filterCol);
        if (line.vals.get(colIndex).equals(filterVal)) {
          return conditionalOp.apply(line);
        }
      }
      return noop.apply(line);
    };
  }

  private static Function<SQLLine, List<SQLLine>> tableMod(final String... targetCols) {
    return line -> {
      List<SQLLine> processed = new ArrayList<>();

      for (String colName : targetCols) {
        String name = colName;
        Function<String, String> mutator = stringValueModifier(ScrubberInsertMods::defaultRandomizer);
        String[] colParts = colName.split(":");
        if (colParts.length > 1) {
          name = colParts[0];
          if (colParts[1].equals("path")) {
            mutator = stringValueModifier(ScrubberInsertMods::pathRandomizer);
          }
          else if (colParts[1].equals("json")) {
            mutator = stringValueModifier(ScrubberInsertMods::jsonRandomizer);
          }
          else if (colParts[1].equals("user")) {
            mutator = stringValueModifier(userRandomizer(name));
          }
        }

        if (line.cols.contains(name)) {
          int colIndex = line.cols.indexOf(name);
          if (line.vals.get(colIndex).contains("SYSTEM_COMBINE_CLOB")) {
            processed.addAll(randomizeClob(line.vals.get(colIndex), mutator));
          }
          else {
            line.vals.set(colIndex, mutator.apply(line.vals.get(colIndex)));
          }
          if (line.cols.contains(name + "_lowercase")) {
            line.vals.set(line.cols.indexOf(name + "_lowercase"),
                line.vals.get(line.cols.indexOf(name)).toLowerCase(Locale.ENGLISH));
          }
          if (line.cols.contains(name + "_lowercase_no_whitespace")) {
            line.vals.set(line.cols.indexOf(name + "_lowercase_no_whitespace"),
                line.vals.get(line.cols.indexOf(name)).replaceAll("\\s", "").toLowerCase(Locale.ENGLISH));
          }
        }
      }
      processed.add(line);
      return processed;
    };
  }

  // visible for testing
  static String h2OdsTable(String tableName) {
    return format("\"insight_brain_ods\".\"{0}\"", tableName);
  }

  static List<SQLLine> scrubInputLine(SQLLine insert) {
    return insertModMap.getOrDefault(insert.table, noop).apply(insert);
  }

  private static Function<SQLLine, List<SQLLine>> noop = Collections::singletonList;

  private static Function<SQLLine, List<SQLLine>> truncate = line -> Collections.emptyList();

  private static final Map<String, Function<SQLLine, List<SQLLine>>> insertModMap = new HashMap<>();

  static {
    // h2 - special
    insertModMap.put("SYSTEM_LOB_STREAM", ScrubberInsertMods::consumeClobChunk);
    // truncate
    insertModMap.put(h2OdsTable("proprietary_config"), truncate);
    insertModMap.put(h2OdsTable("ldap_usermapping"), truncate);
    insertModMap.put(h2OdsTable("ldap_connection"), truncate);
    insertModMap.put(h2OdsTable("ldap_server"), truncate);
    insertModMap.put(h2OdsTable("webhook"), truncate);
    insertModMap.put(h2OdsTable("webhook_event_type"), truncate);
    insertModMap.put(h2OdsTable("system_notice"), truncate);
    insertModMap.put(h2OdsTable("system_configuration_property"), truncate);
    // user spacial
    insertModMap
        .put(h2OdsTable("user"), tableMod("username:user", "password:user", "first_name", "last_name", "email"));
    insertModMap.put(h2OdsTable("membership_mapping"), tableModFiltered("member_type", "'USER'", "member_name:user"));
    insertModMap.put(h2OdsTable("user_viewed_product_notification"), tableMod("username:user"));
    insertModMap.put(h2OdsTable("dashboard_filter"), tableMod("username:user", "name", "based_on_filter_name"));
    // tables with data to scrub
    insertModMap.put(h2OdsTable("application"), tableMod("public_id", "name", "contact_internal_name:user"));
    insertModMap.put(h2OdsTable("application_component"), tableMod("pathnames:path"));
    insertModMap.put(h2OdsTable("hash_component_identifier"), tableMod("comment"));
    insertModMap.put(h2OdsTable("label"), tableMod("label", "description"));
    insertModMap.put(h2OdsTable("license_override"), tableMod("comment"));
    insertModMap.put(h2OdsTable("license_threat_group"), tableMod("name"));
    insertModMap.put(h2OdsTable("organization"), tableMod("name"));
    insertModMap.put(h2OdsTable("policy"), tableMod("name", "content:json"));
    insertModMap.put(h2OdsTable("policy_violation"), tableMod("policy_name"));
    insertModMap.put(h2OdsTable("policy_waiver"), tableMod("comment"));
    insertModMap.put(h2OdsTable("repository"), tableMod("public_id"));
    insertModMap.put(h2OdsTable("repository_component"), tableMod("pathname:path"));
    insertModMap.put(h2OdsTable("repository_policy_violation"), tableMod("pathname:path", "policy_name"));
    insertModMap.put(h2OdsTable("sv_override"), tableMod("comment"));
    insertModMap.put(h2OdsTable("tag"), tableMod("name", "description"));
  }
}
