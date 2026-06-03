/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import * as PropTypes from 'prop-types';
import { NxH3, NxTextLink, NxTile } from '@sonatype/react-shared-components';

const DEFAULT_VISIBLE = 5;

export default function TopConsumingApps({ topApps }) {
  const [expanded, setExpanded] = useState(false);

  if (!topApps || !topApps.apps || topApps.apps.length === 0) {
    return null;
  }

  const { apps, totalApps, totalConsumed } = topApps;
  const visibleApps = expanded ? apps : apps.slice(0, DEFAULT_VISIBLE);
  const remainingCount = apps.length - DEFAULT_VISIBLE;

  return (
    <NxTile className="iq-usage-top-apps-tile">
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH3>Top Consuming Applications</NxH3>
        </NxTile.HeaderTitle>
      </NxTile.Header>
      <NxTile.Content>
        <div className="iq-usage-top-apps__subtitle">{totalApps} applications evaluated this period</div>
        <div className="iq-usage-top-apps__list">
          {visibleApps.map((app) => {
            const percent = totalConsumed > 0 ? Math.round((app.consumed / totalConsumed) * 100) : 0;
            const displayName = app.name ?? app.publicId ?? 'Deleted application';
            return (
              <div key={app.appId} className="iq-usage-top-apps__row">
                <span className="iq-usage-top-apps__name" title={displayName}>
                  {displayName}
                </span>
                <span className="iq-usage-top-apps__count">{app.consumed.toLocaleString()}</span>
                <span className="iq-usage-top-apps__percent">{percent}%</span>
              </div>
            );
          })}
        </div>
        {remainingCount > 0 && (
          <NxTextLink onClick={() => setExpanded(!expanded)}>
            {expanded ? 'Show Less' : `Show More (${remainingCount})`}
          </NxTextLink>
        )}
      </NxTile.Content>
    </NxTile>
  );
}

TopConsumingApps.propTypes = {
  topApps: PropTypes.shape({
    apps: PropTypes.arrayOf(
      PropTypes.shape({
        appId: PropTypes.string.isRequired,
        publicId: PropTypes.string,
        name: PropTypes.string,
        consumed: PropTypes.number.isRequired,
      })
    ),
    totalApps: PropTypes.number,
    totalConsumed: PropTypes.number,
  }),
};
