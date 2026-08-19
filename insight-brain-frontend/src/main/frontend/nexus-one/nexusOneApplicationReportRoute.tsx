/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import ReportPage from 'MainRoot/applicationReport/ReportPage';
import { ClassicComponentMount } from 'MainRoot/nexus-one/ClassicComponentMount';

/**
 * Nexus One embed for Classic {@code applicationReport.policy} (CLM-41538).
 *
 * ReportPage reads {@code publicId} / {@code scanId} from Redux router params
 * (synced by the UI-Router listener), so this wrapper only mounts the page
 * inside {@link ClassicComponentMount} — not {@link mountClassicComponent},
 * which drops router params.
 *
 * {@code applicationReport/route} is also registered in {@link routes.tsx} so
 * in-report navigation (component details, raw data, etc.) works within the
 * Nexus One shell after landing on this embed state.
 */
export function NexusOneApplicationReportRoute(): JSX.Element {
  return (
    <ClassicComponentMount>
      <ReportPage />
    </ClassicComponentMount>
  );
}
