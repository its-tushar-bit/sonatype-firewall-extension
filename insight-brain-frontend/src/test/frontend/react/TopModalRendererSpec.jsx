/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { mount } from 'enzyme';
import TopModalRenderer from '../../../main/frontend/react/TopModalRenderer';

describe('TopModalRenderer', function() {
  let modalContainer;

  beforeEach(function() {
    // Create node where the contents of the modals are expected to render
    modalContainer = document.createElement('div');
    modalContainer.setAttribute('id', 'modal-view');
    document.body.appendChild(modalContainer);
  });

  afterEach(function() {
    if (modalContainer) {
      document.body.removeChild(modalContainer);
      modalContainer = null;
    }
  });

  it('renders its children to the expected modal container node on the dom inside a div', function() {
    const mountedModal = mount(
      <div>
        <TopModalRenderer>
          <div className='modalContents'>
            <h1>Modal contents</h1>
          </div>
        </TopModalRenderer>
      </div>);

    // Expect a new div in the container with the contents of the TopModalRenderer
    expect(modalContainer.firstChild.nodeName).toEqual('DIV');
    expect(modalContainer.firstChild.firstElementChild).toEqual(mountedModal.children().getDOMNode());
  });
});
