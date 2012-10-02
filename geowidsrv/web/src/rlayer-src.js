(function() {

var R, originalR;

if (typeof exports != 'undefined') {
	R = exports;
} else {
	R = {};

	originalR = window.R;

	R.noConflict = function() {
		window.R = originalR;
		return R;
	};

	window.R = R;
}

R.version = '0.1.2x';

R.Layer = L.Class.extend({
	includes: L.Mixin.Events,
	
	initialize: function(options) {
		
	},

	onAdd: function (map) {
		this._map = map;
		this._map._initRaphaelRoot();
		this._paper = this._map._paper;
		this._set = this._paper.set();
		
		map.on('viewreset', this.projectLatLngs, this);
		this.projectLatLngs();
	},

	onRemove: function(map) {
		map.off('viewreset', this.projectLatLngs, this);
		this._map = null;
		this._set.forEach(function(item) {
			item.remove();
		}, this);
		this._set.clear();
	},

	projectLatLngs: function() {
		
	},

	animate: function(attr, ms, easing, callback) {
		this._set.animate(attr, ms, easing, callback);
	
		return this;
	},

	hover: function(f_in, f_out, icontext, ocontext) {
		this._set.hover(f_in, f_out, icontext, ocontext);

		return this;
	},

	attr: function(name, value) {
		this._set.attr(name, value);

		return this;
	}
});

L.Map.include({
	_initRaphaelRoot: function () {
		if (!this._raphaelRoot) {
			this._raphaelRoot = this._panes.overlayPane;
			this._paper = Raphael(this._raphaelRoot);

			this.on('moveend', this._updateRaphaelViewport);
			this._updateRaphaelViewport();
		}
	},

	_updateRaphaelViewport: function () {
		var	p = 0.02,
			size = this.getSize(),
			panePos = L.DomUtil.getPosition(this._mapPane),
			min = panePos.multiplyBy(-1)._subtract(size.multiplyBy(p)),
			max = min.add(size.multiplyBy(1 + p*2)),
			width = max.x - min.x,
			height = max.y - min.y,
			root = this._raphaelRoot,
			pane = this._panes.overlayPane;

		this._paper.setSize(width, height);
		
		L.DomUtil.setPosition(root, min);

		root.setAttribute('width', width);
		root.setAttribute('height', height);
		
		this._paper.setViewBox(min.x, min.y, width, height, false);
		
	}
});

R.PulseLayer = R.Layer.extend({

    _pool: [],
    _used: [],

    _add: function (num) {
        for (var i = 0; i < num; i++)
            this._pool.push({
                    point: this._paper.circle(0, 0, this._radius).attr({ 'opacity': 0 }),
                    wave: this._paper.circle(0, 0, this._radius).attr({ 'opacity': 0, 'stroke-opacity': 0 })
                }
            );
    },

    pulse: function (lat, lng, color, time) {
        var pt = this._pool.pop();
        if (pt === undefined) {
            this._add(this._addNum);
            pt = this._pool.pop();
        }

        var index = this._used.length;
        this._used[index] = {
            data: pt,
            latlng: new L.LatLng(lat, lng)
        };

        var p = this._map.latLngToLayerPoint(new L.LatLng(lat, lng));
        pt.point.attr({ 'fill': color, 'cx': p.x, 'cy': p.y });
        pt.wave.attr({ 'fill': color, 'cx': p.x, 'cy': p.y });

        pt.point.animate({
            '0%': { transform: 's0.01' },
            '10%': { transform: 's1', opacity: 0.85, easing: 'backOut' },
            '100%': { transform: 's0.01', opacity: 0 }
        }, time, function () {
            this._pool.push(pt);
            this._used.splice(index, 1);
        });

        pt.wave.animate({
            '0%': { transform: 's0.01', opacity: 0.85 },
            '100%': { transform: 's2', opacity: 0, easing: '<' }
        }, time*0.5);
    },

	initialize: function(poolSize, options) {
		R.Layer.prototype.initialize.call(this, options);

        this._poolSize = poolSize;
		this._radius = 5;
        this._addNum = 10;
	},

    onAdd: function (map) {
        R.Layer.prototype.onAdd.call(this, map);

        this._add(this._poolSize);
    },

	onRemove: function (map) {
		R.Layer.prototype.onRemove.call(this, map);

		this._paper.clear();
	},

    projectLatLngs: function () {
        for (var i = 0; i < this._used.length; i++) {
            var p = this._map.latLngToLayerPoint(this._used[i].latlng);
            if (this._used[i].data.point) this._used[i].data.point.attr({ 'cx': p.x, 'cy': p.y });
            if (this._used[i].data.wave) this._used[i].data.wave.attr({ 'cx': p.x, 'cy': p.y });
        }
    }
});

}());