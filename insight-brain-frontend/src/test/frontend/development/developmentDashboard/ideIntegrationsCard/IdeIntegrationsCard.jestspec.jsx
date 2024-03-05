/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, within, axiosMockAdapter } from 'TestRoot/SpecUtil';
import IdeIntegrationsCard from 'MainRoot/development/developmentDashboard/sections/overview/ideIntegrationsCard/IdeIntegrationsCard';
import { getIdeIntegratedUserCount } from 'MainRoot/util/CLMLocation';

describe('IDE Integrations Card', () => {
  let axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  it('should render a loading message while network call is pending', () => {
    render(<IdeIntegrationsCard />);

    expect(screen.getByText('Loading…')).toBeInTheDocument();
  });

  describe('on successful http calls', () => {
    beforeEach(() => {
      axiosMock.onGet(getIdeIntegratedUserCount()).reply(200, {
        userCount: 434,
      });
      render(<IdeIntegrationsCard />);
    });

    it('renders a card', () => {
      const card = screen.getByRole('region', { name: /Integrate using IDEs/i });
      expect(card).toBeInTheDocument();
    });

    it('renders the correct user count', async () => {
      const card = await screen.findByRole('region', { name: /Integrate using IDEs/i });
      expect(card).toHaveTextContent('434');
    });
  });

  describe('on a failed http call', () => {
    it('does not render ', () => {
      axiosMock.onGet(getIdeIntegratedUserCount()).reply(404, 'Error');
      render(<IdeIntegrationsCard />);
      const card = screen.getByRole('region', { name: /Integrate using IDEs/i });
      expect(card).toBeInTheDocument();
      expect(within(card).queryByRole('alert')).not.toBeInTheDocument();
    });
  });
});
