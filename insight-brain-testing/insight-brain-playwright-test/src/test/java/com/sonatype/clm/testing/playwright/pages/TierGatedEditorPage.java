/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

/**
 * Shared shape for the three Pro-tier-gated editor pages (Component Label, License Threat Group,
 * Application Category). Lets {@link TierGatedEditorAssertions} share gating checks across the
 * sibling {@code *PageAssertions} classes.
 */
public interface TierGatedEditorPage
{
  /** "Add a {entity}" button on the parent tile. */
  Locator addEntityButton();

  /** Pro-tier read-only editor container. */
  Locator readOnlyEntityView();

  /** Entity-name display inside the read-only view. */
  Locator readOnlyEntityName();

  /** Submit / Update button on the editable form. */
  Locator submitButton();

  /** Delete button on the editable form. */
  Locator deleteButton();
}
