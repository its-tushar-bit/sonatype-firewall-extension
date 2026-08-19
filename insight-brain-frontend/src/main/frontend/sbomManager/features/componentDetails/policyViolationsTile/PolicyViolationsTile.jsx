/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useMemo } from 'react';
import * as PropTypes from 'prop-types';
import { useDispatch, useSelector } from 'react-redux';
import { NxTile, NxH2, NxTable, NxThreatIndicator } from '@sonatype/react-shared-components';
import {
  always,
  compose,
  defaultTo,
  descend,
  flatten,
  has,
  head,
  ifElse,
  isNil,
  map,
  prop,
  propOr,
  sortWith,
  when,
} from 'ramda';

import { actions } from 'MainRoot/sbomManager/features/componentDetails/componentDetailsSlice';
import { selectSbomComponentDetails } from '../componentDetailsSelector';

const PolicyViolationsTile = ({ applicationPublicId, sbomVersion }) => {
  const { componentDetails, sbomPolicyViolations } = useSelector(selectSbomComponentDetails);

  useEffect(() => {
    if (componentDetails?.fileCoordinateId || componentDetails?.hash) {
      dispatch(
        actions.loadSbomPolicyViolations({
          applicationPublicId,
          sbomVersion,
          componentRef: componentDetails.componentRef,
          fileCoordinateId: componentDetails.fileCoordinateId,
          hash: componentDetails.hash,
        })
      );
    }
  }, [applicationPublicId, sbomVersion, componentDetails]);

  const dispatch = useDispatch();

  const sortedViolations = useMemo(
    () =>
      compose(
        sortWith([descend(prop('policyThreatLevel'))]),
        ifElse(has('allViolations'), prop('allViolations'), propOr([], 'activeViolations')),
        defaultTo({})
      )(when(isNil, always([]))(sbomPolicyViolations.policy)),
    [sbomPolicyViolations.policy]
  );

  const openPolicyViolationDetailsDrawerClickHandler = (policyViolationId) => () => {
    dispatch(actions.showPolicyViolationDetailsDrawer(policyViolationId));
  };

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
          <NxTable.Body
            isLoading={sbomPolicyViolations.loading}
            error={sbomPolicyViolations.error}
            emptyMessage="No policy violations"
          >
            {tableRows}
          </NxTable.Body>
        </NxTable>
      </NxTile.Content>
    </NxTile>
  );
};

PolicyViolationsTile.propTypes = {
  applicationPublicId: PropTypes.string,
  sbomVersion: PropTypes.string,
};

export default PolicyViolationsTile;
