/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';
import moment from 'moment';
import { curry, descend, map, prop, sort } from 'ramda';

import {
  NxButton,
  NxFontAwesomeIcon,
  NxReadOnly,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  NxTextLink,
} from '@sonatype/react-shared-components';
import { faTrashAlt } from '@fortawesome/free-solid-svg-icons';

import ComponentDisplay from '../ComponentDisplay/ReactComponentDisplay';
import { violationDetailsPropTypes } from '../violation/ViolationDetailsTile';
import { constraintViolationsPropType } from '../violation/PolicyViolationConstraintInfo';
import { Messages } from '../utilAngular/CommonServices';
import { displayWaiverScope, isWaiverAllVersionsOrExact } from '../util/waiverUtils';
import { STANDARD_DATE_FORMAT } from 'MainRoot/util/dateUtils';
import { loadApplicableWaivers, setWaiverToDelete } from 'MainRoot/waivers/waiverActions';
import { selectManageWaiverSlice } from 'MainRoot/waivers/manageWaiversSelectors';
import DeleteWaiverModalContainer from 'MainRoot/waivers/deleteWaiverModal/DeleteWaiverModalContainer';
import { selectWaiverToDelete } from 'MainRoot/waivers/deleteWaiverModal/deleteWaiverSelector';
import { selectApplicableWaivers } from 'MainRoot/violation/violationSelectors';

export default function ListWaiversTable(props) {
  const { violationDetails, unknownComponentName } = props;

  const dispatch = useDispatch();
  const { activeWaivers, expiredWaivers } = useSelector(selectApplicableWaivers);
  const { loadingApplicableWaivers, loadApplicableWaiversError } = useSelector(selectManageWaiverSlice);
  const waiverToDelete = useSelector(selectWaiverToDelete);
  const displayWaiverInTableRow = curry((isWaiverExpired, waiver) => {
    const rowClass = classnames({
      'list-waivers-row--expired': isWaiverExpired,
    });
    const key = waiver.policyWaiverId;
    return (
      <NxTableRow className={rowClass} key={key}>
        <NxTableCell>
          <NxReadOnly.Label>Created</NxReadOnly.Label>
          <NxReadOnly.Data className="iq-waivers-table--created visual-testing-ignore">
            {moment(waiver.createTime).format(STANDARD_DATE_FORMAT)}
          </NxReadOnly.Data>

          <NxReadOnly.Label>Expiration</NxReadOnly.Label>
          <NxReadOnly.Data className="iq-waivers-table--expiration visual-testing-ignore">
            {waiver.expiryTime ? moment(waiver.expiryTime).format(STANDARD_DATE_FORMAT) : 'Does not expire'}
          </NxReadOnly.Data>
        </NxTableCell>
        <NxTableCell>
          <NxReadOnly.Label>Scope</NxReadOnly.Label>
          <NxReadOnly.Data className="iq-waivers-table--scope">{displayWaiverScope(waiver)}</NxReadOnly.Data>

          <NxReadOnly.Label>Component</NxReadOnly.Label>
          <NxReadOnly.Data className="iq-waivers-table--component">
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
              <NxReadOnly.Data className="iq-waivers-table--comment">{waiver.comment}</NxReadOnly.Data>
            </>
          )}

          <NxReadOnly.Label>Author</NxReadOnly.Label>
          <NxReadOnly.Data className="iq-waivers-table--author">{waiver?.creatorName || '- -'}</NxReadOnly.Data>
        </NxTableCell>
        <NxTableCell className="iq-waivers-table--delete">
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

  const emptyMessage = (
    <span>
      You don&apos;t have any waivers: to learn more about waivers you can check our{' '}
      <NxTextLink external href="https://links.sonatype.com/products/nxiq/doc/waivers">
        help documentation.
      </NxTextLink>
    </span>
  );

  return (
    <>
      {waiverToDelete && <DeleteWaiverModalContainer />}
      <NxTable id="list-waivers-table">
        <NxTableHead>
          <NxTableRow>
            <NxTableCell className="iq-waivers-table--duration">DURATION</NxTableCell>
            <NxTableCell>WAIVER DETAILS</NxTableCell>
            <NxTableCell> </NxTableCell>
          </NxTableRow>
        </NxTableHead>
        <NxTableBody
          emptyMessage={emptyMessage}
          isLoading={loadingApplicableWaivers}
          error={Messages.getHttpErrorMessage(loadApplicableWaiversError)}
          retryHandler={() => dispatch(loadApplicableWaivers(violationDetails.policyViolationId))}
        >
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
