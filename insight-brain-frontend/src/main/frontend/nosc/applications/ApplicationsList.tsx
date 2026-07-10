/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import ApplicationsPage from 'MainRoot/nosc/applications/ApplicationsPage';
// Mock imports are intentional for the CLM-42223 Martha layout shell until
// CLM-42224 wires POST /rest/dashboard/applications/list (PREVIEW_NEXUS_ONE_UI).
import {
  MOCK_APPLICATION_RISK_SCORES,
  MOCK_APPLICATIONS_FILTER_FACETS,
} from 'MainRoot/nosc/applications/mockApplicationsListData';

import '@radix-ui/themes/styles.css';

/**
 * Preview Applications list page (CLM-42223).
 *
 * Martha V1 layout: filter sidebar + evaluation card grid inside the Nexus One
 * Preview shell. Uses mocked {@link ApplicationRiskScoreDTO}-shaped rows until
 * POST /rest/dashboard/applications/list merges (CLM-42228).
 *
 * PREVIEW_NEXUS_ONE_UI gates route registration; this component assumes the
 * Preview shell is active.
 */
export default function ApplicationsList() {
  return (
    <ApplicationsPage
      applications={MOCK_APPLICATION_RISK_SCORES}
      facets={MOCK_APPLICATIONS_FILTER_FACETS}
    />
  );
}
