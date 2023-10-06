(ns net.xledger.pedestal-helidon.request
  (:require [s-exp.mina.request]
            [strojure.zmap.core :as zmap])
  (:import (clojure.lang PersistentHashMap)
           (io.helidon.webserver.http ServerRequest ServerResponse)))

;; Pasted and adapted from mina's request.clj:
;; https://github.com/mpenet/mina/blob/57981ef805b921567373812cb7cd7549c57dfef4/src/s_exp/mina/request.clj

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
                       (.assoc :path-info (.rawPath (.path server-request)))
                       (.assoc :scheme (if (.isSecure server-request) "https" "http"))
                       (.assoc :protocol (s-exp.mina.request/ring-protocol server-request))
                       (.assoc :request-method (s-exp.mina.request/ring-method server-request))
                       (.assoc :headers (s-exp.mina.request/->HeaderMapProxy (.headers server-request) nil))

                       (.assoc ::server-request server-request)
                       (.assoc ::server-response server-response))
        ;; optional
        request (cond-> request
                  qs (.assoc :query-string qs)
                  body (.assoc :body body))]
    {:request (zmap/wrap (.persistent request))
     :response {}}))
