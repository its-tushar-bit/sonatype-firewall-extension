/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { UIView } from '@uirouter/react';
import { useCurrentStateAndParams } from '@uirouter/react';
import DashboardFilter from './filter/dashboardFilter/DashboardFilter';

/**
 * Dashboard layout component that wraps child routes.
 * - Wraps content in <main class="nx-page-main"> except for dashboard.component route
 * - Always includes the dashboard-filter component
 */
export default function DashboardLayout() {
  const { state } = useCurrentStateAndParams();
  const isComponentRoute = state?.name === 'dashboard.component';

  return (
    <>
      {isComponentRoute ? (
        <UIView />
      ) : (
        <main id="dashboard-container" className="nx-page-main">
          <UIView />
        </main>
      )}
      <DashboardFilter />
    </>
  );
}
