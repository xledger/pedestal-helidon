# pedestal-helidon

NOTE: This builds on [Mina](https://github.com/mpenet/mina) to add adapters for pedestal. Originally, it was a complete fork.

[Helidon/Nima](https://helidon.io/nima) [Pedestal](https://github.com/pedestal/pedestal) compliant adapter for clojure, loom based 

**Warning** It's early days so expect breakage.

[Helidon/Nima](https://helidon.io/nima) is alpha status right now, so do not use this in prod please. 

## Installation

Note: You need to use java **21**

## Usage:

In your pedestal service map:

```clojure
{ ...
  ::http/type                  pedestal-helidon/helidon-server-fn
  ::http/chain-provider        pedestal-helidon/direct-helidon-provider
...}
```

The rest should be the same as before.

## Running the tests

`clj -X:test net.xledger.pedestal-helidon-test-runner/run`

## Implemented

- [x] HTTP (1.1 & 2) server/handlers
- [ ] Grpc handlers

## License

Copyright © 2023 Max Penet 
Copyright © 2023 Xledger

Distributed under the Eclipse Public License version 1.
