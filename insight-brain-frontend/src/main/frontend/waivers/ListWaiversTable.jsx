/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';
import moment from 'moment';
import { curry, descend, map, prop, sort } from 'ramda';

import {
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  NxButton,
  NxFontAwesomeIcon,
} from '@sonatype/react-shared-components';
import { faTrashAlt } from '@fortawesome/free-solid-svg-icons/index';

import ComponentDisplay from '../ComponentDisplay/ReactComponentDisplay';
import { violationDetailsPropTypes } from '../violation/ViolationDetailsTile';
import { constraintViolationsPropType } from '../violation/PolicyViolationConstraintInfoTile';
import NxExternalLink from '../react/NxExternalLink';
import { Messages } from '../utilAngular/CommonServices';
import { waiverType, displayWaiverScope } from '../util/waiverUtils';

export default function ListWaiversTable(props) {
  const {
    activeWaivers,
    expiredWaivers,
    violationDetails,
    setWaiverToDelete,
    loadingApplicableWaivers,
    loadApplicableWaiversError,
    reloadApplicableWaivers,
  } = props;

  const displayWaiverInTableRow = curry((isWaiverExpired, waiver) => {
    const rowClass = classnames({
      'list-waivers-row--expired': isWaiverExpired,
    });
    const key = waiver.policyWaiverId;
    return (
      <NxTableRow className={rowClass} key={key}>
        <NxTableCell className="visual-testing-ignore">{moment(waiver.createTime).format('MM/DD/YYYY')}</NxTableCell>
        <NxTableCell className="iq-waivers-table--scope">{displayWaiverScope(waiver)}</NxTableCell>
        <NxTableCell className="iq-waivers-table--component-name">
          {waiver.hash ? <ComponentDisplay component={violationDetails} truncate={true} /> : 'All'}
        </NxTableCell>
        <NxTableCell>{waiver.expiryTime ? moment(waiver.expiryTime).fromNow() : 'Does not expire'}</NxTableCell>
        <NxTableCell className="iq-waivers-table--creator">{waiver?.creatorName || '- -'}</NxTableCell>
        <NxTableCell className="iq-waivers-table--comments">{waiver.comment || '- -'}</NxTableCell>
        <NxTableCell>
          <div className="nx-btn-bar">
            <NxButton
              variant="icon-only"
              key={key}
              className="list-waivers-row__delete-btn"
              onClick={() => setWaiverToDelete(waiver)}
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
      <NxExternalLink href="http://links.sonatype.com/products/nxiq/doc/waivers">help documentation.</NxExternalLink>
    </span>
  );

  return (
    <NxTable id="list-waivers-page-waiver-table">
      <NxTableHead>
        <NxTableRow>
          <NxTableCell>DATE CREATED</NxTableCell>
          <NxTableCell>SCOPE</NxTableCell>
          <NxTableCell>COMPONENTS</NxTableCell>
          <NxTableCell>WAIVER EXPIRATION</NxTableCell>
          <NxTableCell>CREATED BY</NxTableCell>
          <NxTableCell>COMMENTS</NxTableCell>
          <NxTableCell> </NxTableCell>
        </NxTableRow>
      </NxTableHead>
      <NxTableBody
        emptyMessage={emptyMessage}
        isLoading={loadingApplicableWaivers}
        error={Messages.getHttpErrorMessage(loadApplicableWaiversError)}
        retryHandler={reloadApplicableWaivers}
      >
        {activeWaivers && map(displayWaiverInTableRow(false), sort(descend(prop('createTime')), activeWaivers))}
        {expiredWaivers && map(displayWaiverInTableRow(true), sort(descend(prop('createTime')), expiredWaivers))}
      </NxTableBody>
    </NxTable>
  );
}

ListWaiversTable.propTypes = {
  activeWaivers: PropTypes.arrayOf(PropTypes.shape(waiverType)),
  expiredWaivers: PropTypes.arrayOf(PropTypes.shape(waiverType)),
  setWaiverToDelete: PropTypes.func.isRequired,
  reloadApplicableWaivers: PropTypes.func.isRequired,
  violationDetails: PropTypes.shape({
    ...violationDetailsPropTypes,
    constraintViolations: constraintViolationsPropType.isRequired,
    displayName: PropTypes.shape({
      parts: PropTypes.arrayOf(PropTypes.object),
    }),
    filename: PropTypes.string,
    policyViolationId: PropTypes.string.isRequired,
  }),
  loadApplicableWaiversError: PropTypes.oneOfType([PropTypes.string, PropTypes.instanceOf(Error), PropTypes.object]),
  loadingApplicableWaivers: PropTypes.bool,
};
