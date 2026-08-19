/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import * as PropTypes from 'prop-types';
import { map, pipe, reject } from 'ramda';
import { useDispatch } from 'react-redux';
import { NxThreatIndicator, NxTable } from '@sonatype/react-shared-components';
import { actions } from 'MainRoot/OrgsAndPolicies/licenseThreatGroupSlice';
import { isEmptyNonLocal, formatCollapsibleThreatGroups } from 'MainRoot/OrgsAndPolicies/utility/util';
import IqCollapsibleRow from 'MainRoot/react/IqCollapsibleRow/IqCollapsibleRow';

export default function ApplicableLicenseThreatGroupTable({ applicableLTGs }) {
  const formattedThreatGroups = pipe(reject(isEmptyNonLocal), map(formatCollapsibleThreatGroups))(applicableLTGs);
  const dispatch = useDispatch();

  const renderRow = (licenseThreatGroup) => {
    return (
      <Fragment>
        <NxTable.Cell>
          <NxThreatIndicator policyThreatLevel={licenseThreatGroup.threatLevel} />
          <span className="nx-threat-number">{licenseThreatGroup.threatLevel}</span>
        </NxTable.Cell>
        <NxTable.Cell>{licenseThreatGroup.name}</NxTable.Cell>
        {!licenseThreatGroup.inherited ? <NxTable.Cell chevron /> : <NxTable.Cell />}
      </Fragment>
    );
  };

  const getRows = (licenseThreatGroups) => {
    if (!licenseThreatGroups?.length) return null;

    return licenseThreatGroups.map((threatGroup) => {
      if (threatGroup.inherited) {
        return (
          <NxTable.Row className="iq-ltg-summary-table-row" key={threatGroup.id}>
            {renderRow(threatGroup)}
          </NxTable.Row>
        );
      }
      const goToEditLTG = () => dispatch(actions.goToEditLTG(threatGroup.id));
      const accessibleLabel = `Edit ${threatGroup.name} License Threat Group`;
      return (
        <NxTable.Row
          className="iq-ltg-summary-table-row"
          isClickable
          key={threatGroup.id}
          onClick={goToEditLTG}
          clickAccessibleLabel={accessibleLabel}
        >
          {renderRow(threatGroup)}
        </NxTable.Row>
      );
    });
  };

  return (
    <NxTable className="iq-ltg-summary-table">
      <NxTable.Head>
        <NxTable.Row>
          <NxTable.Cell>THREAT</NxTable.Cell>
          <NxTable.Cell>NAME</NxTable.Cell>
          <NxTable.Cell aria-label="view threat group" />
        </NxTable.Row>
      </NxTable.Head>
      {formattedThreatGroups.map((group) => (
        <NxTable.Body
          key={group.headerTitle}
          className={`iq-ltg-table-subsection ${
            group.inherited ? 'iq-ltg-table-inherited-section' : 'iq-ltg-table-local-section'
          }`}
        >
          <IqCollapsibleRow
            headerTitle={group.headerTitle}
            noItemsMessage={group.emptyMessage}
            isCollapsible={group.inherited}
          >
            {getRows(group.sortedThreatGroups)}
          </IqCollapsibleRow>
        </NxTable.Body>
      ))}
    </NxTable>
  );
}

ApplicableLicenseThreatGroupTable.propTypes = {
  applicableLTGs: PropTypes.arrayOf(PropTypes.object),
};
