/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { policiesComparator } from 'MainRoot/OrgsAndPolicies/utility/util';
import { fireEvent, screen, within } from 'TestRoot/SpecUtil';
import { find, propEq, filter, clone, prop, reverse, sortWith } from 'ramda';
import { verifyThreatLevelIndicator, verifyHeaderCell } from '../utils/tileAndTableTestingUtils';

export function getNumberOfColumns(actionStages) {
  return actionStages.length + 3; // threatLevel, policyName, one colum for each actionStage and one clickable arrow
}

function getSortedPolicies(policies, sorting) {
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

  const localPolicies = find(propEq('inherited', false), ownerWithPolicies ?? []);
  const inheritedPolicies = filter(propEq('inherited', true), ownerWithPolicies ?? []);

  //one rowgroup for thead other tbody for local policies and a tbody for each tbody belonging to an inheritance policies
  groups = within(table).getAllByRole('rowgroup');
  expect(groups.length).toBe(2 + inheritedPolicies.length);

  const sortingEnabled = ownerWithPolicies.some((owner) => owner?.policies?.length > 1);

  verifyTableHead(groups[0], actionStages, sortStrategy, isFirewallSupported, isEnforcementSupported, sortingEnabled);
  verifyTableBody(
    groups.filter((el) => el.tagName === 'TBODY'),
    goToEditPolicySpy,
    localPolicies,
    inheritedPolicies,
    actionStages,
    sortStrategy,
    isFirewallSupported,
    isEnforcementSupported
  );
}

function verifyTableHead(
  thead,
  actionStages,
  sortStrategy,
  isFirewallSupported,
  isEnforcementSupported,
  sortingEnabled
) {
  let rows, headers;

  const totalOfColumns = getNumberOfColumns(actionStages);
  rows = within(thead).getAllByRole('row');
  expect(rows.length).toBe(1);
  headers = within(rows[0]).getAllByRole('columnheader');
  expect(headers.length).toBe(totalOfColumns);

  verifyHeaderCell(headers[0], sortingEnabled, '', sortStrategy.key === 'threatLevel', sortStrategy.dir);
  verifyHeaderCell(headers[1], sortingEnabled, 'Name', sortStrategy.key === 'name', sortStrategy.dir);

  if (!isNilOrEmpty(actionStages)) {
    for (const stage of actionStages) {
      const index = actionStages.indexOf(stage);
      const stageShouldBeDisable = isStageDisable(stage.stageTypeId, isFirewallSupported, isEnforcementSupported);
      verifyHeaderCell(
        headers[2 + index],
        sortingEnabled,
        stage.shortName,
        sortStrategy.key === stage.stageTypeId,
        sortStrategy.dir,
        stageShouldBeDisable
      );
    }
  }
}

function verifyTableBody(
  tbodies,
  goToEditPolicySpy,
  localPolicies,
  inheritedPolicies,
  actionStages,
  sortingState,
  isFirewallSupported,
  isEnforcementSupported
) {
  let rows = [],
    editButton;
  const totalOfColumns = getNumberOfColumns(actionStages);

  tbodies.forEach((tbody) => {
    const polciesRow = within(tbody).getAllByRole('row');
    rows = rows.concat(polciesRow);
  });

  const allPolicies = [
    ...localPolicies?.policies,
    ...inheritedPolicies.map((inheritedPolicy) => inheritedPolicy.policies).flat(),
  ];

  // get the total of empty rows
  let emptyRowsMessage = 0;
  if (isNilOrEmpty(localPolicies.policies)) emptyRowsMessage++;
  inheritedPolicies.forEach((inheritedPolicy) => {
    if (isNilOrEmpty(inheritedPolicy.policies)) emptyRowsMessage++;
  });

  // add +1 for the local polcy header
  const collapsibleHeaderRows = inheritedPolicies.length + 1 + emptyRowsMessage;

  if (isNilOrEmpty(allPolicies)) {
    expect(rows.length).toBe(2);
    expect(screen.getByRole('cell', { name: `Local to ${localPolicies?.ownerName}` })).toBeVisible();
    expect(screen.getByRole('cell', { name: 'No local policies defined' })).toBeVisible();
  } else {
    expect(rows.length).toBe(allPolicies.length + collapsibleHeaderRows);

    let sortedPolicies = getSortedPolicies(allPolicies, sortingState);

    // removed all collapsible rows headers and empty rows
    rows = rows.filter((row) => {
      let cells = within(row).getAllByRole('cell');
      return cells.length === totalOfColumns;
    });

    // check row sorting
    rows.forEach((row) => {
      const index = rows.indexOf(row);
      let policy = sortedPolicies[index];

      let cells = within(row).getAllByRole('cell');
      expect(cells).toHaveLength(totalOfColumns);
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
    });
  }
}
