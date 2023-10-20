(ns net.xledger.pedestal-helidon.request
  (:require [s-exp.hirundo.http.request :as h-request]
            [s-exp.hirundo.http.header :as h-header]
            [strojure.zmap.core :as zmap])
  (:import (clojure.lang PersistentHashMap)
           (io.helidon.common.uri UriQuery UriPath)
           (io.helidon.http HttpPrologue Headers)
           (io.helidon.webserver.http ServerRequest ServerResponse)))

;; Pasted and adapted from hirundo's request.clj:
;; https://github.com/mpenet/hirundo/blob/5e00a749c41da574878704fba96566ff1867d849/src/s_exp/hirundo/http/request.clj

(defn pedestal-context
  [^ServerRequest server-request
   ^ServerResponse server-response]
  (let [qs (let [query (.rawValue (.query server-request))]
             (when (not= "" query) query))
        body (let [content (.content server-request)]
               (when-not (.consumed content) (.inputStream content)))
        request (-> (.asTransient PersistentHashMap/EMPTY)
                  ;; delayed
                  (.assoc :server-port (zmap/delay (.port (.localPeer server-request))))
                  (.assoc :server-name (zmap/delay (.host (.localPeer server-request))))
                  (.assoc :remote-addr (zmap/delay
                                         (let [address ^java.net.InetSocketAddress (.address (.remotePeer server-request))]
                                           (-> address .getAddress .getHostAddress))))
                  (.assoc :ssl-client-cert (zmap/delay (some-> server-request .remotePeer .tlsCertificates (.orElse nil) first)))

                  ;; realized
                  (.assoc :uri (.rawPath (.path server-request)))
                  (.assoc :path-info (.rawPath (.path server-request)))
                  (.assoc :scheme (if (.isSecure server-request) "https" "http"))
                  (.assoc :protocol (h-request/ring-protocol (.prologue server-request)))
                  (.assoc :request-method (h-request/ring-method (.prologue server-request)))
                  (.assoc :headers (h-header/->HeaderMapProxy (.headers server-request) nil))
                  ;; Required by interceptors like :body-params in pedestal
                  (.assoc :content-type (h-header/header->value
                                          (.headers server-request)
                                          "content-type" nil))

                  (.assoc ::server-request server-request)
                  (.assoc ::server-response server-response))
        ;; optional
        request (cond-> request
                  qs (.assoc :query-string qs)
                  body (.assoc :body body))]
    {:request  (zmap/wrap (.persistent request))
     :response {}}))
