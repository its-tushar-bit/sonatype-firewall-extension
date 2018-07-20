
export default function renderPlottable() {
  return {
    scope: {
      chart: '=renderPlottable'
    },
    link: function(scope, el) {
      scope.chart.renderTo(el[0]);
      window.addEventListener("resize", function() {
        scope.chart.redraw();
      });
    }
  };
}
