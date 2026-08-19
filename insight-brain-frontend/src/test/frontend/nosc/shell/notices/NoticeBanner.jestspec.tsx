/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, waitFor } from '@testing-library/dom';
import { render, userEvent } from 'TestRoot/SpecUtil';
import { NoticeBanner } from 'MainRoot/nosc/shell/notices/NoticeBanner';

describe('NoticeBanner', () => {
  describe('dismiss button', () => {
    it('does not render a dismiss button when onDismiss prop is omitted', () => {
      render(<NoticeBanner testId="test-notice">Test message</NoticeBanner>);

      expect(screen.getByText('Test message')).toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /dismiss/i })).not.toBeInTheDocument();
    });

    it('renders a dismiss button and calls onDismiss when clicked', async () => {
      const onDismiss = jest.fn();
      const user = userEvent.setup();
      render(
        <NoticeBanner testId="test-notice" onDismiss={onDismiss}>
          Test message
        </NoticeBanner>,
      );

      expect(screen.getByText('Test message')).toBeInTheDocument();
      const dismissButton = screen.getByRole('button', { name: /dismiss notice/i });
      expect(dismissButton).toBeInTheDocument();

      await user.click(dismissButton);
      expect(onDismiss).toHaveBeenCalledTimes(1);
    });
  });

  describe('assertive prop', () => {
    it('renders with role="alert" when assertive={true}', () => {
      render(
        <NoticeBanner testId="assertive-notice" assertive={true}>
          Critical alert
        </NoticeBanner>,
      );
      expect(screen.getByTestId('assertive-notice')).toHaveAttribute('role', 'alert');
    });

    it('renders with role="status" by default (assertive omitted)', () => {
      render(<NoticeBanner testId="default-notice">Info message</NoticeBanner>);
      expect(screen.getByTestId('default-notice')).toHaveAttribute('role', 'status');
    });

    it('renders with role="status" when assertive={false}', () => {
      render(
        <NoticeBanner testId="non-assertive-notice" assertive={false}>
          Info message
        </NoticeBanner>,
      );
      expect(screen.getByTestId('non-assertive-notice')).toHaveAttribute('role', 'status');
    });
  });

  describe('icon prop variants', () => {
    it('renders without crashing for icon="alert-triangle"', () => {
      const { container } = render(
        <NoticeBanner testId="warning-notice" icon="alert-triangle">
          Warning message
        </NoticeBanner>,
      );
      expect(screen.getByText('Warning message')).toBeInTheDocument();
      // Verify an SVG icon element exists (Lucide icons render as <svg>)
      expect(container.querySelector('svg')).toBeInTheDocument();
    });

    it('renders without crashing for icon="info"', () => {
      const { container } = render(
        <NoticeBanner testId="info-notice" icon="info">
          Info message
        </NoticeBanner>,
      );
      expect(screen.getByText('Info message')).toBeInTheDocument();
      expect(container.querySelector('svg')).toBeInTheDocument();
    });

    // Note: Icon identity (alert-triangle vs info) is not independently distinguishable
    // in the DOM without aria-labels or testids on the icon elements themselves.
    // Both render as <svg> elements with no distinguishing attributes.
    // The tests above verify that each variant renders without crashing and produces an icon element.
  });
});
