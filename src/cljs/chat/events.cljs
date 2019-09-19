(ns chat.events
  (:require
    [re-frame.core :as re-frame]
    [chat.db :as db]
    [cljs-time.core :as ct]
    [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
    [ajax.core :as ajax]
    [day8.re-frame.http-fx]
    [goog.object :as gobj]
    [cljs.reader :as reader]
    [secretary.core :as secretary]))

(re-frame/reg-event-fx
  ::initialize-db
  (fn-traced
   [_ _]
   {:db db/default-db
    :dispatch [::send-message _ "What can you do?" _]}))

(re-frame/reg-event-fx
  ::send-message
  (fn [{db :db} [_ id msg user]]
    {:db (update-in db [:messages :ami] (fnil conj [])
                    {:type :text :text msg :user "Me" :time (ct/now)})
     :http-xhrio
     {:method :post
      :uri "http://sub-ves-dev.ch/api/Intents"
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})
      :on-success [::on-messages]
      :params {:txt msg}}}))

(re-frame/reg-event-db
  ::on-messages
  (fn-traced
   [db [_ messages]]
   (update-in db [:messages :ami] (fnil into [])
              (mapv #(assoc % :author "Ami" :user "Ami" :time (ct/now)
                            :avatar-src "images/ami.jpg") messages))))

(comment
  (re-frame/dispatch [::sign-in]))
