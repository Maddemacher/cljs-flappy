(ns cljsflappy.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [reagent.core :as r]
            [cljs.core.async :as async :refer [put! chan <! >! timeout close!]]))

(enable-console-print!)

(def pillar-start-pos 450)
(def pillar-death-mark -50)
(def pillar-gap 200)
(def pillar-width 80)
(def new-pillar-mark 150)
(def new-pillar-range {:min 100 :max 400})

(def flappy-width 50)
(def flappy-height 41)

(def ceiling-height 640)
(def floor-height 85)

(def game-speed 30)

(def flappy-start-state {
  :first-game true
  :game-running false
  :score 0
  :flappy-x 200
  :flappy-y 200
  :vel-y -1
  :acc-y -1
  :vel-x -1
  :acc-x 0
  :flap-vel 10
  :pillars [{:bottom {:pos-x pillar-start-pos :pos-y 300}
             :top {:pos-x pillar-start-pos :pos-y 500}
             :id "pillar-0"}]})

(def flappy-state (r/atom flappy-start-state))

(defn check-top-collition [top-pillar]
  (let [botleft {:x (:pos-x top-pillar) :y (:pos-y top-pillar)}
        botright {:x (+ (:x botleft) pillar-width) :y (:y botleft)}
        flappy-pos {:x (:flappy-x @flappy-state) :y (+ (:flappy-y @flappy-state) flappy-height)}]
        (and (< (:x botleft) (:x flappy-pos) (:x botright))
             (> ceiling-height (:y flappy-pos) (:y botleft)))))

(defn check-bottom-collision [bottom-pillar]
  (let [topleft {:x (:pos-x bottom-pillar) :y (:pos-y bottom-pillar)}
        topright {:x (+ (:x topleft) pillar-width) :y (:y topleft)}
        flappy-pos {:x (:flappy-x @flappy-state) :y (- (:flappy-y @flappy-state) flappy-height)}]
  (and (< (:x topleft) (:x flappy-pos) (:x topright))
       (< floor-height (:y flappy-pos) (:y topleft)))))

(defn check-pillar-collision[pillar]
  (or (check-bottom-collision (:bottom pillar))
      (check-top-collition (:top pillar))))

(defn pillar-hit []
  (some check-pillar-collision (:pillars @flappy-state)))

(defn check-death []
  (or (>= floor-height (:flappy-y @flappy-state))
      (<= ceiling-height (:flappy-y @flappy-state))
      (pillar-hit)))

(defn update-flappy []
  (swap! flappy-state update-in [:vel-y] + (:acc-y @flappy-state))
  (swap! flappy-state update-in [:flappy-y] + (:vel-y @flappy-state)))

(defn update-pillar [pillar]
  (if (= (:pos-x (:bottom pillar)) pillar-death-mark)
    nil
    (-> (update-in pillar [:bottom :pos-x] + (:vel-x @flappy-state))
        (update-in [:top :pos-x] + (:vel-x @flappy-state)))))


(defn generate-pillar []
  (swap! flappy-state update-in [:score] inc)
  (let [ypos  (+ (:min new-pillar-range)
                 (rand-int (- (:max new-pillar-range) (:min new-pillar-range))))]
                    {:bottom {:pos-x pillar-start-pos :pos-y ypos}
                     :top {:pos-x pillar-start-pos :pos-y (+ ypos pillar-gap)}
                     :id (str "pillar-" (:score @flappy-state))}))

(defn should-create-new-pillar[]
  (let [last-pillar-pos (:pos-x (:bottom (first (:pillars @flappy-state))))]
    (< last-pillar-pos new-pillar-mark)))

(defn update-pillars []
  (if (should-create-new-pillar)
    (swap! flappy-state update-in [:pillars] conj (generate-pillar)))
  (let [updated-pillars (filter some? (mapv #(update-pillar %) (:pillars @flappy-state)))]
    (swap! flappy-state assoc :pillars updated-pillars)))

(defn update-state []
  (update-flappy)
  (update-pillars))

(defn reset-game []
  (reset! flappy-state flappy-start-state))

(defn flap []
  (swap! flappy-state update-in [:vel-y] + (:flap-vel @flappy-state)))

(defn game-loop []
  (when (:game-running @flappy-state)
    (go-loop []
      (async/<! (async/timeout game-speed))
      (update-state)
      (if (check-death)
        (do (swap! flappy-state update-in [:game-running] false?)
            (swap! flappy-state update-in [:first-game] #(identity false)))
        (recur)))))

(defn start-game []
  (do (reset-game)
      (swap! flappy-state update-in [:game-running] false?)
      (game-loop)))

(defn start-frame []
  [:input {
           :type "button"
           :style {:position "absolute"
                    :left "200px"
                    :bottom "150px"}
            :value (if (:first-game @flappy-state)
                     "Start"
                     "Restart")
            :on-click start-game}])

(defn flappy-frame []
  [:img {:src "imgs/flappy-base.png"
         :style {:position "absolute"
                 :left (- (:flappy-x @flappy-state) flappy-width)
                 :bottom (:flappy-y @flappy-state)}}])

(defn pillar-base [pillar id min max]
  [:div {:key (str id "base-div")}
        (for [segment (range min max 8)]
          [:img {:key (str (:id pillar) "-bot-base-segment-" segment)
                 :src "imgs/pillar-bkg.png"
                 :style {:position "absolute"
                         :left (+ 4 (:pos-x pillar))
                         :bottom segment}}])])

(defn pillar-head [pillar id]
  [:img {:key id
         :src "imgs/lower-pillar-head.png"
         :style {:position "absolute"
                 :left (:pos-x pillar)
                 :bottom (:pos-y pillar)}}])

(defn pillar-frame []
  [:div
    (for [pillar (:pillars @flappy-state)]
      [:div
          {:key (str (:id pillar) "-div") }
          [pillar-base (:bottom pillar) (:id pillar) 80 (:pos-y (:bottom pillar))]
          [pillar-head (:bottom pillar) (:id pillar)]
          [pillar-base (:top pillar) (:id pillar) (:pos-y (:top pillar)) ceiling-height]
          [pillar-head (:top pillar) (:id pillar)]
       ])])

(defn score-frame []
  [:h1 {:style {:position "absolute"
                :left "200px"
                :top "100px"}}
   (:score @flappy-state)])

(defn get-frame []
  [:div
    {:style {:height "100%" :width "100%"}
     :on-mouse-down flap}
   [flappy-frame]
   [pillar-frame]
   [score-frame]
   (if (false? (:game-running @flappy-state))
      [start-frame])])

(r/render [get-frame]
  (js/document.getElementById "board-area"))
