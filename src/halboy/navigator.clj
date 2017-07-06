(ns halboy.navigator
  (:refer-clojure :exclude [get])
  (:require [clojure.walk :refer [keywordize-keys]]
            [cheshire.core :as json]
            [halboy.resource :as resource]
            [halboy.data :refer [transform-values]]
            [halboy.json :refer [json->resource resource->json]]
            [halboy.http :refer [GET POST]])
  (:import (java.net URL)))

(defrecord Navigator [href response resource])

(defn- resolve-url [url endpoint]
  (-> (URL. url)
      (URL. endpoint)
      (.toString)))

(defn- extract-redirect-location [navigator]
  (let [base-url (:href navigator)
        endpoint (get-in navigator [:response :headers :location])]
    (resolve-url base-url endpoint)))

(defn- response->Navigator [response]
  (let [current-url (get-in response [:opts :url])
        resource (-> (:body response)
                     json->resource)]
    (->Navigator
      current-url
      response
      resource)))

(defn- fetch-url [url]
  (-> (GET url)
      response->Navigator))

(defn- post-url [url body]
  (-> (POST url {:body (json/generate-string body)})
      (response->Navigator)
      (extract-redirect-location)
      (fetch-url)))

(defn- resolve-link [navigator link]
  (let [base (-> navigator
                 :href)
        relative-url (-> navigator
                         :resource
                         (resource/get-link link)
                         :href)]
    (resolve-url base relative-url)))

(defn location
  "Gets the current location of the navigator"
  [navigator]
  (:href navigator))

(defn resource
  "Gets the resource from the navigator"
  [navigator]
  (:resource navigator))

(defn response
  "Gets the last response from the navigator"
  [navigator]
  (:response navigator))

(defn status
  "Gets the status code from the last response from the navigator"
  [navigator]
  (-> (response navigator)
      :status))

(defn discover
  "Starts a conversation with an API. Use this on the discovery endpoint."
  [href]
  (fetch-url href))

(defn get
  "Fetches the contents of a link in an API."
  [navigator link]
  (-> navigator
      (resolve-link link)
      (fetch-url)))

(defn post
  "Posts content to a link in an API."
  [navigator link body]
  (-> navigator
      (resolve-link link)
      (post-url body)))
