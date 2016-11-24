(ns friend-oauth2-examples.github-handler
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [cemerick.friend :as friend]
            [cemerick.url :as url]
            [clj-http.client :as client]
            [clojure.tools.logging :as log]
            [friend-oauth2.workflow :as oauth2]
            [friend-oauth2.util :as util]
            [cheshire.core :as json]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Supporting functions ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; This record is used to reduce the boilerplate needed to configure
;; friend-oauth2, and to do so in a backwards-compatible manner.
(defrecord OAuth2Config
  [client-id
   client-secret
   callback-url
   auth-url
   access-url
   response-type
   grant-type
   scope])

(defn make-client-config
  [rec]
  (let [parsed-url (url/url (:callback-url rec))]
    {:client-id (:client-id rec)
     :client-secret (:client-secret rec)
     :callback
       {:domain (format "%s://%s:%s"
                        (:protocol parsed-url)
                        (:host parsed-url)
                        (:port parsed-url))
        :path (:path parsed-url)}}))

(defn make-uri-config
  [rec]
  {:authentication-uri {
     :url (:auth-url rec)
     :query {
       :client_id (:client-id rec)
       :response_type (:response-type rec)
       :redirect_uri (:callback-url rec)
       :scope (:scope rec)}}
    :access-token-uri {
     :url (:access-url rec)
     :query {
       :client_id (:client-id rec)
       :client_secret (:client-secret rec)
       :grant_type (:grant-type rec)
       :redirect_uri (:callback-url rec)}}})

(defn get-authentications
  [request]
  (log/debug "session:" (:session request))
  (get-in request [:session :cemerick.friend/identity :authentications]))

(defn get-token
  ([request]
    (get-token request 0))
  ([request index]
    (let [authentications (get-authentications request)]
      (log/debug "authentications:" authentications)
      (log/debug "first keys authentications:" (first (keys authentications)))
      (:access-token (nth (keys authentications) index)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Mini Webapp ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn render-status-page [request]
  (let [count (:count (:session request) 0)
        session (assoc (:session request) :count (inc count))]
    (-> (str "<p>We've hit the session page "
             (:count session)
             " times.</p><p>The current session: "
             session
             "</p>")
        (ring.util.response/response)
        (assoc :session session))))

(defn get-github-repos
  "Github API call for the current authenticated users repository list."
  [access-token]
  (log/debug "access-token:" access-token)
  (let [url (str "https://api.github.com/user/repos?access_token=" access-token)
        response (client/get url {:accept :json})
        repos (json/parse-string (:body response) true)]
    repos))

(defn render-repos-page
  "Shows a list of the current users github repositories by calling the github api
   with the OAuth2 access token that the friend authentication has retrieved."
  [request]
  (let [access-token (get-token request)
        repos-response (get-github-repos access-token)]
    (log/debug "access-token:" access-token)
    (log/trace "repos-response:" repos-response)
    (->> repos-response
         (map :name)
         (vec)
         (str))))

(defroutes app-routes
  (GET "/" request
       (str "<a href=\"/repos\">"
            "My Github Repositories</a><br>"
            "<a href=\"/status\">Status</a>"))
  (GET "/status" request
       (render-status-page request))
  (GET "/repos" request
       (friend/authorize #{::user} (render-repos-page request)))
  (friend/logout
    (ANY "/logout" request (ring.util.response/redirect "/"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; OAuth2 Configuration and Integration ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def conf (map->OAuth2Config {
  :client-id (System/getenv "GITHUB_CLIENT_ID")
  :client-secret (System/getenv "GITHUB_CLIENT_SECRET")
  :callback-url (System/getenv "GITHUB_CALLBACK_URL")
  :auth-url "https://github.com/login/oauth/authorize"
  :access-url "https://github.com/login/oauth/access_token"
  :response-type "code"
  :grant-type "authorization_code"
  :scope "user"}))

(defn credential-fn
  [token]
  ;;lookup token in DB or whatever to fetch appropriate :roles
  {:identity token :roles #{::user}})

(def workflow
  (oauth2/workflow
    {:client-config (make-client-config conf)
     :uri-config (make-uri-config conf)
     :access-token-parsefn util/get-access-token-from-params
     :credential-fn credential-fn}))

(def auth-opts
  {:allow-anon? true
   :workflows [workflow]})

(def app
  (->> auth-opts
       (friend/authenticate app-routes)
       (handler/site)))
