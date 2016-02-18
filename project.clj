(defproject cljs-flappy "0.1.1-SNAPSHOT"
  :description "Flappy bird in clojure script based on https://github.com/bhauman/flappy-bird-demo"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/clojurescript "1.7.228"]
                 [reagent "0.6.0-alpha"]
                 [sablono "0.6.0"]
                 [matchbox "0.0.8-SNAPSHOT"]]

  :plugins [[lein-cljsbuild "1.1.2"]
            [lein-figwheel "0.5.0-6"]]

  :clean-targets ^{:protect false} ["resources/public/js/out"
                                    "resources/public/js/cljs_flappy.js"
                                    "release/resources/public/js/cljs_flappy.js",
                                    "release/resources/public/js/out",
                                    :target-path]

  :source-paths ["src"]

  :cljsbuild {
    :builds {
       :dev {
             :source-paths ["src"],
             :figwheel true,
             :compiler
              { :main cljsflappy.core,
                :asset-path "js/out",
                :output-to "resources/public/js/cljs_flappy.js",
                :output-dir "resources/public/js/out",
               :source-map-timestamp true}
             }
       :release { :source-paths ["src"],
                  :compiler,
                  { :main cljsflappy.core,
                    :asset-path "js/out",
                    :output-to "release/resources/public/js/cljs_flappy.js",
                    :output-dir "release/resources/public/js/out",
                    :optimizations :whitespace}}}}

  :figwheel { :css-dirs ["resources/public/css"]
              :open-file-command "emacsclient"
             })
