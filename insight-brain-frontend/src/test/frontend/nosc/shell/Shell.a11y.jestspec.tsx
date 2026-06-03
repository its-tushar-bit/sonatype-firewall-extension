/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { axe, toHaveNoViolations } from 'jest-axe';
import { BRAND_ACCENT } from 'MainRoot/nosc/theme';

import '@radix-ui/themes/styles.css';

expect.extend(toHaveNoViolations);

/**
 * jsdom smoke for semantic a11y (roles, labels, axe rules that do not need layout).
 * Color/contrast and real CSS are not exercised here — cover those in browser
 * functional tests once the shell composition lands (F2/F3/F9).
 */
describe('Nexus One shell semantic a11y (jest-axe jsdom smoke)', () => {
  // Theme-only placeholder until F2/F3/F9 shell components are composed here.

  it.each(['light', 'dark'] as const)(
    'Theme appearance=%s has no jest-axe violations in jsdom',
    async (appearance) => {
      const { container } = render(
        <Theme
          appearance={appearance}
          accentColor={BRAND_ACCENT}
          grayColor="slate"
          radius="medium"
          scaling="100%"
        >
          {/* Friday PM: replace with the real F2 + F3 + F9 shell composition */}
          <main data-testid="shell-placeholder">
            <h1>Shell placeholder</h1>
            <p>Real shell composition arrives once F2 + F3 + F9 land.</p>
          </main>
        </Theme>,
      );

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    },
  );
});
