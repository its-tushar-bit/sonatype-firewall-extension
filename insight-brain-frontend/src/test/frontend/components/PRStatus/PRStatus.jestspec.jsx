/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PRStatus from 'MainRoot/components/prStatus/PRStatus';
import {
  AUTOMATED_REMEDIATION_STATUS as PR_STATUS,
  PR_FAILURE_CATEGORY,
  PR_FAILURE_DISABLED_FALLBACK,
} from 'MainRoot/constants/automatedRemediationStatus';

describe('PRStatus', () => {
  const mockOnCreatePR = jest.fn();
  const mockOnRetry = jest.fn();

  beforeEach(() => {
    mockOnCreatePR.mockClear();
    mockOnRetry.mockClear();
  });

  const renderButton = (props = {}) => {
    const defaultProps = {
      onCreatePR: mockOnCreatePR,
      defaultContent: '-',
      onRetry: mockOnRetry,
    };

    render(<PRStatus {...defaultProps} {...props} />);
  };

  it('renders the defaultContent when automatedRemediationStatus is not provided', () => {
    renderButton();
    expect(screen.getByText('-')).toBeInTheDocument();
  });

  it('renders the defaultContent when automatedRemediationStatus is null', () => {
    renderButton({ automatedRemediationStatus: null });
    expect(screen.getByText('-')).toBeInTheDocument();
  });

  it('renders the defaultContent when status is MANUAL_PULL_REQUEST_NOT_POSSIBLE with reason UNSUPPORTED_STAGE', () => {
    renderButton({
      automatedRemediationStatus: {
        status: PR_STATUS.MANUAL_PULL_REQUEST_NOT_POSSIBLE,
        reason: 'UNSUPPORTED_STAGE',
      },
    });
    expect(screen.getByText('-')).toBeInTheDocument();
  });

  it('renders the defaultContent when status is MANUAL_PULL_REQUEST_NOT_POSSIBLE with reason UNSUPPORTED_DEPENDENCY_TYPE', () => {
    renderButton({
      automatedRemediationStatus: {
        status: PR_STATUS.MANUAL_PULL_REQUEST_NOT_POSSIBLE,
        reason: 'UNSUPPORTED_DEPENDENCY_TYPE',
      },
    });
    expect(screen.getByText('-')).toBeInTheDocument();
  });

  it('renders the defaultContent when status is MANUAL_PULL_REQUEST_NOT_POSSIBLE with reason UNSUPPORTED_FORMAT', () => {
    renderButton({
      automatedRemediationStatus: {
        status: PR_STATUS.MANUAL_PULL_REQUEST_NOT_POSSIBLE,
        reason: 'UNSUPPORTED_FORMAT',
      },
    });
    expect(screen.getByText('-')).toBeInTheDocument();
  });

  it('renders the defaultContent when status is MANUAL_PULL_REQUEST_NOT_POSSIBLE with reason REMEDIATION_EVENT_EXISTS', () => {
    renderButton({
      automatedRemediationStatus: {
        status: PR_STATUS.MANUAL_PULL_REQUEST_NOT_POSSIBLE,
        reason: 'REMEDIATION_EVENT_EXISTS',
      },
    });
    expect(screen.getByText('-')).toBeInTheDocument();
  });

  it('renders the defaultContent when status is MANUAL_PULL_REQUEST_NOT_POSSIBLE with reason NO_REMEDIATION_VERSION_AVAILABLE', () => {
    renderButton({
      automatedRemediationStatus: {
        status: PR_STATUS.MANUAL_PULL_REQUEST_NOT_POSSIBLE,
        reason: 'NO_REMEDIATION_VERSION_AVAILABLE',
      },
    });
    expect(screen.getByText('-')).toBeInTheDocument();
  });

  it('renders the defaultContent when status is MANUAL_PULL_REQUEST_NOT_POSSIBLE with reason INSUFFICIENT_PERMISSIONS', () => {
    renderButton({
      automatedRemediationStatus: {
        status: PR_STATUS.MANUAL_PULL_REQUEST_NOT_POSSIBLE,
        reason: 'INSUFFICIENT_PERMISSIONS',
      },
    });
    expect(screen.getByText('-')).toBeInTheDocument();
  });

  it('does NOT render a dash when status is PULL_REQUEST_CREATION_FAILED with reason that would be hidden for other statuses', () => {
    renderButton({
      automatedRemediationStatus: {
        status: PR_STATUS.PULL_REQUEST_CREATION_FAILED,
        reason: 'UNSUPPORTED_STAGE', //it would be hidden for MANUAL_PULL_REQUEST_NOT_POSSIBLE
      },
    });

    expect(screen.queryByText('-')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument();
  });

  it('renders a "Create PR" button for the MANUAL_PULL_REQUEST_POSSIBLE state', async () => {
    const user = userEvent.setup();
    renderButton({
      automatedRemediationStatus: {
        status: PR_STATUS.MANUAL_PULL_REQUEST_POSSIBLE,
      },
    });

    const button = screen.getByRole('button', { name: 'Create PR' });
    expect(button).toBeVisible();

    await user.click(button);
    expect(mockOnCreatePR).toHaveBeenCalledTimes(1);
  });

  it('renders a disabled "Create PR" button with SCM_NOT_CONFIGURED tooltip', async () => {
    const user = userEvent.setup();

    renderButton({
      automatedRemediationStatus: {
        status: PR_STATUS.MANUAL_PULL_REQUEST_NOT_POSSIBLE,
        reason: 'SCM_NOT_CONFIGURED',
      },
    });

    const button = screen.getByRole('button', { name: 'Create PR' });
    expect(button).toBeVisible();
    expect(button).toHaveClass('disabled');

    await user.hover(button);

    const tooltip = await screen.findByRole('tooltip', {
      name: 'Source Control is not configured',
    });
    expect(tooltip).toBeVisible();
  });

  it('renders a disabled "Create PR" button with generic tooltip for other reasons', async () => {
    const user = userEvent.setup();

    renderButton({
      automatedRemediationStatus: {
        status: PR_STATUS.MANUAL_PULL_REQUEST_NOT_POSSIBLE,
        reason: 'CONFIGURATION_DISABLED',
      },
    });

    const button = screen.getByRole('button', { name: 'Create PR' });
    expect(button).toBeVisible();
    expect(button).toHaveClass('disabled');

    await user.hover(button);

    const tooltip = await screen.findByRole('tooltip', {
      name: 'Manual Pull Requests are disabled',
    });
    expect(tooltip).toBeVisible();
  });

  it('renders a loading spinner with "Creating PR…" text for the PULL_REQUEST_CREATION_PENDING state', () => {
    renderButton({
      automatedRemediationStatus: {
        status: PR_STATUS.PULL_REQUEST_CREATION_PENDING,
      },
    });

    const loadingSpinner = screen.queryByRole('status');
    expect(loadingSpinner).toBeVisible();
    expect(loadingSpinner).toHaveTextContent('Creating PR…');
  });

  it('renders a "Retry" link and shows a tooltip for the PULL_REQUEST_CREATION_FAILED state', async () => {
    const user = userEvent.setup();
    const errorMsg = 'network error';

    renderButton({
      automatedRemediationStatus: {
        status: PR_STATUS.PULL_REQUEST_CREATION_FAILED,
        reason: errorMsg,
      },
    });

    const retryLink = screen.getByRole('button', { name: 'Retry' });
    expect(retryLink).toBeVisible();

    await user.hover(retryLink);

    const tooltip = await screen.findByRole('tooltip', {
      name: `Failure to create PR. ${errorMsg}`,
    });
    expect(tooltip).toBeVisible();

    await user.click(retryLink);
    expect(mockOnRetry).toHaveBeenCalledTimes(1);
  });

  it('renders a "View PR" link using the default success text for the PULL_REQUEST state', () => {
    const prUrl = 'https://github.com/example/repo/pull/123';

    renderButton({
      automatedRemediationStatus: {
        status: PR_STATUS.PULL_REQUEST,
        url: prUrl,
      },
    });

    const link = screen.getByRole('link', { name: 'View PR' });
    expect(link).toBeVisible();
    expect(link).toHaveAttribute('href', prUrl);
  });

  it('renders a link with custom text for the PULL_REQUEST state', () => {
    const prUrl = 'https://github.com/example/repo/pull/123';
    const customPRLinkText = 'View PR#123';

    renderButton({
      automatedRemediationStatus: {
        status: PR_STATUS.PULL_REQUEST,
        url: prUrl,
      },
      defaultPrLinkText: customPRLinkText,
    });

    const link = screen.getByRole('link', { name: customPRLinkText });
    expect(link).toBeVisible();
    expect(link).toHaveAttribute('href', prUrl);
  });

  it('renders a link with PR number when pullRequestNumber is provided', () => {
    const prUrl = 'https://github.com/example/repo/pull/123';

    renderButton({
      automatedRemediationStatus: {
        status: PR_STATUS.PULL_REQUEST,
        url: prUrl,
        pullRequestId: 123,
      },
    });

    const link = screen.getByRole('link', { name: 'PR #123' });
    expect(link).toBeVisible();
    expect(link).toHaveAttribute('href', prUrl);
  });

  it('properly distinguishes between different reason fields based on status', async () => {
    const user = userEvent.setup();

    renderButton({
      automatedRemediationStatus: {
        status: PR_STATUS.PULL_REQUEST_CREATION_FAILED,
        reason: 'API error',
      },
    });

    const retryButton = screen.getByRole('button', { name: 'Retry' });
    await user.hover(retryButton);

    const failureTooltip = await screen.findByRole('tooltip', {
      name: 'Failure to create PR. API error',
    });
    expect(failureTooltip).toBeVisible();

    renderButton({
      automatedRemediationStatus: {
        status: PR_STATUS.MANUAL_PULL_REQUEST_NOT_POSSIBLE,
        reason: 'API error', //same reason text, different status
      },
    });

    const disabledButton = screen.getByRole('button', { name: 'Create PR' });
    await user.hover(disabledButton);

    const disabledTooltip = await screen.findByRole('tooltip', {
      name: 'Manual Pull Requests are disabled',
    });
    expect(disabledTooltip).toBeVisible();
  });

  it('renders Retry enabled with existing tooltip when isRetryable is true (SCM_ERROR)', async () => {
    const user = userEvent.setup();
    renderButton({
      automatedRemediationStatus: {
        status: PR_STATUS.PULL_REQUEST_CREATION_FAILED,
        reason: 'boom',
        failureCategory: PR_FAILURE_CATEGORY.SCM_ERROR,
        isRetryable: true,
      },
    });

    const retry = screen.getByRole('button', { name: /retry/i });
    expect(retry).not.toHaveAttribute('aria-disabled', 'true');
    expect(retry).not.toHaveClass('disabled');

    await user.click(retry);
    expect(mockOnRetry).toHaveBeenCalledTimes(1);

    await user.hover(retry);
    expect(await screen.findByRole('tooltip', { name: 'Failure to create PR. boom' })).toBeInTheDocument();
  });

  it('renders Retry disabled with actionable tooltip for MANIFEST_COMPONENT_NOT_FOUND', async () => {
    const user = userEvent.setup();
    renderButton({
      automatedRemediationStatus: {
        status: PR_STATUS.PULL_REQUEST_CREATION_FAILED,
        reason: 'Pull request creation failed: ...',
        failureCategory: PR_FAILURE_CATEGORY.MANIFEST_COMPONENT_NOT_FOUND,
        isRetryable: false,
      },
    });

    const retry = screen.getByRole('button', { name: /retry/i });
    expect(retry).toHaveAttribute('aria-disabled', 'true');
    expect(retry).toHaveClass('disabled');

    await user.click(retry);
    expect(mockOnRetry).not.toHaveBeenCalled();

    await user.hover(retry);
    const tooltip = await screen.findByRole('tooltip');
    expect(tooltip).toHaveTextContent(/must first be added as a direct dependency/i);
  });

  it('falls back to reason-based tooltip when isRetryable=false but category has no specific copy', async () => {
    const user = userEvent.setup();
    renderButton({
      automatedRemediationStatus: {
        status: PR_STATUS.PULL_REQUEST_CREATION_FAILED,
        reason: 'something',
        failureCategory: 'SOMETHING_UNKNOWN',
        isRetryable: false,
      },
    });

    const retry = screen.getByRole('button', { name: /retry/i });
    expect(retry).toHaveAttribute('aria-disabled', 'true');
    expect(retry).toHaveClass('disabled');

    await user.hover(retry);
    expect(await screen.findByRole('tooltip', { name: 'Failure to create PR. something' })).toBeInTheDocument();
  });

  it('uses the disabled fallback when isRetryable=false, reason is empty, and category is unknown', async () => {
    const user = userEvent.setup();
    renderButton({
      automatedRemediationStatus: {
        status: PR_STATUS.PULL_REQUEST_CREATION_FAILED,
        reason: '',
        failureCategory: PR_FAILURE_CATEGORY.UNKNOWN,
        isRetryable: false,
      },
    });

    const retry = screen.getByRole('button', { name: /retry/i });
    expect(retry).toHaveAttribute('aria-disabled', 'true');
    expect(retry).toHaveClass('disabled');

    await user.hover(retry);
    expect(await screen.findByRole('tooltip', { name: PR_FAILURE_DISABLED_FALLBACK })).toBeInTheDocument();
  });

  it('treats legacy payload without isRetryable or failureCategory as retryable', async () => {
    const user = userEvent.setup();
    renderButton({
      automatedRemediationStatus: {
        status: PR_STATUS.PULL_REQUEST_CREATION_FAILED,
        reason: 'legacy',
      },
    });

    const retry = screen.getByRole('button', { name: /retry/i });
    expect(retry).not.toHaveAttribute('aria-disabled', 'true');
    expect(retry).not.toHaveClass('disabled');

    await user.click(retry);
    expect(mockOnRetry).toHaveBeenCalledTimes(1);
  });

  it('trusts isRetryable=true even if category would be non-retryable (defensive precedence)', () => {
    renderButton({
      automatedRemediationStatus: {
        status: PR_STATUS.PULL_REQUEST_CREATION_FAILED,
        reason: 'inconsistent payload',
        failureCategory: PR_FAILURE_CATEGORY.MANIFEST_COMPONENT_NOT_FOUND,
        isRetryable: true,
      },
    });
    const retry = screen.getByRole('button', { name: /retry/i });
    expect(retry).not.toHaveAttribute('aria-disabled', 'true');
    expect(retry).not.toHaveClass('disabled');
  });

  it('drops the trailing-space tooltip when retryable but reason is empty', async () => {
    const user = userEvent.setup();
    renderButton({
      automatedRemediationStatus: {
        status: PR_STATUS.PULL_REQUEST_CREATION_FAILED,
        reason: '',
        isRetryable: true,
      },
    });

    const retry = screen.getByRole('button', { name: /retry/i });
    expect(retry).not.toHaveAttribute('aria-disabled', 'true');
    expect(retry).not.toHaveClass('disabled');

    await user.hover(retry);
    // No "Failure to create PR. " with a trailing space when reason is empty.
    expect(await screen.findByRole('tooltip', { name: 'Failed to create PR.' })).toBeInTheDocument();
  });

});

