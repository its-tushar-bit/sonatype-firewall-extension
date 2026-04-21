/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render } from '@testing-library/react';
import GitHubAppSuccessModal from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/GitHubAppSuccessModal';

describe('GitHubAppSuccessModal', () => {
  const defaultProps = {
    isOpen: true,
    onClose: jest.fn(),
    autoEnabledGoldenPRs: false,
    autoEnabledManualPRs: false,
    serverId: 'test-server',
    organizationName: 'test-org',
    submitBtnText: 'Create',
  };

  describe('Alert visibility', () => {
    it('shows alert with "create" text when features are auto-enabled and submitBtnText is "Create"', () => {
      const { getByText } = render(
        <GitHubAppSuccessModal {...defaultProps} autoEnabledGoldenPRs={true} submitBtnText="Create" />
      );

      expect(getByText(/Click/)).toBeInTheDocument();
      expect(getByText(/create/)).toBeInTheDocument();
    });

    it('shows alert with "update" text when features are auto-enabled and submitBtnText is "Update"', () => {
      const { getByText } = render(
        <GitHubAppSuccessModal {...defaultProps} autoEnabledGoldenPRs={true} submitBtnText="Update" />
      );

      expect(getByText(/Click/)).toBeInTheDocument();
      expect(getByText(/update/)).toBeInTheDocument();
    });

    it('still shows alert when no features are auto-enabled', () => {
      const { getByText } = render(
        <GitHubAppSuccessModal
          {...defaultProps}
          autoEnabledGoldenPRs={false}
          autoEnabledManualPRs={false}
          isReplacement={false}
        />
      );

      expect(getByText(/Click/)).toBeInTheDocument();
      expect(getByText(/create/)).toBeInTheDocument();
    });

    it('shows update alert when no features are auto-enabled and submitBtnText is "Update"', () => {
      const { getByText } = render(
        <GitHubAppSuccessModal
          {...defaultProps}
          autoEnabledGoldenPRs={false}
          autoEnabledManualPRs={false}
          submitBtnText="Update"
        />
      );

      expect(getByText(/Click/)).toBeInTheDocument();
      expect(getByText(/update/)).toBeInTheDocument();
    });
  });

  describe('Account name display', () => {
    it('shows Organization label for non-personal accounts', () => {
      const { getByText } = render(<GitHubAppSuccessModal {...defaultProps} organizationName="acme-corp" />);

      expect(getByText(/Organization/)).toBeInTheDocument();
      expect(getByText(/acme-corp/)).toBeInTheDocument();
    });

    it('shows Account label and cleans name for personal accounts', () => {
      const { getByText, queryByText } = render(
        <GitHubAppSuccessModal {...defaultProps} organizationName="john-doe(personal)" />
      );

      expect(getByText(/Account/)).toBeInTheDocument();
      expect(getByText(/john-doe/)).toBeInTheDocument();
      // Should NOT show the (personal) marker
      expect(queryByText(/\(personal\)/)).not.toBeInTheDocument();
    });
  });

  describe('Feature descriptions', () => {
    it('shows Golden PRs description when auto-enabled', () => {
      const { getByText } = render(<GitHubAppSuccessModal {...defaultProps} autoEnabledGoldenPRs={true} />);

      expect(getByText(/Create Golden PRs/)).toBeInTheDocument();
      expect(getByText(/Maven dependencies are automatically generated/)).toBeInTheDocument();
    });

    it('shows Manual PRs description when auto-enabled', () => {
      const { getByText } = render(<GitHubAppSuccessModal {...defaultProps} autoEnabledManualPRs={true} />);

      expect(getByText(/Recommend Manual Pull Requests/)).toBeInTheDocument();
      expect(getByText(/Create PR.*button/)).toBeInTheDocument();
    });

    it('shows both descriptions when both features are auto-enabled', () => {
      const { getByText } = render(
        <GitHubAppSuccessModal {...defaultProps} autoEnabledGoldenPRs={true} autoEnabledManualPRs={true} />
      );

      expect(getByText(/Create Golden PRs/)).toBeInTheDocument();
      expect(getByText(/Recommend Manual Pull Requests/)).toBeInTheDocument();
    });

    it('does not show descriptions when no features are auto-enabled', () => {
      const { queryByText } = render(
        <GitHubAppSuccessModal {...defaultProps} autoEnabledGoldenPRs={false} autoEnabledManualPRs={false} />
      );

      expect(queryByText(/Create Golden PRs/)).not.toBeInTheDocument();
      expect(queryByText(/Recommend Manual Pull Requests/)).not.toBeInTheDocument();
    });
  });

  describe('Modal visibility', () => {
    it('renders when isOpen is true', () => {
      const { getByText } = render(<GitHubAppSuccessModal {...defaultProps} isOpen={true} />);

      expect(getByText(/GitHub Setup Complete/)).toBeInTheDocument();
    });

    it('does not render when isOpen is false', () => {
      const { queryByText } = render(<GitHubAppSuccessModal {...defaultProps} isOpen={false} />);

      expect(queryByText(/GitHub Setup Complete/)).not.toBeInTheDocument();
    });
  });

  describe('Integration with form context', () => {
    it('shows "update" instruction for non-root pages', () => {
      const { getByText, queryByText } = render(
        <GitHubAppSuccessModal {...defaultProps} autoEnabledGoldenPRs={true} submitBtnText="Update" />
      );

      expect(getByText(/Click/)).toBeInTheDocument();
      expect(getByText(/update/)).toBeInTheDocument();
      expect(queryByText(/create/)).not.toBeInTheDocument();
    });

    it('shows "create" instruction for root org first-time setup', () => {
      const { getByText, queryByText } = render(
        <GitHubAppSuccessModal {...defaultProps} autoEnabledGoldenPRs={true} submitBtnText="Create" />
      );

      expect(getByText(/Click/)).toBeInTheDocument();
      expect(getByText(/create/)).toBeInTheDocument();
      expect(queryByText(/update/)).not.toBeInTheDocument();
    });

    it('shows "update" instruction for root org reconfiguration', () => {
      const { getByText, queryByText } = render(
        <GitHubAppSuccessModal {...defaultProps} autoEnabledGoldenPRs={true} submitBtnText="Update" />
      );

      expect(getByText(/Click/)).toBeInTheDocument();
      expect(getByText(/update/)).toBeInTheDocument();
      expect(queryByText(/create/)).not.toBeInTheDocument();
    });
  });
});
