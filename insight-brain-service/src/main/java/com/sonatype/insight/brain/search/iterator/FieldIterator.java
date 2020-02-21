/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.iterator;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

import org.apache.lucene.search.suggest.InputIterator;
import org.apache.lucene.util.BytesRef;

public class FieldIterator
    implements InputIterator
{
  private final Iterator<String> fieldIterator;

  public FieldIterator(Iterator<String> fieldIterator) {
    this.fieldIterator = fieldIterator;
  }

  @Override
  public boolean hasContexts() {
    return false;
  }

  @Override
  public boolean hasPayloads() {
    return false;
  }

  @Override
  public BytesRef next() {
    if (fieldIterator.hasNext()) {
      return new BytesRef(fieldIterator.next().getBytes(StandardCharsets.UTF_8));
    }
    else {
      return null;
    }
  }

  @Override
  public BytesRef payload() {
    return null;
  }

  @Override
  public Set<BytesRef> contexts() {
    return null;
  }

  @Override
  public long weight() {
    return 0;
  }
}

