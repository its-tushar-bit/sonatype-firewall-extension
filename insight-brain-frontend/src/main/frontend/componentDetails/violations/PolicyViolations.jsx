/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { sort } from 'ramda';

import PolicyViolationsTable from './PolicyViolationsTable';

export default function PolicyViolations(props) {
  const { violations, loadPolicyViolationsInformation, loading, loadError, goToWaivers } = props;
  useEffect(() => {
    loadPolicyViolationsInformation();
  }, []);

  const orderedViolations = violations
    ? sort((threatA, threatB) => threatB.policyThreatLevel - threatA.policyThreatLevel, violations)
    : [];

  const tableProps = {
    violations: orderedViolations,
    error: loadError,
    retryHandler: loadPolicyViolationsInformation,
    loading,
    goToWaivers,
  };

  return (
    <section id="component-details-policy-violations" className="nx-tile">
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
  loadPolicyViolationsInformation: PropTypes.func.isRequired,
  goToWaivers: PropTypes.func.isRequired,
  loading: PropTypes.bool.isRequired,
  loadError: PropTypes.string,
};
