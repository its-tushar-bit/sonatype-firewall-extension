/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';
import moment from 'moment';
import { curry, descend, map, prop, sort } from 'ramda';

import {
  NxButton,
  NxFontAwesomeIcon,
  NxReadOnly,
  NxSmallTag,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  NxTextLink,
} from '@sonatype/react-shared-components';
import { faTrashAlt, faCog } from '@fortawesome/free-solid-svg-icons';

import ComponentDisplay from '../ComponentDisplay/ReactComponentDisplay';
import { violationDetailsPropTypes } from '../violation/ViolationDetailsTile';
import { constraintViolationsPropType } from '../violation/PolicyViolationConstraintInfo';
import { Messages } from '../utilAngular/CommonServices';
import { displayWaiverScope, isWaiverAllVersionsOrExact } from '../util/waiverUtils';
import { STANDARD_DATE_FORMAT } from 'MainRoot/util/dateUtils';
import { setWaiverToDelete } from 'MainRoot/waivers/waiverActions';
import DeleteWaiverModalContainer from 'MainRoot/waivers/deleteWaiverModal/DeleteWaiverModalContainer';
import { selectWaiverToDelete } from 'MainRoot/waivers/deleteWaiverModal/deleteWaiverSelector';
import { selectApplicableAutoWaiver, selectApplicableWaivers } from 'MainRoot/violation/violationSelectors';
import { loadApplicableAutoWaiver, loadApplicableWaivers } from 'MainRoot/violation/violationActions';
import { selectViolationSlice } from './requestWaiverSelectors';
import { capitalize } from 'MainRoot/util/jsUtil';
import DeleteAutoWaiverModal from 'MainRoot/waivers/DeleteAutoWaiverModal';

export default function ListWaiversTable(props) {
  const { violationDetails, unknownComponentName } = props;

  const [showDeleteAutoWaiverModal, setShowDeleteAutoWaiverModal] = useState(false);
  const dispatch = useDispatch();

  const { activeWaivers, expiredWaivers } = useSelector(selectApplicableWaivers);
  const { loadingApplicableWaivers, loadApplicableWaiversError } = useSelector(selectViolationSlice);
  const { autoWaiver, loadingAutoWaiver, loadAutoWaiverError } = useSelector(selectApplicableAutoWaiver);
  const waiverToDelete = useSelector(selectWaiverToDelete);
  const getExpirationDate = (waiver) => {
    if (waiver.expiryTime) {
      return waiver.expireWhenRemediationAvailable
        ? 'Upgrade Available'
        : moment(waiver.expiryTime).format(STANDARD_DATE_FORMAT);
    }
    return waiver.expireWhenRemediationAvailable ? 'When Remediation Available' : 'Does not expire';
  };
  const displayWaiverInTableRow = curry((isWaiverExpired, waiver) => {
    const rowClass = classnames({
      'list-waivers-row--expired': isWaiverExpired,
    });
    const key = waiver.policyWaiverId;
    return (
      <NxTableRow className={rowClass} key={key}>
        <NxTableCell>
          <NxReadOnly.Label>Created</NxReadOnly.Label>
          <NxReadOnly.Data className="iq-waivers-table__created visual-testing-ignore">
            {moment(waiver.createTime).format(STANDARD_DATE_FORMAT)}
          </NxReadOnly.Data>

          <NxReadOnly.Label>Expiration</NxReadOnly.Label>
          <NxReadOnly.Data className="iq-waivers-table__expiration visual-testing-ignore">
            {getExpirationDate(waiver)}
          </NxReadOnly.Data>
        </NxTableCell>
        <NxTableCell>
          <NxReadOnly.Label>Scope</NxReadOnly.Label>
          <NxReadOnly.Data className="iq-waivers-table__scope">{displayWaiverScope(waiver)}</NxReadOnly.Data>

          <NxReadOnly.Label>Component</NxReadOnly.Label>
          <NxReadOnly.Data className="iq-waivers-table__component">
            {isWaiverAllVersionsOrExact(waiver) ? (
              <ComponentDisplay
                component={violationDetails}
                truncate={false}
                matcherStrategy={waiver.matcherStrategy}
                displayTextIfUnknown={unknownComponentName}
              />
            ) : (
              'All'
            )}
          </NxReadOnly.Data>

          {waiver.comment && (
            <>
              <NxReadOnly.Label>Comment</NxReadOnly.Label>
              <NxReadOnly.Data className="iq-waivers-table__comment">{waiver.comment}</NxReadOnly.Data>
            </>
          )}

          <NxReadOnly.Label>Author</NxReadOnly.Label>
          <NxReadOnly.Data className="iq-waivers-table__author">{waiver?.creatorName || '- -'}</NxReadOnly.Data>
        </NxTableCell>
        <NxTableCell className="iq-waivers-table__delete">
          <div className="nx-btn-bar">
            <NxButton
              variant="icon-only"
              title="delete"
              key={key}
              className="list-waivers-row__delete-btn"
              onClick={() => dispatch(setWaiverToDelete(waiver))}
            >
              <NxFontAwesomeIcon icon={faTrashAlt} />
            </NxButton>
          </div>
        </NxTableCell>
      </NxTableRow>
    );
  });

  const displayAutoWaiverRow = (autoWaiver) => {
    if (!autoWaiver) return null;
    const autoKey = `auto_waiver-${autoWaiver.autoPolicyWaiverId}`;
    return (
      <NxTableRow className="list-auto-waiver-row" key={autoKey}>
        <NxTableCell>
          <NxReadOnly.Label>Created</NxReadOnly.Label>
          <NxReadOnly.Data className="iq-auto-waiver-table__created visual-testing-ignore">
            {moment(autoWaiver.createTime).format(STANDARD_DATE_FORMAT)}
          </NxReadOnly.Data>

          <NxReadOnly.Label>Expiration</NxReadOnly.Label>
          <NxReadOnly.Data className="iq-auto-waiver-table__expiration">
            <NxSmallTag color="green">Auto</NxSmallTag>
          </NxReadOnly.Data>
        </NxTableCell>
        <NxTableCell>
          <NxReadOnly.Label>Scope</NxReadOnly.Label>
          <NxReadOnly.Data className="iq-auto-waiver-table__scope">
            {capitalize(autoWaiver.ownerType)} - {autoWaiver.ownerName}
          </NxReadOnly.Data>

          <NxReadOnly.Label>Component</NxReadOnly.Label>
          <NxReadOnly.Data className="iq-auto-waiver-table__component">Any Component</NxReadOnly.Data>

          <NxReadOnly.Label>Version</NxReadOnly.Label>
          <NxReadOnly.Data className="iq-auto-waiver-table__version">Current or latest non-violating</NxReadOnly.Data>

          <NxReadOnly.Label>Author</NxReadOnly.Label>
          <NxReadOnly.Data className="iq-auto-waiver-table__author">{autoWaiver?.creatorName || '- -'}</NxReadOnly.Data>
        </NxTableCell>
        <NxTableCell className="iq-auto-waiver-table__revocation">
          <div className="nx-btn-bar">
            <NxButton
              variant="icon-only"
              title="Remove auto-waiver for this policy violation"
              className="list-auto-waiver-row__revocation-btn"
              onClick={() => {
                setShowDeleteAutoWaiverModal(true);
              }}
            >
              <NxFontAwesomeIcon icon={faCog} />
            </NxButton>
            <DeleteAutoWaiverModal onClose={handleCloseAutoWaiverModal} showModal={showDeleteAutoWaiverModal} />
          </div>
        </NxTableCell>
      </NxTableRow>
    );
  };

  const handleCloseAutoWaiverModal = () => {
    setShowDeleteAutoWaiverModal(false);
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
          {autoWaiver && displayAutoWaiverRow(autoWaiver)}
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
