/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useMemo } from 'react';
import * as PropTypes from 'prop-types';
import { useDispatch } from 'react-redux';
import { NxTile, NxH2, NxTable, NxThreatIndicator } from '@sonatype/react-shared-components';
import { compose, defaultTo, descend, flatten, has, head, ifElse, map, prop, propOr, sortWith } from 'ramda';

import { actions } from 'MainRoot/sbomManager/features/componentDetails/componentDetailsSlice';

const PolicyViolationsTile = ({ policy }) => {
  const dispatch = useDispatch();

  const sortedViolations = useMemo(
    () =>
      compose(
        sortWith([descend(prop('policyThreatLevel'))]),
        ifElse(has('allViolations'), prop('allViolations'), propOr([], 'activeViolations')),
        defaultTo({})
      )(policy),
    [policy]
  );

  const openPolicyViolationDetailsDrawerClickHandler = (policyViolationId) => () =>
    dispatch(actions.showPolicyViolationDetailsDrawer(policyViolationId));

  const tableRows = sortedViolations.map((violation, index) => (
    <NxTable.Row
      key={index}
      isClickable
      onClick={openPolicyViolationDetailsDrawerClickHandler(violation.policyViolationId)}
    >
      <NxTable.Cell>
        <NxThreatIndicator policyThreatLevel={violation.policyThreatLevel} />
        <span className="nx-threat-number">{violation.policyThreatLevel}</span>
      </NxTable.Cell>
      <NxTable.Cell>
        <span>{violation.policyName}</span>
      </NxTable.Cell>
      <NxTable.Cell>{propOr(null, 'constraintName', head(violation.constraints))}</NxTable.Cell>
      <NxTable.Cell>
        {flatten(map(compose(map(prop('conditionReason')), prop('conditions')), violation.constraints)).map(
          (reason, index) => (
            <p key={index}>{reason}</p>
          )
        )}
      </NxTable.Cell>
      <NxTable.Cell chevron />
    </NxTable.Row>
  ));

  return (
    <NxTile id="sbom-manager-policy-violations-tile" className="sbom-manager-policy-violations-tile">
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2>Policy Violations</NxH2>
        </NxTile.HeaderTitle>
      </NxTile.Header>
      <NxTile.Content>
        <NxTable className="sbom-manager-policy-violations-tile__table">
          <NxTable.Head>
            <NxTable.Row>
              <NxTable.Cell>Threat</NxTable.Cell>
              <NxTable.Cell>Policy</NxTable.Cell>
              <NxTable.Cell>Constraint Name</NxTable.Cell>
              <NxTable.Cell>Condition</NxTable.Cell>
              <NxTable.Cell chevron />
            </NxTable.Row>
          </NxTable.Head>
          <NxTable.Body emptyMessage="No policy violations">{tableRows}</NxTable.Body>
        </NxTable>
      </NxTile.Content>
    </NxTile>
  );
};

PolicyViolationsTile.propTypes = {
  policy: PropTypes.object,
};

export default PolicyViolationsTile;
