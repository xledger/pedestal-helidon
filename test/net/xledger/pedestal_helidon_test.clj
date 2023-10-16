(ns net.xledger.pedestal-helidon-test
  (:require [clj-http.client :as client]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [less.awful.ssl :as ls]
            [net.xledger.pedestal-helidon :as ph]
            [io.pedestal.http :as http]
            [io.pedestal.http.ring-middlewares :as rm]
            [io.pedestal.interceptor :as interceptor])
  (:import (io.helidon.common.tls Tls TlsClientAuth)
           (io.helidon.common.tls TlsConfig)))

(def ^:dynamic *endpoint*)

(defn status-ok? [response]
  (some-> response :status (= 200)))


(def !port (atom 8080))

(defn service-map []
  (-> {::http/port (swap! !port inc) ;; Work around server close timing issue on windows
       ::http/type ph/helidon-server-fn
       ::http/chain-provider ph/direct-helidon-provider
       ::http/interceptors []}))

(defn start-server [options]
  (let [options (-> (service-map)
                  (merge options)
                  (update ::http/interceptors conj
                    (interceptor/map->Interceptor
                      {:name ::handler
                       :leave (fn [ctx]
                                (assoc ctx :response
                                  ((:handler options) (:request ctx))))})))]
    (-> options http/create-server http/start
      (assoc :port (::http/port options)))))

(defmacro with-server [options & body]
  `(let [options# ~options
         server# (start-server options#)]
     (binding [*endpoint* (format "http://localhost:%s/" (:port server#))]
       (println "ENDPOINT: " *endpoint*)
       (try
         ~@body
         (finally
           (http/stop server#))))))

(deftest test-send-headers
  (with-server {:handler (fn [req] {:headers {:foo "bar"}})}
    (is (-> (client/get *endpoint*) :headers :foo (= "bar"))))
  (with-server {:handler (fn [req] {:headers {:foo ["bar" "baz"]}})}
    (is (-> (client/get *endpoint*) :headers :foo (= ["bar" "baz"])))))

(deftest test-status
  (with-server {:handler (fn [req] {:status 201})}
    (is (-> (client/get *endpoint*) :status (= 201)))))

(deftest test-query-string
  (with-server {:handler (fn [req] {:body (:query-string req)})}
    (is (-> (client/get (str *endpoint* "?foo=bar")) :body (= "foo=bar"))))

  (with-server {:handler (fn [req] {:body (:query-string req)})}
    (is (-> (client/get (str *endpoint* "?")) :body (= ""))))

  (with-server {:handler (fn [req] {:body (:query-string req)})}
    (is (-> (client/get (str *endpoint* "")) :body (= "")))))

(deftest test-method
  (with-server {:handler (fn [req] {:body (str (:request-method req))})}
    (is (-> (client/post *endpoint*) :body (= ":post"))))

  (with-server {:handler (fn [req] {:body (str (:request-method req))})}
    (is (-> (client/put *endpoint*) :body (= ":put"))))

  (with-server {:handler (fn [req] {:body (str (:request-method req))})}
    (is (-> (client/delete *endpoint*) :body (= ":delete")))))

(deftest test-uri
  (with-server {:handler (fn [req] {:body (:path-info req)})}
    (is (-> (client/delete (str *endpoint* "foo/bar")) :body (= "/foo/bar")))))

(deftest test-scheme
  (with-server {:handler (fn [req] {:body (str (:scheme req))})}
    (is (-> (client/get *endpoint*) :body (= "http")))))

(deftest test-body
  (with-server {:handler (fn [req] {})}
    (is (-> (client/get *endpoint*) :body (= ""))))

  (with-server {:handler (fn [req] {:body "yes"})}
    (is (-> (client/get *endpoint*) :body (= "yes"))))

  (with-server {:handler (fn [req] {:body ["yes" "no"]})}
    (is (-> (client/get *endpoint*) :body (= "yesno"))))

  (with-server {:handler (fn [req] {:body (.getBytes "yes")})}
    (is (-> (client/get *endpoint*) :body (= "yes"))))

  (with-server {:handler (fn [req] {:body (java.io.ByteArrayInputStream. (.getBytes "yes"))})}
    (is (-> (client/get *endpoint*) :body (= "yes")))))

(deftest content-type
  (with-server {:handler (fn [req]
                           {:body (:content-type req)})}
    (is (-> (client/post *endpoint* {:content-type "application/foo" :body "hm"})
          :body
          (= "application/foo")))))

(defn tls []
  (let [b (doto (TlsConfig/builder)
            (.sslContext (ls/ssl-context "test/server.key"
                           "test/server.crt"
                           "test/server.crt"))
            (.clientAuth TlsClientAuth/REQUIRED)
            (.trustAll true)
            (.endpointIdentificationAlgorithm (Tls/ENDPOINT_IDENTIFICATION_NONE)))]
    (.build b)))

(comment

  ;; Test from mina that does not work yet
  (deftest test-ssl-context
    (with-server {:handler (fn [req] {}) :tls (tls)}
      (let [endpoint (str/replace *endpoint* "http://" "https://")]
        (is (thrown? Exception (client/get endpoint)))
        (is (status-ok? (client/get endpoint
                          {:insecure?        true
                           :keystore         "test/keystore.jks"
                           :keystore-pass    "password"
                           :trust-store      "test/keystore.jks"
                           :trust-store-pass "password"})))))))



