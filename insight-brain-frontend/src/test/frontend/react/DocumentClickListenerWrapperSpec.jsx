/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { mount } from 'enzyme';
import DocumentClickListenerWrapper from '../../../main/frontend/react/DocumentClickListenerWrapper';

describe('DocumentClickListenerWrapper', function() {
  let container, element, handleDocumentClick;

  beforeEach(function() {
    // Avoid rendering directly on the body.
    container = document.createElement('div');
    document.body.appendChild(container);
    handleDocumentClick = jasmine.createSpy('handleDocumentClick');

    element = mount(
      <div>
        <DocumentClickListenerWrapper onDocumentClick={handleDocumentClick}>
          <button id="wrapped-btn">click</button>
        </DocumentClickListenerWrapper>
        <button id="document-btn">click</button>
      </div>,
      { attachTo: container }
    );
  });

  afterEach(function() {
    if (container) {
      document.body.removeChild(container);
      container = null;
    }
  });

  it('calls onDocumentClick handler when document is clicked', function() {
    element.find('#document-btn').getDOMNode().dispatchEvent(new MouseEvent('click', {
      bubbles: true
    }));

    expect(handleDocumentClick).toHaveBeenCalled();
  });

  it('renders wrapped content and calls onDocumentClick handler when wrapped element is clicked', function() {
    element.find('#wrapped-btn').getDOMNode().dispatchEvent(new MouseEvent('click', {
      bubbles: true
    }));

    expect(handleDocumentClick).toHaveBeenCalled();
  });
});
