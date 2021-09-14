/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { isNil, sort } from 'ramda';
import { NxButton, NxWarningAlert } from '@sonatype/react-shared-components';

import { waiverType } from '../../util/waiverUtils';
import PolicyViolationsTable from './PolicyViolationsTable';
import PolicyViolationDetailsPopover from './PolicyViolationDetailsPopover';
import ComponentWaiversPopover from './componentWaivers/ComponentWaiversPopover';
import RequestWaiversPopover from '../../waivers/requestWaiversPopover/RequestWaiversPopover';
import AddWaiverPopover from '../../waivers/addWaiverPopover/AddWaiverPopoverContainer';

export default function ViolationsTableTile({
  violations,
  waivers,
  componentName,
  setWaiverToDelete,
  waiverToDelete,
  showComponentWaiversPopover,
  toggleComponentWaiversPopover,
  loadPolicyViolationsInformation,
  loading,
  loadError,
  showViolationsDetailPopover,
  toggleShowViolationsDetailPopover,
  showAddWaiverPopover,
  toggleAddWaiverPopover,
  showRequestWaiverPopover,
  toggleRequestWaiverPopover,
  hasPermissionToAddWaivers,
  setSelectedPolicyViolationId,
  selectedViolationDetail,
  title,
  showViewAllComponents,
  violationType,
  setViolationType,
}) {
  useEffect(() => {
    loadPolicyViolationsInformation();
  }, []);

  useEffect(() => {
    setViolationType(violationType);
  }, [violationType]);

  const orderedViolations = violations
    ? sort((threatA, threatB) => threatB.policyThreatLevel - threatA.policyThreatLevel, violations)
    : [];

  const containsOldViolations = orderedViolations.some((violation) => isNil(violation.policyViolationId));

  const tableProps = {
      violations: orderedViolations,
      error: loadError,
      retryHandler: loadPolicyViolationsInformation,
      waivers,
      loading,
      toggleShowViolationsDetailPopover,
      toggleAddWaiverPopover,
      toggleRequestWaiverPopover,
      hasPermissionToAddWaivers,
      setSelectedPolicyViolationId,
    },
    viewAllComponentWaiversButton = (
      <NxButton id="component-details-view-waivers" variant="tertiary" onClick={toggleComponentWaiversPopover}>
        <span>View All Component Waivers</span>
      </NxButton>
    );

  return (
    <Fragment>
      <section className="nx-tile">
        {containsOldViolations && (
          <NxWarningAlert>
            Re-evaluate this report to enable <b>waivers functionality</b>.
          </NxWarningAlert>
        )}
        {showViolationsDetailPopover && (
          <PolicyViolationDetailsPopover onClose={() => toggleShowViolationsDetailPopover()} />
        )}
        <header className="nx-tile-header">
          <div className="nx-tile-header__title">
            <h2 className="nx-h2" id="violations__tile__title">
              {title}
            </h2>
          </div>
          {showViewAllComponents && <div className="nx-tile__actions">{viewAllComponentWaiversButton}</div>}
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
      {showAddWaiverPopover && (
        <AddWaiverPopover onClose={toggleAddWaiverPopover} violationId={selectedViolationDetail.policyViolationId} />
      )}
      {showRequestWaiverPopover && (
        <RequestWaiversPopover onClose={toggleRequestWaiverPopover} violationDetails={selectedViolationDetail} />
      )}
    </Fragment>
  );
}

ViolationsTableTile.propTypes = {
  violations: PolicyViolationsTable.propTypes.violations,
  waivers: PropTypes.arrayOf(PropTypes.shape(waiverType)),
  componentName: PropTypes.string.isRequired,
  setWaiverToDelete: PropTypes.func.isRequired,
  waiverToDelete: PropTypes.shape(waiverType),
  showComponentWaiversPopover: PropTypes.bool.isRequired,
  toggleComponentWaiversPopover: PropTypes.func.isRequired,
  loadPolicyViolationsInformation: PropTypes.func.isRequired,
  loading: PropTypes.bool.isRequired,
  loadError: PropTypes.string,
  showViolationsDetailPopover: PropTypes.bool.isRequired,
  showAddWaiverPopover: PropTypes.bool.isRequired,
  showRequestWaiverPopover: PropTypes.bool.isRequired,
  toggleShowViolationsDetailPopover: PropTypes.func.isRequired,
  toggleAddWaiverPopover: PropTypes.func.isRequired,
  toggleRequestWaiverPopover: PropTypes.func.isRequired,
  hasPermissionToAddWaivers: PropTypes.bool.isRequired,
  setSelectedPolicyViolationId: PropTypes.func.isRequired,
  selectedViolationDetail: RequestWaiversPopover.propTypes.violationDetails,
  showViewAllComponents: PropTypes.bool,
  title: PropTypes.string,
  violationType: PropTypes.string,
  setViolationType: PropTypes.func,
};
