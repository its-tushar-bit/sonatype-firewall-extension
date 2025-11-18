/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import SystemPreferencesMenu from 'MainRoot/mainHeader/MenuBar/SystemPreferencesMenu/SystemPreferencesMenu';
import { render, screen } from 'TestRoot/SpecUtil';
import { fireEvent } from '@testing-library/react';

describe('SystemPreferencesMenu', () => {
  const permissions = {
    CONFIGURE_SYSTEM: true,
    VIEW_ROLES: true,
    MANAGE_AUTOMATIC_APPLICATION_CREATION: true,
    MANAGE_AUTOMATIC_SCM_CONFIGURATION: true,
  };

  it('should display the title "System Preferences"', () => {
    render(<SystemPreferencesMenu />);
    const button = screen.getByRole('button');
    fireEvent.click(button);
    expect(screen.getByText('System Preferences')).toBeInTheDocument();
  });

  it('should display the link "Users" if "CONFIGURE_SYSTEM" and "isUserManagementEnabled" are enabled, and license is not null', () => {
    const preloadedState = {
      productLicense: {},
      productFeatures: {
        productFeatures: {
          'user-management-pages': true,
        },
      },
    };
    render(<SystemPreferencesMenu permissions={permissions} />, { preloadedState });
    const button = screen.getByRole('button');
    fireEvent.click(button);
    expect(screen.getByText('Users')).toBeInTheDocument();
  });

  it('should display the link "Users" if "CONFIGURE_SYSTEM" and "isSsoIdpManagedBySonatype" are enabled, and license is not null', () => {
    const preloadedState = { productLicense: {} };
    render(<SystemPreferencesMenu permissions={permissions} isSsoIdpManagedBySonatype={true} />, { preloadedState });
    const button = screen.getByRole('button');
    fireEvent.click(button);
    expect(screen.getByText('Users')).toBeInTheDocument();
  });

  it('should not display the link "Users" if "CONFIGURE_SYSTEM" is enabled but both "isUserManagementEnabled" and "isSsoIdpManagedBySonatype" are false', () => {
    const preloadedState = {
      productLicense: {},
      productFeatures: {
        productFeatures: {
          'user-management-pages': false,
        },
      },
    };
    render(<SystemPreferencesMenu permissions={permissions} isSsoIdpManagedBySonatype={false} />, { preloadedState });
    const button = screen.getByRole('button');
    fireEvent.click(button);
    expect(screen.queryByText('Users')).toBeNull();
  });

  it('should display "User Activity" link when user activity tracking is enabled but user management is disabled (SaaS mode)', () => {
    const preloadedState = {
      productLicense: {},
      productFeatures: {
        productFeatures: {
          'user-activity-tracking': true,
          'user-management-pages': false,
        },
      },
    };
    render(<SystemPreferencesMenu permissions={permissions} />, { preloadedState });
    const button = screen.getByRole('button');
    fireEvent.click(button);
    expect(screen.getByText('User Activity')).toBeInTheDocument();
    expect(screen.queryByText('Users')).toBeNull();
  });

  it('should not display "User Activity" link when user activity tracking is disabled', () => {
    const preloadedState = {
      productLicense: {},
      productFeatures: {
        productFeatures: {
          'user-activity-tracking': false,
          'user-management-pages': false,
        },
      },
    };
    render(<SystemPreferencesMenu permissions={permissions} />, { preloadedState });
    const button = screen.getByRole('button');
    fireEvent.click(button);
    expect(screen.queryByText('User Activity')).toBeNull();
  });

  it('should not display "User Activity" link when both user activity and user management are enabled (shows Users instead)', () => {
    const preloadedState = {
      productLicense: {},
      productFeatures: {
        productFeatures: {
          'user-activity-tracking': true,
          'user-management-pages': true,
        },
      },
    };
    render(<SystemPreferencesMenu permissions={permissions} />, { preloadedState });
    const button = screen.getByRole('button');
    fireEvent.click(button);
    expect(screen.getByText('Users')).toBeInTheDocument();
    expect(screen.queryByText('User Activity')).toBeNull();
  });

  it('should not display "User Activity" link when CONFIGURE_SYSTEM permission is false', () => {
    const noConfigPermissions = { ...permissions, CONFIGURE_SYSTEM: false };
    const preloadedState = {
      productLicense: {},
      productFeatures: {
        productFeatures: {
          'user-activity-tracking': true,
          'user-management-pages': false,
        },
      },
    };
    render(<SystemPreferencesMenu permissions={noConfigPermissions} />, { preloadedState });
    const button = screen.getByRole('button');
    fireEvent.click(button);
    expect(screen.queryByText('User Activity')).toBeNull();
  });

  it('should display the link "Users" if both "isUserManagementEnabled" and "isSsoIdpManagedBySonatype" are enabled', () => {
    const preloadedState = {
      productLicense: {},
      productFeatures: {
        productFeatures: {
          'user-management-pages': true,
        },
      },
    };
    render(<SystemPreferencesMenu permissions={permissions} isSsoIdpManagedBySonatype={true} />, { preloadedState });
    const button = screen.getByRole('button');
    fireEvent.click(button);
    expect(screen.getByText('Users')).toBeInTheDocument();
  });

  it('should display "Roles" if VIEW_ROLES is true and license is not null', () => {
    const preloadedState = { productLicense: {} };
    render(<SystemPreferencesMenu permissions={permissions} />, { preloadedState });
    const button = screen.getByRole('button');
    fireEvent.click(button);
    expect(screen.queryByText('Roles')).toBeInTheDocument();
  });

  it('should display "Administrators" if CONFIGURE_SYSTEM is true and license is not null', () => {
    const preloadedState = { productLicense: {} };
    render(<SystemPreferencesMenu permissions={permissions} />, { preloadedState });
    const button = screen.getByRole('button');
    fireEvent.click(button);
    expect(screen.queryByText('Administrators')).toBeInTheDocument();
  });

  it('should display "Product License" if CONFIGURE_SYSTEM and isProductLicenseConfigurationEnabled are true', () => {
    const preloadedState = { productLicense: {} };
    render(<SystemPreferencesMenu permissions={permissions} isProductLicenseConfigurationEnabled={true} />, {
      preloadedState,
    });
    const button = screen.getByRole('button');
    fireEvent.click(button);
    expect(screen.queryByText('Product License')).toBeInTheDocument();
  });

  it('should display "Product License" if CONFIGURE_SYSTEM is true and license is null', () => {
    render(<SystemPreferencesMenu permissions={permissions} />);
    const button = screen.getByRole('button');
    fireEvent.click(button);
    expect(screen.queryByText('Product License')).toBeInTheDocument();
  });

  it('should not display the links "Users", "Roles", and "Administrators" if license is null', () => {
    const preloadedState = {
      productFeatures: {
        productFeatures: {
          'user-management-pages': true,
        },
      },
    };
    render(<SystemPreferencesMenu permissions={permissions} />, { preloadedState });
    const button = screen.getByRole('button');
    fireEvent.click(button);
    expect(screen.queryByText('Users')).toBeNull();
    expect(screen.queryByText('Roles')).toBeNull();
    expect(screen.queryByText('Administrators')).toBeNull();
  });

  it('should not display the link "Webhooks" if "isWebhooksSupported" is false', () => {
    render(<SystemPreferencesMenu isWebhooksSupported={false} isWebhookConfigurationEnabled={true} />);
    const button = screen.getByRole('button');
    fireEvent.click(button);
    expect(screen.queryByText('Webhooks')).toBeNull();
  });

  // New test cases
  it('should display the link "LDAP" only if "CONFIGURE_SYSTEM" and "isLdapConfigurationEnabled" are true', () => {
    render(<SystemPreferencesMenu permissions={permissions} isLdapConfigurationEnabled={true} />);
    const button = screen.getByRole('button');
    fireEvent.click(button);
    expect(screen.getByText('LDAP')).toBeInTheDocument();
  });

  it('should not display "Email" if "isEmailConfigurationEnabled" is false', () => {
    render(<SystemPreferencesMenu isEmailConfigurationEnabled={false} />);
    const button = screen.getByRole('button');
    fireEvent.click(button);
    expect(screen.queryByText('Email')).toBeNull();
  });

  it('should display "Proxy" link when "CONFIGURE_SYSTEM" and "isProxyConfigurationEnabled" are true', () => {
    render(<SystemPreferencesMenu permissions={permissions} isProxyConfigurationEnabled={true} />);
    const button = screen.getByRole('button');
    fireEvent.click(button);
    expect(screen.getByText('Proxy')).toBeInTheDocument();
  });

  it('should display "System Notice" link when "CONFIGURE_SYSTEM" and "isSystemNoticeConfigurationEnabled" are true', () => {
    render(<SystemPreferencesMenu permissions={permissions} isSystemNoticeConfigurationEnabled={true} />);
    const button = screen.getByRole('button');
    fireEvent.click(button);
    expect(screen.getByText('System Notice')).toBeInTheDocument();
  });

  // Will be enabled in: NEXUS-46126
  // describe('ROI Configuration', () => {
  //   it('should display when has both Lifecycle and Firewall License', () => {
  //     const preloadedState = {
  //       productLicense: { license: { products: ['Sonatype Lifecycle SaaS', 'Sonatype Lifecycle Firewall SaaS'] } },
  //     };
  //     render(<SystemPreferencesMenu permissions={permissions} isSingleTenant={true} />, { preloadedState });
  //     const button = screen.getByRole('button');
  //     fireEvent.click(button);
  //     expect(screen.getByText('ROI Configuration')).toBeInTheDocument();
  //   });

  //   it('should display when has only Lifecycle license', () => {
  //     const preloadedState = {
  //       productLicense: { license: { products: ['Sonatype Lifecycle SaaS'] } },
  //     };
  //     render(<SystemPreferencesMenu permissions={permissions} isSingleTenant={true} />, { preloadedState });
  //     const button = screen.getByRole('button');
  //     fireEvent.click(button);
  //     expect(screen.getByText('ROI Configuration')).toBeInTheDocument();
  //   });

  //   it('should display when has only Firewall License', () => {
  //     const preloadedState = {
  //       productLicense: { license: { products: ['Sonatype Repository Firewall'] } },
  //     };
  //     render(<SystemPreferencesMenu permissions={permissions} isSingleTenant={true} />, { preloadedState });
  //     const button = screen.getByRole('button');
  //     fireEvent.click(button);
  //     expect(screen.getByText('ROI Configuration')).toBeInTheDocument();
  //   });

  //   it('should NOT display when has neither Lifecycle or Firewall license', () => {
  //     const preloadedState = {
  //       productLicense: { license: { products: [] } },
  //     };
  //     render(<SystemPreferencesMenu permissions={permissions} isSingleTenant={true} />, { preloadedState });
  //     const button = screen.getByRole('button');
  //     fireEvent.click(button);
  //     expect(screen.queryByText('ROI Configuration')).not.toBeInTheDocument();
  //   });
  // });

  describe('sbomManagerOnly license', () => {
    it('should not display "Waived Components" if "isSbomManagerOnlyLicense" is true', () => {
      render(
        <SystemPreferencesMenu permissions={permissions} isMonitoringSupported={true} isSbomManagerOnlyLicense={true} />
      );
      const button = screen.getByRole('button');
      fireEvent.click(button);
      expect(screen.queryByText('Waived Components')).toBeNull();
    });

    it('should display "Waived Components" if "isSbomManagerOnlyLicense" is false', () => {
      render(
        <SystemPreferencesMenu
          permissions={permissions}
          isMonitoringSupported={true}
          isSbomManagerOnlyLicense={false}
        />
      );
      const button = screen.getByRole('button');
      fireEvent.click(button);
      expect(screen.queryByText('Waived Components')).toBeInTheDocument();
    });

    it('should not display "Atlassian Crowd" if "isSbomManagerOnlyLicense" is true', () => {
      render(
        <SystemPreferencesMenu
          permissions={permissions}
          isCrowdIntegrationEnabled={true}
          isSbomManagerOnlyLicense={true}
        />
      );
      const button = screen.getByRole('button');
      fireEvent.click(button);
      expect(screen.queryByText('Atlassian Crowd')).toBeNull();
    });

    it('should display "Atlassian Crowd" if "isSbomManagerOnlyLicense" is false', () => {
      render(
        <SystemPreferencesMenu
          permissions={permissions}
          isCrowdIntegrationEnabled={true}
          isSbomManagerOnlyLicense={false}
        />
      );
      const button = screen.getByRole('button');
      fireEvent.click(button);
      expect(screen.queryByText('Atlassian Crowd')).toBeInTheDocument();
    });

    it('should not display "Success Metrics" if "isSbomManagerOnlyLicense" is true', () => {
      render(
        <SystemPreferencesMenu
          permissions={permissions}
          isSuccessMetricsConfigurationEnabled={true}
          isSbomManagerOnlyLicense={true}
        />
      );
      const button = screen.getByRole('button');
      fireEvent.click(button);
      expect(screen.queryByText('Success Metrics')).toBeNull();
    });

    it('should display "Success Metrics" if "isSbomManagerOnlyLicense" is false', () => {
      render(
        <SystemPreferencesMenu
          permissions={permissions}
          isSuccessMetricsConfigurationEnabled={true}
          isSbomManagerOnlyLicense={false}
          isOrgsAndAppsEnabled={true}
        />
      );
      const button = screen.getByRole('button');
      fireEvent.click(button);
      expect(screen.queryByText('Success Metrics')).toBeInTheDocument();
    });

    it('should not display "Automatic Applications" if "isSbomManagerOnlyLicense" is true', () => {
      render(
        <SystemPreferencesMenu
          permissions={permissions}
          isAutomaticApplicationConfigurationEnabled={true}
          isSbomManagerOnlyLicense={true}
          isOrgsAndAppsEnabled={false}
        />
      );
      const button = screen.getByRole('button');
      fireEvent.click(button);
      expect(screen.queryByText('Automatic Applications')).toBeNull();
    });

    it('should display "Automatic Applications" if "isSbomManagerOnlyLicense" is false', () => {
      render(
        <SystemPreferencesMenu
          permissions={permissions}
          isAutomaticApplicationConfigurationEnabled={true}
          isSbomManagerOnlyLicense={false}
        />
      );
      const button = screen.getByRole('button');
      fireEvent.click(button);
      expect(screen.queryByText('Automatic Applications')).toBeInTheDocument();
    });

    it('should not display "Automatic SCM Configuration" if "isSbomManagerOnlyLicense" is true', () => {
      render(
        <SystemPreferencesMenu
          permissions={permissions}
          isAutomaticScmConfigurationEnabled={true}
          isSbomManagerOnlyLicense={true}
        />
      );
      const button = screen.getByRole('button');
      fireEvent.click(button);
      expect(screen.queryByText('Automatic SCM Configuration')).toBeNull();
    });

    it('should display "Automatic SCM Configuration" if "isSbomManagerOnlyLicense" is false', () => {
      render(
        <SystemPreferencesMenu
          permissions={permissions}
          isAutomaticScmConfigurationEnabled={true}
          isSbomManagerOnlyLicense={false}
        />
      );
      const button = screen.getByRole('button');
      fireEvent.click(button);
      expect(screen.queryByText('Automatic SCM Configuration')).toBeInTheDocument();
    });

    it('should not display "Advanced Search" if "isSbomManagerOnlyLicense" is true', () => {
      render(
        <SystemPreferencesMenu
          permissions={permissions}
          isAdvancedSearchConfigurationEnabled={true}
          isSbomManagerOnlyLicense={true}
        />
      );
      const button = screen.getByRole('button');
      fireEvent.click(button);
      expect(screen.queryByText('Advanced Search')).toBeNull();
    });

    it('should display "Advanced Search" if "isSbomManagerOnlyLicense" is false', () => {
      render(
        <SystemPreferencesMenu
          permissions={permissions}
          isAdvancedSearchConfigurationEnabled={true}
          isSbomManagerOnlyLicense={false}
        />
      );
      const button = screen.getByRole('button');
      fireEvent.click(button);
      expect(screen.queryByText('Advanced Search')).toBeInTheDocument();
    });
  });

  describe('OIDC configuration', () => {
    it('should display "OIDC" when CONFIGURE_SYSTEM, isOAuth2ConfigurationEnabled, and isSingleTenant are true', () => {
      render(
        <SystemPreferencesMenu permissions={permissions} isOAuth2ConfigurationEnabled={true} isSingleTenant={true} />
      );
      const button = screen.getByRole('button');
      fireEvent.click(button);
      expect(screen.getByText('OIDC')).toBeInTheDocument();
    });

    it('should not display "OIDC" when isSingleTenant is false (SaaS license)', () => {
      render(
        <SystemPreferencesMenu permissions={permissions} isOAuth2ConfigurationEnabled={true} isSingleTenant={false} />
      );
      const button = screen.getByRole('button');
      fireEvent.click(button);
      expect(screen.queryByText('OIDC')).toBeNull();
    });

    it('should not display "OIDC" when isOAuth2ConfigurationEnabled is false', () => {
      render(
        <SystemPreferencesMenu permissions={permissions} isOAuth2ConfigurationEnabled={false} isSingleTenant={true} />
      );
      const button = screen.getByRole('button');
      fireEvent.click(button);
      expect(screen.queryByText('OIDC')).toBeNull();
    });

    it('should not display "OIDC" when CONFIGURE_SYSTEM is false', () => {
      const noConfigPermissions = { ...permissions, CONFIGURE_SYSTEM: false };
      render(
        <SystemPreferencesMenu
          permissions={noConfigPermissions}
          isOAuth2ConfigurationEnabled={true}
          isSingleTenant={true}
        />
      );
      const button = screen.getByRole('button');
      fireEvent.click(button);
      expect(screen.queryByText('OIDC')).toBeNull();
    });
  });

  describe('standalone firewall', () => {
    it.each(['isStandaloneFirewall', 'isFirewallOnlyLicense'])(
      'should not display "Success Metrics" if %s is true',
      (item) => {
        render(
          <SystemPreferencesMenu
            permissions={permissions}
            isSuccessMetricsConfigurationEnabled={true}
            {...{ [item]: true }}
          />
        );
        const button = screen.getByRole('button');
        fireEvent.click(button);
        expect(screen.queryByText('Success Metrics')).toBeNull();
      }
    );

    it.each(['isStandaloneFirewall', 'isFirewallOnlyLicense'])(
      'should display "Success Metrics" if %s is false',
      (item) => {
        render(
          <SystemPreferencesMenu
            permissions={permissions}
            isSuccessMetricsConfigurationEnabled={true}
            isOrgsAndAppsEnabled={true}
            {...{ [item]: false }}
          />
        );
        const button = screen.getByRole('button');
        fireEvent.click(button);
        expect(screen.queryByText('Success Metrics')).toBeInTheDocument();
      }
    );

    it.each(['isStandaloneFirewall', 'isFirewallOnlyLicense'])(
      'should not display "Automatic Applications" if %s is true',
      (item) => {
        render(
          <SystemPreferencesMenu
            permissions={permissions}
            isAutomaticApplicationConfigurationEnabled={true}
            {...{ [item]: true }}
          />
        );
        const button = screen.getByRole('button');
        fireEvent.click(button);
        expect(screen.queryByText('Automatic Applications')).toBeNull();
      }
    );

    it.each(['isStandaloneFirewall', 'isFirewallOnlyLicense'])(
      'should display "Automatic Applications" if %s is false',
      (item) => {
        render(
          <SystemPreferencesMenu
            permissions={permissions}
            isAutomaticApplicationConfigurationEnabled={true}
            {...{ [item]: false }}
          />
        );
        const button = screen.getByRole('button');
        fireEvent.click(button);
        expect(screen.queryByText('Automatic Applications')).toBeInTheDocument();
      }
    );

    it.each(['isStandaloneFirewall', 'isFirewallOnlyLicense'])(
      'should not display "Automatic SCM Configuration" if %s is true',
      (item) => {
        render(
          <SystemPreferencesMenu
            permissions={permissions}
            isAutomaticScmConfigurationEnabled={true}
            {...{ [item]: true }}
          />
        );
        const button = screen.getByRole('button');
        fireEvent.click(button);
        expect(screen.queryByText('Automatic SCM Configuration')).toBeNull();
      }
    );

    it.each(['isStandaloneFirewall', 'isFirewallOnlyLicense'])(
      'should display "Automatic SCM Configuration" if %s is false',
      (item) => {
        render(
          <SystemPreferencesMenu
            permissions={permissions}
            isAutomaticScmConfigurationEnabled={true}
            {...{ [item]: false }}
          />
        );
        const button = screen.getByRole('button');
        fireEvent.click(button);
        expect(screen.queryByText('Automatic SCM Configuration')).toBeInTheDocument();
      }
    );

    it.each(['isStandaloneFirewall', 'isFirewallOnlyLicense'])(
      'should not display "Advanced Search" if %s is true',
      (item) => {
        render(
          <SystemPreferencesMenu
            permissions={permissions}
            isAdvancedSearchConfigurationEnabled={true}
            {...{ [item]: true }}
          />
        );
        const button = screen.getByRole('button');
        fireEvent.click(button);
        expect(screen.queryByText('Advanced Search')).toBeNull();
      }
    );

    it.each(['isStandaloneFirewall', 'isFirewallOnlyLicense'])(
      'should display "Advanced Search" if %s is false',
      (item) => {
        render(
          <SystemPreferencesMenu
            permissions={permissions}
            isAdvancedSearchConfigurationEnabled={true}
            {...{ [item]: false }}
          />
        );
        const button = screen.getByRole('button');
        fireEvent.click(button);
        expect(screen.queryByText('Advanced Search')).toBeInTheDocument();
      }
    );

    it.each(['isStandaloneFirewall', 'isFirewallOnlyLicense'])('should display "Zscaler" if %s is true', (item) => {
      render(<SystemPreferencesMenu permissions={permissions} isZscalerEnabled={true} {...{ [item]: true }} />);
      const button = screen.getByRole('button');
      fireEvent.click(button);
      expect(screen.queryByText('Zscaler')).toBeInTheDocument();
    });

    it.each(['isStandaloneFirewall', 'isFirewallOnlyLicense'])(
      'should not display "Zscaler" if %s is false',
      (item) => {
        render(
          <SystemPreferencesMenu
            permissions={permissions}
            isAdvancedSearchConfigurationEnabled={true}
            isZscalerEnabled={true}
            {...{ [item]: false }}
          />
        );
        const button = screen.getByRole('button');
        fireEvent.click(button);
        expect(screen.queryByText('Zscaler')).toBeNull();
      }
    );

    it('should not display "Zscaler" if isZscalerEnabled is false', () => {
      render(<SystemPreferencesMenu permissions={permissions} isStandaloneFirewall={true} isZscalerEnabled={false} />);
      const button = screen.getByRole('button');
      fireEvent.click(button);
      expect(screen.queryByText('Zscaler')).toBeNull();
    });
  });
});
