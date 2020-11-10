/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faEdit } from '@fortawesome/free-solid-svg-icons';

export default function NoticeTextsTile() {

  return (
    <section id="notice-texts-tile" className="nx-tile">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">Notice Texts</h2>
        </div>
        <div className="nx-tile__actions">
          <a href="">View Details</a>
          <NxButton variant="tertiary">
            <NxFontAwesomeIcon icon={ faEdit }/>
            <span>Edit</span>
          </NxButton>
        </div>
      </header>
      <div className="nx-tile-content">
        <blockquote>
          Lorem ipsum dolor sit amet, consectetur adipiscing elit. Pellentesque euismod aliquam
          euismod. Nunc fermentum porta lorem vitae mattis. Integer at ultrices neque. Nam ultricies orci
          cursus odio malesuada, aliquam pulvinar velit vehicula. Mauris auctor, metus ut sodales laoreet,
          lectus nisl rhoncus dolor, sed porttitor sapien metus nec quam. Vestibulum rutrum vestibulum quam,
          a iaculis enim condimentum ac. Proin aliquet ullamcorper elit eu malesuada. Nunc feugiat laoreet
          convallis. Pellentesque eu congue tellus, sed pretium orci. Sed luctus turpis facilisis, faucibus
          lorem eget, sodales turpis. Donec pretium, sem ut efficitur blandit, nibh lacus imperdiet nisi
        </blockquote>
      </div>
    </section>
  );
}
