/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import ScmWizard from 'MainRoot/integrations/sections/scmWizard/ScmWizard';
import * as baseUrlConfigurationSelectors from 'MainRoot/configuration/baseUrl/baseUrlConfigurationSelectors';
import {
  getSCMProviderTokenDocUrl,
  getSCMProviderTokenUrl,
} from '../../../../../main/frontend/integrations/sections/scmWizard/scmWizardUtil';

describe('ScmWizard', () => {
  let selectShouldDisplayNotice;
  const ScmProvider = ['github', 'gitlab', 'azure', 'bitbucket'];
  const mockScmProvider = 'github';
  const mockApplicationPublicId = 'test-application-1';

  beforeEach(() => {
    selectShouldDisplayNotice = jest
      .spyOn(baseUrlConfigurationSelectors, 'selectShouldDisplayNotice')
      .mockReturnValue(true);
  });

  ScmProvider.forEach((scmProvider) => {
    it(`renders the correct token URL for ${scmProvider}`, () => {
      renderComponent(scmProvider, mockApplicationPublicId);
      const expectedTokenUrl = getSCMProviderTokenUrl(scmProvider);
      expect(screen.getByText(expectedTokenUrl)).toBeInTheDocument();
    });

    it(`renders the correct token doc URL for ${scmProvider}`, () => {
      renderComponent(scmProvider, mockApplicationPublicId);
      const expectedTokenDocUrl = getSCMProviderTokenDocUrl(scmProvider);
      expect(screen.getAllByRole('link', { name: 'here' })[0]).toHaveAttribute('href', expectedTokenDocUrl);
    });
  });

  it('renders the correct permission URL link', () => {
    renderComponent(mockScmProvider, mockApplicationPublicId);
    const expectedPermissionUrl = 'http://links.sonatype.com/products/nxiq/doc/scm-token-permissions';
    expect(screen.getByRole('link', { name: 'Check required permissions here' })).toHaveAttribute(
      'href',
      expectedPermissionUrl
    );
  });

  it('renders the configure base url section when shouldDisplayNotice is true', () => {
    renderComponent(mockScmProvider, mockApplicationPublicId);
    expect(screen.getByRole('heading', { name: 'Configure Base URL' })).toBeInTheDocument();
    const expectedBaseUrlUrl = '/assets/#/baseUrl';
    expect(screen.getAllByRole('link', { name: 'Click here' })[0]).toHaveAttribute('href', expectedBaseUrlUrl);
  });

  it('renders the correct application source control configuration page base on publicId', () => {
    const expectedApplicationSourceControlPage = `/assets/#/management/edit/application/${mockApplicationPublicId}/source-control`;
    renderComponent(mockScmProvider, mockApplicationPublicId);
    expect(screen.getAllByRole('link', { name: 'Click here' })[1]).toHaveAttribute(
      'href',
      expectedApplicationSourceControlPage
    );
  });

  it('renders the correct automatic source control configuration page', () => {
    renderComponent(mockScmProvider, mockApplicationPublicId);
    expect(screen.getByRole('link', { name: 'Automatic Source Control' })).toHaveAttribute(
      'href',
      '/assets/#/automaticSourceControlConfiguration'
    );
  });

  it('renders the correct configure source control help link', () => {
    renderComponent(mockScmProvider, mockApplicationPublicId);
    const expectedConfigureSourceControlHelpLink = 'http://links.sonatype.com/products/nxiq/doc/scm-connect-iq';
    expect(screen.getAllByRole('link', { name: 'here' })[1]).toHaveAttribute(
      'href',
      expectedConfigureSourceControlHelpLink
    );
  });

  it('does not render the configure base url section when shouldDisplayNotice is false', () => {
    selectShouldDisplayNotice.mockReturnValue(false);
    renderComponent(mockScmProvider, mockApplicationPublicId);
    expect(screen.queryByRole('heading', { name: 'Configure Base URL' })).not.toBeInTheDocument();
  });

  function renderComponent(mockScmProvider, mockApplicationPublicId) {
    render(<ScmWizard scmProvider={mockScmProvider} applicationPublicId={mockApplicationPublicId} />);
  }
});
