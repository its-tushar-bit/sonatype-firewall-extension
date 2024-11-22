/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { render, screen } from 'TestRoot/SpecUtil';

import useConditionalAutoFocus from 'MainRoot/react/useConditionalAutoFocus';

const HOOK_INNER_TEXT = 'Hook Wrapper';

function HookWrapper({ condition }) {
  const ref = useConditionalAutoFocus(condition);

  return (
    <button type="button" ref={ref}>
      {HOOK_INNER_TEXT}
    </button>
  );
}

HookWrapper.propTypes = {
  condition: PropTypes.bool.isRequired,
};

describe('useConditionalAutoFocus', function () {
  let renderComponent, minimalProps;

  beforeEach(function () {
    minimalProps = {
      condition: true,
    };

    renderComponent = (additionalProps) => render(<HookWrapper {...minimalProps} {...additionalProps} />);
  });

  it('focus an element on render if the condition is true', function (done) {
    renderComponent();

    setTimeout(() => {
      expect(screen.getByText(HOOK_INNER_TEXT)).toHaveFocus();
      done();
    }, 1);
  });

  it('do not focus an element on render if the condition is false', function (done) {
    renderComponent({ condition: false });

    setTimeout(() => {
      expect(screen.getByText(HOOK_INNER_TEXT)).not.toHaveFocus();
      done();
    }, 100);
  });
});
