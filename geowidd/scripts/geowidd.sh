#!/bin/sh

ARGS="./geowid_daemon_settings.xml ./GeoLiteCity.dat"
USER=vio
OUT=geowidd.log
ERR=geowidd.log
PID=geowidd.pid
JSVC=/usr/bin/jsvc
CP="./geowidd.jar":"./commons-daemon-1.0.10.jar"
CLASS=com.ecwid.geowid.daemon.GeowidDaemon
JAVA=/usr/lib/jvm/jdk1.6.0_33
DEBUG=false

do_exec()
{
    if $DEBUG
    then
        $JSVC -home $JAVA -Xdebug -Xrunjdwp:transport=dt_socket,address=9967,server=y -user $USER -procname 'geowidd' -server -cp $CP -outfile $OUT -errfile $ERR -pidfile $PID $1 $CLASS $ARGS
    else
        $JSVC -home $JAVA -user $USER -procname 'geowidd' -server -cp $CP -outfile $OUT -errfile $ERR -pidfile $PID $1 $CLASS $ARGS
    fi
}

case "$1" in
    start)
        if [ -f "$PID" ]; then
            echo "Service already running"
            exit 1
        else
            do_exec
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
            echo "Usage: geowidd.sh {start|stop|restart}" >&2
            exit 3
            ;;
esac