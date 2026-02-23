/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import moment from 'moment';
import { render, screen, within } from 'TestRoot/SpecUtil';
import { STANDARD_DATE_FORMAT } from 'MainRoot/util/dateUtils';
import FirewallExistingWaiversPopover from 'MainRoot/firewall/firewallComponentDetailsPage/policyViolations/policyViolationsTile/FirewallExistingWaiversPopover';

import 'TestRoot/SpecUtil';

describe('FirewallExistingWaiversPopover', () => {
  let minimalProps, renderComponent, setShowComponentWaiversPopoverSpy;
  const expectedTime = 1661928739954;

  beforeEach(() => {
    setShowComponentWaiversPopoverSpy = jest.fn().mockName('setShowComponentWaiversPopover');

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
    expect(screen.getByRole('columnheader', { name: 'DURATION' })).toBeVisible();
    expect(screen.getByRole('columnheader', { name: 'WAIVER DETAILS' })).toBeVisible();
    expect(screen.getByRole('columnheader', { name: '' })).toBeVisible();

    const componentRows = screen.getAllByRole('row');
    expect(componentRows.length).toBeGreaterThanOrEqual(1);
    const firstRow = componentRows[1];
    const cells = within(firstRow).getAllByRole('cell');
    expect(cells[0]).toHaveTextContent(moment.parseZone(expectedTime).format(STANDARD_DATE_FORMAT));
    expect(cells[1]).toHaveTextContent('Admin BuiltIn');
    expect(cells[1]).toHaveTextContent('Repository - maven-central');
  });
});
