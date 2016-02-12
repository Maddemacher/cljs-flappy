(ns cljsflappy.firebase
  (:require [matchbox.core :as m :refer [connect auth-anon get-in deref reset! listen-children]]))

(enable-console-print!)

(def firebase-root (m/connect "https://scorching-heat-8182.firebaseio.com/"))

(m/auth-anon firebase-root)

(def high-score (m/get-in firebase-root [:high-score]))

(defn get-high-score [callback]
  (let [data (m/get-in firebase-root [:high-score])]
    (m/deref high-score callback)))

(defn setup-high-score-listener [callback]
  (m/listen-children
    firebase-root [:high-score]
    (fn [[event-type data]] (callback data))))

(defn post-high-score [new-high-score]
  (m/reset! high-score new-high-score))
