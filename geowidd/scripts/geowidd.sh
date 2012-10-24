#!/bin/sh

ARGS=etc/geowid_daemon_settings.xml   # параметры запуска демона
USER=vio    # пользователь, с правами которого будет запущен демон
OUT=logs/geowidd.log # файл для перенаправления потока вывода
ERR=logs/geowidd.log # файл для перенаправления потока ошибок
PID=resources/geowidd.pid # файл для хранения pid запущенного демона
JAVA=$JAVA_HOME # каталог расположения Java
JSVC=/usr/bin/jsvc # исполняемый файл jsvc

# лучше не редактировать, если точно не уверен
CP="lib/geowidd.jar":"lib/commons-daemon-1.0.10.jar"
CLASS=com.ecwid.geowid.daemon.GeowidDaemon
DEBUG_PARAMS="-Xdebug -Xrunjdwp:transport=dt_socket,address=9967,server=y"
USAGE_MSG="Usage: geowidd.sh {start [debug]|stop|restart}"

do_exec()
{
    $JSVC -jvm 'server' -cp $CP -home $JAVA $2 -user $USER -procname 'geowidd' -outfile $OUT -errfile $ERR -pidfile $PID $1 $CLASS $ARGS
}

case "$1" in
    start)
        if [ ! -d "logs/" ]; then
            mkdir logs
        fi

        if [ -f "$PID" ]; then
            echo "Service already running"
            exit 1
        else
            if [ "$2" = "debug" ]; then
                echo "Run in debug mode"
                do_exec "" "$DEBUG_PARAMS"
            else
                if [ -z "$2" ]; then
                    do_exec
                else
                    echo $USAGE_MSG >&2
                    exit 2
                fi
            fi
        fi
            ;;
    stop)
        if [ -f "$PID" ]; then
            do_exec "-stop"
        else
            echo "Service not running"
            exit 1
        fi
            ;;
    restart)
        if [ -f "$PID" ]; then
            do_exec "-stop"
            do_exec
        else
            echo "Service not running"
            exit 1
        fi
            ;;
    *)
            echo $USAGE_MSG >&2
            exit 2
            ;;
esac