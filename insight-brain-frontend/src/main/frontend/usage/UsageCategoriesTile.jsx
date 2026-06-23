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
  if (!summary) return null;

  // Always render all 6 canonical categories — even when a category has zero
  // consumption for the active period. Empty values appear as `0` so the
  // dashboard layout stays stable across period filter selections; previously
  // the whole tile vanished when every count was zero (e.g. selecting a
  // historical range with no API/IDE/etc. activity), which read as a broken
  // page rather than honest empty data. The role="list" landmark always
  // contains exactly 6 items in this branch.
  const activityBreakdown = summary.activityBreakdown ?? {};

  return (
    <NxTile className="iq-usage-categories-tile">
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2>Usage Categories</NxH2>
        </NxTile.HeaderTitle>
      </NxTile.Header>
      <NxTile.Content>
        <div className="iq-usage-categories-tile__grid" role="list" aria-label="Usage categories">
          {CATEGORY_ORDER.map((key) => (
            <div key={key} role="listitem" className="iq-usage-categories-tile__category">
              <span className="iq-usage-categories-tile__category-label">{key}</span>
              <span className="iq-usage-categories-tile__category-count">
                {formatNumber(activityBreakdown[key] ?? 0)}
              </span>
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
