/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import * as PropTypes from 'prop-types';
import { useDispatch } from 'react-redux';
import { NxThreatIndicator, NxTable } from '@sonatype/react-shared-components';
import { actions } from 'MainRoot/OrgsAndPolicies/licenseThreatGroupSlice';
import { sortByThreatLevel } from 'MainRoot/OrgsAndPolicies/utility/util';

export default function ApplicableLicenseThreatGroupTable({ ownerName, licenseThreatGroups, inherited }) {
  const dispatch = useDispatch();

  const renderRow = (ltg) => {
    return (
      <Fragment>
        <NxTable.Cell>
          <NxThreatIndicator policyThreatLevel={ltg.threatLevel} />
          <span className="nx-threat-number">{ltg.threatLevel}</span>
        </NxTable.Cell>
        <NxTable.Cell>{ltg.name}</NxTable.Cell>
        {!inherited ? <NxTable.Cell chevron /> : null}
      </Fragment>
    );
  };

  const renderRows = () => {
    const sortedLTGs = sortByThreatLevel(licenseThreatGroups);
    return sortedLTGs.map((ltg) => {
      if (inherited) {
        return <NxTable.Row key={ltg.id}>{renderRow(ltg)}</NxTable.Row>;
      }
      const goToEditLTG = () => dispatch(actions.goToEditLTG(ltg.id));
      const accessibleLabel = `Edit ${ltg.name} License Threat Group`;
      return (
        <NxTable.Row isClickable key={ltg.id} onClick={goToEditLTG} clickAccessibleLabel={accessibleLabel}>
          {renderRow(ltg)}
        </NxTable.Row>
      );
    });
  };

  const name = inherited ? ownerName : 'local';
  const emptyMessage = `No ${name} threat groups defined.`;
  return (
    <NxTable className="iq-ltg-summary-table">
      <NxTable.Head>
        <NxTable.Row>
          <NxTable.Cell>THREAT</NxTable.Cell>
          <NxTable.Cell>NAME</NxTable.Cell>
          {!inherited ? <NxTable.Cell chevron /> : null}
        </NxTable.Row>
      </NxTable.Head>
      <NxTable.Body emptyMessage={emptyMessage}>{renderRows()}</NxTable.Body>
    </NxTable>
  );
}

ApplicableLicenseThreatGroupTable.propTypes = {
  ownerName: PropTypes.string,
  licenseThreatGroups: PropTypes.arrayOf(PropTypes.object),
  inherited: PropTypes.bool,
};
