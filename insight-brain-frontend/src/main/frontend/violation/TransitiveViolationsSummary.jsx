/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxThreatCounter } from '@sonatype/react-shared-components';
import { threatCountsPropType } from './transitiveViolationsPropTypes';

export default function TransitiveViolationsSummary(props) {
  const { threatCountsTotal, componentCount, threatCounts } = props;

  const nullIfZero = (value) => {
    return value === 0 ? null : value;
  };

  const singularIfOne = (value) => {
    return value === 1 ? '' : 's';
  };

  return (
    <div className="transitive-violations-counts-group">
      <dl className="nx-read-only">
        <div className="nx-read-only__item">
          <dt className="nx-read-only__label">Summary of violations being waived</dt>
          <dd className="nx-read-only__data">
            {threatCountsTotal +
              ' total violation' +
              singularIfOne(threatCountsTotal) +
              ' brought in by ' +
              componentCount +
              ' component' +
              singularIfOne(componentCount)}
          </dd>
          <dd className="nx-read-only__data">
            <NxThreatCounter
              layout="column"
              criticalCount={nullIfZero(threatCounts.critical)}
              severeCount={nullIfZero(threatCounts.severe)}
              moderateCount={nullIfZero(threatCounts.moderate)}
              lowCount={nullIfZero(threatCounts.low)}
              noneCount={nullIfZero(threatCounts.none)}
            />
          </dd>
        </div>
      </dl>
    </div>
  );
}

TransitiveViolationsSummary.propTypes = {
  threatCountsTotal: PropTypes.number,
  componentCount: PropTypes.number,
  threatCounts: threatCountsPropType,
};
