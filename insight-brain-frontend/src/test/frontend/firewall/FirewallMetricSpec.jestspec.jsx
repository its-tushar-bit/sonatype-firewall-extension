/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import FirewallMetrics from 'MainRoot/firewall/FirewallMetrics';
import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import * as routerContext from '../../../main/frontend/react/RouterStateContext';

describe('FirewallMetrics', () => {
  let routerContextMock, hrefSpyForReactRouterLinks;

  const metrics = {
    supplyChainAttacksBlocked: 1111,
    namespaceAttacksBlocked: 2222,
    componentsQuarantined: 3333,
    componentsAutoReleased: 4444,
    saferVersionsSelectedAutomatically: 5555,
    waivedComponents: 6666,
    onSupplyChainAttacksBlockedLinkClick: jest.fn(),
    onNamespaceAttacksBlockedLinkClick: jest.fn(),
    onComponentsQuarantinedLinkClick: jest.fn(),
  };

  it('link handlers are called when details buttons are clicked', () => {
    const supplyChainAttacksLinkMock = jest.fn();
    const namespaceAttacksBlockedLinkMock = jest.fn();
    const onComponentsQuarantinedLinkClickMock = jest.fn();

    render(
      <FirewallMetrics
        {...metrics}
        onSupplyChainAttacksBlockedLinkClick={supplyChainAttacksLinkMock}
        onNamespaceAttacksBlockedLinkClick={namespaceAttacksBlockedLinkMock}
        onComponentsQuarantinedLinkClick={onComponentsQuarantinedLinkClickMock}
      />
    );

    expect(supplyChainAttacksLinkMock.mock.calls.length).toBe(0);
    expect(namespaceAttacksBlockedLinkMock.mock.calls.length).toBe(0);
    expect(onComponentsQuarantinedLinkClickMock.mock.calls.length).toBe(0);

    const seeDetailsBelowButtons = screen.queryAllByRole('button', { name: 'See details below' });
    expect(seeDetailsBelowButtons.length).toBe(3);

    fireEvent.click(seeDetailsBelowButtons[0]);
    expect(supplyChainAttacksLinkMock.mock.calls.length).toBe(1);

    fireEvent.click(seeDetailsBelowButtons[1]);
    expect(namespaceAttacksBlockedLinkMock.mock.calls.length).toBe(1);

    fireEvent.click(seeDetailsBelowButtons[2]);
    expect(onComponentsQuarantinedLinkClickMock.mock.calls.length).toBe(1);
  });

  it('renders a grid with the correct content', async () => {
    hrefSpyForReactRouterLinks = jest
      .fn('href')
      .mockImplementation((stateName) =>
        stateName === 'firewall.firewallAutoUnquarantinePage' ? 'componentsAutoReleasedLink' : 'waivedComponentsLink'
      );
    routerContextMock = { href: hrefSpyForReactRouterLinks };
    jest.spyOn(routerContext, 'useRouterState').mockReturnValue(routerContextMock);

    const { container } = render(<FirewallMetrics {...metrics} />);

    expect(hrefSpyForReactRouterLinks).toHaveBeenCalledWith('firewall.waivers');
    const headings = screen.getAllByRole('heading', { level: 3 });

    expect(headings[0]).toHaveTextContent('Supply chain attacks blocked');
    expect(headings[1]).toHaveTextContent('Namespace attacks blocked');
    expect(headings[2]).toHaveTextContent('Components quarantined');
    expect(headings[3]).toHaveTextContent('Components auto-released');
    expect(headings[4]).toHaveTextContent('Safe components auto-selected');
    expect(headings[5]).toHaveTextContent('Components waived');

    const values = container.querySelectorAll('.iq-firewall-metrics-content__values');
    expect(values[0]).toHaveTextContent('1,111(all time)');
    expect(values[1]).toHaveTextContent('2,222(all time)');
    expect(values[2]).toHaveTextContent('3,333Last 12 months');
    expect(values[3]).toHaveTextContent('4,444Last 12 months');
    expect(values[4]).toHaveTextContent('5,555Last 12 months');
    expect(values[5]).toHaveTextContent('6,666Last 12 months');

    const links = screen.getAllByRole('link');
    expect(links[0]).toHaveTextContent('View auto-released components');
    expect(links[1]).toHaveTextContent('Learn more');
    expect(links[2]).toHaveTextContent('View waived components');

    const buttons = screen.getAllByRole('button');
    expect(buttons[0]).toHaveTextContent('See details below');
    expect(buttons[1]).toHaveTextContent('See details below');
    expect(buttons[2]).toHaveTextContent('See details below');

    const icons = container.querySelectorAll('.iq-firewall-metrics-content__icon');

    let tooltip;

    fireEvent.mouseOver(icons[0]);
    tooltip = await screen.findByRole('tooltip', {
      name:
        'Firewall has detected these violations for "security-malicious" policy condition and is blocking malicious components.',
    });
    expect(tooltip).toBeInTheDocument();

    fireEvent.mouseOver(icons[1]);
    tooltip = await screen.findByRole('tooltip', {
      name:
        'Firewall has detected these violations for "namespace-conflict" policy condition and is blocking components with namespace conflict.',
    });
    expect(tooltip).toBeInTheDocument();

    fireEvent.mouseOver(icons[2]);
    tooltip = await screen.findByRole('tooltip', {
      name: 'Firewall has detected and blocked these components that violate your governance policies.',
    });
    expect(tooltip).toBeInTheDocument();

    fireEvent.mouseOver(icons[3]);
    tooltip = await screen.findByRole('tooltip', {
      name: 'Firewall has cleared these previously blocked components for use.',
    });
    expect(tooltip).toBeInTheDocument();

    fireEvent.mouseOver(icons[4]);
    tooltip = await screen.findByRole('tooltip', {
      name: 'Firewall auto-selected these policy compliant components found when installing dependencies.',
    });
    expect(tooltip).toBeInTheDocument();

    fireEvent.mouseOver(icons[5]);
    tooltip = await screen.findByRole('tooltip', {
      name: 'Firewall has waived the failing policy violations for these components.',
    });
    expect(tooltip).toBeInTheDocument();
  });
});
