/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxH2, NxTile } from '@sonatype/react-shared-components';
import classnames from 'classnames';
import moment from 'moment';

import { formatNumber } from './usageFormatters';

export default function MyUsageTile({ summary }) {
  if (!summary) return null;

  const { consumed, limit, percentUsed, remaining, resetDate } = summary;
  const hasLimit = limit !== null && limit !== undefined;
  const displayPct = hasLimit && typeof percentUsed === 'number' ? Math.floor(percentUsed * 10) / 10 : null;
  const progressBarValue = displayPct !== null ? Math.min(displayPct, 100) : 0;
  const isOverLimit = displayPct !== null && displayPct >= 100;
  const isStrictlyOverLimit = hasLimit && consumed > limit;

  return (
    <NxTile className="iq-my-usage-tile">
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2>My usage</NxH2>
        </NxTile.HeaderTitle>
      </NxTile.Header>
      <NxTile.Content>
        <div className="iq-my-usage-tile__summary">
          <div className="iq-my-usage-tile__value">
            <span className="iq-my-usage-tile__consumed">{formatNumber(consumed)}</span>
            {hasLimit && (
              <>
                <span className="iq-my-usage-tile__separator"> / </span>
                <span className="iq-my-usage-tile__limit">{formatNumber(limit)}</span>
              </>
            )}
            <span className="iq-my-usage-tile__value-suffix"> components evaluated</span>
          </div>
          {hasLimit && (
            <div className="iq-my-usage-tile__progress-row">
              <div
                className={classnames('iq-my-usage-tile__progress-track', {
                  'iq-my-usage-tile__progress-track--over': isOverLimit,
                })}
                role="progressbar"
                // aria-label conveys the consumed/limit context that's visually
                // shown above the bar; aria-valuetext announces the over-limit
                // state explicitly because aria-valuenow is clamped at 100 (so
                // a screen reader on the over-limit path would otherwise just
                // read "100%" with no indication of the overage).
                aria-label={`Usage progress: ${formatNumber(consumed)} of ${formatNumber(limit)} components evaluated`}
                aria-valuenow={Math.round(progressBarValue)}
                aria-valuemin={0}
                aria-valuemax={100}
                aria-valuetext={
                  displayPct === null
                    ? // Defensive path: backend should always supply percentUsed when limit is
                      // set, but if it omits it while consumed > limit, still announce the
                      // over-limit state so screen readers don't announce a misleading "0%".
                      isStrictlyOverLimit
                      ? 'Over limit'
                      : undefined
                    : isStrictlyOverLimit
                    ? `Over limit: ${displayPct.toFixed(1)}% of monthly limit`
                    : `${displayPct.toFixed(1)}% of monthly limit`
                }
              >
                <div
                  className={classnames('iq-my-usage-tile__progress-fill', {
                    'iq-my-usage-tile__progress-fill--over': isStrictlyOverLimit,
                  })}
                  style={{ width: `${progressBarValue}%` }}
                  aria-hidden="true"
                />
              </div>
              <span className="iq-my-usage-tile__progress-percent">
                {displayPct !== null ? `${displayPct.toFixed(1)}%` : '—'}
              </span>
            </div>
          )}
          {hasLimit && (
            <div className="iq-my-usage-tile__details">
              {isStrictlyOverLimit ? (
                <span className="iq-my-usage-tile__over-limit">Over limit by {formatNumber(consumed - limit)}</span>
              ) : isOverLimit ? (
                <span className="iq-my-usage-tile__over-limit">Limit reached</span>
              ) : (
                <span>{formatNumber(remaining)} remaining</span>
              )}
              {resetDate && (
                <span className="iq-my-usage-tile__resets">Resets on {moment(resetDate).format('MMM D, YYYY')}</span>
              )}
            </div>
          )}
        </div>
      </NxTile.Content>
    </NxTile>
  );
}

MyUsageTile.propTypes = {
  summary: PropTypes.shape({
    consumed: PropTypes.number,
    limit: PropTypes.number,
    percentUsed: PropTypes.number,
    remaining: PropTypes.number,
    resetDate: PropTypes.string,
    activityBreakdown: PropTypes.object,
  }),
};
