(defproject chat "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.238"]
                 [reagent "0.9.0-rc1"]
                 [re-frame "0.11.0-rc1"]
                 [secretary "1.2.3"]]

  :plugins [[cider/cider-nrepl "0.22.3"]
            [lein-cljsbuild "1.1.7"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj" "src/cljs" "dev"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :figwheel {:css-dirs ["resources/public/css"]}

  :profiles
  {:dev
         {:dependencies [[binaryage/devtools "0.9.10"]
                         [day8.re-frame/re-frame-10x "0.4.3"]
                         [day8.re-frame/tracing "0.5.1"]
                         [figwheel-sidecar "0.5.16"]
                         [cider/piggieback "0.4.1"]]
          :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}
          :plugins      [[lein-figwheel "0.5.16"]]}
   :prod {:dependencies [[day8.re-frame/tracing-stubs "0.5.1"]]}}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs"]
     :figwheel     {:on-jsload "chat.core/mount-root"}
     :compiler     {:main                 chat.core
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true
                    :preloads             [devtools.preload
                                           day8.re-frame-10x.preload]
                    :closure-defines      {"re_frame.trace.trace_enabled_QMARK_"        true
                                           "day8.re_frame.tracing.trace_enabled_QMARK_" true}
                    :external-config      {:devtools/config {:features-to-install :all}}}}


    {:id           "min"
     :source-paths ["src/cljs"]
     :compiler     {:main            chat.core
                    :output-to       "resources/public/js/compiled/app.js"
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}]})
