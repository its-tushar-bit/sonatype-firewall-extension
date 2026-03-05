/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from '@testing-library/react';
import GitHubAppDetailsBox from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/GitHubAppDetailsBox';

describe('GitHubAppDetailsBox', () => {
  const mockGithubAppOrganization = {
    installationId: '12345',
    accountName: 'my-org',
    accountType: 'organization',
    name: 'MyApp',
    configurationDate: '2024-01-15T10:30:00Z',
  };

  const mockGithubAppPersonal = {
    installationId: '67890',
    accountName: 'john-doe',
    accountType: 'personal',
    name: 'PersonalApp',
    configurationDate: '2024-02-20T14:45:00Z',
  };

  describe('Rendering', () => {
    it('renders all GitHub App details for organization account', () => {
      render(<GitHubAppDetailsBox githubApp={mockGithubAppOrganization} />);

      expect(screen.getByText('Organization:')).toBeInTheDocument();
      expect(screen.getByText('my-org')).toBeInTheDocument();
      expect(screen.getByText('App:')).toBeInTheDocument();
      expect(screen.getByText('MyApp')).toBeInTheDocument();
      expect(screen.getByText('Repositories:')).toBeInTheDocument();
      expect(screen.getByText('Configuration Date:')).toBeInTheDocument();
    });

    it('renders all GitHub App details for personal account', () => {
      render(<GitHubAppDetailsBox githubApp={mockGithubAppPersonal} />);

      expect(screen.getByText('Organization:')).toBeInTheDocument();
      expect(screen.getByText('john-doe')).toBeInTheDocument();
      expect(screen.getByText('App:')).toBeInTheDocument();
      expect(screen.getByText('PersonalApp')).toBeInTheDocument();
      expect(screen.getByText('Repositories:')).toBeInTheDocument();
      expect(screen.getByText('Configuration Date:')).toBeInTheDocument();
    });

    it('renders nothing when installationId is missing', () => {
      const { container } = render(<GitHubAppDetailsBox githubApp={{ accountName: 'my-org', name: 'MyApp' }} />);

      expect(container.firstChild).toBeNull();
    });

    it('renders nothing when githubApp is null', () => {
      const { container } = render(<GitHubAppDetailsBox githubApp={null} />);

      expect(container.firstChild).toBeNull();
    });

    it('renders nothing when githubApp is undefined', () => {
      const { container } = render(<GitHubAppDetailsBox githubApp={undefined} />);

      expect(container.firstChild).toBeNull();
    });

    it('renders empty string for missing accountName', () => {
      const githubAppNoAccount = {
        installationId: '12345',
        name: 'MyApp',
      };

      render(<GitHubAppDetailsBox githubApp={githubAppNoAccount} />);

      expect(screen.getByText('Organization:')).toBeInTheDocument();
      const organizationValue = screen.getByText('Organization:').nextElementSibling;
      expect(organizationValue.textContent).toBe('');
    });

    it('does not render App section when name is missing', () => {
      const githubAppNoName = {
        installationId: '12345',
        accountName: 'my-org',
      };

      render(<GitHubAppDetailsBox githubApp={githubAppNoName} />);

      expect(screen.queryByText('App:')).not.toBeInTheDocument();
      expect(screen.getByText('Organization:')).toBeInTheDocument();
    });

    it('does not render Configuration Date when missing', () => {
      const githubAppNoDate = {
        installationId: '12345',
        accountName: 'my-org',
        name: 'MyApp',
      };

      render(<GitHubAppDetailsBox githubApp={githubAppNoDate} />);

      expect(screen.queryByText('Configuration Date:')).not.toBeInTheDocument();
      expect(screen.getByText('Organization:')).toBeInTheDocument();
    });
  });

  describe('Installation Link', () => {
    it('generates correct URL for organization account', () => {
      render(<GitHubAppDetailsBox githubApp={mockGithubAppOrganization} />);

      const link = screen.getByRole('link', { name: /view github app configuration/i });
      expect(link).toHaveAttribute('href', 'https://github.com/organizations/my-org/settings/installations/12345');
    });

    it('generates correct URL for personal account', () => {
      render(<GitHubAppDetailsBox githubApp={mockGithubAppPersonal} />);

      const link = screen.getByRole('link', { name: /view github app configuration/i });
      expect(link).toHaveAttribute('href', 'https://github.com/settings/installations/67890');
    });

    it('uses custom link text when provided', () => {
      render(<GitHubAppDetailsBox githubApp={mockGithubAppOrganization} linkText="Go to Settings" />);

      expect(screen.getByRole('link', { name: 'Go to Settings' })).toBeInTheDocument();
    });

    it('uses default link text when not provided', () => {
      render(<GitHubAppDetailsBox githubApp={mockGithubAppOrganization} />);

      expect(screen.getByRole('link', { name: 'View GitHub App configuration' })).toBeInTheDocument();
    });

    it('does not render Repositories section when installationId is missing', () => {
      const githubAppNoInstallation = {
        accountName: 'my-org',
        name: 'MyApp',
      };

      render(<GitHubAppDetailsBox githubApp={githubAppNoInstallation} />);

      expect(screen.queryByText('Repositories:')).not.toBeInTheDocument();
    });
  });

  describe('Date Formatting', () => {
    it('formats configuration date correctly', () => {
      render(<GitHubAppDetailsBox githubApp={mockGithubAppOrganization} />);

      const dateText = screen.getByText(/Jan 15, 2024/);
      expect(dateText).toBeInTheDocument();
      // Verify it includes time and timezone
      expect(dateText.textContent).toMatch(/\d{1,2}:\d{2}\s(AM|PM)/);
    });

    it('handles different date formats', () => {
      const githubAppCustomDate = {
        ...mockGithubAppOrganization,
        configurationDate: '2023-12-25T23:59:59Z',
      };

      render(<GitHubAppDetailsBox githubApp={githubAppCustomDate} />);

      expect(screen.getByText(/Dec 25, 2023/)).toBeInTheDocument();
    });
  });

  describe('Edge Cases', () => {
    it('handles missing accountType gracefully (defaults to organization URL)', () => {
      const githubAppNoAccountType = {
        installationId: '12345',
        accountName: 'test-account',
        name: 'TestApp',
      };

      render(<GitHubAppDetailsBox githubApp={githubAppNoAccountType} />);

      const link = screen.getByRole('link');
      expect(link).toHaveAttribute(
        'href',
        'https://github.com/organizations/test-account/settings/installations/12345'
      );
    });

    it('renders with minimal valid data (only installationId and accountName)', () => {
      const minimalGithubApp = {
        installationId: '12345',
        accountName: 'minimal-org',
      };

      render(<GitHubAppDetailsBox githubApp={minimalGithubApp} />);

      expect(screen.getByText('Organization:')).toBeInTheDocument();
      expect(screen.getByText('minimal-org')).toBeInTheDocument();
      expect(screen.getByText('Repositories:')).toBeInTheDocument();
    });

    it('handles empty string installationId as falsy', () => {
      const { container } = render(<GitHubAppDetailsBox githubApp={{ installationId: '', accountName: 'test' }} />);

      expect(container.firstChild).toBeNull();
    });
  });
});
