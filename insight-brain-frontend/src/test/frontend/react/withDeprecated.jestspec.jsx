/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import withDeprecated from 'MainRoot/react/withDeprecated';

import 'TestRoot/SpecUtil';

describe('withDeprecated', () => {
  const deprecatedComponent = () => <div>deprecated</div>;
  const WithDeprecatedComponent = withDeprecated(deprecatedComponent);
  const renderComponent = () => render(<WithDeprecatedComponent />);

  it('console logs a warning about the deprecated component on mount', () => {
    const originalEnv = process.env.NODE_ENV;
    process.env.NODE_ENV = 'development';
    const consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});

    try {
      renderComponent();

      expect(screen.getByText('deprecated')).toBeVisible();
      expect(consoleWarnSpy).toHaveBeenCalledTimes(1);
      expect(consoleWarnSpy.mock.calls[consoleWarnSpy.mock.calls.length - 1][0]).toMatch(/is deprecated./);
    } finally {
      process.env.NODE_ENV = originalEnv;
    }
  });
});
