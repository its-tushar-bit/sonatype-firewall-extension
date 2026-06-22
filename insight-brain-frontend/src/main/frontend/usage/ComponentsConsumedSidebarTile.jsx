/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { useDispatch, useSelector } from 'react-redux';
import classnames from 'classnames';

import { actions } from './usageSlice';
import { selectSummary, selectLoadingSummary, selectLoadErrorSummary } from './usageSelectors';
import { selectIsUsageDashboardEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { formatCount } from './usageFormatters';

export default function ComponentsConsumedSidebarTile({ collapsed = false }) {
  const dispatch = useDispatch();
  const enabled = useSelector(selectIsUsageDashboardEnabled);
  const summary = useSelector(selectSummary);
  const loading = useSelector(selectLoadingSummary);
  const error = useSelector(selectLoadErrorSummary);

  useEffect(() => {
    // Skip the fetch when the sidebar is collapsed — the tile isn't visible,
    // and firing a request for data that won't render wastes a round-trip.
    // Without `!collapsed` the effect would still run if a parent kept this
    // mounted with `collapsed={true}` rather than unmounting.
    if (!collapsed && enabled && summary === null && !loading && !error) {
      dispatch(actions.loadSummary());
    }
  }, [collapsed, enabled, summary, loading, error, dispatch]);

  if (!enabled) return null;

  if (collapsed) {
    return null;
  }

  // Transient error path: render a small inline placeholder rather than
  // disappearing. With `return null` here, a single 5xx during loadSummary
  // would silently hide the sidebar widget with no recovery — the useEffect's
  // `!error` guard prevents auto-retry, so the user would have to full-page
  // reload to see the tile again. Showing a tooltip-only placeholder keeps
  // the layout slot reserved and signals the failure.
  if (error && !summary) {
    return (
      <div
        className="iq-components-consumed-tile iq-components-consumed-tile--error"
        role="status"
        title="Couldn't load consumption data"
      >
        <div className="iq-components-consumed-tile__label">Components</div>
        <div className="iq-components-consumed-tile__error-text">Couldn’t load data</div>
      </div>
    );
  }

  if (loading && !summary) {
    return (
      <div className="iq-components-consumed-tile" data-testid="iq-components-consumed-tile__skeleton">
        <div className="iq-components-consumed-tile__label-skeleton" />
        <div className="iq-components-consumed-tile__bar-skeleton" />
      </div>
    );
  }

  if (!summary) return null;

  const { consumed, limit } = summary;
  const hasLimit = limit !== null && limit !== undefined;
  const overLimit = hasLimit && consumed > limit;
  const pctRaw = hasLimit && limit > 0 ? (consumed / limit) * 100 : 0;
  const pct = Math.min(100, pctRaw);

  const handleClick = () => dispatch(stateGo('usage'));

  return (
    <button type="button" className="iq-components-consumed-tile" onClick={handleClick}>
      <div className="iq-components-consumed-tile__label">
        Components{' '}
        <span className="iq-components-consumed-tile__count">
          {formatCount(consumed)}
          {hasLimit && (
            <>
              {' / '}
              {formatCount(limit)}
            </>
          )}
        </span>
      </div>
      {hasLimit && (
        <div
          className="iq-components-consumed-tile__bar-track"
          role="progressbar"
          // aria-label includes consumed/limit so screen readers get domain
          // context, not just a raw percentage. aria-valuetext announces the
          // over-limit state explicitly because aria-valuenow is clamped at 100.
          aria-label={`Components consumed: ${formatCount(consumed)} of ${formatCount(limit)}`}
          aria-valuenow={Math.round(pct)}
          aria-valuemin={0}
          aria-valuemax={100}
          aria-valuetext={
            overLimit ? `Over limit: ${Math.round(pctRaw)}% of monthly limit` : `${Math.round(pct)}% of monthly limit`
          }
        >
          <div
            data-testid="iq-components-consumed-tile__bar-fill"
            className={classnames('iq-components-consumed-tile__bar-fill', {
              'iq-components-consumed-tile__bar-fill--over': overLimit,
            })}
            style={{ width: `${pct}%` }}
          />
        </div>
      )}
    </button>
  );
}

ComponentsConsumedSidebarTile.propTypes = {
  collapsed: PropTypes.bool,
};
