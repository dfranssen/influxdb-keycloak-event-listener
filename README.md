Keycloak Event Listener saving events to InfluxDB
=================================================

[Keycloak](https://www.keycloak.org) provide some out-of-the box event listeners and also a way to save the events to its own database in order to visualize them in the admin console.

Keycloak eventing can also be easily extended via its [Event Listener SPI](https://www.keycloak.org/docs/latest/server_development/index.html#_events).

This project is such an extension in order to save those events to [InfluxDB](https://www.influxdata.com/time-series-platform/), an opensource time series database.

Deploy option 1
---------------
To deploy copy target/influxdb-keycloak-event-listener-jar-with-dependencies.jar to the `providers` folder in Keycloak (create folder if it doesn't exist).

Deploy option 2
---------------
Alternatively you can deploy it as a module by running:

    KEYCLOAK_HOME/bin/jboss-cli.sh --command="module add --name=com.ditavision.keycloak.event-influxdb --resources=target/influxdb-keycloak-event-listener-jar-with-dependencies.jar --dependencies=org.keycloak.keycloak-core,org.keycloak.keycloak-server-spi,org.keycloak.keycloak-server-spi-private"

Then register the provider by editing `standalone/configuration/standalone.xml` and adding the module to the providers element:

    <providers>
        ...
        <provider>module:com.ditavision.keycloak.event-influxdb</provider>
    </providers>

Configuration
-------------
Make sure the following environment variables are provided with the InfluxDB details:
* `INFLUXDB_HOST` host to connect to. E.g. `localhost`. By default `influxdb`.
* `INFLUXDB_PORT`: port to connect to. E.g. `8086`. By default `8086`.
* `INFLUXDB_USER`: user to connect with. E.g. `root`. By default `root`.
* `INFLUXDB_PWD`: password of the user. E.g. `your-super-secrect-pwd`. By default `root`.
* `INFLUXDB_DB`: name of the database to use. E.g. `monitoring`. By default `keycloak`.
* `INFLUXDB_DB_RETENTION_POLICY`: Data retention policy. E.g. `2y`. By default `14d`.

Then start (or restart) the server. Once started open the admin console, select your realm, then click on Events, 
followed by config. Click on Listeners select box, then pick `influxDB` from the dropdown. After this try to logout and 
login again to see events being saved in InfluxDB.

The event listener can be configured to exclude certain events, for example to exclude REFRESH_TOKEN and
CODE_TO_TOKEN events add the following to `standalone.xml`:

    ...
    <spi name="eventsListener">
        <provider name="influxDB">
            <properties>
                <property name="exclude-events" value="[&quot;REFRESH_TOKEN&quot;, &quot;CODE_TO_TOKEN&quot;]"/>
            </properties>
        </provider
    </spi>

Dashboarding
------------
See the `grafana-dashboards` folder for some [Grafana](https://grafana.com) dashboards, based on a `14d` retention policy,  to visualize those KeyCloak events/logins per realm.
Those json files can be easily imported in Grafana.

Integrated example
------------------
See [https://github.com/dfranssen/communities-demo/tree/extra](https://github.com/dfranssen/communities-demo/tree/solution)