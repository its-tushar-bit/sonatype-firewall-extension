/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import {
  NxFilterInput,
  NxFontAwesomeIcon,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  NxThreatIndicator,
} from '@sonatype/react-shared-components';
import { componentTransitivePolicyViolationsPropType } from './transitiveViolationsPropTypes';
import classnames from 'classnames';
import { faExclamationCircle, faExclamationTriangle } from '@fortawesome/pro-solid-svg-icons';
import { capitalize } from '../util/jsUtil';

export default function TransitiveViolationsPageTable(props) {
  const {
    stageTypeId,
    componentTransitivePolicyViolations,
    setSortingParameters,
    setFilteringParameters,
    setSelectedPolicyViolationId,
    toggleShowViolationsDetailPopover,
    setViolationsDetailRowClicked,
  } = props;

  const getThreatColorClass = (threatLevel) => {
    return threatLevel > 7
      ? 'iq-threat-level--critical'
      : threatLevel > 3
      ? 'iq-threat-level--severe'
      : threatLevel > 1
      ? 'iq-threat-level--moderate'
      : threatLevel > 0
      ? 'iq-threat-level--low'
      : 'iq-threat-level--none';
  };

  const getActionDescription = (action) => {
    if (action === 'fail') {
      return (
        <div className="iq-transitive-violations-page-action">
          <NxFontAwesomeIcon icon={faExclamationCircle} className="nx-icon--fail" />
          <span>{'Failing ' + capitalize(stageTypeId)}</span>
        </div>
      );
    }
    if (action === 'warn') {
      return (
        <div className="iq-transitive-violations-page-action">
          <NxFontAwesomeIcon icon={faExclamationTriangle} className="nx-icon--warn" />
          <span>Warning</span>
        </div>
      );
    }
    return undefined;
  };

  const selectPolicyViolation = (policyViolationId) => {
    setSelectedPolicyViolationId(policyViolationId);
    toggleShowViolationsDetailPopover();
    setViolationsDetailRowClicked();
  };

  const createRow = (transitivePolicyViolation) => {
    return (
      <NxTableRow
        key={transitivePolicyViolation.policyViolationId}
        isClickable
        onClick={() => selectPolicyViolation(transitivePolicyViolation.policyViolationId)}
      >
        <NxTableCell>
          <NxThreatIndicator policyThreatLevel={transitivePolicyViolation.threatLevel} />
          <span className="nx-threat-number">{transitivePolicyViolation.threatLevel}</span>
        </NxTableCell>
        <NxTableCell>
          <div
            className={classnames(
              getThreatColorClass(transitivePolicyViolation.threatLevel),
              'iq-transitive-violations-page-policy-name'
            )}
          >
            {transitivePolicyViolation.policyName}
          </div>
          {getActionDescription(transitivePolicyViolation.action)}
        </NxTableCell>
        <NxTableCell>{transitivePolicyViolation.displayName}</NxTableCell>
      </NxTableRow>
    );
  };

  const getSortDir = (key) => {
    if (componentTransitivePolicyViolations.sortConfiguration.key === key) {
      return componentTransitivePolicyViolations.sortConfiguration.dir;
    }
    return null;
  };

  return (
    <NxTable>
      <NxTableHead>
        <NxTableRow>
          <NxTableCell
            id="iq-transitive-violations-page-threat-level"
            isSortable
            sortDir={getSortDir('threatLevel')}
            onClick={() => setSortingParameters('threatLevel')}
          >
            Threat
          </NxTableCell>
          <NxTableCell
            id="iq-transitive-violations-page-policy-name"
            isSortable
            sortDir={getSortDir('policyName')}
            onClick={() => setSortingParameters('policyName')}
          >
            Policy/Action
          </NxTableCell>
          <NxTableCell
            id="iq-transitive-violations-page-display-name"
            isSortable
            sortDir={getSortDir('displayName')}
            onClick={() => setSortingParameters('displayName')}
          >
            Component
          </NxTableCell>
        </NxTableRow>
        <NxTableRow isFilterHeader>
          <NxTableCell />
          <NxTableCell>
            <NxFilterInput
              id="iq-transitive-violations-page-policy-name-filter"
              placeholder="Policy Name"
              value={componentTransitivePolicyViolations.filterConfiguration.policyName}
              onChange={(value) => setFilteringParameters({ policyName: value })}
            />
          </NxTableCell>
          <NxTableCell>
            <NxFilterInput
              id="iq-transitive-violations-page-display-name-filter"
              placeholder="Component Name"
              value={componentTransitivePolicyViolations.filterConfiguration.displayName}
              onChange={(value) => setFilteringParameters({ displayName: value })}
            />
          </NxTableCell>
        </NxTableRow>
      </NxTableHead>
      <NxTableBody emptyMessage="None">
        {componentTransitivePolicyViolations.data.displayedViolations.map(createRow)}
      </NxTableBody>
    </NxTable>
  );
}

TransitiveViolationsPageTable.propTypes = {
  stageTypeId: PropTypes.string.isRequired,
  componentTransitivePolicyViolations: componentTransitivePolicyViolationsPropType.isRequired,
  setSortingParameters: PropTypes.func.isRequired,
  setFilteringParameters: PropTypes.func.isRequired,
  setSelectedPolicyViolationId: PropTypes.func.isRequired,
  toggleShowViolationsDetailPopover: PropTypes.func.isRequired,
  setViolationsDetailRowClicked: PropTypes.func.isRequired,
};
