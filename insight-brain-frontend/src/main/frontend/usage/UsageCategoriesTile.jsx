/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxH2, NxTile } from '@sonatype/react-shared-components';

import { formatNumber } from './usageFormatters';

export const CATEGORY_ORDER = [
  'APIs',
  'App Scan + Re-evaluate',
  'Component Details',
  'Continuous Monitoring',
  'Reachability Analysis',
  'Version Recommendations',
];

export default function UsageCategoriesTile({ summary }) {
  if (!summary || !summary.activityBreakdown) return null;

  const { activityBreakdown } = summary;

  // Filter to non-zero categories first so we never render a role="list"
  // landmark with zero items (which screen readers announce as "list, 0 items").
  const visibleCategories = CATEGORY_ORDER.filter((key) => (activityBreakdown[key] ?? 0) > 0);
  if (visibleCategories.length === 0) return null;

  return (
    <NxTile className="iq-usage-categories-tile">
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2>Usage Categories</NxH2>
        </NxTile.HeaderTitle>
      </NxTile.Header>
      <NxTile.Content>
        <div className="iq-usage-categories-tile__grid" role="list" aria-label="Usage categories">
          {visibleCategories.map((key) => (
            <div key={key} role="listitem" className="iq-usage-categories-tile__category">
              <span className="iq-usage-categories-tile__category-label">{key}</span>
              <span className="iq-usage-categories-tile__category-count">{formatNumber(activityBreakdown[key])}</span>
            </div>
          ))}
        </div>
      </NxTile.Content>
    </NxTile>
  );
}

UsageCategoriesTile.propTypes = {
  summary: PropTypes.shape({
    activityBreakdown: PropTypes.object,
  }),
};
