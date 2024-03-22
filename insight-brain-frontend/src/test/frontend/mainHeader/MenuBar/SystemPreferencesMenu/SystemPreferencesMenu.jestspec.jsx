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
    MANAGE_AUTOMATIC_APPLICATION_CREATION: true,
    MANAGE_AUTOMATIC_SCM_CONFIGURATION: true,
  };

  it('should display the title "System Preferences"', () => {
    render(<SystemPreferencesMenu />);
    const button = screen.getByRole('button');
    fireEvent.click(button);
    expect(screen.getByText('System Preferences')).toBeInTheDocument();
  });

  it('should display the link "Users" if "CONFIGURE_SYSTEM" and "isSingleTenant" are enabled', () => {
    render(<SystemPreferencesMenu permissions={permissions} isSingleTenant={true} />);
    const button = screen.getByRole('button');
    fireEvent.click(button);
    expect(screen.getByText('Users')).toBeInTheDocument();
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
      <SystemPreferencesMenu permissions={permissions} isMonitoringSupported={true} isSbomManagerOnlyLicense={false} />
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
