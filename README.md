# friend-oauth2-examples


[friend-oauth2](https://github.com/ddellacosta/friend-oauth2) is an OAuth2
workflow for [Friend](https://github.com/cemerick/friend). This repo contains
several example athentication service configurations for friend-oauth2:

* [Google OAuth 2.0 Login](https://developers.google.com/accounts/docs/OAuth2Login)
* [Facebook (server-side authentication)](https://developers.facebook.com/docs/authentication/server-side/)
* [App.net](https://github.com/appdotnet/api-spec/blob/master/auth.md)
* [Github](http://developer.github.com/v3/oauth/)


## Running

For the Github example, you don't need to change any configuration. Just set
the `GITHUB_CLIENT_ID` and `GITHUB_CLIENT_SECRET` env vars:

```
$ export GITHUB_CLIENT_ID=xxx
$ export GITHUB_CALLBACK_URL=http://localhost:8999/process-token
```

For the other examples, you will need to update the code with your client
id/secret and callback url in the handler code.

```clojure
(def client-config
  {:client-id "<HERE>"
   :client-secret "<HERE>"
   :callback {:domain "http://<HERE>" :path "/<AND HERE>"}})
```

At that point, you should be able to start it up using one of the `lein`
aliases, which will load ring server in headless mode:

```
$ GITHUB_CLIENT_SECRET=yyy lein github
```

or

```
$ lein appdotnet
$ lein facebook
$ lein google
```


## License

Distributed under the MIT License (http://dd.mit-license.org/)


## Authors

* Facebook, App.net, Google API examples by [ddellacosta](https://github.com/ddellacosta)
* Github example by [kanej](https://github.com/kanej)
