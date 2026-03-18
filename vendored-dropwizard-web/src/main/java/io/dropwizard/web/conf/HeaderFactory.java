/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package io.dropwizard.web.conf;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Map;

public abstract class HeaderFactory
{
  @JsonProperty
  private boolean enabled = false;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Map<String, String> build() {
    if (enabled) {
      return buildHeaders();
    }
    else {
      return Collections.emptyMap();
    }
  }

  protected abstract Map<String, String> buildHeaders();
}
