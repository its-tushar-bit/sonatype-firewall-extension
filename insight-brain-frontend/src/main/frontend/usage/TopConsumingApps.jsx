/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import * as PropTypes from 'prop-types';
import { NxButton, NxH2, NxTile } from '@sonatype/react-shared-components';

const DEFAULT_VISIBLE = 10;

export default function TopConsumingApps({ topApps }) {
  const [expanded, setExpanded] = useState(false);

  if (!topApps || !topApps.apps || topApps.apps.length === 0) {
    return null;
  }

  const { apps, totalConsumed } = topApps;
  const visibleApps = expanded ? apps : apps.slice(0, DEFAULT_VISIBLE);
  // Guard: when apps.length < DEFAULT_VISIBLE, this would be negative. The render
  // is gated by `> 0` below so a negative value never paints, but Math.max keeps
  // the variable's intent obvious and prevents a future regression if the gate
  // is ever weakened.
  const remainingCount = Math.max(0, apps.length - DEFAULT_VISIBLE);

  return (
    <NxTile className="iq-usage-top-apps-tile">
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2>Top 10 Consuming Applications</NxH2>
        </NxTile.HeaderTitle>
      </NxTile.Header>
      <NxTile.Content>
        <ul className="iq-usage-top-apps__list">
          {visibleApps.map((app) => {
            // Clamp at 100: when an app's consumed count drifts above totalConsumed
            // (data race, partial aggregation, rounding), the raw ratio can exceed 100,
            // which would produce aria-valuenow > aria-valuemax and a malformed
            // progressbar in the accessibility tree. WAI-ARIA 1.2 requires valuenow
            // within [valuemin, valuemax].
            const percent = totalConsumed > 0 ? Math.min(100, Math.round((app.consumed / totalConsumed) * 100)) : 0;
            const displayName = app.name ?? app.publicId ?? 'Deleted application';
            return (
              <li key={app.appId} className="iq-usage-top-apps__row">
                <div className="iq-usage-top-apps__row-top">
                  <span className="iq-usage-top-apps__name" title={displayName}>
                    {displayName}
                  </span>
                  <span className="iq-usage-top-apps__count">{app.consumed.toLocaleString('en-US')}</span>
                </div>
                <div
                  className="iq-usage-top-apps__bar-track"
                  role="progressbar"
                  aria-label={`${displayName} consumption`}
                  aria-valuenow={percent}
                  aria-valuemin={0}
                  aria-valuemax={100}
                  aria-valuetext={`${percent}% of total`}
                >
                  <div className="iq-usage-top-apps__bar-fill" style={{ width: `${percent}%` }} aria-hidden="true" />
                </div>
                <span className="iq-usage-top-apps__percent">{percent}%</span>
              </li>
            );
          })}
        </ul>
        {remainingCount > 0 && (
          // Use NxButton (variant="tertiary") for this in-page expand/collapse
          // toggle rather than NxTextLink, which renders <a> and reads as
          // navigation to screen readers / keyboard users. The toggle isn't
          // navigation — it flips local state — so a button gives correct
          // role, Space-key activation, and aria-expanded semantics.
          <NxButton
            variant="tertiary"
            className="iq-usage-top-apps__show-more"
            onClick={() => setExpanded(!expanded)}
            aria-expanded={expanded}
          >
            {expanded ? 'Show Less' : `Show More (${remainingCount})`}
          </NxButton>
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
