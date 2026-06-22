/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import classnames from 'classnames';

import { actions } from './usageSlice';
import { selectCumulativeFilter } from './usageSelectors';

const OPTIONS = [
  { value: 'thisMonth', label: 'This month' },
  { value: 'last3Months', label: 'Last 3 months' },
  { value: 'last6Months', label: 'Last 6 months' },
];

export default function CumulativeChartFilter() {
  const dispatch = useDispatch();
  const active = useSelector(selectCumulativeFilter);

  return (
    <div className="iq-cumulative-filter" role="group" aria-label="Cumulative chart range">
      {OPTIONS.map(({ value, label }) => (
        <button
          key={value}
          type="button"
          aria-pressed={value === active}
          className={classnames('iq-cumulative-filter__option', {
            'iq-cumulative-filter__option--active': value === active,
          })}
          onClick={() => dispatch(actions.changeCumulativeFilter(value))}
        >
          {label}
        </button>
      ))}
    </div>
  );
}
