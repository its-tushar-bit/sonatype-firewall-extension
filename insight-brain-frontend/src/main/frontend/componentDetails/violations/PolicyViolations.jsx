/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { sort } from 'ramda';

import PolicyViolationsTable from './PolicyViolationsTable';

export default function PolicyViolations({ violations, componentDetails, loadComponentDetails, loadError }) {
  useEffect(() => {
    if (!componentDetails) {
      loadComponentDetails();
    }
  }, [componentDetails]);

  const orderedViolations = violations
    ? sort((threatA, threatB) => threatB.policyThreatLevel - threatA.policyThreatLevel, violations)
    : [];

  const tableProps = {
    violations: orderedViolations,
    loading: !loadError && !violations,
    error: loadError,
    retryHandler: loadComponentDetails,
  };

  return (
    <section className="nx-tile">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">Policy Violations</h2>
        </div>
      </header>
      <div className="nx-tile-content">
        <PolicyViolationsTable {...tableProps} />
      </div>
    </section>
  );
}

PolicyViolations.propTypes = {
  violations: PolicyViolationsTable.propTypes.violations,
  componentDetails: PropTypes.object,
  loadComponentDetails: PropTypes.func.isRequired,
  loadError: PropTypes.string,
};
