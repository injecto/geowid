var chunkSize = 4;              // размер группы единовременно выводимых точек
var timeBetweenChunks = 250;    // интервал времени между группами выводимых точек
var ajaxTimeOut = 3000;         // таймаут AJAX-запроса
// способ вывода маркеров:
// 'svg' анимированные высококачественные векторные маркеры
// 'pic' статичные маркеры, заданные картинками
// 'anim' анимированные gif-маркеры
var viewType = 'svg';

var pointsQueue = [];

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

/**********************************************************************************************************************/

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
    var hexDigits = '0123456789abcdef';
    for (var i = 0; i < 36; i++)
        s[i] = hexDigits.substr(Math.floor(Math.random() * 0x10), 1);

    s[14] = '4';
    s[19] = hexDigits.substr((s[19] & 0x3) | 0x8, 1);
    s[8] = s[13] = s[18] = s[23] = '-';

    return s.join('');
}

var SimpleMarker = L.Icon.extend({
    options: {
        iconSize: [10, 10],
        iconAnchor: [5, 5],
        popupAnchor: [9, 1]
    }
});

var blueMarker = new SimpleMarker({ iconUrl: 'pics/blue.png' }),
    redMarker = new SimpleMarker({ iconUrl: 'pics/red.png' }),
    greenMarker = new SimpleMarker({ iconUrl: 'pics/green.png' }),
    yellowMarker = new SimpleMarker({ iconUrl: 'pics/yellow.png' });

var AnimMarker = L.Icon.extend({
    options: {
        iconSize: [26, 26],
        iconAnchor: [13, 13],
        popupAnchor: [25, 1]
    }
});

var blueAnimMarker = new AnimMarker({ iconUrl: 'pics/blue.gif' }),
    redAnimMarker = new AnimMarker({ iconUrl: 'pics/red.gif' }),
    greenAnimMarker = new AnimMarker({ iconUrl: 'pics/green.gif' }),
    yellowAnimMarker = new AnimMarker({ iconUrl: 'pics/yellow.gif' });

/**
 * вывести одну точку
 * @param point точка
 * @param layer слой
 */
function show(point, layer, map) {
    if (pointColor[point.type] === undefined)
        return;

    switch (viewType) {
        case 'svg':
            layer.pulse(point.lat, point.lng, pointColor[point.type]);
            break;
        case 'pic':
            var m;
            if (pointColor._all_flag === true)
                m = L.marker([point.lat, point.lng], { icon: blueMarker });
            else
                switch (point.type) {
                    case 'def':
                        m = L.marker([point.lat, point.lng], { icon: redMarker });
                        break;
                    case 'mob':
                        m = L.marker([point.lat, point.lng], { icon: greenMarker });
                        break;
                    case 'api':
                        m = L.marker([point.lat, point.lng], { icon: yellowMarker });
                        break;
                    default:
                        console.log('Unknown marker type "' + point.type + '"');
                }
            map.addLayer(m);
            setTimeout(function () {
                map.removeLayer(m);
            }, 7000);
            break;
        case 'anim':
            var m;
            if (pointColor._all_flag === true)
                m = L.marker([point.lat, point.lng], { icon: blueAnimMarker });
            else
                switch (point.type) {
                    case 'def':
                        m = L.marker([point.lat, point.lng], { icon: redAnimMarker });
                        break;
                    case 'mob':
                        m = L.marker([point.lat, point.lng], { icon: greenAnimMarker });
                        break;
                    case 'api':
                        m = L.marker([point.lat, point.lng], { icon: yellowAnimMarker });
                        break;
                    default:
                        console.log('Unknown marker type "' + point.type + '"');
                }
            map.addLayer(m);
            setTimeout(function () {
                map.removeLayer(m);
            }, ptTime);
            break;
        default:
            console.log('Unknown marker view type');
    }
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
        zoomControl: false,
        worldCopyJump: false
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

    var pulseLayer = new R.PulseLayer(100);
    map.addLayer(pulseLayer);

    var noDataCounter = 0;
    var flush = false;
    var flushNum = 0;
    var url = 'get/?id=' + getPseudoGUID();

    (function poll() {
        $.ajax({
            url: url,
            success: function(points) {
                flush = true;
                flushNum = pointsQueue.length;
                pointsQueue = pointsQueue.concat(points);

                if (pointsQueue.length >= chunkSize) {
                    noDataCounter = 0;
                    $('#wait').fadeOut('slow');
                }
            },
            dataType: 'json',
            complete: poll,
            timeout: ajaxTimeOut,
            cache: false
        });
    })();

    setInterval(function () {
        if (flush && flushNum != 0) {
            flush = false;
            var chunk = pointsQueue.splice(0, flushNum);
            for (var i = 0; i < flushNum; i++)
                show(chunk[i], pulseLayer, map);
            return;
        }
        if (pointsQueue.length >= chunkSize) {
            var chunk = pointsQueue.splice(0, chunkSize);
            for (var i = 0; i < chunkSize; i++)
                show(chunk[i], pulseLayer, map);
        } else {
            noDataCounter++;
            if (noDataCounter >= 8000/timeBetweenChunks)
                $('#wait').fadeIn('slow');
        }
    }, timeBetweenChunks);
});
