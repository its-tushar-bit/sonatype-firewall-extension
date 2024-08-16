/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import ScmWizard from 'MainRoot/development/developmentDashboard/sections/scmWizard/ScmWizard';
import * as baseUrlConfigurationSelectors from 'MainRoot/configuration/baseUrl/baseUrlConfigurationSelectors';
import {
  getSCMProviderTokenDocUrl,
  getSCMProviderTokenUrl,
} from 'MainRoot/development/developmentDashboard/sections/scmWizard/scmWizardUtil';
import * as routerStateContext from 'MainRoot/react/RouterStateContext';

describe('ScmWizard', () => {
  let selectShouldDisplayNotice, hrefSpy, getSpy;
  const ScmProvider = ['github', 'gitlab', 'azure devops', 'bitbucket'];
  const mockScmProvider = 'github';
  const mockApplicationPublicId = 'test-application-1';

  beforeEach(() => {
    selectShouldDisplayNotice = jest
      .spyOn(baseUrlConfigurationSelectors, 'selectShouldDisplayNotice')
      .mockReturnValue(true);

    hrefSpy = jest.fn('href').mockImplementation((stateName) => stateName);
    getSpy = jest.fn('get').mockImplementation((state) => state);
    const routerContextMock = { href: hrefSpy, get: getSpy };
    jest.spyOn(routerStateContext, 'useRouterState').mockReturnValue(routerContextMock);
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

    it(`renders the correct data analytics id for token link for the provider ${scmProvider}`, () => {
      renderComponent(scmProvider, mockApplicationPublicId);
      const dataAnalyticsIdToken = `sonatype-developer-scm-${scmProvider}-token`;
      expect(screen.getAllByRole('link', { name: 'here' })[0]).toHaveAttribute(
        'data-analytics-id',
        dataAnalyticsIdToken
      );
    });

    it(`renders the correct data analytics id for permission link for ${scmProvider}`, () => {
      renderComponent(scmProvider, mockApplicationPublicId);
      const dataAnalyticsIdPermission = `sonatype-developer-scm-${scmProvider}-permission`;
      expect(screen.getAllByRole('link', { name: 'Check required permissions here' })[0]).toHaveAttribute(
        'data-analytics-id',
        dataAnalyticsIdPermission
      );
    });

    it(`renders the correct data analytics id for application source control link for ${scmProvider}`, () => {
      renderComponent(scmProvider, mockApplicationPublicId);
      const dataAnalyticsIdApplicationSourceControl = `sonatype-developer-scm-${scmProvider}-application-source-control`;
      expect(screen.getAllByRole('link', { name: 'click here' })[0]).toHaveAttribute(
        'data-analytics-id',
        dataAnalyticsIdApplicationSourceControl
      );
    });

    it(`renders the correct data analytics id for automatic source control link for ${scmProvider}`, () => {
      renderComponent(scmProvider, mockApplicationPublicId);
      const dataAnalyticsIdAutomaticSourceControlConf = `sonatype-developer-scm-${scmProvider}-automatic-source-control-configuration`;
      expect(screen.getAllByRole('link', { name: 'Automatic Source Control' })[0]).toHaveAttribute(
        'data-analytics-id',
        dataAnalyticsIdAutomaticSourceControlConf
      );
    });

    it(`renders the correct data analytics id for base url link for ${scmProvider}`, () => {
      renderComponent(scmProvider, mockApplicationPublicId);
      const dataAnalyticsIdBaseUrl = `sonatype-developer-scm-${scmProvider}-base-url`;
      expect(screen.getAllByRole('link', { name: 'Click here' })[0]).toHaveAttribute(
        'data-analytics-id',
        dataAnalyticsIdBaseUrl
      );
    });

    it(`renders the correct data analytics id for help link for ${scmProvider}`, () => {
      renderComponent(scmProvider, mockApplicationPublicId);
      const dataAnalyticsIdHelp = `sonatype-developer-scm-${scmProvider}-configuration-source-control-help-link`;
      expect(screen.getAllByRole('link', { name: 'here' })[1]).toHaveAttribute(
        'data-analytics-id',
        dataAnalyticsIdHelp
      );
    });
  });

  it('renders the correct permission URL link', () => {
    renderComponent(mockScmProvider, mockApplicationPublicId);
    const expectedPermissionUrl = 'https://links.sonatype.com/products/nxiq/doc/scm-token-permissions';
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
    const expectedApplicationSourceControlPage = 'management.edit.application.edit-source-control';
    renderComponent(mockScmProvider, mockApplicationPublicId);
    expect(screen.getAllByRole('link', { name: 'click here' })[0]).toHaveAttribute(
      'href',
      expectedApplicationSourceControlPage
    );
  });

  it('renders the correct automatic source control configuration page', () => {
    renderComponent(mockScmProvider, mockApplicationPublicId);
    expect(screen.getByRole('link', { name: 'Automatic Source Control' })).toHaveAttribute(
      'href',
      'automaticSourceControlConfiguration'
    );
  });

  it('renders the correct configure source control help link', () => {
    renderComponent(mockScmProvider, mockApplicationPublicId);
    const expectedConfigureSourceControlHelpLink = 'https://links.sonatype.com/products/nxiq/doc/scm-connect-iq';
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
