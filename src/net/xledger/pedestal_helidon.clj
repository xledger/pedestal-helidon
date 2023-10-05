(ns net.xledger.pedestal-helidon
  (:require [io.pedestal.interceptor.chain :as chain]
            [net.xledger.pedestal-helidon.handler]
            [net.xledger.pedestal-helidon.options :as options]
            [io.pedestal.http :as-alias http]
            [io.pedestal.http.route]
            [net.xledger.pedestal-helidon.request :as request]
            [net.xledger.pedestal-helidon.response :as response])
  (:import (io.helidon.webserver WebServer WebServerConfig WebServerConfig$Builder)
           (io.helidon.webserver.http Handler HttpRouting)))

(set! *warn-on-reflection* true)

(def default-options {:connection-provider false})

(defn- server-builder
  ^WebServerConfig$Builder
  [options]
  (reduce (fn [builder [k v]]
            (options/set-server-option! builder k v options))
    (WebServerConfig/builder)
    options))

(defn direct-helidon-provider
  "Given a service-map,
  provide all necessary functionality to execute the interceptor chain,
  including extracting and packaging the base :request into context.
  These functions/objects are added back into the service map for use within
  the server-fn.
  See io.pedestal.http.impl.servlet-interceptor.clj as an example.

  Interceptor chains:
   * Terminate based on the list of :terminators in the context.
   * Call the list of functions in :enter-async when going async.  Use these to toggle async mode on the container
   * Will use the fn at :async? to determine if the chain has been operating in async mode (so the container can handle on the outbound)"
  [service-map]
  (let [interceptors (::http/interceptors service-map)
        routes (::http/routes service-map)
        routes (if (fn? routes) (routes) routes)
        router (some-> routes io.pedestal.http.route/router)
        interceptors (if router (conj interceptors router)
                       interceptors)]
    (assoc service-map ::handler
      (reify Handler
        (handle [_ server-request server-response]
          (let [initial-context (request/pedestal-context server-request server-response)
                resp-ctx (chain/execute initial-context interceptors)]
            (response/set-response! server-response (:response resp-ctx))))))))

(defn helidon-server-fn
  "Given a service map (with interceptor provider established) and a server-opts map,
  Return a map of :server, :start-fn, and :stop-fn.
  Both functions are 0-arity"
  [service-map server-opts]
  (let [handler (::handler service-map)
        {:keys [host port join?]
         :or   {host  "127.0.0.1"
                port  8080
                join? false}} server-opts
        server (-> (server-builder
                     (-> default-options
                       (merge server-opts)
                       (assoc :handler handler)))
                 .build)]
    {:server   server
     :start-fn (fn [] (.start server))
     :stop-fn  (fn [] (.stop server) server)}))

;; (def r {:status 200})
;; (def h (fn [req]
;;          {:body (str (counted? (:headers req)))}))
;; (def h (fn [_]
;;          ;; (prn :aasdf ((:headers _) "accept"))
;;          ;; (prn (:headers _))
;;          r))
;; (def s (start!
;;         #'h
;;         {:host "0.0.0.0" :port 8080
;;          :write-queue-length 10240
;;          :connection-options {:socket-send-buffer-size 1024}}))

;; (stop! s)

;; https://api.github.com/repos/mpenet/mina/commits/main?per_page=1

