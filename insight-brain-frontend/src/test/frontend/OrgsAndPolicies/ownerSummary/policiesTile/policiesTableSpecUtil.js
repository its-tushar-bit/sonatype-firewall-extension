/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { policiesComparator } from 'MainRoot/OrgsAndPolicies/utility/util';
import { fireEvent, screen, within } from 'TestRoot/SpecUtil';
import { clone, prop, reverse, sortWith } from 'ramda';
import { verifyThreatLevelIndicator, verifyHeaderCell } from '../utils/tileAndTableTestingUtils';

export function getNumberOfColumns(actionStages) {
  return actionStages.length + 3; // threatLevel, policyName, one colum for each actionStage and one clickable arrow
}

function getSortedPolicies(ownerName, policies, sortingConfig) {
  const sorting = sortingConfig[ownerName];
  const customSort = sortWith(policiesComparator(prop(sorting.key), sorting.key));
  const sortedPolicies = clone(policies);
  sorting.dir === 'asc' ? customSort(sortedPolicies) : reverse(customSort(sortedPolicies));
  return sortedPolicies;
}

function isStageDisable(stageTypeId, isFirewallSupported, isEnforcementSupported) {
  return (isFirewallSupported && stageTypeId === 'proxy') || isEnforcementSupported;
}

export function verifyPoliciesTable(
  table,
  goToEditPolicySpy,
  ownerWithPolicies,
  actionStages,
  sortStrategy,
  isFirewallSupported,
  isEnforcementSupported
) {
  let groups;

  //one rowgroup for thead other for tbody
  groups = within(table).getAllByRole('rowgroup');
  expect(groups.length).toBe(2);

  const sortingEnabled = within(groups[1]).getAllByRole('row').length > 1;

  verifyTableHead(
    groups[0],
    ownerWithPolicies.ownerName,
    actionStages,
    sortStrategy,
    isFirewallSupported,
    isEnforcementSupported,
    sortingEnabled
  );
  verifyTableBody(
    groups[1],
    goToEditPolicySpy,
    ownerWithPolicies,
    actionStages,
    sortStrategy,
    isFirewallSupported,
    isEnforcementSupported
  );
}

function verifyTableHead(
  thead,
  ownerName,
  actionStages,
  sortStrategy,
  isFirewallSupported,
  isEnforcementSupported,
  sortingEnabled
) {
  let rows, headers;
  const sorting = sortStrategy[ownerName];

  const totalOfColumns = getNumberOfColumns(actionStages);
  rows = within(thead).getAllByRole('row');
  expect(rows.length).toBe(1);
  headers = within(rows[0]).getAllByRole('columnheader');
  expect(headers.length).toBe(totalOfColumns);

  verifyHeaderCell(headers[0], sortingEnabled, '', sorting.key === 'threatLevel', sorting.dir);
  verifyHeaderCell(headers[1], sortingEnabled, 'Name', sorting.key === 'name', sorting.dir);

  if (!isNilOrEmpty(actionStages)) {
    for (const stage of actionStages) {
      const index = actionStages.indexOf(stage);
      const stageShouldBeDisable = isStageDisable(stage.stageTypeId, isFirewallSupported, isEnforcementSupported);
      verifyHeaderCell(
        headers[2 + index],
        sortingEnabled,
        stage.shortName,
        sorting.key === stage.stageTypeId,
        sorting.dir,
        stageShouldBeDisable
      );
    }
  }
}

function verifyTableBody(
  tbody,
  goToEditPolicySpy,
  ownerWithPolicies,
  actionStages,
  sortingState,
  isFirewallSupported,
  isEnforcementSupported
) {
  let policies, rows, editButton;
  const totalOfColumns = getNumberOfColumns(actionStages);

  rows = within(tbody).getAllByRole('row');
  policies = ownerWithPolicies.policies;
  if (isNilOrEmpty(policies)) {
    expect(rows.length).toBe(1);
    expect(screen.getByRole('cell', { name: 'No local policies defined' })).toBeVisible();
  } else {
    expect(rows.length).toBe(policies.length);

    let sortedPolicies = getSortedPolicies(ownerWithPolicies.ownerName, ownerWithPolicies.policies, sortingState);

    for (const row of rows) {
      const index = rows.indexOf(row);
      let policy = sortedPolicies[index];

      let cells = within(row).getAllByRole('cell');
      expect(cells).toHaveSize(totalOfColumns);
      verifyThreatLevelIndicator(row, policy.threatLevel);
      expect(within(row).getByRole('cell', { name: policy.name })).toBeVisible();
      editButton = within(row).getByRole('button', { name: `Edit ${policy.name} policy` });
      expect(editButton).not.toBeNull();
      expect(editButton).toBeVisible();
      fireEvent.click(editButton);
      expect(goToEditPolicySpy).toHaveBeenCalledWith(policy.id);

      if (!isNilOrEmpty(actionStages)) {
        for (const stage of actionStages) {
          const stageShouldBeDisable = isStageDisable(stage.stageTypeId, isFirewallSupported, isEnforcementSupported);
          const index = actionStages.indexOf(stage);
          const cell = cells[2 + index];
          if (isNilOrEmpty(policy.enforcementAction[stage.stageTypeId])) {
            expect(within(cell).getByText('—')).toBeVisible();
          } else {
            expect(within(cell).getByText(policy.enforcementAction[stage.stageTypeId])).toBeVisible();
          }
          if (stageShouldBeDisable) {
            expect(cell).toHaveClass('policy-tile__cell--disabled');
          }
        }
      }
    }
  }
}
