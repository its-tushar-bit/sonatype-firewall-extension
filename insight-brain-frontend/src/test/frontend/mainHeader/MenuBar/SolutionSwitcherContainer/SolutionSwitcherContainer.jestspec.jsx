/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { render, axiosMockAdapter, screen } from 'TestRoot/SpecUtil';
import { fireEvent, within } from '@testing-library/react';
import SolutionSwitcherContainer from 'MainRoot/mainHeader/MenuBar/SolutionSwitcherContainer/SolutionSwitcherContainer';
import { getLicensedSolutionsUrl } from 'MainRoot/util/CLMLocation';
import React from 'react';

describe('SolutionSwitcher', function () {
  let renderComponent;
  let axiosMock;

  beforeAll(function () {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    renderComponent = () => render(<SolutionSwitcherContainer />);
  });

  it('should render a loading message when network call is pending', async () => {
    axiosMock.onGet(getLicensedSolutionsUrl()).reply(200, []);
    const wrapper = renderComponent();
    const SolutionSwitcherComponent = wrapper.getByRole('button', { name: 'Solution Switcher' });
    fireEvent.click(SolutionSwitcherComponent);

    expect(screen.getByRole('status')).toBeInTheDocument();
  });

  it('it renders all solutions under "Explore" if user has no licensed solutions', async () => {
    axiosMock.onGet(getLicensedSolutionsUrl()).reply(200, []);
    const wrapper = renderComponent();
    const SolutionSwitcherComponent = wrapper.getByRole('button', { name: 'Solution Switcher' });
    fireEvent.click(SolutionSwitcherComponent);

    const exploreSection = screen.getByRole('region', { name: 'Explore' });
    within(exploreSection).getByText('Explore');
    within(exploreSection).getByRole('link', { name: 'Lifecycle' });
    within(exploreSection).getByRole('link', { name: 'Nexus Repository Manager' });
    within(exploreSection).getByRole('link', { name: 'Repository Firewall' });
    within(exploreSection).getByRole('link', { name: 'SBOM Manager' });
  });

  it('it renders all solutions under "Explore" if there is an error', async () => {
    axiosMock.onGet(getLicensedSolutionsUrl()).reply(404, 'Error');
    const wrapper = renderComponent();
    const SolutionSwitcherComponent = wrapper.getByRole('button', { name: 'Solution Switcher' });
    fireEvent.click(SolutionSwitcherComponent);

    const exploreSection = screen.getByRole('region', { name: 'Explore' });
    within(exploreSection).getByText('Explore');
    within(exploreSection).getByRole('link', { name: 'Lifecycle' });
    within(exploreSection).getByRole('link', { name: 'Nexus Repository Manager' });
    within(exploreSection).getByRole('link', { name: 'Repository Firewall' });
    within(exploreSection).getByRole('link', { name: 'SBOM Manager' });
  });

  it('it renders the solutions under the correct sections after fetch', async () => {
    const mockResponse = [
      {
        id: 'firewall',
        url: 'firewalllink',
      },
      {
        id: 'lifecycle',
        url: 'lifecyclelink',
      },
      {
        id: 'developer',
        url: 'developerlink',
      },
    ];

    axiosMock.onGet(getLicensedSolutionsUrl()).reply(200, mockResponse);
    const wrapper = renderComponent();
    const SolutionSwitcherComponent = wrapper.getByRole('button', { name: 'Solution Switcher' });

    fireEvent.click(SolutionSwitcherComponent);
    expect(await screen.findByText('Explore')).toBeInTheDocument();
    expect(await screen.findByText('My Sonatype Solutions')).toBeInTheDocument();

    const mySonatypeSection = screen.getByRole('region', { name: 'My Sonatype Solutions' });
    within(mySonatypeSection).getByText('My Sonatype Solutions');
    const lifecycle = within(mySonatypeSection).getByRole('link', { name: 'Lifecycle' });
    expect(lifecycle).toHaveAttribute('href', 'lifecyclelink');
    const firewall = within(mySonatypeSection).getByRole('link', { name: 'Repository Firewall' });
    expect(firewall).toHaveAttribute('href', 'firewalllink');
    const developer = within(mySonatypeSection).getByRole('link', { name: 'Developer' });
    expect(developer).toHaveAttribute('href', 'developerlink');

    const exploreSection = screen.getByRole('region', { name: 'Explore' });
    within(exploreSection).getByText('Explore');
    within(exploreSection).getByRole('link', { name: 'Nexus Repository Manager' });
    within(exploreSection).getByRole('link', { name: 'SBOM Manager' });
  });

  it('renders AI Developer once under "My Sonatype Solutions" (not duplicated in "Explore") for an AI Developer license', async () => {
    // The backend reports the new AI Developer SKU as id 'aiDeveloper' (GUIDE-3124), but the
    // switcher package keys AI Developer as 'guide' in its default list. The slice canonicalizes
    // 'aiDeveloper' -> 'guide' so it is matched against the default list and not shown again in
    // Explore.
    axiosMock.onGet(getLicensedSolutionsUrl()).reply(200, [{ id: 'aiDeveloper', url: 'aidevlink' }]);
    const wrapper = renderComponent();
    fireEvent.click(wrapper.getByRole('button', { name: 'Solution Switcher' }));

    const mySonatypeSection = await screen.findByRole('region', { name: 'My Sonatype Solutions' });
    const aiDeveloper = within(mySonatypeSection).getByRole('link', { name: 'AI Developer' });
    expect(aiDeveloper).toHaveAttribute('href', 'aidevlink');

    const exploreSection = screen.getByRole('region', { name: 'Explore' });
    expect(within(exploreSection).queryByRole('link', { name: 'AI Developer' })).not.toBeInTheDocument();
  });

  it('it should not render developer under "Explore" even if user has no licensed solutions', () => {
    axiosMock.onGet(getLicensedSolutionsUrl()).reply(200, []);
    const wrapper = renderComponent();
    const SolutionSwitcherComponent = wrapper.getByRole('button', { name: 'Solution Switcher' });
    fireEvent.click(SolutionSwitcherComponent);

    const exploreSection = screen.getByRole('region', { name: 'Explore' });
    expect(within(exploreSection).queryByRole('link', { name: 'Developer' })).not.toBeInTheDocument();
  });
});
