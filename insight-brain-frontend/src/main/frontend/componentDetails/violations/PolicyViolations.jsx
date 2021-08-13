/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { sort } from 'ramda';
import { NxButton } from '@sonatype/react-shared-components';
import PolicyViolationsTable from './PolicyViolationsTable';
import PolicyViolationDetailsPopover from './PolicyViolationDetailsPopover';
import ComponentWaiversPopover from './componentWaivers/ComponentWaiversPopover';
import { waiverType } from '../../util/waiverUtils';

export default function PolicyViolations({
  violations,
  waivers,
  componentName,
  setWaiverToDelete,
  waiverToDelete,
  goToWaivers,
  showComponentWaiversPopover,
  toggleComponentWaiversPopover,
  loadPolicyViolationsInformation,
  loading,
  loadError,
  selectedViolationId,
  setSelectedViolationId,
}) {
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
    waivers,
    loading,
    goToWaivers,
    setSelectedViolationId,
  };
  const viewAllComponentWaiversButton = (
    <NxButton id="component-details-view-waivers" variant="tertiary" onClick={toggleComponentWaiversPopover}>
      <span>View All Component Waivers</span>
    </NxButton>
  );

  return (
    <Fragment>
      <section id="component-details-policy-violations" className="nx-tile">
        {selectedViolationId && <PolicyViolationDetailsPopover onClose={() => setSelectedViolationId('')} />}
        <header className="nx-tile-header">
          <div className="nx-tile-header__title">
            <h2 className="nx-h2">Policy Violations</h2>
          </div>
          <div className="nx-tile__actions">{viewAllComponentWaiversButton}</div>
        </header>
        <div className="nx-tile-content">
          <PolicyViolationsTable {...tableProps} />
        </div>
      </section>
      {showComponentWaiversPopover && (
        <ComponentWaiversPopover
          componentName={componentName}
          toggleComponentWaiversPopover={toggleComponentWaiversPopover}
          waivers={waivers}
          setWaiverToDelete={setWaiverToDelete}
          waiverToDelete={waiverToDelete}
        />
      )}
    </Fragment>
  );
}

PolicyViolations.propTypes = {
  violations: PolicyViolationsTable.propTypes.violations,
  waivers: PropTypes.arrayOf(PropTypes.shape(waiverType)),
  componentName: PropTypes.string.isRequired,
  setWaiverToDelete: PropTypes.func.isRequired,
  waiverToDelete: PropTypes.shape(waiverType),
  showComponentWaiversPopover: PropTypes.bool.isRequired,
  toggleComponentWaiversPopover: PropTypes.func.isRequired,
  loadPolicyViolationsInformation: PropTypes.func.isRequired,
  goToWaivers: PropTypes.func.isRequired,
  loadError: PropTypes.string,
  loading: PropTypes.bool.isRequired,
  setSelectedViolationId: PropTypes.func,
  selectedViolationId: PropTypes.string,
};
