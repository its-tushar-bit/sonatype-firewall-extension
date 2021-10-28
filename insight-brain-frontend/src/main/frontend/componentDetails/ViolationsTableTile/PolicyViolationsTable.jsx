/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { isNil, sort } from 'ramda';
import {
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  NxWarningAlert,
} from '@sonatype/react-shared-components';

import { waiverType } from '../../util/waiverUtils';
import { violationDetailsPropTypes } from 'MainRoot/util/violationDetailsUtil';
import PolicyViolationDetailsPopover from './PolicyViolationDetailsPopover';
import ComponentWaiversPopover from './componentWaivers/ComponentWaiversPopover';
import RequestWaiversPopover from '../../waivers/requestWaiversPopover/RequestWaiversPopover';
import AddWaiverPopover from '../../waivers/addWaiverPopover/AddWaiverPopoverContainer';
import PolicyViolationsTableRow, { violationPropTypes } from './PolicyViolationsTableRow';

export default function PolicyViolationsTable({
  violations,
  error,
  loading,
  loadPolicyViolationsInformation,
  toggleShowViolationsDetailPopover,
  toggleAddWaiverPopover,
  toggleRequestWaiverPopover,
  hasPermissionToAddWaivers,
  setSelectedPolicyViolationId,
  showViolationsDetailPopover,
  showComponentWaiversPopover,
  componentName,
  waivers,
  toggleComponentWaiversPopover,
  waiverToDelete,
  setWaiverToDelete,
  showAddWaiverPopover,
  showRequestWaiverPopover,
  selectedViolationDetail,
}) {
  useEffect(() => {
    loadPolicyViolationsInformation();
  }, []);

  const orderedViolations = violations
    ? sort((threatA, threatB) => threatB.policyThreatLevel - threatA.policyThreatLevel, violations)
    : [];

  const containsOldViolations = orderedViolations.some((violation) => isNil(violation.policyViolationId));

  return (
    <Fragment>
      {!loading && containsOldViolations && (
        <NxWarningAlert>
          Re-evaluate this report to enable <b>waivers functionality</b>.
        </NxWarningAlert>
      )}
      <NxTable className="iq-policy-violations-table">
        <NxTableHead>
          <NxTableRow>
            <NxTableCell>Threat</NxTableCell>
            <NxTableCell>Policy/Action</NxTableCell>
            <NxTableCell>Constraint Name</NxTableCell>
            <NxTableCell>Condition</NxTableCell>
            <NxTableCell />
            <NxTableCell />
          </NxTableRow>
        </NxTableHead>
        <NxTableBody
          emptyMessage="No policy violations"
          error={error}
          isLoading={loading}
          retryHandler={loadPolicyViolationsInformation}
        >
          {orderedViolations.map((violation) => (
            <PolicyViolationsTableRow
              key={violation.policyViolationId}
              violation={violation}
              toggleShowViolationsDetailPopover={toggleShowViolationsDetailPopover}
              toggleAddWaiverPopover={toggleAddWaiverPopover}
              toggleRequestWaiverPopover={toggleRequestWaiverPopover}
              hasPermissionToAddWaivers={hasPermissionToAddWaivers}
              setSelectedPolicyViolationId={setSelectedPolicyViolationId}
            />
          ))}
        </NxTableBody>
      </NxTable>
      {!loading && !error && (
        <Fragment>
          {showViolationsDetailPopover && <PolicyViolationDetailsPopover onClose={toggleShowViolationsDetailPopover} />}
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
            <AddWaiverPopover
              onClose={toggleAddWaiverPopover}
              violationId={selectedViolationDetail.policyViolationId}
            />
          )}
          {showRequestWaiverPopover && (
            <RequestWaiversPopover onClose={toggleRequestWaiverPopover} violationDetails={selectedViolationDetail} />
          )}
        </Fragment>
      )}
    </Fragment>
  );
}

PolicyViolationsTable.propTypes = {
  selectedViolationDetail: violationDetailsPropTypes,
  waivers: PropTypes.arrayOf(PropTypes.shape(waiverType)),
  componentName: PropTypes.string,
  showViolationsDetailPopover: PropTypes.bool.isRequired,
  showAddWaiverPopover: PropTypes.bool.isRequired,
  showRequestWaiverPopover: PropTypes.bool.isRequired,
  toggleShowViolationsDetailPopover: PropTypes.func.isRequired,
  toggleAddWaiverPopover: PropTypes.func.isRequired,
  toggleRequestWaiverPopover: PropTypes.func.isRequired,
  loadPolicyViolationsInformation: PropTypes.func.isRequired,
  violations: PropTypes.arrayOf(PropTypes.shape(violationPropTypes)),
  error: PropTypes.string,
  loading: PropTypes.bool,
  showComponentWaiversPopover: PropTypes.bool.isRequired,
  toggleComponentWaiversPopover: PropTypes.func.isRequired,
  hasPermissionToAddWaivers: PropTypes.bool.isRequired,
  setSelectedPolicyViolationId: PropTypes.func.isRequired,
  setWaiverToDelete: PropTypes.func.isRequired,
  waiverToDelete: PropTypes.shape(waiverType),
};
