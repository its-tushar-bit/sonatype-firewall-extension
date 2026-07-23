/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.security.AuthenticationLoggingFilter;

import ch.qos.logback.access.common.spi.IAccessEvent;
import io.dropwizard.logging.json.AccessAttribute;
import io.dropwizard.logging.json.layout.AccessJsonLayout;
import io.dropwizard.logging.json.layout.JsonFormatter;
import io.dropwizard.logging.json.layout.TimestampFormatter;

/**
 * An {@link AccessJsonLayout} that renders the authenticated username under the canonical {@code remoteUser}
 * JSON field. The stock layout populates {@code remoteUser} from {@link IAccessEvent#getRemoteUser()}, whose
 * backing {@code ch.qos.logback.access.jetty.RequestWrapper.getRemoteUser()} is stubbed to {@code null}, so the
 * field is omitted for authenticated requests. This reads the username from the
 * {@link AuthenticationLoggingFilter#REQUEST_LOG_REMOTE_USER_ATTRIBUTE} request attribute instead (the same
 * attribute the pattern path renders via {@code %reqAttribute}). {@code IAccessEvent.getAttribute} returns the
 * value's string when set and {@code "-"} when absent, so anonymous requests render {@code "-"}, consistent with
 * the classic and pattern paths. CLM-42654.
 * <p>
 * The resulting JSON-shape change (the {@code remoteUser} key is now always present, and cannot be suppressed via
 * {@code includes}) is documented for downstream log consumers in {@code doc/devdocs/request-log-remote-user.md}.
 */
public class RemoteUserAccessJsonLayout
    extends AccessJsonLayout
{
  public RemoteUserAccessJsonLayout(
      final JsonFormatter jsonFormatter,
      final TimestampFormatter timestampFormatter,
      final Set<AccessAttribute> includes,
      final Map<String, String> customFieldNames,
      final Map<String, Object> additionalFields)
  {
    super(jsonFormatter, timestampFormatter, includes, customFieldNames, additionalFields);
  }

  @Override
  protected Map<String, Object> toJsonMap(final IAccessEvent event) {
    // Defensive copy so we never rely on the stock layout's map being mutable, then always populate the
    // canonical remoteUser field. getAttribute returns the username when set, or "-" when absent (anonymous).
    // remoteUser is set even if an operator excluded it via includes - surfacing the username is the point.
    Map<String, Object> jsonMap = new LinkedHashMap<>(super.toJsonMap(event));
    jsonMap.put("remoteUser", event.getAttribute(AuthenticationLoggingFilter.REQUEST_LOG_REMOTE_USER_ATTRIBUTE));
    return jsonMap;
  }
}
