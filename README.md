Purpose:  Sources and publishers request from delivery servers ads.  These requestors have a budget of how many queries allowed per feed.  The storm throttle limits the number search queries allowed access to feeds across a distributed set of delivery servers and is updated in real time.

Mechanics:
The usage spout streams usage-detail records recieved from delivery servers to the accumlator bolt which then totals up all the usage across the nodes.This summary is  emitted to the scorekeeprer bolt. 

The scorekeeper assembles a budget of allocated query request and current count for the feed source tuples. These budget item tuples are emitted to the govenor bolt that updates the budget cache.

The budget records are streamed as jsonified tuples from the centralized budget cache via client to the governor proxy running on each delivery node.

The proxy inserts the tuples into the budget caches on each of the delivery servers. The delivery server consumes these budget items by grabbing the budget records out of the cache and decoding the json into proper data structure.
