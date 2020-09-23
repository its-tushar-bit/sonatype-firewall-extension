/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.dbmodifier;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.dataaccess.policy.PolicyInternal;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.conditions.PackageUrlConditionType;
import com.sonatype.insight.json.store.JsonUtils;

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
  static final String DEFAULT_PASS =
      "$shiro1$SHA-256$10$7PC5QqeewnJK3iBQLPoq+Q==$5G44CC6HIYL8113tbp9lL0lNDP5CQJzbar0mWWkKbIM=";

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

  private static String componentIdentifierRandomizer(String componentIdCoordinatesJson) {
    try {
      @SuppressWarnings("unchecked")
      Map<String, String> componentCoordinateMap = JsonUtils.parse(componentIdCoordinatesJson, Map.class);
      for (Entry<String, String> componentCoordinate : componentCoordinateMap.entrySet()) {
        componentCoordinateMap.put(componentCoordinate.getKey(),
            consistentRandomString(componentCoordinate.getValue()));
      }
      return ComponentIdentifierAdapter.toJson(componentCoordinateMap);
    }
    catch (IOException e) {
      throw new UncheckedIOException("Failed to parse component coordinates json:" + componentIdCoordinatesJson, e);
    }
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
    if (src.startsWith("STRINGDECODE(") && src.endsWith(")")) {
      return javaDecode(src.substring(13, src.length() - 1));
    }
    else {
      return src;
    }
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

  private static Function<SQLLine, List<SQLLine>> tableMod(final String... targetCols) {
    return line -> {
      List<SQLLine> processed = new ArrayList<>();

      for (String colName : targetCols) {
        String name = colName;
        Function<String, String> mutator = stringValueModifier(ScrubberInsertMods::defaultRandomizer);
        String[] colParts = colName.split(":");
        if (colParts.length > 1) {
          name = colParts[0];
          if (colParts[1].equals("componentIdentifier")) {
            mutator = stringValueModifier(ScrubberInsertMods::componentIdentifierRandomizer);
          }
          else if (colParts[1].equals("path")) {
            mutator = stringValueModifier(ScrubberInsertMods::pathRandomizer);
          }
          else if (colParts[1].equals("user")) {
            mutator = stringValueModifier(userRandomizer(name));
          }
        }

        if (line.cols.contains(name)) {
          scrubColumnValue(line, name, mutator, processed);
        }
      }
      processed.add(line);
      return processed;
    };
  }

  private static void scrubColumnValue(
      SQLLine insertSqlLine,
      String columnName,
      Function<String, String> mutator,
      List<SQLLine> processedSqlLines)
  {
    int columnIndex = insertSqlLine.cols.indexOf(columnName);
    String columnValue = insertSqlLine.vals.get(columnIndex);
    if (columnValue.contains("SYSTEM_COMBINE_CLOB")) {
      processedSqlLines.addAll(randomizeClob(columnValue, mutator));
    }
    else {
      insertSqlLine.vals.set(columnIndex, mutator.apply(columnValue));
    }
    if (insertSqlLine.cols.contains(columnName + "_lowercase")) {
      insertSqlLine.vals.set(insertSqlLine.cols.indexOf(columnName + "_lowercase"),
          insertSqlLine.vals.get(columnIndex).toLowerCase(Locale.ENGLISH));
    }
    if (insertSqlLine.cols.contains(columnName + "_lowercase_no_whitespace")) {
      insertSqlLine.vals.set(insertSqlLine.cols.indexOf(columnName + "_lowercase_no_whitespace"),
          insertSqlLine.vals.get(columnIndex).replaceAll("\\s", "").toLowerCase(Locale.ENGLISH));
    }
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

  // The rules for scrubbing customer sensitive data are documented at:
  // https://docs.sonatype.com/pages/viewpage.action?pageId=172133687
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
    insertModMap.put(h2OdsTable("mail_configuration"), truncate);
    insertModMap.put(h2OdsTable("proxy_server_configuration"), truncate);
    insertModMap.put(h2OdsTable("saml_configuration"), truncate);
    insertModMap.put(h2OdsTable("user_token"), truncate);
    insertModMap.put(h2OdsTable("source_control"), truncate);
    insertModMap.put(h2OdsTable("source_control_pull_request_comment"), truncate);
    insertModMap.put(h2OdsTable("source_control_default_branch_commit_history"), truncate);
    insertModMap.put(h2OdsTable("source_control_event"), truncate);
    // user special
    insertModMap
        .put(h2OdsTable("user"), tableMod("username:user", "password:user", "first_name", "last_name", "email"));
    insertModMap.put(h2OdsTable("membership_mapping"), tableMod("member_name:user"));
    insertModMap.put(h2OdsTable("user_viewed_product_notification"), tableMod("username:user"));
    insertModMap.put(h2OdsTable("dashboard_filter"), tableMod("username:user", "name", "based_on_filter_name"));
    // tables with data to scrub
    insertModMap.put(h2OdsTable("application"), tableMod("public_id", "name", "contact_internal_name:user"));
    insertModMap.put(h2OdsTable("application_component"), applicationComponentScrubber());
    insertModMap.put(h2OdsTable("hash_component_identifier"),
        tableMod("component_id_coordinates_json:componentIdentifier", "comment"));
    insertModMap.put(h2OdsTable("label"), tableMod("label", "description"));
    insertModMap.put(h2OdsTable("license_override"),
        tableMod("component_id_coordinates_json:componentIdentifier", "comment"));
    insertModMap.put(h2OdsTable("license_threat_group"), tableMod("name"));
    insertModMap.put(h2OdsTable("organization"), tableMod("name"));
    insertModMap.put(h2OdsTable("policy"), policyScrubber());
    insertModMap.put(h2OdsTable("policy_evaluation"), tableMod("commit_hash"));
    insertModMap.put(h2OdsTable("policy_violation"),
        tableMod("policy_name", "policy_waiver_comment", "component_id_coordinates_json:componentIdentifier",
            "filename:path"));
    insertModMap.put(h2OdsTable("policy_waiver"), tableMod("comment"));
    insertModMap.put(h2OdsTable("repository"), tableMod("public_id"));
    insertModMap.put(h2OdsTable("repository_component"), repositoryComponentScrubber());
    insertModMap.put(h2OdsTable("repository_manager"), tableMod("instance_id"));
    insertModMap.put(h2OdsTable("repository_policy_violation"),
        tableMod("pathname:path", "policy_name", "policy_waiver_comment",
            "component_id_coordinates_json:componentIdentifier"));
    insertModMap.put(h2OdsTable("role"), tableMod("name", "description"));
    insertModMap.put(h2OdsTable("sv_override"), tableMod("comment"));
    insertModMap.put(h2OdsTable("tag"), tableMod("name", "description"));
  }

  private static Function<SQLLine, List<SQLLine>> applicationComponentScrubber() {
    return insertSqlLine -> {
      List<SQLLine> scrubbedLines = new ArrayList<>();

      int columnIndex = 0;
      for (String columnName : insertSqlLine.cols) {
        switch (columnName) {
          case "component_id_coordinates_json":
            String proprietary = getColumnValue(insertSqlLine, "proprietary");
            String identificationSourceId = getColumnValue(insertSqlLine, "identification_source_id");
            if ("true".equalsIgnoreCase(proprietary)
                || !IdentificationSource.SONATYPE.getId().equals(stripQuotes(identificationSourceId))) {
              String componentIdCoordinatesJson = insertSqlLine.vals.get(columnIndex);
              insertSqlLine.vals.set(columnIndex, stringValueModifier(ScrubberInsertMods::componentIdentifierRandomizer)
                  .apply(componentIdCoordinatesJson));
            }
            break;
          case "pathnames":
            Function<String, String> mutator = stringValueModifier(ScrubberInsertMods::pathRandomizer);
            scrubColumnValue(insertSqlLine, columnName, mutator, scrubbedLines);
            break;
          default:
            break;
        }
        
        columnIndex++;
      }
      
      scrubbedLines.add(insertSqlLine);
      return scrubbedLines;
    };
  }

  private static Function<SQLLine, List<SQLLine>> repositoryComponentScrubber() {
    return insertSqlLine -> {
      List<SQLLine> scrubbedLines = new ArrayList<>();

      int columnIndex = 0;
      for (String columnName : insertSqlLine.cols) {
        switch (columnName) {
          case "component_id_coordinates_json":
            String identificationSourceId = getColumnValue(insertSqlLine, "identification_source_id");
            if (!IdentificationSource.SONATYPE.getId().equals(stripQuotes(identificationSourceId))) {
              String componentIdCoordinatesJson = insertSqlLine.vals.get(columnIndex);
              insertSqlLine.vals.set(columnIndex, stringValueModifier(ScrubberInsertMods::componentIdentifierRandomizer)
                  .apply(componentIdCoordinatesJson));
            }
            break;
          case "pathname":
            Function<String, String> mutator = stringValueModifier(ScrubberInsertMods::pathRandomizer);
            scrubColumnValue(insertSqlLine, columnName, mutator, scrubbedLines);
            break;
          default:
            break;
        }

        columnIndex++;
      }

      scrubbedLines.add(insertSqlLine);
      return scrubbedLines;
    };
  }

  private static Function<SQLLine, List<SQLLine>> policyScrubber() {
    return insertSqlLine -> {
      List<SQLLine> scrubbedLines = new ArrayList<>();

      String policyJson = stripQuotes(stripStringDecode(getColumnValue(insertSqlLine, "content")));
      String policyName = getColumnValue(insertSqlLine, "name");
      String policyOwnerId = getColumnValue(insertSqlLine, "owner_id");
      Policy policy = PolicyInternal.fromJson(policyJson, policyName, policyOwnerId);
      int columnIndex = 0;
      int contentColumnIndex = -1;
      int droolsCodeColumnIndex = -1;
      for (String columnName : insertSqlLine.cols) {
        switch (columnName) {
          case "name":
            Function<String, String> mutator = stringValueModifier(ScrubberInsertMods::defaultRandomizer);
            scrubColumnValue(insertSqlLine, columnName, mutator, scrubbedLines);
            String scrubbedPolicyName = getColumnValue(insertSqlLine, "name");
            policy.setName(scrubbedPolicyName);
            break;
          case "content":
            contentColumnIndex = columnIndex;
            break;
          case "drools_code":
            droolsCodeColumnIndex = columnIndex;
            break;
          default:
            break;
        }

        columnIndex++;
      }

      for (Constraint constraint : policy.getConstraints()) {
        constraint.setName(defaultRandomizer(constraint.getName()));
        for (Condition condition : constraint.getConditions()) {
          switch (condition.getConditionTypeId()) {
            case CoordinatesConditionType.ID: {
              String[] coordinates = condition.getValue().split(":", 2);
              condition.setValue(coordinates[0] + ":" + defaultRandomizer(coordinates[1]));
              break;
            }
            case PackageUrlConditionType.ID: {
              String[] coordinates = condition.getValue().split("/", 2);
              condition.setValue(coordinates[0] + "/" + defaultRandomizer(coordinates[1]));
              break;
            }
            default:
              // Checkstyle wants a default for all switches
              break;
          }
        }
      }

      policy.setNotifications(null);

      String scrubbedPolicyJson = PolicyInternal.toJson(policy);
      insertSqlLine.vals.set(contentColumnIndex, wrapStringDecode(wrapQuotes(scrubbedPolicyJson)));
      // Remove the drools code. It can be re-generated if needed.
      insertSqlLine.vals.set(droolsCodeColumnIndex, "''");

      scrubbedLines.add(insertSqlLine);
      return scrubbedLines;
    };
  }

  private static String getColumnValue(SQLLine insertSqlLine, String columnName) {
    int columnIndex = insertSqlLine.cols.indexOf(columnName);
    return insertSqlLine.vals.get(columnIndex);
  }
}
