/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.dbmodifier;

import java.util.List;
import java.util.stream.Collectors;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.insert.Insert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InputParser
{
  private static final Logger log = LoggerFactory.getLogger(InputParser.class);

  static SQLLine parseInput(String insertSqlString) {
    try {
      Insert insertSql = (Insert) CCJSqlParserUtil.parse(insertSqlString);
      String tableName = insertSql.getTable().getSchemaName() + "." + insertSql.getTable().getName();
      return new SQLLine(tableName, getColumnNames(insertSql), getColumnValues(insertSql));
    }
    catch (JSQLParserException e) {
      log.error("Error parsing line {}", insertSqlString, e);
      return null;
    }
  }

  private static List<String> getColumnNames(Insert insertSql) {
    return insertSql.getColumns().stream().map(column -> column.getColumnName().replace("\"", ""))
        .collect(Collectors.toList());
  }

  private static List<String> getColumnValues(Insert insertSql) {
    return ((ExpressionList) insertSql.getItemsList()).getExpressions().stream().map(Object::toString)
        .collect(Collectors.toList());
  }
}
