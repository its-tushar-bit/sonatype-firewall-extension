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

import { NxTable, NxTableBody, NxTableCell, NxTableHead, NxTableRow } from '@sonatype/react-shared-components';
import ComponentDisplay from '../ComponentDisplay/ReactComponentDisplay';
import { violationDetailsPropTypes } from '../violation/ViolationDetailsTile';
import { constraintViolationsPropType } from '../violation/PolicyViolationConstraintInfoTile';

export default function ListWaiversTable(props) {
  const {
    activeWaivers,
    expiredWaivers,
    violationDetails
  } = props;

  const displayWaiverScope = (waiver) => {
    switch (waiver.scopeOwnerType) {
      case 'root_organization': {
        return 'Root Organization';
      }
      case 'organization': {
        return `Organization - ${waiver.scopeOwnerName}`;
      }
      case 'application': {
        return `Application - ${waiver.scopeOwnerName}`;
      }
    }
    return null;
  };

  const displayWaiverInTableRow = curry((isWaiverExpired, waiver) => {
    const rowClass = classnames({ 'list-waivers-row--expired': isWaiverExpired });
    return (
      <NxTableRow className={ rowClass }
                  key={ waiver.policyWaiverId }>
        <NxTableCell>{ moment(waiver.createTime).format('MM/DD/YYYY') }</NxTableCell>
        <NxTableCell>{ displayWaiverScope(waiver) }</NxTableCell>
        <NxTableCell>
          { waiver.hash ? <ComponentDisplay component={ violationDetails } truncate={true} /> : 'All' }
        </NxTableCell>
        <NxTableCell>{ waiver.expiryTime ? moment(waiver.expiryTime).fromNow() : 'Does not expire' }</NxTableCell>
        <NxTableCell>{ waiver.comment || '- -' }</NxTableCell>
      </NxTableRow>
    );
  });

  const waiverTableBody = (
    <NxTableBody>
      { activeWaivers && map(displayWaiverInTableRow(false), sort(descend(prop('createTime')), activeWaivers)) }
      { expiredWaivers && map(displayWaiverInTableRow(true), sort(descend(prop('createTime')), expiredWaivers)) }
    </NxTableBody>
  );

  const emptyTableBody = (
    <NxTableBody>
      <NxTableRow>
        <NxTableCell colSpan='5' className='nx-cell--empty'>
          You don&#39;t have any waivers: to learn more about waivers you can check
          our <a href="https://help.sonatype.com/iqserver/reporting/application-composition-report/waivers">
          help documentation.</a>
        </NxTableCell>
      </NxTableRow>
    </NxTableBody>
  );

  const hasWaiversToDisplay = activeWaivers && activeWaivers.length || expiredWaivers && expiredWaivers.length;

  return (
    <NxTable id="list-waivers-page-waiver-table">
      <NxTableHead>
        <NxTableRow>
          <NxTableCell>DATE CREATED</NxTableCell>
          <NxTableCell>SCOPE</NxTableCell>
          <NxTableCell>COMPONENTS</NxTableCell>
          <NxTableCell>WAIVER EXPIRATION</NxTableCell>
          <NxTableCell>COMMENTS</NxTableCell>
        </NxTableRow>
      </NxTableHead>
      { hasWaiversToDisplay ? waiverTableBody : emptyTableBody }
    </NxTable>
  );
}

export const waiverType = {
  comment: PropTypes.string,
  createTime: PropTypes.string,
  hash: PropTypes.string,
  policyId: PropTypes.string,
  policyWaiverId: PropTypes.string,
  scopeOwnerId: PropTypes.string,
  scopeOwnerName: PropTypes.string,
  scopeOwnerType: PropTypes.string
};

ListWaiversTable.propTypes = {
  activeWaivers: PropTypes.arrayOf(PropTypes.shape(waiverType)),
  expiredWaivers: PropTypes.arrayOf(PropTypes.shape(waiverType)),
  violationDetails: PropTypes.shape({
    ...violationDetailsPropTypes,
    constraintViolations: constraintViolationsPropType.isRequired,
    displayName: PropTypes.shape({
      parts: PropTypes.arrayOf(PropTypes.object)
    }),
    filename: PropTypes.string,
    policyViolationId: PropTypes.string.isRequired
  })
};
