(ns build
  (:require [clojure.tools.build.api :as b]
            [deps-deploy.deps-deploy]))

(def lib 'net.xledger/pedestal-helidon)
(def version (slurp "VERSION"))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/pedestal-helidon-%s.jar" version))

(defn clean [_]
  (b/delete {:path "target"}))

(defn- jar-opts [opts]
  (assoc opts
    :lib lib
    :version version
    :jar-file jar-file
    :basis (b/create-basis {})
    :class-dir class-dir
    :target "target"
    :src-dirs ["src"]
    :src-pom "template-pom.xml"))

(defn jar [_]
  (let [opts (jar-opts {})]
    (b/write-pom opts)
    (b/copy-dir {:src-dirs   ["src" "resources"] :target-dir class-dir})
    (b/jar {:class-dir class-dir
            :jar-file  jar-file})))

(defn deploy "Deploy the JAR to Clojars." [opts]
  (let [{:keys [jar-file] :as opts} (jar-opts opts)]
    (deps-deploy.deps-deploy/deploy
      {:installer :remote
       :artifact  jar-file
       :pom-file  (b/pom-path (select-keys opts [:lib :class-dir]))}))
  opts)