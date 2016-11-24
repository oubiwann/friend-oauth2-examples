(defproject friend-oauth2-examples "0.0.5"
  :description "Friend Oauth2 Workflow examples"
  :url "https://github.com/oubiwann/friend-oauth2-examples"
  :license {
    :name "MIT License"
    :url "http://dd.mit-license.org"}
  :dependencies [
    [org.clojure/clojure "1.8.0"]
    [org.clojure/tools.logging "0.3.1"]
    [compojure "1.5.1"
     :exclusions [ring/ring-core
                  org.clojure/core.incubator
                  org.apache.httpcomponents/httpclient]]
    [com.cemerick/friend "0.2.3" :exclusions [ring/ring-core]]
    [friend-oauth2 "0.1.3"
     :exclusions [org.apache.httpcomponents/httpcore
                  clj-http]]
    [org.apache.httpcomponents/httpclient "4.5.2"]
    [clj-http "3.4.1"]
    [cheshire "5.6.3"]
    [ring-server "0.4.0"
     :exclusions [ring]]]
  :profiles {
    :dev {:plugins [[lein-ring "0.10.0" :exclusions [org.clojure/clojure]]]}
    :appdotnet {:ring {:handler friend-oauth2-examples.appdotnet-handler/app}}
    :facebook {:ring {:handler friend-oauth2-examples.facebook-handler/app}}
    :github {:ring {:handler friend-oauth2-examples.github-handler/app :port 8999}}
    :google {:ring {:handler friend-oauth2-examples.google-handler/app}}}
  :aliases {
    "facebook" ["with-profile" "dev,facebook" "do" "ring" "server-headless"]
    "appdotnet" ["with-profile" "dev,appdotnet" "do" "ring" "server-headless"]
    "github" ["with-profile" "dev,github" "do" "ring" "server-headless"]
    "google" ["with-profile" "dev,google" "do" "ring" "server-headless"]})
