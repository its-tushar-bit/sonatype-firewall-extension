/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import ApplicationReportVulnerabilitiesHeader from 'MainRoot/applicationReport/vulnerabilities/ApplicationReportVulnerabilitiesHeader';

// CLM-42090: The Hosted Repository component report threads componentDisplayName through the
// URL so downstream pages (Raw Data / Vulnerabilities / Latest Evaluations) can render the
// friendly component coordinate title instead of the synthetic application public id. These
// tests pin that behaviour on the Vulnerabilities page header.
describe('ApplicationReportVulnerabilitiesHeader', () => {
  const metadata = {
    reportTime: 1702041439230,
    reportTitle: 'Release Report',
    application: {
      name: 'maven-releases_ansible_ansible_2_ansible-2.tar.gz',
    },
  };

  const renderWithRouter = (routerParams = {}) =>
    render(<ApplicationReportVulnerabilitiesHeader metadata={metadata} />, {
      preloadedState: {
        router: {
          currentParams: routerParams,
        },
      },
    });

  it('renders the synthetic application public id as the H1 for non-hosted-repo entries', () => {
    renderWithRouter({});

    expect(
      screen.getByRole('heading', {
        name: 'Vulnerabilities for maven-releases_ansible_ansible_2_ansible-2.tar.gz Release Report',
      })
    ).toBeInTheDocument();
  });

  it('renders componentDisplayName instead of application.name when origin=hostedRepoComponents', () => {
    renderWithRouter({ origin: 'hostedRepoComponents', componentDisplayName: 'ansible 2.8.0 (.tar.gz)' });

    expect(
      screen.getByRole('heading', { name: 'Vulnerabilities for ansible 2.8.0 (.tar.gz) Release Report' })
    ).toBeInTheDocument();
  });

  it('falls back to application.name when origin is hostedRepoComponents but componentDisplayName is missing', () => {
    renderWithRouter({ origin: 'hostedRepoComponents' });

    // Guards against the empty/undefined case — without a valid componentDisplayName the header
    // must not render a blank title.
    expect(
      screen.getByRole('heading', {
        name: 'Vulnerabilities for maven-releases_ansible_ansible_2_ansible-2.tar.gz Release Report',
      })
    ).toBeInTheDocument();
  });

  it('does not use componentDisplayName when origin is not hostedRepoComponents (regression guard)', () => {
    renderWithRouter({ origin: 'somethingElse', componentDisplayName: 'ansible 2.8.0 (.tar.gz)' });

    expect(
      screen.getByRole('heading', {
        name: 'Vulnerabilities for maven-releases_ansible_ansible_2_ansible-2.tar.gz Release Report',
      })
    ).toBeInTheDocument();
  });
});
