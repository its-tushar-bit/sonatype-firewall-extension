/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration;

import java.io.IOException;
import java.io.UncheckedIOException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.97
 */
@Entity
@Table(name = "firewall_ignore_patterns")
public class FirewallIgnorePatterns
    implements HasStringId
{
  @Id
  @Column(name = "firewall_ignore_patterns_id")
  private String id;

  @Column(name = "firewall_ignore_patterns_json")
  private String firewallIgnorePatternsJson;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public FirewallIgnorePatterns() {
  }

  public FirewallIgnorePatterns(com.sonatype.clm.dto.model.component.FirewallIgnorePatterns firewallIgnorePatterns) {
    setFirewallIgnorePatterns(firewallIgnorePatterns);
  }

  public com.sonatype.clm.dto.model.component.FirewallIgnorePatterns getFirewallIgnorePatterns() {
    if (firewallIgnorePatternsJson == null) {
      return null;
    }
    try {
      return JsonUtils
          .parse(firewallIgnorePatternsJson, com.sonatype.clm.dto.model.component.FirewallIgnorePatterns.class);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public void setFirewallIgnorePatterns(
      com.sonatype.clm.dto.model.component.FirewallIgnorePatterns firewallIgnorePatterns)
  {
    if (firewallIgnorePatterns == null) {
      firewallIgnorePatternsJson = null;
    }
    else {
      firewallIgnorePatternsJson = JsonUtils.writeUnformatted(firewallIgnorePatterns);
    }
  }

  public String getFirewallIgnorePatternsJson() {
    return firewallIgnorePatternsJson;
  }
}
