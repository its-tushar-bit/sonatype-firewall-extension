/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.search.lucene.LuceneComponents;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.IndexSearcher.TooManyClauses;
import org.apache.lucene.search.Query;

import static com.sonatype.insight.brain.search.index.AbstractSearchIndexClient.TOO_MANY_CLAUSES_EXCEPTION;

@Named
@Singleton
public class ConversionHelper
{
  private final LuceneComponents luceneComponents;

  @Inject
  public ConversionHelper(final LuceneComponents luceneComponents) {
    this.luceneComponents = luceneComponents;
  }

  public Query stringToQuery(final String query) {
    try {
      return luceneComponents.newQueryParser().apply(query);
    }
    catch (Exception e) {
      if (ExceptionUtils.getRootCause(e) instanceof TooManyClauses) {
        throw TOO_MANY_CLAUSES_EXCEPTION;
      }
      throw e;
    }
  }

  public Map<String, Object> documentToMap(final Document document) {
    Map<String, Object> map = new HashMap<>();
    for (IndexableField field : document.getFields()) {
      String name = field.name();
      Object value = field.stringValue();
      if (field.numericValue() != null) {
        value = field.numericValue();
      }
      if (map.containsKey(name)) {
        Object existing = map.get(name);
        if (existing instanceof List list) {
          list.add(value);
        }
        else {
          List<Object> list = new ArrayList<>();
          list.add(existing);
          list.add(value);
          map.put(name, list);
        }
      }
      else {
        map.put(name, value);
      }
    }
    return map;
  }

  public Document mapToDocument(final Map<String, Object> map) {
    Document document = new Document();
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      List<Field> fields = keyAndValueToFields(key, value);
      fields.forEach(document::add);
    }
    return document;
  }

  private List<Field> keyAndValueToFields(final String key, final Object value) {
    List<Field> fields = new ArrayList<>();
    if (value instanceof String stringValue) {
      fields.add(new TextField(key, stringValue, Store.YES));
    }
    else if (value instanceof Integer intValue) {
      fields.add(new IntPoint(key, intValue));
      fields.add(new StoredField(key, intValue));
    }
    else if (value instanceof Long longValue) {
      fields.add(new LongPoint(key, longValue));
      fields.add(new StoredField(key, longValue));
    }
    else if (value instanceof Float floatValue) {
      fields.add(new FloatPoint(key, floatValue));
      fields.add(new StoredField(key, floatValue));
    }
    else if (value instanceof Double doubleValue) {
      fields.add(new DoublePoint(key, doubleValue));
      fields.add(new StoredField(key, doubleValue));
    }
    else if (value instanceof List list) {
      for (Object v : list) {
        fields.addAll(keyAndValueToFields(key, v));
      }
    }
    else {
      throw new IllegalArgumentException("Unsupported value type " + value.getClass());
    }
    return fields;
  }
}
