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
  NxFontAwesomeIcon
} from '@sonatype/react-shared-components';
import { faTrashAlt } from '@fortawesome/free-solid-svg-icons/index';

import ComponentDisplay from '../ComponentDisplay/ReactComponentDisplay';
import { violationDetailsPropTypes } from '../violation/ViolationDetailsTile';
import { constraintViolationsPropType } from '../violation/PolicyViolationConstraintInfoTile';
import NxExternalLink from '../react/NxExternalLink';

export default function ListWaiversTable(props) {
  const {
    activeWaivers,
    expiredWaivers,
    violationDetails,
    setWaiverToDelete
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
    const key = waiver.policyWaiverId;
    return (
      <NxTableRow className={ rowClass }
                  key={ key }>
        <NxTableCell className="visual-testing-ignore">{ moment(waiver.createTime).format('MM/DD/YYYY') }</NxTableCell>
        <NxTableCell>{ displayWaiverScope(waiver) }</NxTableCell>
        <NxTableCell>
          { waiver.hash ? <ComponentDisplay component={ violationDetails } truncate={true} /> : 'All' }
        </NxTableCell>
        <NxTableCell>{ waiver.expiryTime ? moment(waiver.expiryTime).fromNow() : 'Does not expire' }</NxTableCell>
        <NxTableCell>{ waiver.comment || '- -' }</NxTableCell>
        <NxTableCell>
          <NxButton variant="tertiary"
                    key={ key }
                    className="list-waivers-row__delete-btn"
                    iconOnly={ true }
                    onClick={ () => setWaiverToDelete(waiver) }>
            <NxFontAwesomeIcon icon={faTrashAlt} />
          </NxButton>
        </NxTableCell>
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
          our <NxExternalLink
            href={'https://help.sonatype.com/iqserver/reporting/application-composition-report/waivers'}>help
          documentation.</NxExternalLink>
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
          <NxTableCell>{' '}</NxTableCell>
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
  setWaiverToDelete: PropTypes.func.isRequired,
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
