(ns net.xledger.pedestal-helidon.handler
  (:require [net.xledger.pedestal-helidon.options :as options]
            [net.xledger.pedestal-helidon.request :as request]
            [net.xledger.pedestal-helidon.response :as response])
  (:import (io.helidon.webserver WebServerConfig$Builder)
           (io.helidon.webserver.http Handler
                                      HttpRouting)))

(defn set-ring1-handler! ^WebServerConfig$Builder
  [^WebServerConfig$Builder builder ^Handler handler _options]
  (doto builder
    (.addRouting
     (.build
      (doto (HttpRouting/builder)
        (.any
         (into-array Handler [handler])))))))

(defmethod options/set-server-option! :handler
  [^WebServerConfig$Builder builder _ handler options]
  (set-ring1-handler! builder handler options))
