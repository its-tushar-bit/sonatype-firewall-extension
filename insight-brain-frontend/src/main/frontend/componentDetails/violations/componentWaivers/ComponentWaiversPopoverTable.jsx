/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

import {
  NxButton,
  NxFontAwesomeIcon,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
} from '@sonatype/react-shared-components';
import { faTrashAlt } from '@fortawesome/pro-solid-svg-icons';

import { formatDate, STANDARD_DATE_FORMAT } from '../../../util/dateUtils';
import { waiverType, displayWaiverScope } from '../../../util/waiverUtils';

export const ComponentWaiversTableRow = ({ waiver, setWaiverToDelete, componentName }) => {
  return (
    <NxTableRow>
      <NxTableCell className="iq-component-violations-waivers-table--policy-name">{waiver.policyName}</NxTableCell>
      <NxTableCell className="iq-component-violations-waivers-table--constraints">
        {waiver.constraintFacts && waiver.constraintFacts[0].constraintName}
      </NxTableCell>
      <NxTableCell className="visual-testing-ignore iq-component-violations-waivers-table--created">
        {formatDate(waiver.createTime, STANDARD_DATE_FORMAT)}
      </NxTableCell>
      <NxTableCell className="iq-component-violations-waivers-table--scope">{displayWaiverScope(waiver)}</NxTableCell>
      <NxTableCell className="iq-component-violations-waivers-table--component-name">
        {waiver.hash ? componentName : 'All'}
      </NxTableCell>
      <NxTableCell className="iq-component-violations-waivers-table--comments">{waiver.comment || '- -'}</NxTableCell>
      <NxTableCell>
        <NxButton
          onClick={() => setWaiverToDelete(waiver)}
          variant="icon-only"
          title="Delete"
          className="nx-btn--delete-waiver"
        >
          <NxFontAwesomeIcon icon={faTrashAlt} />
        </NxButton>
      </NxTableCell>
    </NxTableRow>
  );
};

ComponentWaiversTableRow.propTypes = {
  componentName: PropTypes.string,
  waiver: PropTypes.shape(waiverType),
  setWaiverToDelete: PropTypes.func.isRequired,
};

export default function ComponentWaiversPopoverTable({ componentName, waivers = [], setWaiverToDelete }) {
  return (
    <NxTable className="iq-policy-violations-table">
      <NxTableHead>
        <NxTableRow>
          <NxTableCell>Policy</NxTableCell>
          <NxTableCell>Constraint</NxTableCell>
          <NxTableCell>Created</NxTableCell>
          <NxTableCell>Scope</NxTableCell>
          <NxTableCell>Components</NxTableCell>
          <NxTableCell>Comment</NxTableCell>
          <NxTableCell />
        </NxTableRow>
      </NxTableHead>
      <NxTableBody emptyMessage="No existing component waivers">
        {waivers.map((waiver) => (
          <ComponentWaiversTableRow key={waiver.policyWaiverId} {...{ waiver, setWaiverToDelete, componentName }} />
        ))}
      </NxTableBody>
    </NxTable>
  );
}

ComponentWaiversPopoverTable.propTypes = {
  componentName: PropTypes.string,
  waivers: PropTypes.arrayOf(PropTypes.shape(waiverType)),
  setWaiverToDelete: PropTypes.func.isRequired,
};
