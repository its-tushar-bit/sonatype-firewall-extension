/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.dbmodifier;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.parser.ParseException;
import net.sf.jsqlparser.parser.Token;
import net.sf.jsqlparser.statement.insert.Insert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InputParser
{
  private static final Pattern BINARY_VALUE_REGEX = Pattern.compile("X( ?'[ a-fA-F0-9]*')+");

  private static final Logger log = LoggerFactory.getLogger(InputParser.class);

  static SQLLine parseInput(String insertSqlString) {
    try {
      Insert insertSql = (Insert) CCJSqlParserUtil.parse(insertSqlString);
      String tableName = insertSql.getTable().getFullyQualifiedName();
      return new SQLLine(tableName, getColumnNames(insertSql), getColumnValues(insertSql));
    }
    catch (JSQLParserException e) {
      SQLLine sqlLine = handle(insertSqlString, e);
      if (sqlLine != null) {
        return sqlLine;
      }
      log.error("Error parsing line {}", insertSqlString, e);
      return null;
    }
  }

  // CLM-16041
  private static SQLLine handle(String insertSqlString, JSQLParserException e) {
    if (!(e.getCause() instanceof ParseException)) {
      return null;
    }
    ParseException parseException = (ParseException) e.getCause();
    Token currentToken = parseException.currentToken;
    if (currentToken == null) {
      return null;
    }
    String valueToEnd = insertSqlString.substring(currentToken.absoluteBegin - 1);
    Matcher matcher = BINARY_VALUE_REGEX.matcher(valueToEnd);
    if (!matcher.find() || matcher.start() != 0) {
      return null;
    }
    String value = matcher.group();
    String placeholder = "'" + UUID.randomUUID().toString().replace("-", "") + "'";
    insertSqlString = insertSqlString.substring(0, currentToken.absoluteBegin - 1) + placeholder +
        insertSqlString.substring(currentToken.absoluteBegin - 1 + value.length());
    SQLLine sqlLine = parseInput(insertSqlString);
    if (sqlLine == null) {
      return null;
    }
    sqlLine.vals.set(sqlLine.vals.indexOf(placeholder), value);
    return sqlLine;
  }

  private static List<String> getColumnNames(Insert insertSql) {
    return Optional.ofNullable(insertSql.getColumns()).orElse(Collections.emptyList()).stream()
        .map(column -> column.getColumnName().replace("\"", "")).collect(Collectors.toList());
  }

  private static List<String> getColumnValues(Insert insertSql) {
    return ((ExpressionList) insertSql.getItemsList()).getExpressions().stream().map(Object::toString)
        .collect(Collectors.toList());
  }
}
