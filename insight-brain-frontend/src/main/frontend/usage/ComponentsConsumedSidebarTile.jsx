/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { useDispatch, useSelector } from 'react-redux';
import classnames from 'classnames';
import { NxFontAwesomeIcon, NxTooltip } from '@sonatype/react-shared-components';
import { faCubes } from '@fortawesome/pro-regular-svg-icons';

import { actions } from './usageSlice';
import { selectSummary, selectLoadingSummary, selectLoadErrorSummary } from './usageSelectors';
import { selectIsUsageDashboardEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { formatCount, formatNumber } from './usageFormatters';

const RING_RADIUS = 13;
const RING_CIRCUMFERENCE = 2 * Math.PI * RING_RADIUS;

function ProgressRing({ pct, overLimit }) {
  const safePct = Math.max(0, Math.min(100, pct));
  const dashOffset = RING_CIRCUMFERENCE * (1 - safePct / 100);
  return (
    <svg className="iq-components-consumed-tile__ring" width="32" height="32" viewBox="0 0 32 32" aria-hidden="true">
      <circle cx="16" cy="16" r={RING_RADIUS} className="iq-components-consumed-tile__ring-track" />
      <circle
        cx="16"
        cy="16"
        r={RING_RADIUS}
        className={classnames('iq-components-consumed-tile__ring-fill', {
          'iq-components-consumed-tile__ring-fill--over': overLimit,
        })}
        strokeDasharray={RING_CIRCUMFERENCE}
        strokeDashoffset={dashOffset}
        transform="rotate(-90 16 16)"
      />
    </svg>
  );
}

ProgressRing.propTypes = {
  pct: PropTypes.number.isRequired,
  overLimit: PropTypes.bool.isRequired,
};

export default function ComponentsConsumedSidebarTile({ collapsed = false }) {
  const dispatch = useDispatch();
  const enabled = useSelector(selectIsUsageDashboardEnabled);
  const summary = useSelector(selectSummary);
  const loading = useSelector(selectLoadingSummary);
  const error = useSelector(selectLoadErrorSummary);

  useEffect(() => {
    if (enabled && summary === null && !loading && !error) {
      dispatch(actions.loadSummary());
    }
  }, [enabled, summary, loading, error, dispatch]);

  if (!enabled) return null;
  if (error && !summary) return null;

  if (loading && !summary) {
    return (
      <div
        className={classnames('iq-components-consumed-tile', {
          'iq-components-consumed-tile--collapsed': collapsed,
        })}
        data-testid="iq-components-consumed-tile__skeleton"
      >
        {!collapsed && <div className="iq-components-consumed-tile__label-skeleton" />}
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

  // Tooltip text used in collapsed state. Full-precision numbers so the
  // tooltip is the canonical source of truth even when the visible label is compact.
  const tooltipText = hasLimit
    ? `Components: ${formatNumber(consumed)} / ${formatNumber(limit)}` + (limit > 0 ? ` (${Math.round(pctRaw)}%)` : '')
    : `Components: ${formatNumber(consumed)}`;

  if (collapsed) {
    return (
      <NxTooltip title={tooltipText} placement="right">
        <button
          type="button"
          className="iq-components-consumed-tile iq-components-consumed-tile--collapsed"
          onClick={handleClick}
          aria-label={tooltipText}
        >
          {hasLimit && <ProgressRing pct={pct} overLimit={overLimit} />}
          <NxFontAwesomeIcon icon={faCubes} className="iq-components-consumed-tile__icon" />
        </button>
      </NxTooltip>
    );
  }

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
          aria-label="Components consumed"
          aria-valuenow={Math.round(pct)}
          aria-valuemin={0}
          aria-valuemax={100}
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
