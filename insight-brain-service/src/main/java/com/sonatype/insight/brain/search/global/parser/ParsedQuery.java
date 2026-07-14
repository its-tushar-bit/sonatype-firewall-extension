/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global.parser;

import java.util.List;

/**
 * Result of parsing a raw query string. The AST is always well-formed; any
 * problems the parser tolerated are surfaced as {@code warnings}.
 */
public record ParsedQuery(AstNode ast, List<String> warnings)
{
  public ParsedQuery {
    warnings = List.copyOf(warnings);
  }
}
