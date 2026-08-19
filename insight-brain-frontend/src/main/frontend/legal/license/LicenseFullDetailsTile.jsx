/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useEffect, useState } from 'react';
import { selectableColorClasses } from '@sonatype/react-shared-components';
import { licenseLegalMetadataPropType } from '../advancedLegalPropTypes';
import * as PropTypes from 'prop-types';
import { partial, uniqWith } from 'ramda';

export default function LicenseFullDetailsTile(props) {
  const { componentLicenseDetails, licenseLegalMetadata } = props;

  const license = licenseLegalMetadata[componentLicenseDetails.licenseIndex];
  const obligations = license ? license.obligations : [];
  const licenseText = license ? license.licenseText : '';

  const [highlight, setHighlight] = useState('');

  const markRef = React.useRef(new Map());

  let obligationColorIndex = 0;
  let licenseSpanColorIndex = 0;

  const createObligationContentTexts = (licenseObligationLicenseText, index) => {
    const colorClass = selectableColorClasses[obligationColorIndex % selectableColorClasses.length];
    obligationColorIndex = obligationColorIndex + 1;

    const classes = `license-full-details__obligation-text ${colorClass}`;
    return (
      <dd className="nx-read-only__data" key={index}>
        <q className={classes} onClick={partial(setHighlight, [licenseObligationLicenseText])}>
          {licenseObligationLicenseText}
        </q>
      </dd>
    );
  };

  const obligationCamelCase = (text) =>
    text.replace(/[-_\s.]+(.)?/g, (_, c) => (c ? c.toUpperCase() : '')).replaceAll(/\s+/g, '');

  // builds ordered list of obligation text spans
  const obligationHighlightSpans = (text) => {
    let licenseSpans = [];

    if (!text) {
      return [];
    }
    obligations.map((obligation) => {
      obligation.obligationTexts.map((obligationText, textIndex) => {
        const colorClass = selectableColorClasses[licenseSpanColorIndex % selectableColorClasses.length];
        licenseSpanColorIndex += 1;
        const classes = `license-full-details__license-obligation-highlight ${colorClass}`;

        const reg = new RegExp(escapeTextSnippetForRegExp(obligationText), 'm');
        const match = reg.exec(text);
        if (match && match[0]) {
          const start = match.index;
          const end = start + match[0].length;
          licenseSpans.push({
            classes,
            start,
            end,
            obligationText,
            obligationAnchor: obligationCamelCase(obligation.name) + (textIndex > 0 ? textIndex : ''),
          });
        }
      });
    });

    licenseSpans.sort((a, b) => a.start - b.start);

    return uniqWith(uniqueObligationsTextSpan, licenseSpans);
  };

  const uniqueObligationsTextSpan = (obligationFirst, obligationSecond) =>
    obligationFirst.obligationText === obligationSecond.obligationText;

  const licenseTextWithHighlights = (text) => {
    const spans = obligationHighlightSpans(text);

    if (spans.length === 0) {
      return 'Nothing found';
    }

    let highlightedTexts = [];

    let lastPos = 0;
    spans.forEach((span, index) => {
      highlightedTexts.push(<Fragment key={`license-text-span-${index}`}>{text.slice(lastPos, span.start)}</Fragment>);
      highlightedTexts.push(
        <mark
          key={`license-text-span-highlight-${index}`}
          id={span.obligationAnchor}
          className={span.classes}
          ref={(element) => markRef.current.set(span.obligationText, element)}
        >
          {text.slice(span.start, span.end)}
        </mark>
      );
      lastPos = span.end;
    });

    highlightedTexts.push(<Fragment key={`license-text-span-last`}>{text.slice(lastPos)}</Fragment>);
    return highlightedTexts;
  };

  useEffect(() => {
    if (highlight && markRef.current && markRef.current.get(highlight)) {
      markRef.current.get(highlight).scrollIntoView({
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
    return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&').replace(/(\n| |\t)+/g, '[ \\r\\n]+');
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
                const texts = obligation.obligationTexts.map((obligationText) =>
                  createObligationContentTexts(obligationText, index)
                );
                return (
                  <div key={index}>
                    <dt className="nx-read-only__label">{obligation.name}</dt>
                    {texts}
                  </div>
                );
              })}
            </dl>
          </div>
          <section
            aria-labelledby="license-full-details-tile__license-header"
            className={
              'nx-grid-col nx-scrollable nx-viewport-sized__scrollable ' + 'component-license-details-license-container'
            }
          >
            <h3 className="nx-h3" id="license-full-details-tile__license-header">
              Standard License Text: {license ? license.licenseName : ''}
            </h3>
            <p className="nx-p component-license-details-license-preformatted">
              {licenseTextWithHighlights(licenseText)}
            </p>
          </section>
        </div>
      </div>
    </section>
  );
}

LicenseFullDetailsTile.propTypes = {
  componentLicenseDetails: PropTypes.object,
  licenseLegalMetadata: licenseLegalMetadataPropType,
};
