/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import JenkinsWizard from 'MainRoot/integrations/sections/CiCdWizards/Jenkins/JenkinsWizard';

describe('JenkinsWizard', () => {
  it('renders the correct install URL link', () => {
    renderComponent();
    const expectedInstallUrl = 'https://links.sonatype.com/products/nxiq/doc/integrations/jenkins/installation';
    expect(screen.getAllByRole('link', { name: 'Link' })[0]).toHaveAttribute('href', expectedInstallUrl);
  });

  it('renders the connect URL link', () => {
    renderComponent();
    const connectUrl = 'https://links.sonatype.com/products/nxiq/doc/integrations/jenkins/integrating';
    expect(screen.getAllByRole('link', { name: 'Link' })[1]).toHaveAttribute('href', connectUrl);
  });

  it('renders the evaluation URL link', () => {
    const evaluationUrl = `https://links.sonatype.com/products/nxiq/doc/integrations/jenkins/evaluation`;
    renderComponent();
    expect(screen.getAllByRole('link', { name: 'Link' })[2]).toHaveAttribute('href', evaluationUrl);
  });

  it('renders the correct iq organization', () => {
    const expectedIqOrganization = /iqOrganization/;
    renderComponent();
    expect(screen.getByText(expectedIqOrganization)).toBeInTheDocument();
  });

  it('renders the correct iq application', () => {
    const expectedIqApplication = /iqApplication/;
    renderComponent();
    expect(screen.getByText(expectedIqApplication)).toBeInTheDocument();
  });

  function renderComponent() {
    const mockIqOrganization = 'iqOrganization';
    const mockIqApplication = 'iqApplication';
    render(<JenkinsWizard iqOrganization={mockIqOrganization} iqApplication={mockIqApplication} />);
  }
});
