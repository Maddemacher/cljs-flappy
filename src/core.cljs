(ns cljsflappy.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [reagent.core :as r]
            [cljs.core.async :as async :refer [<! timeout]]
            [cljsflappy.highscore :as hs]))

(enable-console-print!)

(def pillar-start-pos 450)
(def pillar-death-mark -60)
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
  :acc-x 0
  :flap-vel 10
  :pillars [{:bottom {:pos-x pillar-start-pos :pos-y 300}
             :top {:pos-x pillar-start-pos :pos-y 500}
             :id "pillar-0"}]})

(defonce flappy-state (r/atom flappy-start-state))

(defn get-game-velocity[]
  (js/Math.floor (- (- 5) (/ (:score @flappy-state) 4))))

(defn check-top-collition [top-pillar flappy-pos]
  (let [botleft {:x (:pos-x top-pillar) :y (:pos-y top-pillar)}
        botright {:x (+ (:x botleft) pillar-width) :y (:y botleft)}]
        (and (< (:x botleft) (:x flappy-pos) (:x botright))
             (> ceiling-height (:y flappy-pos) (:y botleft)))))

(defn check-bottom-collision [bottom-pillar flappy-pos]
  (let [topleft {:x (:pos-x bottom-pillar) :y (:pos-y bottom-pillar)}
        topright {:x (+ (:x topleft) pillar-width) :y (:y topleft)}]
  (and (< (:x topleft) (:x flappy-pos) (:x topright))
       (< floor-height (:y flappy-pos) (:y topleft)))))

(defn check-pillar-collision[pillar flappy-pos]
  (or (check-bottom-collision (:bottom pillar) flappy-pos)
      (check-top-collition (:top pillar) flappy-pos)))

(defn pillar-hit [flappy-pos pillars]
  (some #(check-pillar-collision % flappy-pos) pillars))

(defn check-death [flappy-pos pillars]
  (or (>= floor-height (:y flappy-pos))
      (<= ceiling-height (:y flappy-pos))
      (pillar-hit flappy-pos pillars)))

(defn update-flappy [state]
  (-> (update-in state [:vel-y] + (:acc-y state))
      (update-in [:flappy-y] + (:vel-y state))))


(defn update-pillar [pillar x-velocity]
  (if (< (:pos-x (:bottom pillar)) pillar-death-mark)
    nil
    (-> (update-in pillar [:bottom :pos-x] + x-velocity)
        (update-in [:top :pos-x] + x-velocity))))


(defn generate-pillar [state]
  (let [ypos  (+ (:min new-pillar-range)
                 (rand-int (- (:max new-pillar-range) (:min new-pillar-range))))
        new-pillar {:bottom {:pos-x pillar-start-pos :pos-y ypos}
                    :top {:pos-x pillar-start-pos :pos-y (+ ypos pillar-gap)}
                    :id (str "pillar-" (:score state))}]
        (update-in state [:pillars] conj new-pillar)))

(defn should-create-new-pillar[state]
  (let [last-pillar-pos (:pos-x (:bottom (first (:pillars state))))]
    (< last-pillar-pos new-pillar-mark)))

(defn update-pillars [state x-velocity]
  (assoc state :pillars (filter some? (mapv #(update-pillar % x-velocity) (:pillars state)))))

(defn update-score [state]
  (update-in state [:score] inc))

(defn update-state [state]
  (let [updated-flappy (update-flappy state)]
      (if (should-create-new-pillar updated-flappy)
          (-> (update-score updated-flappy)
              (generate-pillar)
              (update-pillars (get-game-velocity)))
          (update-pillars updated-flappy (get-game-velocity)))))

(defn reset-game []
  (println "resetting game")
  (reset! flappy-state flappy-start-state))

(defn flap []
  (swap! flappy-state update-in [:vel-y] + (:flap-vel @flappy-state)))

(defn get-new-game-state [current-state]
  (let [updated-state-stage-one (update-state current-state)
        updated-state-stage-two (if (check-death {:x (:flappy-x updated-state-stage-one) :y (:flappy-y updated-state-stage-one)} (:pillars updated-state-stage-one))
                                    (-> (update-in updated-state-stage-one [:game-running] false?)
                                        (update-in [:first-game] #(identity false)))
                                    updated-state-stage-one)]
    updated-state-stage-two))

(defn game-loop []
  (when (:game-running @flappy-state)
    (go-loop []
      (async/<! (async/timeout game-speed))
        (let [new-game-state (get-new-game-state @flappy-state)]
          (reset! flappy-state new-game-state)
          (if (:game-running new-game-state)
              (recur))))))

(defn start-game []
  (do (reset-game)
      (swap! flappy-state update-in [:game-running] false?)
      (game-loop)))

(defn handle-keydown [e]
  (when (:game-running @flappy-state)
    (do
      (flap)
      (.preventDefault e))))

(defn start-frame []
  [:div
  [hs/high-score-frame]
  [:input {:key "start-button"
           :type "button"
           :style {:position "absolute"
                    :left "200px"
                    :bottom "150px"}
            :value (if (:first-game @flappy-state)
                     "Start"
                     "Restart")
            :on-click start-game}]])

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
  [:div
   [:h1
    {:key "score-frame"
     :style {:position "absolute"
                :left "200px"
                :top "100px"}}
   (:score @flappy-state)]])

(defn edge-frame []
  [:img {:key "edge-frame"
         :src "imgs/Vintage-Floral-Photo-Frame.png"
         :style {:position "absolute"
                 :left -80
                 :bottom -50
                 :width 650}}])

(defn get-frame []
  [:div
    {:style {:height "100%" :width "100%"}
     :on-mouse-down flap
     }
   [flappy-frame]
   [pillar-frame]
   [score-frame]
   [edge-frame]
   (if (false? (:game-running @flappy-state))
      (if (hs/is-high-score (:score @flappy-state))
        [hs/set-high-score-frame (:score @flappy-state) reset-game]
        [start-frame])
      )])

(defn init []
  (.addEventListener js/document "keydown" handle-keydown)
  (r/render [get-frame]
    (js/document.getElementById "board-area")))

(defonce start (init))
