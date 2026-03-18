/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.jira;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Atlassian Document Format Node used to defined content for the rich test fields in Jira Cloud.
 *
 * @since 1.95.0
 */
public class ADFNode
{
  /**
   * Defines the type of block node such as paragraph, table, and alike.
   * Required: true
   */
  private Type type;

  /**
   * Defines the version of ADF used in this representation
   * Required: for the root node; otherwise not applicable
   */
  @JsonInclude(Include.NON_NULL)
  private Integer version;

  /**
   * An list containing inline and block nodes that define the content of a section of the document.
   * Required: in block nodes only; not applicable in inline nodes
   */
  @JsonInclude(Include.NON_NULL)
  private List<ADFNode> content;

  /**
   * Required: in text nodes only; otherwise not applicable
   */
  @JsonInclude(Include.NON_NULL)
  private String text;

  /**
   * Defines text decoration or formatting.
   * Required: no; applicable to text nodes
   */
  @JsonInclude(Include.NON_NULL)
  public List<ADFNode> marks;

  /**
   * Further information defining attributes of the block such as the language represented in a block of code.
   * Required: no
   */
  @JsonInclude(Include.NON_NULL)
  private Map<String, Object> attrs;

  public Type getType() {
    return type;
  }

  public Integer getVersion() {
    return version;
  }

  public List<ADFNode> getContent() {
    return content;
  }

  public String getText() {
    return text;
  }

  public Map<String, Object> getAttrs() {
    return attrs;
  }

  public List<ADFNode> getMarks() {
    return marks;
  }

  public ADFNode setType(final Type type) {
    this.type = type;
    return this;
  }

  public ADFNode setVersion(final Integer version) {
    this.version = version;
    return this;
  }

  public ADFNode setText(final String text) {
    this.text = text;
    return this;
  }

  public ADFNode addContent(final ADFNode node) {
    if (content == null) {
      content = new ArrayList<>();
    }
    content.add(node);
    return this;
  }

  public ADFNode addAttribute(final String key, final Object value) {
    if (attrs == null) {
      attrs = new HashMap<>();
    }
    attrs.put(key, value);
    return this;
  }

  public ADFNode addMarks(final ADFNode node) {
    if (node != null) {
      if (marks == null) {
        marks = new ArrayList<>();
      }
      marks.add(node);
    }
    return this;
  }

  @Override
  public String toString() {
    return "ADFNode{" +
        "type='" + type + '\'' +
        ", content=" + content +
        ", text=" + text +
        ", marks=" + marks +
        ", version=" + version +
        ", attrs=" + attrs +
        '}';
  }

  enum Type
  {
    // The top-level block nodes include:
    doc,
    blockquote,
    bulletList,
    codeBlock,
    heading,
    mediaGroup,
    mediaSingle,
    orderedList,
    panel,
    paragraph,
    rule,
    table,
    // The child block nodes include:
    listItem,
    media,
    tableCell,
    tableHeader,
    tableRow,
    // The inline nodes include:
    emoji,
    hardBreak,
    inlineCard,
    mention,
    text,
    // Mark types:
    code,
    em,
    link,
    strike,
    strong,
    subsup,
    textColor,
    underline
  }
}
