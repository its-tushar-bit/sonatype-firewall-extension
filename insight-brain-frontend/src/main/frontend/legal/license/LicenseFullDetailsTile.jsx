/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useEffect, useState } from 'react';
import { selectableColors } from '@sonatype/react-shared-components';
import { licenseLegalMetadataPropType } from '../advancedLegalPropTypes';
import * as PropTypes from 'prop-types';
import { partial } from 'ramda';

export default function LicenseFullDetailsTile(props) {
  const { componentLicenseDetails, licenseLegalMetadata } = props;

  const license = licenseLegalMetadata[componentLicenseDetails.licenseIndex];
  const obligations = license ? license.obligations : [];
  const licenseText = license ? license.licenseText : '';

  const [highlight, setHighlight] = useState('');

  const markRef = React.useRef(null);

  let colorIndex = 0;

  const createObligationContentTexts = (licenseObligationLicenseText, index) => {
    colorIndex = colorIndex + 1;

    const color = selectableColors[colorIndex % selectableColors.length];
    const classes = `license-full-details__obligation-text--${color}`;
    return (
      <dd key={index} className="nx-read-only__data">
        <q className={classes} key={index} onClick={partial(setHighlight, [licenseObligationLicenseText])}>
          {licenseObligationLicenseText}
        </q>
      </dd>
    );
  };

  useEffect(() => {
    if (highlight && markRef.current) {
      markRef.current.scrollIntoView({
        behavior: 'smooth',
        block: 'center',
      });
    }
  }, [highlight]);

  /**
   * A routine that converts a text snippet into a regular expression to be used to search
   * for this exact snipped in a larger text block.
   * In addition to escaping regular expression's special characters it also replaces spaces
   * and line breaks with a pattern to match any number of them.
   * @param string Input snippet to be escaped
   */
  function escapeTextSnippetForRegExp(string) {
    return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&').replace(/(\n| )+/g, '[ \\r\\n]+');
  }

  function highlightInText(context, toHighlight) {
    if (!context) {
      return 'Nothing found';
    }
    const reg = new RegExp(escapeTextSnippetForRegExp(toHighlight), 'm');
    const match = reg.exec(context);
    if (!match || !match[0]) {
      return context;
    }

    const start = match.index;
    const end = start + match[0].length;
    return (
      <Fragment>
        {context.slice(0, start)}
        <mark className="component-license-details-text-highlight" ref={markRef}>
          {context.slice(start, end)}
        </mark>
        {context.slice(end)}
      </Fragment>
    );
  }

  return (
    <section id="license-full-details-tile" className="nx-tile nx-viewport-sized__container">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">{license ? license.licenseName : ''} License Obligations</h2>
        </div>
      </header>
      <div className="nx-tile-content nx-viewport-sized__container nx-grid">
        <div
          id="license-full-details-tile__obligations-and-license"
          className="nx-grid-row nx-grid-h-keyline nx-viewport-sized__container"
        >
          <div className="nx-grid-col nx-scrollable nx-viewport-sized__scrollable">
            <dl className="nx-read-only" id="license-full-details-tile__obligations-container">
              {obligations.map((obligation, index) => {
                const texts = obligation.obligationTexts.map(createObligationContentTexts);
                return (
                  <div key={index}>
                    <dt className="nx-read-only__label">{obligation.name}</dt>
                    {texts}
                  </div>
                );
              })}
            </dl>
          </div>
          <div
            className={
              'nx-grid-col nx-scrollable nx-viewport-sized__scrollable ' + 'component-license-details-license-container'
            }
          >
            <h3 className="nx-h3" id="license-full-details-tile__license-header">
              Standard License Text: {license ? license.licenseName : ''}
            </h3>
            <p className="nx-p component-license-details-license-preformatted">
              {highlightInText(licenseText, highlight)}
            </p>
          </div>
        </div>
      </div>
    </section>
  );
}

LicenseFullDetailsTile.propTypes = {
  componentLicenseDetails: PropTypes.object,
  licenseLegalMetadata: licenseLegalMetadataPropType,
};
