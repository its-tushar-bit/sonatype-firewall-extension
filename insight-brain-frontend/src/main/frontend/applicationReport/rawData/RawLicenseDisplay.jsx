/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import * as PropTypes from 'prop-types';
import { NxTooltip } from '@sonatype/react-shared-components';
import { pipe, join, defaultTo } from 'ramda';
import { getDeclaredLicensesDisplay, getObservedLicensesDisplay } from '../licenseDisplayUtils';

const getLicenseTooltipTitle = (license) => {
  const placeholder = '-';
  const joiner = pipe(defaultTo([]), join(', '));
  const declaredLicenses = joiner(license.declaredLicenses);
  const observedLicenses = joiner(license.observedLicenses);

  return (
    <Fragment>
      <div>
        <strong>Declared: </strong>
        {declaredLicenses || placeholder}
      </div>
      <div>
        <strong>Observed: </strong>
        {observedLicenses || placeholder}
      </div>
    </Fragment>
  );
};

export default function RawLicenseDisplay({ license }) {
  const declared = getDeclaredLicensesDisplay(license);
  const observed = getObservedLicensesDisplay(license);
  return (
    <NxTooltip placement="top" title={getLicenseTooltipTitle(license)}>
      <div className="raw-license-tooltip">
        <strong>{declared}</strong>
        {observed && ', '}
        {observed && <span>{observed}</span>}
      </div>
    </NxTooltip>
  );
}

RawLicenseDisplay.propTypes = {
  license: PropTypes.shape({
    declaredLicenses: PropTypes.arrayOf(PropTypes.string),
    observedLicenses: PropTypes.arrayOf(PropTypes.string),
  }),
};
