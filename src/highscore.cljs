(ns cljsflappy.highscore
    (:require [reagent.core :as r]
              [cljsflappy.firebase :as firebase]))

(enable-console-print!)

(defonce newhighscore (r/atom {:name "" :position 0}))

(defonce highscore (r/atom [{:name "Emil" :score 5}
                        {:name "Emil" :score 4}
                        {:name "Emil" :score 3}
                        {:name "Emil" :score 2}
                        {:name "Emil" :score 1}]))

(defn high-score-frame []
  [:div
    {:key "high-score-frame"
     :style {:position "absolute"
                :left "200px"
                :top "150px"}}
    (for [score @highscore]
      [:h3 {:key (str (:name score) "-" (:score score))} (str (:name score) " - " (:score score))])])

(defn sort-score [unsorted]
   (reverse (sort-by :score unsorted)))

(defn save-high-score [score]
  (let [updatedhighscore (sort-score (conj @highscore {:name (:name @newhighscore) :score score}))]
    (println "updated " updatedhighscore)
    (reset! highscore (take 5 updatedhighscore))
    (firebase/post-high-score @highscore)))

(defn set-high-score-frame [score callback]
  [:div
    {:key "set-high-score-frame"}
    [:input {:key "high-score-name-input"
             :type "text"
             :value (:name @newhighscore)
             :style {:position "absolute"
                      :left "200px"
                      :bottom "180px"}
             :on-change #(reset! newhighscore (update-in @newhighscore [:name] (fn [] (.-value (.-target %)))))}]
    [:input {:key "save-high-score-button"
             :type "button"
             :style {:position "absolute"
                      :left "200px"
                      :bottom "150px"}
              :value "save"
              :on-click #(do (save-high-score score)
                             (callback))}]])

(defn is-high-score [score]
  (let [pos (first (keep-indexed (fn [idx value] (if (< (:score value) score) idx))  @highscore))]
    (swap! newhighscore update-in [:position] #(identity pos))
    (and (some? pos)
         (> 5 pos))))

(defonce get-high-score-from-server
  (let [high-score-resetter  #(if (some? %)
                                      (reset! highscore (take 5 (sort-score %))))]
    (firebase/setup-high-score-listener #(high-score-resetter (conj @highscore (last %))))
    (firebase/get-high-score  high-score-resetter)))
