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
import ComponentWaiversPopover from './componentWaivers/ComponentWaiversPopover';
import PolicyViolationsTableRow, { violationPropTypes } from './PolicyViolationsTableRow';

const sortByThreatLevelAndWaiverStatus = sort(
  (threatA, threatB) => threatA.waived - threatB.waived || threatB.policyThreatLevel - threatA.policyThreatLevel
);

export default function PolicyViolationsTable({
  violations,
  error,
  loading,
  loadPolicyViolationsInformation,
  toggleShowViolationsDetailPopover,
  setSelectedPolicyViolationId,
  showComponentWaiversPopover,
  componentName,
  componentNameWithoutVersion,
  waivers,
  isAutoWaiverEnabled,
  toggleComponentWaiversPopover,
  setViolationsDetailRowClicked,
  waiverToDelete,
  setWaiverToDelete,
}) {
  useEffect(() => {
    loadPolicyViolationsInformation();
  }, []);

  const orderedViolations = violations ? sortByThreatLevelAndWaiverStatus(violations) : [];

  const containsOldViolations = orderedViolations.some((violation) => isNil(violation.policyViolationId));

  const toggleShowViolationsDetail = () => {
    setViolationsDetailRowClicked();
    toggleShowViolationsDetailPopover();
  };

  return (
    <Fragment>
      {!loading && containsOldViolations && (
        <NxWarningAlert>
          Re-evaluate this report to enable <b>waivers functionality</b>.
        </NxWarningAlert>
      )}
      <NxTable id="iq-policy-violations-table" className="iq-policy-violations-table">
        <NxTableHead>
          <NxTableRow>
            <NxTableCell>Threat</NxTableCell>
            <NxTableCell>Policy/Action</NxTableCell>
            <NxTableCell>Constraint Name</NxTableCell>
            <NxTableCell>Condition</NxTableCell>
            <NxTableCell />
            <NxTableCell chevron />
          </NxTableRow>
        </NxTableHead>
        <NxTableBody
          emptyMessage="No policy violations"
          error={error}
          isLoading={loading}
          retryHandler={loadPolicyViolationsInformation}
        >
          {orderedViolations.map((violation, index) => (
            <PolicyViolationsTableRow
              // in some scenarios data corruption causes old report to lose policyViolationIds, falling back
              // to index makes sure we still have unique key. See CLM-25312 for more details.
              key={violation.policyViolationId || index}
              violation={violation}
              toggleShowViolationsDetailPopover={toggleShowViolationsDetail}
              setSelectedPolicyViolationId={setSelectedPolicyViolationId}
              isAutoWaiversEnabled={isAutoWaiverEnabled}
            />
          ))}
        </NxTableBody>
      </NxTable>
      {!loading && !error && (
        <Fragment>
          {showComponentWaiversPopover && (
            <ComponentWaiversPopover
              componentName={componentName}
              componentNameWithoutVersion={componentNameWithoutVersion}
              toggleComponentWaiversPopover={toggleComponentWaiversPopover}
              waivers={waivers}
              setWaiverToDelete={setWaiverToDelete}
              waiverToDelete={waiverToDelete}
            />
          )}
        </Fragment>
      )}
    </Fragment>
  );
}

PolicyViolationsTable.propTypes = {
  waivers: PropTypes.arrayOf(PropTypes.shape(waiverType)),
  isAutoWaiverEnabled: PropTypes.bool,
  componentName: PropTypes.string,
  componentNameWithoutVersion: PropTypes.string,
  toggleShowViolationsDetailPopover: PropTypes.func.isRequired,
  setViolationsDetailRowClicked: PropTypes.func.isRequired,
  loadPolicyViolationsInformation: PropTypes.func.isRequired,
  violations: PropTypes.arrayOf(PropTypes.shape(violationPropTypes)),
  error: PropTypes.string,
  loading: PropTypes.bool,
  showComponentWaiversPopover: PropTypes.bool.isRequired,
  toggleComponentWaiversPopover: PropTypes.func.isRequired,
  setSelectedPolicyViolationId: PropTypes.func.isRequired,
  setWaiverToDelete: PropTypes.func.isRequired,
  waiverToDelete: PropTypes.shape(waiverType),
};
