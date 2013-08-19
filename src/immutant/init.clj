(ns immutant.init
  (:require [immutant.cache     :as cache]
            [immutant.messaging :as msg]
            [immutant.daemons   :as daemon]
            [immutant.jobs      :as job]
            [immutant.web       :as web])
  (:use [ring.util.response :only [response]]
        [ring.middleware.session :only [wrap-session]]
        [immutant.web.session :only [servlet-store]]))

;;; Create a message queue
(msg/start "/queue/msg")

;;; Create a distributed cache to hold our counters
(def cache (cache/lookup-or-create "counters"))

;;; Define a consumer for our queue
(def listener (msg/listen "/queue/msg" #(println (format "recv %s, %s" % cache))))

;;; Our singleton daemon
(let [done (atom false)]

  ;; Our daemon's start function
  (defn start []
    (reset! done false)
    (while (not @done)
      (let [i (:messages cache 1)]
        (println "send" i)
        (try
          (msg/publish "/queue/msg" i)
          (cache/put cache :messages (inc i))
          (catch Throwable e (println "Not expecting this!" e)))
        (Thread/sleep 5000))))

  ;; Our daemon's stop function
  (defn stop []
    (reset! done true))

  ;; Register our daemon
  (daemon/daemonize "counter" start stop))

;;; Schedule a singleton job named "ajob"
(job/schedule "ajob"
              #(let [i (:jobs cache 1)]
                 (println "job" i)
                 (cache/put cache :jobs (inc i)))
              :every [20 :seconds])

;;; Our main web request handler
(defn handler [request]
  (println (format "web [%s]" (:path-info request)))
  (response (format "messages=%s, jobs=%s\n" (:messages cache) (:jobs cache))))
;;; Mount the handler at our app's root context
(web/start handler)

;;; A handler to increment a counter in the web session
(defn count [{session :session}]
  (let [count (:count session 1)
        session (assoc session :count (inc count))]
    (println (format "count=%s" count))
    (-> (response (str "You accessed this page " count " times\n"))
        (assoc :session session))))
;;; Use Immutant's session store for automatic replication
(web/start "/count" (wrap-session #'count {:store (servlet-store)}))
