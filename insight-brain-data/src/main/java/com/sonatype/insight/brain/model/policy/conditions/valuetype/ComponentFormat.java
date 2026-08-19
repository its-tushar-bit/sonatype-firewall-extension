/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions.valuetype;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

/**
 * Specifies the component formats supported for policy condition types.
 *
 * It is serialized into json as key-value pairs (like: {"id":"maven","name":"maven"}), as expected by the policy UI.
 */
public class ComponentFormat
{
  private static final List<ComponentFormat> all;

  private static final Set<String> allAsStrings;

  static {
    Set<String> formats = new TreeSet<>(ComponentIdentifier.getAllFormats());
    // CONTAINER may contain ":" in coordinates, which is a coordinate separator.
    formats.remove(ComponentIdentifier.FORMAT_CONTAINER);
    // CPE is identified as GENERIC.
    formats.remove(ComponentIdentifier.FORMAT_CPE);
    // GENERIC has undetermined coordinates.
    formats.remove(ComponentIdentifier.FORMAT_GENERIC);
    // HUGGINGFACE_REPO is used only internally by HDS.
    formats.remove(ComponentIdentifier.FORMAT_HUGGINGFACE_REPO);
    // IAC is obsolete.
    formats.remove(ComponentIdentifier.FORMAT_IAC);
    // TERRAFORM is obsolete.
    formats.remove(ComponentIdentifier.FORMAT_TERRAFORM);

    allAsStrings = Collections.unmodifiableSet(formats);

    all = Collections.unmodifiableList(formats.stream().map(format -> new ComponentFormat(format, format)).toList());
  }

  private final String id;

  private final String name;

  private ComponentFormat(String id, String name) {
    this.id = id;
    this.name = name;
  }

  public static List<ComponentFormat> getAll() {
    return all;
  }

  public static Set<String> getAllAsStrings() {
    return allAsStrings;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return id;
  }
}
