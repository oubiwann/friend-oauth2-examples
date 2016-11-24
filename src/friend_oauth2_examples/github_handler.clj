(ns friend-oauth2-examples.github-handler
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [cemerick.friend :as friend]
            [clj-http.client :as client]
            [clojure.tools.logging :as log]
            [friend-oauth2.workflow :as oauth2]
            [friend-oauth2.util :as util]
            [cheshire.core :as j]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])))

(def client-config {
  :client-id (System/getenv "GITHUB_CLIENT_ID")
  :client-secret (System/getenv "GITHUB_CLIENT_SECRET")
  :callback {
    :domain (System/getenv "GITHUB_CALLBACK_DOMAIN")
    :path (System/getenv "GITHUB_CALLBACK_PATH")}})

(def uri-config {
  :authentication-uri {
    :url "https://github.com/login/oauth/authorize"
    :query {
      :client_id (:client-id client-config)
      :response_type "code"
      :redirect_uri (util/format-config-uri client-config)
      :scope "user"}}
   :access-token-uri {
    :url "https://github.com/login/oauth/access_token"
    :query {
      :client_id (:client-id client-config)
      :client_secret (:client-secret client-config)
      :grant_type "authorization_code"
      :redirect_uri (util/format-config-uri client-config)}}})

(defn credential-fn
  [token]
  ;;lookup token in DB or whatever to fetch appropriate :roles
  {:identity token :roles #{::user}})

(def workflow
  (oauth2/workflow
    {:client-config client-config
     :uri-config uri-config
     :access-token-parsefn util/get-access-token-from-params
     :credential-fn credential-fn}))

(defn get-authentications
  [request]
  (log/info "session:" (:session request))
  (get-in request [:session :cemerick.friend/identity :authentications]))

(defn get-token
  [request]
  (let [authentications (get-authentications request)]
    (log/info "authentications:" authentications)
    (log/info "first keys authentications:" (first (keys authentications)))
    (:access-token (first (keys authentications)))))

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
  (log/info "access-token:" access-token)
  (let [url (str "https://api.github.com/user/repos?access_token=" access-token)
        response (client/get url {:accept :json})
        repos (j/parse-string (:body response) true)]
    repos))

(defn render-repos-page
  "Shows a list of the current users github repositories by calling the github api
   with the OAuth2 access token that the friend authentication has retrieved."
  [request]
  (let [access-token (get-token request)
        repos-response (get-github-repos access-token)]
    (log/info "access-token:" access-token)
    (log/trace "repos-response:" repos-response)
    (->> repos-response
         (map :name)
         (vec)
         (str))))

(defroutes app-routes
  (GET "/" request "<a href=\"/repos\">My Github Repositories</a><br><a href=\"/status\">Status</a>")
  (GET "/status" request
       (render-status-page request))
  (GET "/repos" request
       (friend/authorize #{::user} (render-repos-page request)))
  (friend/logout (ANY "/logout" request (ring.util.response/redirect "/"))))

(def auth-opts
  {:allow-anon? true
   :workflows [workflow]})

(def app
  (->> auth-opts
       (friend/authenticate app-routes)
       (handler/site)))
