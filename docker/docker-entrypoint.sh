#!/bin/bash

INIT_FILE="/opt/jboss/initialized.done"

function waitWildfly {
	status=""
	while [ "$status" != "running" ];do
		status=$(/opt/jboss/wildfly/bin/jboss-cli.sh -c --user=admin --password=password --commands="read-attribute server-state")
		if [ "$status" != "running" ]; then
			echo "Waiting for Wildfly..."
			sleep 1
		fi
	done
}

function initWildfly {
	waitWildfly
	/opt/jboss/wildfly/bin/jboss-cli.sh -c --user=admin --password=password --file=/tmp/wildfly_db.cli
	/opt/jboss/wildfly/bin/jboss-cli.sh -c --user=admin --password=password --command='/subsystem=transactions:write-attribute(name=default-timeout,value=36000)'
	/opt/jboss/wildfly/bin/jboss-cli.sh -c --user=admin --password=password --command='/subsystem=undertow/server=default-server/http-listener=default/:write-attribute(name=max-post-size,value=100485760)'
	/opt/jboss/wildfly/bin/jboss-cli.sh -c --user=admin --password=password --command="/:reload"
	/opt/jboss/wildfly/bin/jboss-cli.sh -c --user=admin --password=password --command="deploy /tmp/chouette.ear"
	touch $INIT_FILE
}

function waitPostgres {
  status="closed"
  while [[ $status == *"closed"* ]];do
    status=$(nmap -v -p 5432 chouette-postgres)
    echo 'Waiting for Postgres...'
    sleep 2
  done
  exec $@
}

[ ! -e $INIT_FILE ] && initWildfly &

waitPostgres $@

