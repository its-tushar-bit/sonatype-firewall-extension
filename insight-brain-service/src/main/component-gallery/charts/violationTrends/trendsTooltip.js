/**
 * @param id - tooltip id attribute.
 * @param container - container element to which tooltip is appended.
 */
export default function TrendsTooltip(id, container) {
  const tooltip = document.getElementById(id) || createTooltipElement(id, container);
  return {
    show(left, top, content) {
      tooltip.innerHTML = content;
      tooltip.style.display = 'block';
      tooltip.style.left = left + 'px';
      tooltip.style.top = top + 'px';
    },
    hide() {
      tooltip.style.display = 'none';
    }
  };
}

function createTooltipElement(id, container) {
  const tooltip = document.createElement('div');
  tooltip.setAttribute('id', id);
  tooltip.setAttribute('class', 'trends-tooltip');
  container.appendChild(tooltip);

  return tooltip;
}
