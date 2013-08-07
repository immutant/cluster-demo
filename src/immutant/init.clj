(ns immutant.init
  (:require [immutant.cache     :as cache]
            [immutant.messaging :as messaging]
            [immutant.daemons   :as daemon]
            [immutant.jobs      :as job]
            [immutant.web       :as web])
  (:use [ring.util.response :only [response]]))

;;; Create a message queue
(messaging/start "/queue/msg")

;;; Define a consumer for our queue
(def listener (messaging/listen "/queue/msg" #(println "listener:" %)))

;;; Create a distributed cache to hold our counter value
(def cache (cache/lookup-or-create "counters"))

;;; Controls the state of our daemon
(def done (atom false))

;;; Our daemon's start function
(defn start []
  (reset! done false)
  (while (not @done)
    (let [i (:daemon cache 1)]
      (println "daemon:" i)
      (try
        (messaging/publish "/queue/msg" i)
        (cache/put cache :daemon (inc i))
        (catch Throwable e (println "Not expecting this!" e)))
      (Thread/sleep 5000))))

;;; Our daemon's stop function
(defn stop []
  (reset! done true))

;;; Register the daemon
(daemon/daemonize "counter" start stop)

;;; Our web request handler
(defn handler [request]
  (println (format "web [%s]: %s" (:path-info request) cache))
  (response (format "daemon=%s, job=%s\n" (:daemon cache) (:job cache))))

;;; Mount the handler at our app's root context
(web/start handler)

;;; For completeness, schedule a singleton job named "ajob"
(job/schedule "ajob"
              #(let [i (:job cache 1)]
                 (println "job:" i)
                 (cache/put cache :job (inc i)))
              :every [20 :seconds])
