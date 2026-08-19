/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global.parser;

import java.util.List;

/**
 * Node in the query AST. All permitted subtypes are records so callers can
 * pattern-match without needing custom accessors.
 */
public sealed interface AstNode
    permits AstNode.TermNode,
    AstNode.PhraseNode,
    AstNode.FieldNode,
    AstNode.AndNode,
    AstNode.OrNode,
    AstNode.NotNode,
    AstNode.EmptyNode
{
  record TermNode(String value)
      implements AstNode
  {
  }

  record PhraseNode(String value)
      implements AstNode
  {
  }

  record FieldNode(String field, FieldValue value)
      implements AstNode
  {
  }

  record AndNode(List<AstNode> children)
      implements AstNode
  {
    public AndNode {
      children = List.copyOf(children);
    }
  }

  record OrNode(List<AstNode> children)
      implements AstNode
  {
    public OrNode {
      children = List.copyOf(children);
    }
  }

  record NotNode(AstNode child)
      implements AstNode
  {
  }

  record EmptyNode()
      implements AstNode
  {
  }
}
