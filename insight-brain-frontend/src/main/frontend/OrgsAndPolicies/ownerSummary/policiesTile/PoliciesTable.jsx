/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { useDispatch } from 'react-redux';

import classnames from 'classnames';
import { NxTable, NxThreatIndicator, NxTooltip, NxOverflowTooltip } from '@sonatype/react-shared-components';
import { find, propEq, filter, isEmpty } from 'ramda';
import { actions } from 'MainRoot/OrgsAndPolicies/policySlice';
import IqCollapsibleRow from 'MainRoot/react/IqCollapsibleRow/IqCollapsibleRow';

export default function PoliciesTable({
  ariaLabel,
  emptyMessage = 'No policies defined',
  policiesByOwner,
  stages,
  isFirewallSupported,
  isEnforcementSupported,
  collapsibleSorting,
  isNavigationDisabled,
}) {
  const dispatch = useDispatch();

  const goToEditPolicy = (policyId) => !isNavigationDisabled && dispatch(actions.goToEditPolicy(policyId));
  const changeSortField = (sorting) => dispatch(actions.changeCollapsibleSortField(sorting));

  const local = find(propEq('inherited', false), policiesByOwner ?? []);
  const inherited = filter(propEq('inherited', true), policiesByOwner ?? []);

  const getSortCollapseDir = (fieldName) => {
    const { key, dir } = collapsibleSorting;
    return key === fieldName ? dir : null;
  };

  const sortingEnabled = policiesByOwner.some((owner) => owner?.policies?.length > 1);

  const sort = (fieldName) => {
    if (isEnforcementSupportedForStage(fieldName) && sortingEnabled) {
      changeSortField({
        key: fieldName,
        dir: getSortCollapseDir(fieldName) === 'asc' ? 'desc' : 'asc',
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

  function renderTableHeader() {
    return (
      <NxTable.Head>
        <NxTable.Row>
          <NxTable.Cell
            isSortable={sortingEnabled}
            sortDir={getSortCollapseDir('threatLevel')}
            onClick={() => sort('threatLevel')}
          />
          <NxTable.Cell isSortable={sortingEnabled} sortDir={getSortCollapseDir('name')} onClick={() => sort('name')}>
            <div className="policy-tile__cell--overflow">Name</div>
          </NxTable.Cell>
          {stages?.map((stage) => (
            <NxTable.Cell
              isSortable={sortingEnabled}
              sortDir={getSortCollapseDir(stage.stageTypeId)}
              onClick={() => sort(stage.stageTypeId)}
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
        isClickable={!isNavigationDisabled}
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
                <NxTooltip title={policy?.hasLocalActionsOverrides ? 'Policy Actions are overridden' : ''}>
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
        <NxTable.Cell chevron={!isNavigationDisabled} />
      </NxTable.Row>
    );
  }

  const renderPolicies = (policies) => {
    if (isEmpty(policies)) return null;
    return policies.map(renderTableRow);
  };

  return (
    <NxTable aria-label={ariaLabel}>
      {renderTableHeader()}
      {local && (
        <NxTable.Body
          emptyMessage={emptyMessage}
          className="iq-policy-table iq-policy-table-local-section"
          aria-label="Policy rows local policies"
        >
          <IqCollapsibleRow
            key={local?.ownerName}
            headerTitle={`Local to ${local?.ownerName}`}
            noItemsMessage={`No local policies defined`}
            isCollapsible={false}
            textEllipsis={true}
          >
            {renderPolicies(local?.policies)}
          </IqCollapsibleRow>
        </NxTable.Body>
      )}
      {inherited &&
        inherited?.map((owner) => (
          <NxTable.Body
            emptyMessage={emptyMessage}
            key={owner.ownerName}
            aria-label={`Inherited from ${owner.ownerName}`}
            className="iq-policy-table iq-policy-table-inherited-section"
          >
            <IqCollapsibleRow
              headerTitle={`Inherited from ${owner.ownerName}`}
              noItemsMessage={`No ${owner.ownerName} policies defined`}
              isCollapsible={true}
            >
              {renderPolicies(owner.policies)}
            </IqCollapsibleRow>
          </NxTable.Body>
        ))}
    </NxTable>
  );
}

PoliciesTable.propTypes = {
  ariaLabel: PropTypes.string.isRequired,
  emptyMessage: PropTypes.string,
  policiesByOwner: PropTypes.arrayOf(
    PropTypes.shape({
      ownerId: PropTypes.string,
      ownerName: PropTypes.string,
      ownerType: PropTypes.string,
      inherited: PropTypes.bool,
      policies: PropTypes.arrayOf(PropTypes.object),
    })
  ).isRequired,
  stages: PropTypes.arrayOf(PropTypes.object),
  isFirewallSupported: PropTypes.bool,
  isEnforcementSupported: PropTypes.bool,
  isNavigationDisabled: PropTypes.bool,
  sorting: PropTypes.object,
  collapsibleSorting: PropTypes.shape({
    key: PropTypes.string,
    dir: PropTypes.string,
  }),
};
