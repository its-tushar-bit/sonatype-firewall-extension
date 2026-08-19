pv.Behavior.panX = function(viz) {
    var scene, // scene context
    index, // scene context
    m1, // transformation matrix at the start of panning
    v1, // mouse location at the start of panning
    k; // inverse scale

    /** @private */
    function mousedown() {
        if (!viz)
            viz = this;
        index = viz.index;
        scene = viz.scene;
        v1 = pv.vector(pv.event.pageX, pv.event.pageY);
        m1 = viz.transform();
        k = 1 / (m1.k * viz.scale);
        bound = (1 - m1.k) * viz.height();
    }

    /** @private */
    function mousemove() {
        if (!scene)
            return;
        if (!viz)
            viz = this;
        scene.mark.context(scene, index, function() {
            var x = (pv.event.pageX - v1.x) * k, y = (pv.event.pageY - v1.y) * k, m = m1.translate(x, y);
            m.y = Math.max(bound, Math.min(0, m.y));
            viz.transform(m).render();
        });
        pv.Mark.dispatch("pan", scene, index);
    }

    /** @private */
    function mouseup() {
        scene = null;
    }

    pv.listen(window, "mousemove", mousemove);
    pv.listen(window, "mouseup", mouseup);
    return mousedown;
};