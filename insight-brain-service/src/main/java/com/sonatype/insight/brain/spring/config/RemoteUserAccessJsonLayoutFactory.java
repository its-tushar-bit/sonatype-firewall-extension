/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import java.util.TimeZone;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.LayoutBase;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.dropwizard.logging.json.AccessJsonLayoutBaseFactory;

/**
 * Discoverable layout factory for {@link RemoteUserAccessJsonLayout}, registered under the internal layout type
 * {@value #TYPE_NAME}. {@code RequestLoggingConfiguration} rewrites operator-configured {@code access-json}
 * layouts to this type so the authenticated username renders under {@code remoteUser} - operators keep
 * configuring {@code access-json} unchanged. Extends {@link AccessJsonLayoutBaseFactory}, so every
 * {@code access-json} option (includes, requestHeaders, responseHeaders, requestAttributes, customFieldNames,
 * additionalFields) deserializes and is honoured identically. CLM-42654.
 */
@JsonTypeName(RemoteUserAccessJsonLayoutFactory.TYPE_NAME)
public class RemoteUserAccessJsonLayoutFactory
    extends AccessJsonLayoutBaseFactory
{
  public static final String TYPE_NAME = "iq-access-json";

  @Override
  public LayoutBase<IAccessEvent> build(final LoggerContext context, final TimeZone timeZone) {
    RemoteUserAccessJsonLayout layout = new RemoteUserAccessJsonLayout(
        createDropwizardJsonFormatter(),
        createTimestampFormatter(timeZone),
        getIncludes(),
        getCustomFieldNames(),
        getAdditionalFields());
    layout.setContext(context);
    layout.setRequestHeaders(getRequestHeaders());
    layout.setResponseHeaders(getResponseHeaders());
    layout.setRequestAttributes(getRequestAttributes());
    return layout;
  }
}
