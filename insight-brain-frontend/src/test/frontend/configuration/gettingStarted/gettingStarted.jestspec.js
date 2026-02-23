/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen, within } from 'TestRoot/SpecUtil';
import GettingStarted from '../../../../main/frontend/configuration/gettingStarted/GettingStarted';

import 'TestRoot/SpecUtil';

describe('gettingStarted', function () {
  const minimalProps = {
      load: () => {},
      loading: false,
      loadError: null,
      isDataLoaded: false,
      isAuthorizedToViewSystemSetup: false,
      shouldDisplayHdsUnreachable: false,
      hdsUnreachableErrorMessage: null,
      hdsUnreachableIncidentId: null,
      license: null,
      prevState: { prevPage: { url: 'test' } },
    },
    renderComponent = (additionalProps) => render(<GettingStarted {...minimalProps} {...additionalProps} />);

  describe('on load', function () {
    it('calls load function', () => {
      const mockLoad = jest.fn().mockName('load');
      renderComponent({ load: mockLoad });
      expect(mockLoad).toHaveBeenCalledTimes(1);
    });

    describe('connectivity', function () {
      it('does not render an alert if sonatype services were reached', function () {
        renderComponent();
        expect(screen.queryByText('Sonatype Data Services unreachable')).not.toBeInTheDocument();
      });

      it('render an alert if sonatype services were not reached', function () {
        renderComponent({ shouldDisplayHdsUnreachable: true });
        expect(screen.getByText('Sonatype Data Services unreachable')).toBeInTheDocument();
      });
    });
  });

  describe('license data', function () {
    const propsWithLicense = {
      isDataLoaded: true,
      daysToExpiration: 5,
      expiryDate: 'August 01, 2021',
      license: {
        productEdition: 'Lifecycle',
        fingerprint: '99c9cd6be744c30439b4260010bf14d7e2c3013a',
        expiryTimestamp: 1627862400000,
        licensedUsersToDisplay: 100,
        applicationLimitToDisplay: null,
        applicationCountToDisplay: null,
        firewallUsersToDisplay: 100,
        contactName: 'Nick Cook',
        contactCompany: 'Sonatype Inc',
        contactEmail: 'ncook@sonatype.com',
        products: [
          'Nexus Lifecycle',
          'Nexus Firewall',
          'Nexus Firewall for Artifactory',
          'Nexus Advanced Development Pack',
        ],
        sbomCountToDisplay: null,
        sbomLimitToDisplay: null,
      },
    };

    it('does not load when not admin', function () {
      const summaryTile = renderComponent(propsWithLicense).queryByRole('region', { name: 'Product License' });
      expect(summaryTile).not.toBeInTheDocument();
    });

    it('loads when admin', function () {
      const summaryTile = renderComponent({ ...propsWithLicense, isAdmin: true }).getByRole('region', {
        name: 'Product License',
      });
      expect(summaryTile).toBeInTheDocument();
    });
  });

  describe('Authorizations', function () {
    it('renders an error alert when loadError is specified', function () {
      const { rerender, getByRole } = renderComponent({ loadError: 'oops' }),
        alert = getByRole('alert');
      expect(alert).toBeInTheDocument();
      expect(alert).toHaveTextContent('oops');

      rerender(<GettingStarted {...minimalProps} loadError={null} />);
      expect(alert).not.toBeInTheDocument();
    });

    it('is not authorized to view system setup when isAuthorizedToViewSystemSetup is false', function () {
      const setupTile = renderComponent().queryByRole('region', {
        name: 'System Setup',
      });
      expect(setupTile).not.toBeInTheDocument();
    });

    it('is authorized to view system setup', function () {
      const setupTile = renderComponent({ isAuthorizedToViewSystemSetup: true }).getByRole('region', {
        name: 'System Setup',
      });
      expect(setupTile).toBeInTheDocument();
    });
  });

  describe('Learning Topics Section', function () {
    it('renders Learning Topics', function () {
      const learningTile = renderComponent().getByRole('region', { name: 'Learning Topics' });
      expect(learningTile).toBeInTheDocument();
    });
    it('when tenantMode is not multi-tenant renders all section', function () {
      const learningTile = renderComponent().getByRole('region', { name: 'Learning Topics' });

      expect(within(learningTile).getByRole('heading', { name: 'Policies' })).toBeInTheDocument();
      expect(within(learningTile).getByRole('heading', { name: 'Hierarchy and Inheritance' })).toBeInTheDocument();
      expect(within(learningTile).getByRole('heading', { name: 'Integrations' })).toBeInTheDocument();
      expect(within(learningTile).getByRole('heading', { name: 'Dashboard' })).toBeInTheDocument();
      expect(within(learningTile).getByRole('heading', { name: 'Evaluation Reports' })).toBeInTheDocument();
      expect(within(learningTile).getByRole('heading', { name: 'Success Metrics' })).toBeInTheDocument();
    });

    it('when tenantMode is multi-tenant renders relevant sections', function () {
      const learningTile = renderComponent({ tenantMode: 'multi-tenant' }).getByRole('region', {
        name: 'Learning Topics',
      });

      expect(within(learningTile).getByRole('heading', { name: 'Policies' })).toBeInTheDocument();
      expect(within(learningTile).getByRole('heading', { name: 'Hierarchy and Inheritance' })).toBeInTheDocument();
      expect(within(learningTile).getByRole('heading', { name: 'Dashboard' })).toBeInTheDocument();
      expect(within(learningTile).queryByRole('heading', { name: 'Integrations' })).not.toBeInTheDocument();
      expect(within(learningTile).queryByRole('heading', { name: 'Evaluation Reports' })).not.toBeInTheDocument();
      expect(within(learningTile).queryByRole('heading', { name: 'Success Metrics' })).not.toBeInTheDocument();
    });
  });

  describe('System Setup Section', function () {
    describe('when tenantMode is not multi-tenant', function () {
      it('renders all sections', function () {
        const setupTile = renderComponent({ isAuthorizedToViewSystemSetup: true }).getByRole('region', {
          name: 'System Setup',
        });

        expect(within(setupTile).getByText('Storage and Backup')).toBeInTheDocument();
        expect(within(setupTile).getByText('Adding Users')).toBeInTheDocument();
        expect(within(setupTile).getByText('Onboarding Applications')).toBeInTheDocument();
      });

      it('renders all documentation sections for adding users', function () {
        const preloadedState = {
          productFeatures: {
            productFeatures: {
              'saml-enabled': true,
              'user-management-pages': true,
            },
          },
        };

        const additionalProps = { isAuthorizedToViewSystemSetup: true };
        const setupTile = render(<GettingStarted {...minimalProps} {...additionalProps} />, {
          preloadedState,
        }).getByRole('region', { name: 'System Setup' });

        expect(within(setupTile).getByRole('heading', { name: 'CONFIGURE LDAP' })).toBeInTheDocument();
        expect(within(setupTile).getByRole('heading', { name: 'CONFIGURE SAML' })).toBeInTheDocument();
        expect(within(setupTile).getByRole('heading', { name: 'MANUALLY ADD USERS' })).toBeInTheDocument();
        expect(within(setupTile).queryByRole('heading', { name: 'INVITE USERS' })).not.toBeInTheDocument();
      });

      it('does not render LDAP section when user management is disabled', function () {
        const preloadedState = {
          productFeatures: {
            productFeatures: {
              'saml-enabled': true,
              'user-management-pages': false,
            },
          },
        };

        const additionalProps = { isAuthorizedToViewSystemSetup: true };
        const setupTile = render(<GettingStarted {...minimalProps} {...additionalProps} />, {
          preloadedState,
        }).getByRole('region', { name: 'System Setup' });

        expect(within(setupTile).queryByRole('heading', { name: 'CONFIGURE LDAP' })).not.toBeInTheDocument();
        expect(within(setupTile).getByRole('heading', { name: 'CONFIGURE SAML' })).toBeInTheDocument();
        expect(within(setupTile).getByRole('heading', { name: 'MANUALLY ADD USERS' })).toBeInTheDocument();
        expect(within(setupTile).queryByRole('heading', { name: 'INVITE USERS' })).not.toBeInTheDocument();
      });

      it('renders the appropriate link for manually adding users', function () {
        const setupTile = renderComponent({ isAuthorizedToViewSystemSetup: true, isAdmin: false }).getByRole('region', {
          name: 'System Setup',
        });
        // in on-prem, when LDAP and SAML are disabled, the third link rendered is for manually adding users
        const linkToDoc = within(setupTile).getAllByRole('link', { name: 'Documentation' })[2];
        expect(linkToDoc).toHaveAttribute(
          'href',
          'https://links.sonatype.com/products/nxiq/doc/user-management/creating-a-user'
        );
      });
    });

    describe('when tenantMode is multi-tenant', function () {
      const multiTenantProps = {
        isAuthorizedToViewSystemSetup: true,
        tenantMode: 'multi-tenant',
      };

      it('renders relevant sections', function () {
        const setupTile = renderComponent(multiTenantProps).getByRole('region', {
          name: 'System Setup',
        });

        expect(within(setupTile).queryByText('Storage and Backup')).not.toBeInTheDocument();
        expect(within(setupTile).getByText('Adding Users')).toBeInTheDocument();
        expect(within(setupTile).getByText('Onboarding Applications')).toBeInTheDocument();
      });

      it('renders only necessary sections for adding users', function () {
        const setupTile = renderComponent(multiTenantProps).getByRole('region', { name: 'System Setup' });

        expect(within(setupTile).queryByRole('heading', { name: 'CONFIGURE LDAP' })).not.toBeInTheDocument();
        expect(within(setupTile).queryByRole('heading', { name: 'CONFIGURE SAML' })).not.toBeInTheDocument();
        expect(within(setupTile).queryByRole('heading', { name: 'MANUALLY ADD USERS' })).not.toBeInTheDocument();
        expect(within(setupTile).getByRole('heading', { name: 'INVITE USERS' })).toBeInTheDocument();
      });

      it('renders the appropriate link for inviting users', function () {
        const setupTile = renderComponent(multiTenantProps).getByRole('region', {
          name: 'System Setup',
        });
        // in SAAS, the first link rendered is for inviting users
        const linkToDoc = within(setupTile).getAllByRole('link', { name: 'Documentation' })[0];
        expect(linkToDoc).toHaveAttribute(
          'href',
          'https://links.sonatype.com/products/nxiq/doc/firewall-saas-getting-started-on-cloud/user-management'
        );
      });
    });

    describe('when tenantMode is multi-tenant and user-management-pages is enabled', function () {
      const multiTenantWithUserMgmtProps = {
        isAuthorizedToViewSystemSetup: true,
        tenantMode: 'multi-tenant',
      };

      const preloadedState = {
        productFeatures: {
          productFeatures: {
            'user-management-pages': true,
          },
        },
      };

      it('renders sections for manual user flow when user-management-pages is enabled', function () {
        const { getByRole } = render(<GettingStarted {...minimalProps} {...multiTenantWithUserMgmtProps} />, {
          preloadedState,
        });
        const setupTile = getByRole('region', { name: 'System Setup' });

        expect(within(setupTile).getByRole('heading', { name: 'MANUALLY ADD USERS' })).toBeInTheDocument();
        expect(within(setupTile).queryByRole('heading', { name: 'INVITE USERS' })).not.toBeInTheDocument();
      });

      it('renders the appropriate link for manually adding users when user-management-pages is enabled', function () {
        const { getByRole } = render(<GettingStarted {...minimalProps} {...multiTenantWithUserMgmtProps} />, {
          preloadedState,
        });
        const setupTile = getByRole('region', { name: 'System Setup' });

        // when user management is enabled in multi-tenant and saml is disabled, the first link
        // rendered is for manually adding users
        const linkToDoc = within(setupTile).getAllByRole('link', { name: 'Documentation' })[0];
        expect(linkToDoc).toHaveAttribute(
          'href',
          'https://links.sonatype.com/products/nxiq/doc/user-management/creating-a-user'
        );
      });
    });

    describe('when tenantMode is multi-tenant and user-management-pages is disabled', function () {
      const multiTenantWithoutUserMgmtProps = {
        isAuthorizedToViewSystemSetup: true,
        tenantMode: 'multi-tenant',
      };

      const preloadedState = {
        productFeatures: {
          productFeatures: {
            'user-management-pages': false,
          },
        },
      };

      it('renders sections for invite user flow when user-management-pages is disabled', function () {
        const { getByRole } = render(<GettingStarted {...minimalProps} {...multiTenantWithoutUserMgmtProps} />, {
          preloadedState,
        });
        const setupTile = getByRole('region', { name: 'System Setup' });

        expect(within(setupTile).queryByRole('heading', { name: 'CONFIGURE LDAP' })).not.toBeInTheDocument();
        expect(within(setupTile).queryByRole('heading', { name: 'CONFIGURE SAML' })).not.toBeInTheDocument();
        expect(within(setupTile).queryByRole('heading', { name: 'MANUALLY ADD USERS' })).not.toBeInTheDocument();
        expect(within(setupTile).getByRole('heading', { name: 'INVITE USERS' })).toBeInTheDocument();
      });

      it('renders the appropriate link for inviting users when user-management-pages is disabled', function () {
        const { getByRole } = render(<GettingStarted {...minimalProps} {...multiTenantWithoutUserMgmtProps} />, {
          preloadedState,
        });
        const setupTile = getByRole('region', { name: 'System Setup' });

        // when user management is disabled in multi-tenant, the first link rendered is for inviting users
        const linkToDoc = within(setupTile).getAllByRole('link', { name: 'Documentation' })[0];
        expect(linkToDoc).toHaveAttribute(
          'href',
          'https://links.sonatype.com/products/nxiq/doc/firewall-saas-getting-started-on-cloud/user-management'
        );
      });
    });

    describe('when tenantMode is multi-tenant and isSamlEnabled is enabled', function () {
      const multiTenantProps = {
        isAuthorizedToViewSystemSetup: true,
        tenantMode: 'multi-tenant',
        isAdmin: false,
      };

      const preloadedState = {
        productFeatures: {
          productFeatures: {
            'user-management-pages': false,
            'saml-enabled': true,
          },
        },
      };

      it('renders sections for manual user flow when isSamlEnabled is enabled', function () {
        const { getByRole } = render(<GettingStarted {...minimalProps} {...multiTenantProps} />, {
          preloadedState,
        });
        const setupTile = getByRole('region', { name: 'System Setup' });

        expect(within(setupTile).queryByRole('heading', { name: 'CONFIGURE LDAP' })).not.toBeInTheDocument();
        expect(within(setupTile).getByRole('heading', { name: 'CONFIGURE SAML' })).toBeInTheDocument();
        expect(within(setupTile).queryByRole('heading', { name: 'MANUALLY ADD USERS' })).not.toBeInTheDocument();
        expect(within(setupTile).getByRole('heading', { name: 'INVITE USERS' })).toBeInTheDocument();
      });
    });

    describe('when tenantMode is multi-tenant and isSamlEnabled is disabled', function () {
      const multiTenantProps = {
        isAuthorizedToViewSystemSetup: true,
        tenantMode: 'multi-tenant',
      };

      const preloadedState = {
        productFeatures: {
          productFeatures: {
            'user-management-pages': false,
            'saml-enabled': false,
          },
        },
      };

      it('renders sections for invite user flow when isSamlEnabled is disabled', function () {
        const { getByRole } = render(<GettingStarted {...minimalProps} {...multiTenantProps} />, {
          preloadedState,
        });
        const setupTile = getByRole('region', { name: 'System Setup' });

        expect(within(setupTile).queryByRole('heading', { name: 'CONFIGURE LDAP' })).not.toBeInTheDocument();
        expect(within(setupTile).queryByRole('heading', { name: 'CONFIGURE SAML' })).not.toBeInTheDocument();
        expect(within(setupTile).queryByRole('heading', { name: 'MANUALLY ADD USERS' })).not.toBeInTheDocument();
        expect(within(setupTile).getByRole('heading', { name: 'INVITE USERS' })).toBeInTheDocument();
      });

      it('renders the appropriate link for inviting users when isSamlEnabled is disabled', function () {
        const { getByRole } = render(<GettingStarted {...minimalProps} {...multiTenantProps} />, {
          preloadedState,
        });
        const setupTile = getByRole('region', { name: 'System Setup' });

        // when SAML is disabled in multi-tenant, the first link rendered is for inviting users
        const linkToDoc = within(setupTile).getAllByRole('link', { name: 'Documentation' })[0];
        expect(linkToDoc).toHaveAttribute(
          'href',
          'https://links.sonatype.com/products/nxiq/doc/firewall-saas-getting-started-on-cloud/user-management'
        );
      });
    });
  });
});
