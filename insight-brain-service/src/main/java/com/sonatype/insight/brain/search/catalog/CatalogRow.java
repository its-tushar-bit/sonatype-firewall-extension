/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.catalog;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"entityType", "source", "id", "title", "subtitle", "fields", "href"})
@JsonInclude(Include.NON_NULL)
public final class CatalogRow
{
  private final String entityType;

  private final String source;

  private final String id;

  private final String title;

  private final String subtitle;

  private final Map<String, Object> fields;

  private final String href;

  private CatalogRow(final Builder b) {
    this.entityType = Objects.requireNonNull(b.entityType, "entityType");
    this.source = Objects.requireNonNull(b.source, "source");
    this.id = Objects.requireNonNull(b.id, "id");
    this.title = b.title;
    this.subtitle = b.subtitle;
    this.fields = b.fields.isEmpty() ? Map.of() : Map.copyOf(b.fields);
    this.href = b.href;
  }

  public String getEntityType() {
    return entityType;
  }

  public String getSource() {
    return source;
  }

  public String getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getSubtitle() {
    return subtitle;
  }

  public Map<String, Object> getFields() {
    return fields;
  }

  public String getHref() {
    return href;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder
  {
    private String entityType;

    private String source;

    private String id;

    private String title;

    private String subtitle;

    private final Map<String, Object> fields = new LinkedHashMap<>();

    private String href;

    public Builder entityType(final String entityType) {
      this.entityType = entityType;
      return this;
    }

    public Builder source(final String source) {
      this.source = source;
      return this;
    }

    public Builder id(final String id) {
      this.id = id;
      return this;
    }

    public Builder title(final String title) {
      this.title = title;
      return this;
    }

    public Builder subtitle(final String subtitle) {
      this.subtitle = subtitle;
      return this;
    }

    /** A {@code null} value is a no-op; the key is omitted. */
    public Builder field(final String name, final Object value) {
      if (value != null) {
        fields.put(name, value);
      }
      return this;
    }

    public Builder href(final String href) {
      this.href = href;
      return this;
    }

    public CatalogRow build() {
      return new CatalogRow(this);
    }
  }
}
