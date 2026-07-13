/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import ApplicationsPage from 'MainRoot/nosc/applications/ApplicationsPage';
import { useApplicationsList } from 'MainRoot/nosc/applications/useApplicationsList';

import '@radix-ui/themes/styles.css';

/**
 * Preview Applications list page (CLM-42223 / CLM-42224).
 *
 * Martha V1 layout: filter sidebar + evaluation card grid backed by
 * POST /rest/dashboard/applications/list inside the Nexus One Preview shell.
 */
export default function ApplicationsList() {
  const {
    applications,
    facets,
    loading,
    error,
    info,
    retry,
    total,
    page,
    pageSize,
    hasNextPage,
    setPage,
  } = useApplicationsList();

  return (
    <ApplicationsPage
      applications={applications}
      facets={facets}
      loading={loading}
      error={error}
      info={info}
      onRetry={retry}
      totalCount={total}
      page={page + 1}
      pageSize={pageSize}
      hasNextPage={hasNextPage}
      onPageChange={(nextPage) => setPage(nextPage - 1)}
    />
  );
}
