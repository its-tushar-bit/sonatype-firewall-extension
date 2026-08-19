/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import FirewallPolicyViolationsTile from 'MainRoot/firewall/firewallComponentDetailsPage/policyViolations/policyViolationsTile/FirewallPolicyViolationsTile';
import { mergeDeepRight } from 'ramda';
import { firewallTestData, firewallViolationsTestData } from 'TestRoot/firewall/firewallComponentDetailsPage/data';

describe('FirewallPolicyViolationsTile', () => {
  let renderComponent;
  const state = {
    router: {
      currentParams: {
        pathname: 'ant/ant/1.6.1/ant-1.6.1.jar',
        repositoryId: 'repositoryId',
        componentDisplayName: 'ant : ant : 1.6.1',
      },
      currentState: {
        name: 'firewall',
      },
    },
    firewall: firewallTestData,
  };

  beforeEach(() => {
    renderComponent = (props, preloadedState = state) =>
      render(<FirewallPolicyViolationsTile {...props} />, { preloadedState });
  });

  it('render Tile component with title and empty table', () => {
    renderComponent({ title: 'Policy violations title', violations: [] });
    const tileTitle = screen.getByRole('heading');
    const emptyMessage = screen.getByRole('row', { name: 'No policy violations' });
    const viewWaiversButton = screen.getByRole('button', { name: 'View Existing Waivers' });
    expect(tileTitle).toBeVisible();
    expect(emptyMessage).toBeVisible();
    expect(viewWaiversButton).toBeVisible();
    expect(tileTitle).toHaveTextContent('Policy violations title');
  });

  it('render Tile component with title and populated table', () => {
    renderComponent({
      title: 'Policy violations title',
      violations: firewallViolationsTestData,
    });
    const tileTitle = screen.getByRole('heading');
    const rows = screen.getAllByRole('row');
    expect(tileTitle).toBeVisible();
    expect(rows.length).toBe(4);
    expect(tileTitle).toHaveTextContent('Policy violations title');
  });

  it('render Tile component loading indicator', () => {
    const preloadedState = mergeDeepRight(state, {
      firewall: {
        componentDetailsPage: {
          isLoadingPolicyViolations: true,
        },
      },
    });
    renderComponent({ title: 'Policy violations loading', violations: [] }, preloadedState);
    const tileTitle = screen.getByRole('heading');
    const loading = screen.getByText('Loading…');
    expect(tileTitle).toBeVisible();
    expect(loading).toBeVisible();
    expect(tileTitle).toHaveTextContent('Policy violations loading');
  });

  it('render Tile component with error', () => {
    const preloadedState = mergeDeepRight(state, {
      firewall: {
        componentDetailsPage: {
          policyViolationsError: 'some violations error',
        },
      },
    });
    renderComponent({ title: 'Policy violations', violations: [] }, preloadedState);
    const tileTitle = screen.getByRole('heading');
    const error = screen.getByRole('alert');
    expect(tileTitle).toBeVisible();
    expect(error).toBeVisible();
    expect(tileTitle).toHaveTextContent('Policy violations');
    expect(error).toHaveTextContent('some violations error');
  });
});
