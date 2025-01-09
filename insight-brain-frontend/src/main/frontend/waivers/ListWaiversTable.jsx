/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import * as PropTypes from 'prop-types';
import { curry, descend, map, prop, sort } from 'ramda';

import {
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  NxTextLink,
} from '@sonatype/react-shared-components';
import { violationDetailsPropTypes } from '../violation/ViolationDetailsTile';
import { constraintViolationsPropType } from '../violation/PolicyViolationConstraintInfo';
import { Messages } from '../utilAngular/CommonServices';
import { setWaiverToDelete } from 'MainRoot/waivers/waiverActions';
import DeleteWaiverModalContainer from 'MainRoot/waivers/deleteWaiverModal/DeleteWaiverModalContainer';
import { selectWaiverToDelete } from 'MainRoot/waivers/deleteWaiverModal/deleteWaiverSelector';
import { selectApplicableAutoWaiver, selectApplicableWaivers } from 'MainRoot/violation/violationSelectors';
import { loadApplicableAutoWaiver, loadApplicableWaivers } from 'MainRoot/violation/violationActions';
import { selectViolationSlice } from './requestWaiverSelectors';
import WaiverRow from './WaiverRow';

export default function ListWaiversTable({ violationDetails, unknownComponentName }) {
  const dispatch = useDispatch();

  const { activeWaivers, expiredWaivers } = useSelector(selectApplicableWaivers);
  const { loadingApplicableWaivers, loadApplicableWaiversError } = useSelector(selectViolationSlice);
  const { autoWaiver, loadingAutoWaiver, loadAutoWaiverError } = useSelector(selectApplicableAutoWaiver);
  const waiverToDelete = useSelector(selectWaiverToDelete);

  const displayWaiverInTableRow = curry((isWaiverExpired, waiver) => {
    return (
      <WaiverRow
        waiver={waiver}
        isWaiverExpired={isWaiverExpired}
        violationDetails={violationDetails}
        unknownComponentName={unknownComponentName}
        deleteWaiver={deleteWaiver}
        key={waiver.policyWaiverId}
      />
    );
  });

  const deleteWaiver = (waiver) => {
    dispatch(setWaiverToDelete(waiver));
  };

  const emptyMessage = (
    <span>
      You don&apos;t have any waivers: to learn more about waivers you can check our{' '}
      <NxTextLink external href="https://links.sonatype.com/products/nxiq/doc/waivers">
        help documentation.
      </NxTextLink>
    </span>
  );

  const retryHandler = () => {
    const violationId = violationDetails.policyViolationId;
    dispatch(loadApplicableWaivers(violationId));
    dispatch(loadApplicableAutoWaiver(violationId));
  };

  const isLoading = loadingApplicableWaivers || loadingAutoWaiver;
  const error = loadApplicableWaiversError || loadAutoWaiverError;

  return (
    <>
      {waiverToDelete && <DeleteWaiverModalContainer />}
      <NxTable id="list-waivers-table">
        <NxTableHead>
          <NxTableRow>
            <NxTableCell className="iq-waivers-table__duration">DURATION</NxTableCell>
            <NxTableCell>WAIVER DETAILS</NxTableCell>
            <NxTableCell> </NxTableCell>
          </NxTableRow>
        </NxTableHead>
        <NxTableBody
          emptyMessage={emptyMessage}
          isLoading={isLoading}
          error={Messages.getHttpErrorMessage(error)}
          retryHandler={retryHandler}
        >
          {autoWaiver && <WaiverRow waiver={autoWaiver} isAutoWaiver />}
          {activeWaivers && map(displayWaiverInTableRow(false), sort(descend(prop('createTime')), activeWaivers))}
          {expiredWaivers && map(displayWaiverInTableRow(true), sort(descend(prop('createTime')), expiredWaivers))}
        </NxTableBody>
      </NxTable>
    </>
  );
}

ListWaiversTable.propTypes = {
  violationDetails: PropTypes.shape({
    ...violationDetailsPropTypes,
    constraintViolations: constraintViolationsPropType.isRequired,
    displayName: PropTypes.shape({
      parts: PropTypes.arrayOf(PropTypes.object),
    }),
    filename: PropTypes.string,
    policyViolationId: PropTypes.string.isRequired,
  }),
  unknownComponentName: PropTypes.string,
};
