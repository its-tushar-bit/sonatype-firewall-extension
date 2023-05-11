/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useCallback, useRef, useEffect, useState } from 'react';
import * as PropTypes from 'prop-types';
import { NxFontAwesomeIcon, NxButton } from '@sonatype/react-shared-components';
import useResizeObserver from '@react-hook/resize-observer';
import { useDebounceCallback } from '@react-hook/debounce';
import { faArrowRight, faArrowLeft } from '@fortawesome/free-solid-svg-icons';

const PILLS_SCROLL_DISTANCE = 200;

const NavPills = ({ list, root }) => {
  const ref = useRef(null);

  const [leftArrowVisible, setLeftArrowVisible] = useState(false);
  const [rightArrowVisible, setRightArrowVisible] = useState(false);

  const updateArrowVisibility = function updateArrowVisibility() {
    const el = ref.current,
      scrollLeft = el?.scrollLeft ?? 0,
      scrollRight = (el?.scrollWidth ?? 0) - (scrollLeft + (el?.clientWidth ?? 0));

    setLeftArrowVisible(!!scrollLeft);

    // In Chrome, sometimes a fully scrolled element will have a fractional scrollLeft such that the scrollLeft
    // and clientWidth don't quite add up to the scrollWidth, but are within 1px
    setRightArrowVisible(scrollRight >= 1);
  };

  const scrollNavs = useDebounceCallback(
    useCallback(
      (leftScroll) =>
        ref.current?.scroll({
          top: 0,
          left: leftScroll,
          behavior: 'smooth',
        }),
      []
    ),
    350
  );

  const scrollTo = ({ target }) => {
    const id = target.dataset.scroll;
    const scrollRoot = document.querySelector(root);
    const element = document.querySelector(`#${id}`);
    const y = element.offsetTop - 16;
    scrollRoot.scrollTo({ top: y, behavior: 'smooth' });
  };

  const scrollRight = () => {
    if (ref.current.scrollLeft + PILLS_SCROLL_DISTANCE >= ref.current?.scrollWidth - ref.current?.offsetWidth) {
      return ref.current?.scroll({
        top: 0,
        left: ref.current?.scrollWidth - ref.current?.offsetWidth,
        behavior: 'smooth',
      });
    }

    ref.current?.scroll({
      top: 0,
      left: ref.current.scrollLeft + PILLS_SCROLL_DISTANCE,
      behavior: 'smooth',
    });
  };

  const scrollLeft = () => {
    if (ref.current.scrollLeft - PILLS_SCROLL_DISTANCE < 0) {
      ref.current?.scroll({
        top: 0,
        left: 0,
        behavior: 'smooth',
      });
      return;
    }

    ref.current?.scroll({
      top: 0,
      left: ref.current.scrollLeft - PILLS_SCROLL_DISTANCE,
      behavior: 'smooth',
    });
  };

  useEffect(() => {
    let observer;

    // Wait for the tiles to render. This is a hack but once the parent page is no longer in angular it
    // should be feasible to completely change how this component gets the references to the tiles and get rid
    // of this setTimeout
    const timeout = setTimeout(() => {
      const options = {
        root: document.querySelector(root),
        rootMargin: '0px 0px -70% 0px',
        threshold: 0.1,
      };

      const observedList = list
        .filter(({ isDisplayed }) => isDisplayed)
        .map(({ target }) => document.querySelector(`#${target}`));

      const intersectionCallback = (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            const activePill = [...ref.current?.children].find(
              (el) => el.firstChild.dataset.scroll === entry.target.id
            );

            const leftScroll = activePill.offsetLeft + activePill.offsetWidth * 2 - ref.current.scrollWidth / 2;
            scrollNavs(leftScroll);
          }
        });
      };

      observer = new IntersectionObserver(intersectionCallback, options);
      observedList.forEach((elem) => observer.observe(elem));
    }, 10);

    return () => {
      observer?.disconnect();
      clearTimeout(timeout);
    };
  }, [scrollNavs]);

  useResizeObserver(ref, updateArrowVisibility);

  return (
    <div className="iq-nav-pills-menu">
      {leftArrowVisible && (
        <div className="iq-arrow-left iq-arrow">
          <NxButton onClick={scrollLeft} variant="icon-only" title="scroll left">
            <NxFontAwesomeIcon icon={faArrowLeft} />
          </NxButton>
        </div>
      )}

      {rightArrowVisible && (
        <div className="iq-arrow-right iq-arrow">
          <NxButton onClick={scrollRight} variant="icon-only" title="scroll right">
            <NxFontAwesomeIcon icon={faArrowRight} />
          </NxButton>
        </div>
      )}

      <ul className="iq-nav-pills-menu__list" ref={ref} onScroll={updateArrowVisibility}>
        {list.map(
          ({ label, target, isDisplayed }) =>
            isDisplayed && (
              <li className="iq-nav-pills-menu__pill" key={label}>
                <button
                  data-testid={`${target}-button`}
                  id={`${target}-button`}
                  type="button"
                  data-scroll={target}
                  onClick={scrollTo}
                >
                  {label}
                </button>
              </li>
            )
        )}
      </ul>
    </div>
  );
};

NavPills.propTypes = {
  root: PropTypes.string.isRequired,
  list: PropTypes.arrayOf(
    PropTypes.shape({
      label: PropTypes.string.isRequired,
      target: PropTypes.string.isRequired,
      isDisplayed: PropTypes.bool,
    })
  ).isRequired,
};

export default NavPills;
