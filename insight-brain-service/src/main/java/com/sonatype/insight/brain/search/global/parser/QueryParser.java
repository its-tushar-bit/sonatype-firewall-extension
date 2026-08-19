/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global.parser;

import java.util.ArrayList;
import java.util.List;

import static com.sonatype.insight.brain.search.global.parser.QueryParser.TokenType.AND;
import static com.sonatype.insight.brain.search.global.parser.QueryParser.TokenType.COLON;
import static com.sonatype.insight.brain.search.global.parser.QueryParser.TokenType.IDENT;
import static com.sonatype.insight.brain.search.global.parser.QueryParser.TokenType.LBRACE;
import static com.sonatype.insight.brain.search.global.parser.QueryParser.TokenType.LBRACK;
import static com.sonatype.insight.brain.search.global.parser.QueryParser.TokenType.LPAREN;
import static com.sonatype.insight.brain.search.global.parser.QueryParser.TokenType.MINUS;
import static com.sonatype.insight.brain.search.global.parser.QueryParser.TokenType.NOT;
import static com.sonatype.insight.brain.search.global.parser.QueryParser.TokenType.OR;
import static com.sonatype.insight.brain.search.global.parser.QueryParser.TokenType.QUOTE;
import static com.sonatype.insight.brain.search.global.parser.QueryParser.TokenType.RBRACE;
import static com.sonatype.insight.brain.search.global.parser.QueryParser.TokenType.RBRACK;
import static com.sonatype.insight.brain.search.global.parser.QueryParser.TokenType.RPAREN;
import static com.sonatype.insight.brain.search.global.parser.QueryParser.TokenType.STAR;
import static com.sonatype.insight.brain.search.global.parser.QueryParser.TokenType.TO;

/**
 * Tolerant recursive-descent parser for the global search query language (grammar in
 * docs/global-search/syntax-reference.md). Reserved words {@code AND}, {@code OR}, {@code NOT},
 * {@code TO} are recognised only in uppercase; field names stay case-sensitive.
 *
 * <p>
 * The parser never throws: malformed input becomes a well-formed AST plus warnings on the
 * {@link ParsedQuery}.
 */
public final class QueryParser
{
  /**
   * Cap on raw query length before lexing; longer input is truncated so a pathological string cannot
   * produce a huge token list or deep AST. Matches search-box UX limits and is comfortably above the
   * cursor {@code MAX_ENCODED_LENGTH} (1024).
   */
  static final int MAX_QUERY_LENGTH = 4000;

  private QueryParser() {
  }

  public static ParsedQuery parse(String rawQuery) {
    List<String> warnings = new ArrayList<>();
    if (rawQuery == null) {
      return new ParsedQuery(new AstNode.EmptyNode(), warnings);
    }
    String trimmed = rawQuery.trim();
    if (trimmed.isEmpty()) {
      return new ParsedQuery(new AstNode.EmptyNode(), warnings);
    }
    if (rawQuery.length() > MAX_QUERY_LENGTH) {
      warnings.add("Query exceeds maximum length; truncated.");
      rawQuery = rawQuery.substring(0, MAX_QUERY_LENGTH);
    }
    List<Token> tokens = new Lexer(rawQuery, warnings).tokenize();
    AstNode ast = new Parser(tokens, warnings).parseTop();
    return new ParsedQuery(ast, warnings);
  }

  enum TokenType
  {
    IDENT,
    QUOTE,
    COLON,
    LBRACK,
    RBRACK,
    LBRACE,
    RBRACE,
    LPAREN,
    RPAREN,
    AND,
    OR,
    NOT,
    TO,
    MINUS,
    STAR
  }

  record Token(TokenType type, String value, int position)
  {
  }

  private static final class Lexer
  {
    private final String input;

    private final List<String> warnings;

    private final List<Token> out = new ArrayList<>();

    private int i;

    private boolean lastWasWhitespace = true;

    Lexer(String input, List<String> warnings) {
      this.input = input;
      this.warnings = warnings;
    }

    List<Token> tokenize() {
      int n = input.length();
      while (i < n) {
        char c = input.charAt(i);

        if (isWhitespace(c)) {
          i++;
          lastWasWhitespace = true;
          continue;
        }

        int start = i;
        switch (c) {
          case '(' -> {
            out.add(new Token(LPAREN, "(", start));
            i++;
            lastWasWhitespace = false;
            continue;
          }
          case ')' -> {
            out.add(new Token(RPAREN, ")", start));
            i++;
            lastWasWhitespace = false;
            continue;
          }
          case '[' -> {
            out.add(new Token(LBRACK, "[", start));
            i++;
            lastWasWhitespace = false;
            continue;
          }
          case ']' -> {
            out.add(new Token(RBRACK, "]", start));
            i++;
            lastWasWhitespace = false;
            continue;
          }
          case '{' -> {
            out.add(new Token(LBRACE, "{", start));
            i++;
            lastWasWhitespace = false;
            continue;
          }
          case '}' -> {
            out.add(new Token(RBRACE, "}", start));
            i++;
            lastWasWhitespace = false;
            continue;
          }
          case '*' -> {
            out.add(new Token(STAR, "*", start));
            i++;
            lastWasWhitespace = false;
            continue;
          }
          default -> {
            /* fall through */ }
        }

        if (c == '-' && isClauseStart()) {
          out.add(new Token(MINUS, "-", start));
          i++;
          lastWasWhitespace = false;
          continue;
        }

        if (c == '"') {
          readQuote(start);
          lastWasWhitespace = false;
          continue;
        }

        if (c == ':') {
          // Bare colon with no leading identifier; treat as a literal term.
          warnings.add("Bare ':' at position " + start + " has no field name; treated as literal.");
          out.add(new Token(IDENT, ":", start));
          i++;
          lastWasWhitespace = false;
          continue;
        }

        if (isIdentStart(c)) {
          readIdent(start);
          lastWasWhitespace = false;
          continue;
        }

        warnings.add("Ignored unexpected character '" + c + "' at position " + start + ".");
        i++;
        lastWasWhitespace = false;
      }
      return out;
    }

    private void readQuote(int start) {
      int n = input.length();
      int j = i + 1;
      StringBuilder buf = new StringBuilder();
      while (j < n && input.charAt(j) != '"') {
        buf.append(input.charAt(j));
        j++;
      }
      if (j == n) {
        warnings.add("Unclosed quote at position " + start + ".");
      }
      out.add(new Token(QUOTE, buf.toString(), start));
      i = (j == n) ? n : j + 1;
    }

    private void readIdent(int start) {
      int n = input.length();
      int j = i;
      while (j < n && isIdentBody(input.charAt(j))) {
        j++;
      }
      String word = input.substring(i, j);
      i = j;

      TokenType kw = keywordType(word);
      if (kw == TO) {
        // 'TO' is a range delimiter only inside a bracket range; otherwise it's a bare term.
        if (lastIs(LBRACK) || lastIs(LBRACE) || insideOpenRange()) {
          out.add(new Token(TO, word, start));
        }
        else {
          out.add(new Token(IDENT, word, start));
        }
      }
      else if (kw != null) {
        out.add(new Token(kw, word, start));
      }
      else {
        out.add(new Token(IDENT, word, start));
      }

      // Adjacent colon (no whitespace) becomes a synthetic COLON so `name:value` parses as a unit.
      if (i < input.length() && input.charAt(i) == ':') {
        out.add(new Token(COLON, ":", i));
        i++;
      }
    }

    private static TokenType keywordType(String word) {
      return switch (word) {
        case "AND" -> AND;
        case "OR" -> OR;
        case "NOT" -> NOT;
        case "TO" -> TO;
        default -> null;
      };
    }

    private boolean lastIs(TokenType type) {
      return !out.isEmpty() && out.get(out.size() - 1).type() == type;
    }

    /**
     * Returns true when a range bracket has been opened but not yet closed.
     * Scans backward past the current bounds and any interleaved tokens.
     *
     * <p>
     * This is O(n) per call in the number of accumulated tokens, so calling it once per
     * {@code TO} candidate is O(n^2) worst-case. That is acceptable because the token count is
     * bounded by {@link #MAX_QUERY_LENGTH} (a few hundred tokens at most). A future maintainer
     * who raises that cap should revisit this scan.
     */
    private boolean insideOpenRange() {
      int depth = 0;
      for (int k = out.size() - 1; k >= 0; k--) {
        TokenType t = out.get(k).type();
        if (t == RBRACK || t == RBRACE) {
          depth++;
        }
        else if (t == LBRACK || t == LBRACE) {
          if (depth == 0) {
            return true;
          }
          depth--;
        }
      }
      return false;
    }

    private boolean isClauseStart() {
      if (lastWasWhitespace) {
        return true;
      }
      if (out.isEmpty()) {
        return true;
      }
      TokenType t = out.get(out.size() - 1).type();
      return t == LPAREN
          || t == OR
          || t == AND
          || t == NOT
          || t == MINUS;
    }

    private static boolean isWhitespace(char c) {
      return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    private static boolean isIdentStart(char c) {
      if (c >= 'a' && c <= 'z')
        return true;
      if (c >= 'A' && c <= 'Z')
        return true;
      if (c >= '0' && c <= '9')
        return true;
      return c == '_' || c == '.' || c == '/' || c == '@' || c == '+' || c == '%' || c == '#' || c == '$';
    }

    private static boolean isIdentBody(char c) {
      if (isWhitespace(c))
        return false;
      return switch (c) {
        case '(', ')', '[', ']', '{', '}', '"', ':', '*' -> false;
        default -> true;
      };
    }
  }

  private static final class Parser
  {
    /** Cap on parenthesis nesting; deeper groups are ignored to keep the "never throws" contract. */
    private static final int MAX_NESTING_DEPTH = 100;

    private final List<Token> tokens;

    private final List<String> warnings;

    private int pos;

    private int depth;

    Parser(List<Token> tokens, List<String> warnings) {
      this.tokens = tokens;
      this.warnings = warnings;
    }

    AstNode parseTop() {
      AstNode node = parseOr();
      if (peek() != null) {
        warnings.add("Ignored trailing tokens after position " + peek().position() + ".");
      }
      return node;
    }

    // orExpr = andExpr (OR andExpr)*
    private AstNode parseOr() {
      AstNode first = parseAnd();
      if (peek() == null || peek().type() != OR) {
        return first;
      }
      List<AstNode> children = new ArrayList<>();
      addForOr(children, first);
      while (peek() != null && peek().type() == OR) {
        advance();
        AstNode next = parseAnd();
        addForOr(children, next);
      }
      if (children.isEmpty()) {
        return new AstNode.EmptyNode();
      }
      if (children.size() == 1) {
        return children.get(0);
      }
      return new AstNode.OrNode(children);
    }

    private static void addForOr(List<AstNode> into, AstNode node) {
      if (node instanceof AstNode.EmptyNode) {
        return;
      }
      into.add(node);
    }

    // andExpr = unary (AND? unary)* — juxtaposition is implicit AND
    private AstNode parseAnd() {
      List<AstNode> nodes = new ArrayList<>();
      AstNode first = parseUnary();
      if (!(first instanceof AstNode.EmptyNode)) {
        nodes.add(first);
      }
      while (true) {
        Token t = peek();
        if (t == null)
          break;
        if (t.type() == OR || t.type() == RPAREN)
          break;
        if (t.type() == AND) {
          advance();
        }
        AstNode next = parseUnary();
        if (next instanceof AstNode.EmptyNode) {
          break;
        }
        nodes.add(next);
      }
      if (nodes.isEmpty()) {
        return new AstNode.EmptyNode();
      }
      if (nodes.size() == 1) {
        return nodes.get(0);
      }
      return new AstNode.AndNode(nodes);
    }

    // unary = (NOT | -)+ atom | atom
    private AstNode parseUnary() {
      int notCount = 0;
      int startPos = -1;
      while (peek() != null && (peek().type() == NOT || peek().type() == MINUS)) {
        if (startPos < 0) {
          startPos = peek().position();
        }
        notCount++;
        advance();
      }
      if (notCount == 0) {
        return parseAtom();
      }
      AstNode child = parseAtom();
      if (child instanceof AstNode.EmptyNode) {
        warnings.add("Dangling NOT operator at position " + startPos + " has no operand.");
        return new AstNode.EmptyNode();
      }
      if (notCount > 1) {
        warnings.add("Nested NOT operators collapsed at position " + startPos + ".");
      }
      // Odd count → wrap in NOT; even count → NOTs cancel out.
      if ((notCount & 1) == 1) {
        return new AstNode.NotNode(child);
      }
      return child;
    }

    // atom = ( orExpr ) | "phrase" | IDENT (: value)?
    private AstNode parseAtom() {
      Token t = peek();
      if (t == null) {
        return new AstNode.EmptyNode();
      }

      if (t.type() == LPAREN) {
        advance();
        if (depth >= MAX_NESTING_DEPTH) {
          // Degrade to fail-open: skip to the matching close without recursing, so pathological
          // nesting cannot overflow the stack. The over-deep group becomes an empty node.
          warnings.add("Parenthesis nesting exceeds " + MAX_NESTING_DEPTH
              + "; over-deep group at position " + t.position() + " ignored.");
          skipBalancedGroup();
          return new AstNode.EmptyNode();
        }
        depth++;
        AstNode inner;
        try {
          inner = parseOr();
        }
        finally {
          depth--;
        }
        if (peek() != null && peek().type() == RPAREN) {
          advance();
        }
        else {
          warnings.add("Unclosed parenthesis opened at position " + t.position() + ".");
        }
        return inner;
      }

      if (t.type() == QUOTE) {
        advance();
        return new AstNode.PhraseNode(t.value());
      }

      if (t.type() == IDENT) {
        advance();
        if (peek() != null && peek().type() == COLON) {
          advance();
          // Field names must not start with a digit; a digit-leading token before ':' is a bare
          // term, not a filter (FieldMap would reject it anyway). Keep it (and the colon) as text.
          if (!t.value().isEmpty() && Character.isDigit(t.value().charAt(0))) {
            warnings.add("Field name \"" + t.value() + "\" must not start with a digit; treated as a term.");
            return new AstNode.TermNode(t.value());
          }
          return parseFieldedNode(t.value(), t.position());
        }
        return new AstNode.TermNode(t.value());
      }

      // Stray operator / close-bracket / etc — skip and let caller decide.
      advance();
      return new AstNode.EmptyNode();
    }

    private AstNode parseFieldedNode(String fieldName, int fieldPos) {
      Token t = peek();
      if (t == null) {
        warnings.add("Field \"" + fieldName + "\" has no value.");
        return new AstNode.FieldNode(fieldName, new FieldValue.EmptyValue());
      }

      // An operator here means the user typed `field:` and moved on: empty value, leave the operator.
      switch (t.type()) {
        case OR, AND, NOT, MINUS, RPAREN, RBRACK, RBRACE -> {
          warnings.add("Field \"" + fieldName + "\" has no value.");
          return new AstNode.FieldNode(fieldName, new FieldValue.EmptyValue());
        }
        default -> {
          /* fall through */
        }
      }

      if (t.type() == QUOTE) {
        advance();
        return new AstNode.FieldNode(fieldName, new FieldValue.PhraseValue(t.value()));
      }

      if (t.type() == LBRACK || t.type() == LBRACE) {
        return parseRange(fieldName, fieldPos);
      }

      if (t.type() == IDENT) {
        advance();
        if (peek() != null && peek().type() == STAR) {
          advance();
          return new AstNode.FieldNode(fieldName, new FieldValue.PrefixValue(t.value()));
        }
        return new AstNode.FieldNode(fieldName, new FieldValue.ExactValue(t.value()));
      }

      if (t.type() == STAR) {
        advance();
        return new AstNode.FieldNode(fieldName, new FieldValue.PrefixValue(""));
      }

      // Structural token we don't consume: leave it in place, return EmptyValue.
      warnings.add("Field \"" + fieldName + "\" has no value.");
      return new AstNode.FieldNode(fieldName, new FieldValue.EmptyValue());
    }

    private AstNode parseRange(String fieldName, int fieldPos) {
      Token open = advance();
      boolean loInclusive = open.type() == LBRACK;

      Bound loBound = readRangeBound();
      boolean hasTo = false;
      if (peek() != null && peek().type() == TO) {
        advance();
        hasTo = true;
      }
      Bound hiBound = readRangeBound();

      boolean hasCloser = false;
      boolean hiInclusive = loInclusive;
      if (peek() != null && (peek().type() == RBRACK || peek().type() == RBRACE)) {
        Token close = advance();
        hasCloser = true;
        hiInclusive = close.type() == RBRACK;
      }

      // Well-formed iff TO plus a token on each side. `*` is a null bound but still "present".
      if (!hasTo || !loBound.present || !hiBound.present) {
        warnings.add("Malformed range for field \"" + fieldName + "\" at position " + fieldPos
            + "; converted to bare terms.");
        if (!hasCloser) {
          warnings.add("Unclosed range bracket at position " + open.position() + ".");
        }
        return rangeAsBareTerms(fieldName, loBound.value, hiBound.value);
      }

      if (!hasCloser) {
        warnings.add("Unclosed range bracket at position " + open.position() + ".");
      }

      return new AstNode.FieldNode(fieldName,
          new FieldValue.RangeValue(loBound.value, hiBound.value, loInclusive, hiInclusive));
    }

    private record Bound(String value, boolean present)
    {
      static final Bound MISSING = new Bound(null, false);

      static Bound star() {
        return new Bound(null, true);
      }

      static Bound of(String v) {
        return new Bound(v, true);
      }
    }

    private Bound readRangeBound() {
      Token t = peek();
      if (t == null)
        return Bound.MISSING;
      return switch (t.type()) {
        case STAR -> {
          advance();
          yield Bound.star();
        }
        case IDENT -> {
          advance();
          yield Bound.of(t.value());
        }
        case QUOTE -> {
          advance();
          yield Bound.of(t.value());
        }
        default -> Bound.MISSING;
      };
    }

    private AstNode rangeAsBareTerms(String fieldName, String lo, String hi) {
      List<AstNode> parts = new ArrayList<>();
      parts.add(new AstNode.TermNode(fieldName));
      if (lo != null)
        parts.add(new AstNode.TermNode(lo));
      if (hi != null)
        parts.add(new AstNode.TermNode(hi));
      if (parts.size() == 1) {
        return parts.get(0);
      }
      return new AstNode.AndNode(parts);
    }

    /**
     * Consume tokens until the paren opened just before this call is balanced (or input ends),
     * without recursing. Assumes the opening LPAREN has already been advanced past.
     */
    private void skipBalancedGroup() {
      int open = 1;
      while (open > 0) {
        Token t = advance();
        if (t == null) {
          break;
        }
        if (t.type() == LPAREN) {
          open++;
        }
        else if (t.type() == RPAREN) {
          open--;
        }
      }
    }

    private Token peek() {
      return pos < tokens.size() ? tokens.get(pos) : null;
    }

    private Token advance() {
      return pos < tokens.size() ? tokens.get(pos++) : null;
    }
  }
}
