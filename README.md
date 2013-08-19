# Immutant clustering features

This app shows off the features of an Immutant cluster, specifically:

* Load-balanced messaging with automatic peer discovery
* HA singleton daemons 
* HA singleton jobs
* Cache and web session replication

All the code is in `src/immutant/init.clj`. A simple web app is
included that returns the contents of the distributed cache. When
fronted by a round-robin, HTTP reverse-proxy, the data should remain
consistent and available as long as at least one cluster node is up.

You can view all components of the app by "tailing" the
`standalone/logs/server.log` beneath `$IMMUTANT_HOME` on each node.

Failover for jobs and daemons can be observed by killing whichever
Immutant is running the job or daemon. This should result in the
immediate migration of those services to another node in the cluster
automatically.

## Usage

Form a cluster of Immutants by running the following on each node:

    $ lein immutant run --clustered

Deploy the app to each Immutant

    $ lein immutant deploy

## License

Copyright © 2013 Jim Crossley

Distributed under the Eclipse Public License, the same as Clojure.
