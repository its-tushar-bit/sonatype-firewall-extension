/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/**
 * @param id - tooltip id attribute.
 * @param container - container element to which tooltip is appended.
 */
export default function TrendsTooltip(id, container) {
  const tooltipElement =
    document.getElementById(id) || createTooltipElement(id, container);
  return {
    setContent(content) {
      tooltipElement.innerHTML = content;
    },
    /**
     * @param left
     * @param top
     * @param content - optional, overrides existing content
     */
    show(left, top, content) {
      if (content) {
        tooltipElement.innerHTML = content;
      }
      tooltipElement.style.left = left + 'px';
      tooltipElement.style.top = top + 'px';
      tooltipElement.style.visibility = 'visible';
    },
    hide() {
      // use visibility:hidden instead of display:none, to be able to get tooltip width before showing it again
      tooltipElement.style.visibility = 'hidden';
    },
    getWidth() {
      return tooltipElement.offsetWidth;
    },
  };
}

function createTooltipElement(id, container) {
  const tooltip = document.createElement('div');
  tooltip.setAttribute('id', id);
  tooltip.setAttribute('class', 'iq-violation-trends__tooltip');
  container.appendChild(tooltip);
  return tooltip;
}
