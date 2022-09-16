/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useDispatch } from 'react-redux';

import { actions } from 'MainRoot/OrgsAndPolicies/policySlice';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';
import { NxTable, NxThreatIndicator, NxTooltip, NxOverflowTooltip } from '@sonatype/react-shared-components';

export default function PoliciesTable({
  ariaLabel,
  emptyMessage = 'No policies defined',
  owner,
  stages,
  isFirewallSupported,
  isEnforcementSupported,
  sorting,
}) {
  const dispatch = useDispatch();

  const goToEditPolicy = (policyId) => dispatch(actions.goToEditPolicy(policyId));
  const changeSortField = (sorting) => dispatch(actions.changeSortField(sorting));

  const getSortDir = (ownerName, fieldName) => {
    const { key, dir } = sorting[ownerName];
    return key === fieldName ? dir : null;
  };

  const sortingEnabled = owner?.policies?.length > 1;

  const sort = (ownerName, fieldName) => {
    if (isEnforcementSupportedForStage(fieldName) && sortingEnabled) {
      changeSortField({
        ownerName,
        key: fieldName,
        dir: getSortDir(ownerName, fieldName) === 'asc' ? 'desc' : 'asc',
      });
    }
  };

  const isEnforcementSupportedForStage = (stageTypeId) => {
    return (isFirewallSupported && stageTypeId === 'proxy') || isEnforcementSupported;
  };

  const getCellClassNames = (stage) =>
    classnames(stage.stageTypeId, {
      'policy-tile__cell--disabled': !isEnforcementSupportedForStage(stage.stageTypeId),
    });

  function renderTableHeader(ownerName) {
    return (
      <NxTable.Head>
        <NxTable.Row>
          <NxTable.Cell
            isSortable={sortingEnabled}
            sortDir={getSortDir(ownerName, 'threatLevel')}
            onClick={() => sort(ownerName, 'threatLevel')}
          />
          <NxTable.Cell
            isSortable={sortingEnabled}
            sortDir={getSortDir(ownerName, 'name')}
            onClick={() => sort(ownerName, 'name')}
          >
            Name
          </NxTable.Cell>
          {stages?.map((stage) => (
            <NxTable.Cell
              isSortable={sortingEnabled}
              sortDir={getSortDir(ownerName, stage.stageTypeId)}
              onClick={() => sort(ownerName, stage.stageTypeId)}
              key={stage.stageTypeId}
              className={getCellClassNames(stage)}
            >
              <div className="policy-tile__cell--overflow">
                <span>{stage.shortName}</span>
              </div>
            </NxTable.Cell>
          ))}
          <NxTable.Cell chevron />
        </NxTable.Row>
      </NxTable.Head>
    );
  }

  function renderTableRow(policy) {
    const accessibleLabel = `Edit ${policy.name} policy`;
    return (
      <NxTable.Row
        key={policy.id}
        isClickable
        onClick={() => goToEditPolicy(policy.id)}
        clickAccessibleLabel={accessibleLabel}
      >
        <NxTable.Cell>
          <span>
            <NxThreatIndicator policyThreatLevel={policy.threatLevel} />
            <span className="nx-threat-number">{policy.threatLevel}</span>
          </span>
        </NxTable.Cell>
        <NxTable.Cell>
          <NxOverflowTooltip>
            <div className="nx-truncate-ellipsis">
              {policy.hasLocalActionsOverrides && (
                <NxTooltip title={!!policy.hasLocalActionsOverrides ? 'Policy Actions are overridden' : ''}>
                  <span>*</span>
                </NxTooltip>
              )}
              {policy.name}
            </div>
          </NxOverflowTooltip>
        </NxTable.Cell>
        {stages?.map((stage) => (
          <NxTable.Cell key={stage.stageTypeId} className={getCellClassNames(stage)}>
            {policy.enforcementAction[stage.stageTypeId] ? (
              <span className={`policy-tile__enforcement-action ${policy.enforcementAction[stage.stageTypeId]}`}>
                {policy.enforcementAction[stage.stageTypeId]}
              </span>
            ) : (
              '—'
            )}
          </NxTable.Cell>
        ))}
        <NxTable.Cell chevron />
      </NxTable.Row>
    );
  }

  return (
    <NxTable aria-label={ariaLabel}>
      {owner && renderTableHeader(owner?.ownerName)}
      <NxTable.Body emptyMessage={emptyMessage}>{owner?.policies.map(renderTableRow)}</NxTable.Body>
    </NxTable>
  );
}

PoliciesTable.propTypes = {
  ariaLabel: PropTypes.string.isRequired,
  emptyMessage: PropTypes.string,
  owner: PropTypes.shape({
    ownerId: PropTypes.string,
    ownerName: PropTypes.string,
    ownerType: PropTypes.string,
    inherited: PropTypes.bool,
    policies: PropTypes.arrayOf(PropTypes.object),
  }),
  stages: PropTypes.arrayOf(PropTypes.object),
  isFirewallSupported: PropTypes.bool,
  isEnforcementSupported: PropTypes.bool,
  sorting: PropTypes.object,
};
