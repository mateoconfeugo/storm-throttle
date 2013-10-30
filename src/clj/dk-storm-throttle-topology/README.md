# dk-storm-throttle-topology

The usage spout streams usage-detail records recieved from delivery servers to the accumlator bolt which then totals up all the usage across the nodes.This summary is emitted to the scorekeeprer bolt.

The scorekeeper assembles a budget of allocated query request and current count for the feed source tuples. These budget item tuples are emitted to the govenor bolt that updates the budget cache.

The budget records are streamed as jsonified tuples from the centralized budget cache via client to the governor proxy running on each delivery node.

## Usage

storm jar dk-storm-throttle-0.0.1-SNAPSHOT-standalone.jar dk.storm.throttle.topology  storm-throttling-topo

## License

Copyright © 2012 Matt Burns

Distributed under the Eclipse Public License, the same as Clojure.
