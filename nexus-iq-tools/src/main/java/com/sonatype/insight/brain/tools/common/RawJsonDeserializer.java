/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.common;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

class RawJsonDeserializer
    extends JsonDeserializer<String>
{
  @Override
  public String deserialize(JsonParser jp, DeserializationContext context) throws IOException {
    TreeNode tree = jp.getCodec().readTree(jp);
    return tree.toString();
  }
}
