/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.render.model;

import java.io.IOException;

import com.sonatype.insight.brain.api.v2.service.ConfigurationProperty;
import com.sonatype.insight.brain.git.render.model.MDImages.MDImageSerializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import static com.sonatype.insight.brain.api.v2.service.ConfigurationProperty.getConfigurationPropertiesByName;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.CDN_URL;

@JsonSerialize(using = MDImageSerializer.class)
public enum MDImages
{
  DIRECT_DEP_LOGO("Direct Dependency", "d-logo.png"),
  TRANSITIVE_DEP_LOGO("Transitive Dependency", "t-logo.png"),
  CRITICAL_INDICATOR("Critical", "red-bar.png"),
  SEVERE_INDICATOR("Severe", "orange-bar.png"),
  MODERATE_INDICATOR("Moderate", "yellow-bar.png"),
  LOW_INDICATOR("Low", "dark-blue-bar.png"),
  UNKNOWN_INDICATOR("", "light-blue-bar.png"),
  SONATYPE_FAST_TRACK_TAG("Sonatype Fast Track", "sonatype-fast-track.svg"),
  SONATYPE_DEEP_DIVE_TAG("Sonatype Deep Dive", "sonatype-deep-dive.svg");

  private static final String CDN_SCM_URL = resolveCdnBaseUrl() + "iq-for-scm/1.0/";

  private final String title;

  private final String alt;

  private final String logoFilename;

  MDImages(final String title, final String logoFilename) {
    this(title, title, logoFilename);
  }

  MDImages(final String title, final String alt, final String logoFilename) {
    this.title = title;
    this.alt = alt;
    this.logoFilename = logoFilename;

  }

  public String getAlt() {
    return alt;
  }

  public String getSrc() {
    return CDN_SCM_URL + logoFilename;
  }

  public String getTitle() {
    return title;
  }

  static class MDImageSerializer
      extends StdSerializer<MDImages>
  {
    public MDImageSerializer() {
      this(null);
    }

    public MDImageSerializer(final Class<MDImages> t) {
      super(t);
    }

    @Override
    public void serialize(
        final MDImages mdImage,
        final JsonGenerator jgen,
        final SerializerProvider serializerProvider) throws IOException
    {
      jgen.writeStartObject();
      jgen.writeStringField("title", mdImage.getTitle());
      jgen.writeStringField("alt", mdImage.getAlt());
      jgen.writeStringField("src", mdImage.getSrc());
      jgen.writeEndObject();
    }
  }

  private static String resolveCdnBaseUrl() {
    final ConfigurationProperty prop = getConfigurationPropertiesByName().get(CDN_URL);
    return prop.getValueToString().apply(null, prop.getStringToValue().apply(null, null));
  }
}
