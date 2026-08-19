/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.sonatype.insight.brain.search.global.catalog.GlobalSearchResultsCatalogClient;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * One row of a {@link ResultsResponse}, regardless of which tab it was emitted on.
 *
 * <p>
 * {@code type} is the row's entity type (matches the {@link Tab} discriminator for IQ-local rows, or a
 * catalog entity type for catalog-sourced rows). {@code source} is the source discriminator tag (see
 * {@link SearchSource}): {@code "catalog"} for catalog rows, {@code "local"} for IQ-local rows including
 * fall-through {@code NON_VULNERABLE_COMPONENT} / {@code SECURITY_VULNERABILITY} rows when the catalog
 * source is unavailable.
 *
 * <p>
 * {@code fields} is an open per-row bag of entity-appropriate properties (license, latestStable, maxCvss,
 * cvssSeverity, kev, epss, policyEvaluationStage, applicationPublicId, etc.) populated by either the
 * {@link GlobalSearchResultsIqLocalClient} row builder or {@link GlobalSearchResultsCatalogClient}.
 */
@JsonPropertyOrder({"type", "source", "id", "title", "subtitle", "fields", "href"})
@JsonInclude(Include.NON_NULL)
public final class ResultRow
{
  private final String type;

  private final String source;

  private final String id;

  private final String title;

  private final String subtitle;

  private final Map<String, Object> fields;

  private final String href;

  public ResultRow(
      String type,
      String source,
      String id,
      String title,
      String subtitle,
      Map<String, Object> fields,
      String href)
  {
    this.type = Objects.requireNonNull(type, "type");
    this.source = Objects.requireNonNull(source, "source");
    this.id = Objects.requireNonNull(id, "id");
    this.title = Objects.requireNonNull(title, "title");
    this.subtitle = subtitle;
    if (fields == null || fields.isEmpty()) {
      this.fields = Map.of();
    }
    else {
      // Match the Builder: drop entries with null values, keep ordering.
      Map<String, Object> filtered = new LinkedHashMap<>();
      fields.forEach((k, v) -> {
        if (v != null) {
          filtered.put(k, v);
        }
      });
      this.fields = filtered.isEmpty() ? Map.of() : Map.copyOf(filtered);
    }
    this.href = href;
  }

  public String getType() {
    return type;
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
    private String type;

    private String source;

    private String id;

    private String title;

    private String subtitle;

    private final Map<String, Object> fields = new LinkedHashMap<>();

    private String href;

    public Builder type(String type) {
      this.type = type;
      return this;
    }

    public Builder source(String source) {
      this.source = source;
      return this;
    }

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder title(String title) {
      this.title = title;
      return this;
    }

    public Builder subtitle(String subtitle) {
      this.subtitle = subtitle;
      return this;
    }

    /**
     * Set a single field. A null value is a no-op; if you need to intentionally skip a key, use
     * {@link #fields(Map)} with an absent key instead of passing null.
     */
    public Builder field(String name, Object value) {
      if (value != null) {
        fields.put(name, value);
      }
      return this;
    }

    public Builder fields(Map<String, Object> values) {
      if (values != null) {
        values.forEach((k, v) -> {
          if (v != null) {
            fields.put(k, v);
          }
        });
      }
      return this;
    }

    public Builder href(String href) {
      this.href = href;
      return this;
    }

    public ResultRow build() {
      return new ResultRow(type, source, id, title, subtitle, fields, href);
    }
  }
}
