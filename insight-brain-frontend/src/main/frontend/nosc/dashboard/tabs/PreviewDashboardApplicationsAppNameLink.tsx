/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Link } from '@radix-ui/themes';

/**
 * Preview-side link for an application name. Targets the Nexus One application-detail route
 * (`nexusOneApplicationsDetail`, url `/applications/{publicId}`) — a Coming-Soon stub today,
 * replaced by the real entity view in the Applications module PR (#16043 / CLM-39709). The hash
 * has NO `/preview/` prefix: the routes are registered under `/applications/{publicId}` (see
 * `nexus-one/routes.tsx`), so a `/preview/...` href fell through to the default `/dashboard`.
 *
 * Pulled out of the table component so the link contract (URL encoding, focus, test-id) is
 * verifiable in isolation.
 */
export default function PreviewDashboardApplicationsAppNameLink({
  publicId,
  name,
}: {
  publicId: string;
  name: string;
}): JSX.Element {
  const href = `#/applications/${encodeURIComponent(publicId)}`;
  return (
    <Link
      href={href}
      underline="hover"
      data-testid="nosc-dashboard-app-link"
    >
      {name}
    </Link>
  );
}
