/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import ViolationPageContainer from 'MainRoot/violation/ViolationPageContainer';
import { ClassicComponentMount } from 'MainRoot/nexus-one/ClassicComponentMount';

/**
 * Nexus One embed for the Classic {@code sidebarView.violation} detail page
 * (CLM-42256).
 *
 * ViolationPageContainer is Redux-connected and reads the violation id from
 * router params (synced by the UI-Router listener), so this wrapper only mounts
 * the page inside {@link ClassicComponentMount} — not {@link mountClassicComponent},
 * which drops router params.
 *
 * {@code violation/route} is also side-imported in {@link routes.tsx} so
 * in-detail navigation (transitive violations, waiver flows) resolves inside the
 * Nexus One bundle after landing on this embed state.
 */
export function NexusOneViolationDetailRoute(): JSX.Element {
  return (
    <ClassicComponentMount>
      <ViolationPageContainer />
    </ClassicComponentMount>
  );
}
