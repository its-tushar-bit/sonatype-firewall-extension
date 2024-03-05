/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import GitlabWizard from 'MainRoot/development/developmentDashboard/sections/CiCdWizards/GitLab/GitlabWizard';

describe('GitlabWizard', () => {
  it('renders the correct configure URL link', () => {
    renderComponent();
    const expectedInstallUrl = 'https://links.sonatype.com/products/nxiq/doc/integrations/gitlab-ci-configuration';
    expect(screen.getAllByRole('link', { name: 'View documentation' })[0]).toHaveAttribute('href', expectedInstallUrl);
  });

  it('renders the correct connection information URL link', () => {
    renderComponent();
    const connectUrl = 'https://links.sonatype.com/products/nxiq/doc/integrations/gitlab-ci-connect-info';
    expect(screen.getAllByRole('link', { name: 'View documentation' })[1]).toHaveAttribute('href', connectUrl);
  });

  it('renders the correct dockerhub image link', () => {
    const dockerUrl = 'https://links.sonatype.com/products/nxiq/doc/integrations/gitlab-docker-image';
    renderComponent();
    expect(screen.getByRole('link', { name: 'documentation provided with the image on Docker Hub' })).toHaveAttribute(
      'href',
      dockerUrl
    );
  });

  it('renders the correct sonatype documentation link', () => {
    const docUrl = `https://links.sonatype.com/products/nxiq/doc/integrations/gitlab`;
    renderComponent();
    expect(screen.getByRole('link', { name: 'Sonatype Documentation' })).toHaveAttribute('href', docUrl);
  });

  it('renders the correct iq application in code snippet', () => {
    const expectedIqApplication = 'test-public-id';
    renderComponent();
    expect(screen.getByText(expectedIqApplication)).toBeInTheDocument();
  });

  it('renders the correct data analytics id for the more information link', () => {
    renderComponent();
    expect(screen.getAllByRole('link', { name: 'Sonatype Documentation' })[0]).toHaveAttribute(
      'data-analytics-id',
      'sonatype-developer-cicd-gitlab-more-info'
    );
  });

  it('renders the correct data analytics id for the create pipeline link', () => {
    renderComponent();
    expect(screen.getAllByRole('link', { name: 'View documentation' })[0]).toHaveAttribute(
      'data-analytics-id',
      'sonatype-developer-cicd-gitlab-create-pipeline-card'
    );
  });

  it('renders the correct data analytics id for the configure link', () => {
    renderComponent();
    expect(screen.getAllByRole('link', { name: 'View documentation' })[1]).toHaveAttribute(
      'data-analytics-id',
      'sonatype-developer-cicd-gitlab-configure-card'
    );
  });

  function renderComponent() {
    const mockIqApplicationPublicId = 'test-public-id';
    render(<GitlabWizard iqApplication={mockIqApplicationPublicId} />);
  }
});
