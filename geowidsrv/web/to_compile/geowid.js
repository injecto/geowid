var pointsQueue = [];   // очередь точек для отображения
var chunkSize = 8;     // размер группы единовременно выводимых точек

/**
 * объект события карты (точки)
 * @param caption описание (для вывода в диалоге)
 * @param color цвет точки
 * @param enabled true = включена по умолчанию в вывод
 * @constructor
 */
function MapEvent(caption, color, enabled) {
    this.caption = caption;
    this.color = color;
    this.enabled = enabled;
}

/**
 * возможные события
 * ключ = идентификатор типа события
 * значение = описание вида
 * @type {Object}
 */
var mapEvents = {
    def: new MapEvent('customer', '#FF2121', true),
    mob: new MapEvent('mobile customer', '#1AE832', true),
    api: new MapEvent('APIs call', '#FFFF63', false)
};

var generalColor = '#21E9FF'; // цвет вывода всех точек по умолчанию

/**
 * Контрол для отображения окна настроек показа точек
 * @type {*}
 */
L.Control.View = L.Control.extend({
    options: {
        position: 'bottomright'
    },

    onAdd: function (map) {
        var container = L.DomUtil.create('div', 'leaflet-control-view');
        this._map = map;
        this._createButton('View', 'leaflet-control-view-button', container, this._click, this);
        return container;
    },

    _createButton: function (title, className, container, fn, context) {
        var link = L.DomUtil.create('a', className, container);
        link.href = '#';
        link.title = title;

        L.DomEvent
            .on(link, 'click', L.DomEvent.preventDefault)
            .on(link, 'click', fn, context);

        L.DomEvent.disableClickPropagation(link);

        return link;
    },

    _click: function(e) {
        var timer1Id, timer2Id;
        $('#view-dialog')
            .css({ right: $(window).width() - L.DomEvent.getMousePosition(e).x,
                   bottom: $(window).height() - L.DomEvent.getMousePosition(e).y })
            .mouseleave(function () {
                timer1Id = setTimeout(function () { $('#view-dialog').fadeOut('slow'); }, 500);
            })
            .mouseenter(function () {
                if (timer1Id !== undefined)
                    clearTimeout(timer1Id);
                if (timer2Id !== undefined)
                    clearTimeout(timer2Id);
            })
            .fadeIn('fast');
        timer2Id = setTimeout(function () { $('#view-dialog').fadeOut('slow'); }, 5000)
    }
});

/**
 * Палитра карты
 * @type {Object}
 */
var pointColor = {
    _all_flag: false,
    _add: function (type) {
        if (mapEvents[type] !== undefined)
            this[type] = this._all_flag ? generalColor : mapEvents[type].color;
    },

    _rem: function (type) {
        if (this[type] !== undefined)
            delete this[type];
    },

    _all: function () {
        this._all_flag = true;
        for (var e in this)
            if (mapEvents[e] !== undefined)
                this[e] = generalColor;
    },

    _default: function () {
        this._all_flag = false;
        for (var e in this)
            if (mapEvents[e] !== undefined)
                this[e] = mapEvents[e].color;
    }
};

/**
 * колбэк для чекбоксов в окне настроек
 * @param id
 */
function checkHandler(id) {
    if (id == 'all')
        $('#all').is(':checked') ? pointColor._all() : pointColor._default();
    else
        $('#'+id).is(':checked') ? pointColor._add(id) : pointColor._rem(id);
}

/**
 * вернуть псевдоGUID
 * @return {String} псевдоGUID
 */
function getPseudoGUID() {
    var s = [];
    var hexDigits = "0123456789abcdef";
    for (var i = 0; i < 36; i++)
        s[i] = hexDigits.substr(Math.floor(Math.random() * 0x10), 1);

    s[14] = "4";
    s[19] = hexDigits.substr((s[19] & 0x3) | 0x8, 1);
    s[8] = s[13] = s[18] = s[23] = "-";

    return s.join("");
}

/**
 * вывести одну точку
 * @param point точка
 * @param map карта
 */
function pt(point, map) {
    if (pointColor[point.type] === undefined)
        return;

    var p = new R.Pulse(
        new L.LatLng(point.lat, point.lng),
        5,
        {'fill': pointColor[point.type], 'fill-opacity': 0.75, 'stroke-opacity': 0.75 },
        {'fill': pointColor[point.type], 'fill-opacity': 0.5, 'stroke-opacity': 0 }
    );
    map.addLayer(p);
    setTimeout(function () {
        map.removeLayer(p);
    }, 3500);
}

/**
 * коррекция отображения карты (подгонка масштаба и центрирование)
 * @param map карта
 */
function correct(map) {;
    var world = L.latLngBounds([-50, -150],[78, 179]);
    map.panTo(world.getCenter());
    map.setZoom(map.getBoundsZoom(world));
}

$(function () {
    $('#wait').fadeIn('slow');
    var isFirstLoad = true;

    for (var e in mapEvents) {
        $('#view-dialog').append("<label><input type='checkbox' id='"+e+"'>" +
            "<span style='color: " + mapEvents[e].color + "'>" + mapEvents[e].caption + "</span></label><br>");
        $('#'+e).attr('onchange', 'checkHandler("' + e + '");');
        if (mapEvents[e].enabled) {
            pointColor._add(e);
            $('#'+e).attr('checked', 'checked');
        }
    }
    $('#view-dialog').append("<hr noshade size='1'><label><input type='checkbox'" +
        " onchange='checkHandler(\"all\");' id='all' checked='checked'>" +
        "<span style='color: "+generalColor+"'>one color for all</span></label>");
    pointColor._all();

    var map = L.map('map', {
        zoomControl: false
    }).fitWorld();

    correct(map);

    $(window).resize(function () {
        correct(map);
    });

    L.tileLayer('http://{s}.tile.cloudmade.com/{key}/{styleId}/256/{z}/{x}/{y}.png', {
        key: '8d75191ff95943ad89dc5e650d23ffeb',
        styleId: 999,
        attribution: 'Map data by <a href="http://www.ecwid.com/">Ecwid</a>',
        detectRetina: true,
        reuseTiles: true
    }).addTo(map);

    map.addControl(new L.Control.View());

    (function poll() {
        $.ajax({
            url: "./get/?id=" + getPseudoGUID(),
            success: function(points) {
                if (isFirstLoad) {
                    $('#wait').fadeOut('slow');
                    isFirstLoad = false;
                }
                pointsQueue = pointsQueue.concat(points);
            },
            dataType: "json",
            complete: poll,
            timeout: 2000,
            cache: false
        });
    })();

    setInterval(function () {
        if (pointsQueue.length >= chunkSize) {
            var chunk = pointsQueue.splice(0, chunkSize);
            for (var i = 0; i < chunkSize; i++)
                pt(chunk[i], map);
        }
    }, 250);
});
