/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import moment from 'moment';
import { render, screen } from 'TestRoot/SpecUtil';
import FirewallExistingWaiversPopover from 'MainRoot/firewall/firewallComponentDetailsPage/policyViolations/policyViolationsTile/FirewallExistingWaiversPopover';

describe('FirewallExistingWaiversPopover', () => {
  let minimalProps, renderComponent, setShowComponentWaiversPopoverSpy;
  const expectedTime = 1661928739954;

  beforeEach(() => {
    setShowComponentWaiversPopoverSpy = jasmine.createSpy('setShowComponentWaiversPopover');

    minimalProps = {
      componentName: 'a component',
      waivers: null,
      showViolationsDetailPopover: false,
      setShowComponentWaiversPopover: setShowComponentWaiversPopoverSpy,
    };

    renderComponent = (additionalProps = {}) =>
      render(<FirewallExistingWaiversPopover {...minimalProps} {...additionalProps} />);
  });

  it('renders a FirewallExistingWaiversPopover component', () => {
    renderComponent();
    expect(screen.getByText('Component Waivers')).toBeVisible();
  });

  it('render empty table existing waivers popover', () => {
    renderComponent({ ...minimalProps, showViolationsDetailPopover: true });
    expect(screen.getByText('No existing component waivers')).toBeVisible();
  });

  it('render table existing waivers popover with data', () => {
    const waiversData = [
      {
        id: '468e1552699445d48e448bf22740ad8b',
        hash: '7a3c2521ae0c6f53e044',
        policyId: '6f085a73545f443ab92ce7a109c83935',
        ownerId: '8098fb28cfc84ff99b3c34e66d2b9ccf',
        comment: '',
        createTime: expectedTime,
        expiryTime: null,
        creatorId: 'admin',
        creatorName: 'Admin BuiltIn',
        constraintFactsJson:
          '[{"constraintId":"d17bd2a78ada49d6b40df2dd596d8e19","constraintName":"older than one day","operatorName":"AND","conditionFacts":[{"conditionTypeId":"License","conditionIndex":0,"summary":"License is \'Apache-1.1\'","reason":"Found \'Apache-1.1\' license","reference":null,"triggerJson":"{\\"conditionIndex\\":0,\\"trigger\\":{\\"id\\":\\"Apache-1.1\\"}}"}]}]',
        constraintFacts: [
          {
            constraintId: 'd17bd2a78ada49d6b40df2dd596d8e19',
            constraintName: 'older than one day',
            operatorName: 'AND',
            conditionFacts: [
              {
                conditionTypeId: 'License',
                conditionIndex: 0,
                summary: "License is 'Apache-1.1'",
                reason: "Found 'Apache-1.1' license",
                reference: null,
                triggerJson: '{"conditionIndex":0,"trigger":{"id":"Apache-1.1"}}',
              },
            ],
          },
        ],
        associatedPackageUrl: null,
        componentMatchStrategy: 'EXACT_COMPONENT',
        componentIdentifier: null,
        policyName: 'test-policy',
        policyWaiverId: '468e1552699445d48e448bf22740ad8b',
        scopeOwnerId: '8098fb28cfc84ff99b3c34e66d2b9ccf',
        scopeOwnerType: 'repository',
        scopeOwnerName: 'maven-central',
      },
    ];
    renderComponent({ ...minimalProps, showViolationsDetailPopover: true, waivers: waiversData });
    expect(screen.getByRole('table')).toBeVisible();
    expect(screen.getByRole('columnheader', { name: 'Policy/Constraint' })).toBeVisible();
    expect(screen.getByRole('columnheader', { name: 'Created' })).toBeVisible();
    expect(screen.getByRole('columnheader', { name: 'Scope' })).toBeVisible();
    expect(screen.getByRole('columnheader', { name: 'Components' })).toBeVisible();
    expect(screen.getByRole('columnheader', { name: 'Created by' })).toBeVisible();
    expect(screen.getByRole('columnheader', { name: 'Comment' })).toBeVisible();

    const componentRaws = screen.getAllByRole('row');
    expect(componentRaws.length).toBeGreaterThanOrEqual(1);

    expect(screen.getByRole('cell', { name: `Admin BuiltIn` })).toBeVisible();
    expect(screen.getByRole('cell', { name: `Repository - maven-central` })).toBeVisible();
    expect(screen.getByRole('cell', { name: moment.parseZone(expectedTime).format('MM/DD/YYYY') })).toBeVisible();
  });
});
