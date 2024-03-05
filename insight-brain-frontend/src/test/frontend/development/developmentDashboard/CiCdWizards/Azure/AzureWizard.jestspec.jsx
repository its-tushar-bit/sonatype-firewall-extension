/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import AzureWizard from 'MainRoot/development/developmentDashboard/sections/CiCdWizards/Azure/AzureWizard';

describe('AzureWizard', () => {
  it('renders the correct download URL link', () => {
    renderComponent();
    const expectedInstallUrl = 'https://links.sonatype.com/products/nxiq/doc/integrations/azure-devops/marketplace';
    expect(screen.getAllByRole('link', { name: 'View documentation' })[0]).toHaveAttribute('href', expectedInstallUrl);
  });

  it('renders the correct install URL link', () => {
    renderComponent();
    const connectUrl = 'https://links.sonatype.com/products/nxiq/doc/integrations/azure-devops/installation';
    expect(screen.getAllByRole('link', { name: 'View documentation' })[1]).toHaveAttribute('href', connectUrl);
  });

  it('renders the correct evaluation URL link', () => {
    const evaluationUrl = `https://links.sonatype.com/products/nxiq/doc/integrations/azure-devops/evaluation`;
    renderComponent();
    expect(screen.getAllByRole('link', { name: 'View documentation' })[2]).toHaveAttribute('href', evaluationUrl);
  });

  it('renders the correct iq organization', () => {
    const expectedIqOrganization = /mockIqOrganization/;
    renderComponent();
    expect(screen.getByText(expectedIqOrganization)).toBeInTheDocument();
  });

  it('renders the correct iq application', () => {
    const expectedIqApplication = /mockIqApplication/;
    renderComponent();
    expect(screen.getByText(expectedIqApplication)).toBeInTheDocument();
  });

  it('renders the correct data analytics id for the more information link', () => {
    renderComponent();
    expect(screen.getAllByRole('link', { name: 'Sonatype Documentation' })[0]).toHaveAttribute(
      'data-analytics-id',
      'sonatype-developer-cicd-azure-more-info'
    );
  });

  it('renders the correct data analytics id for the download link', () => {
    renderComponent();
    expect(screen.getAllByRole('link', { name: 'View documentation' })[0]).toHaveAttribute(
      'data-analytics-id',
      'sonatype-developer-cicd-azure-download-card'
    );
  });

  it('renders the correct data analytics id for the install link', () => {
    renderComponent();
    expect(screen.getAllByRole('link', { name: 'View documentation' })[1]).toHaveAttribute(
      'data-analytics-id',
      'sonatype-developer-cicd-azure-install-card'
    );
  });

  it('renders the correct data analytics id for the evaluation link', () => {
    renderComponent();
    expect(screen.getAllByRole('link', { name: 'View documentation' })[2]).toHaveAttribute(
      'data-analytics-id',
      'sonatype-developer-cicd-azure-review-card'
    );
  });

  function renderComponent() {
    const mockIqOrganization = 'mockIqOrganization';
    const mockIqApplication = 'mockIqApplication';
    render(<AzureWizard iqOrganization={mockIqOrganization} iqApplication={mockIqApplication} />);
  }
});
