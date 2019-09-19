(ns chat.views
  (:require-macros [chat.ui :refer [defui]])
  (:require
   [cljs-time.core :as ct]
   [cljs-time.format :as ctf]
   [re-frame.core :as re-frame]
   [chat.subs :as subs]
   [chat.events :as events]
   [goog.fx.dom]
   [reagent.core :as r]))


;; Auto-scrolling ==============================================================

(defn scroll! [el start end time]
  (.play (goog.fx.dom.Scroll. el (clj->js start) (clj->js end) time)))

(defn scrolled-to-end? [el tolerance]
  ;; at-end?: element.scrollHeight - element.scrollTop === element.clientHeight
  (> tolerance (- (.-scrollHeight el) (.-scrollTop el) (.-clientHeight el))))

(defn autoscroll-list [{:keys [children class scroll?] :as opts}]
  (let [should-scroll (r/atom true)]
    (r/create-class
     {:display-name "autoscroll-list"
      :component-did-mount
      (fn [this]
        (let [n (r/dom-node this)]
          (scroll! n [0 (.-scrollTop n)] [0 (.-scrollHeight n)] 0)))
      :component-will-update
      (fn [this]
        (let [n (r/dom-node this)]
          (reset! should-scroll (scrolled-to-end? n 100))))
      :component-did-update
      (fn [this]
        (let [scroll? (:scroll? (r/props this))
              n       (r/dom-node this)]
          (when (and scroll? @should-scroll)
            (scroll! n [0 (.-scrollTop n)] [0 (.-scrollHeight n)] 600))))
      :reagent-render
      ;; When getting next and prev props here it would be possible to detect if children have changed
      ;; and to disable scrollbars for the duration of the scroll animation
      (fn [{:keys [children]}]
        (into [:div {:class class}] children))})))

(defn button [attrs text]
  [:button.button attrs text])

(defn avatar [{:keys [src size]}]
  [:img.avatar
   {:style {:height 48 :width 48} :src (or src "images/luke.png")}])

(defn header [{:keys [left right title]}]
  [:div.header {:style {:height 48}}
   [:div.header-left left]
   [:div.header-title title]
   [:div.header-right right]])

(defmulti message
  (fn [x] (.log js/console x (:type x) (keyword (:type x)))
    (:type x)) :default "text")

(defmethod message "text"
  [{:keys [user uid time text avatar-src]} me?]
  [:div.message {:class (if me? "me" "other")}
   [avatar {:src avatar-src}]
   [:div.message-buble {:style {:margin-bottom 15}}
    [:div.message-meta
     [:div.message-user (str "@" user)]
     [:div.message-time (ctf/unparse (ctf/formatters :date-time) (or time (ct/now)))]]
    [:div {:class "message-text" :dangerouslySetInnerHTML {:__html text}}]]])

(defmethod message "button"
  [m _]
  (message (assoc m :type "list")))

(defmethod message "list"
  [{:keys [items avatar-src caption user time]} me?]
  [:<>
   [:div.message {:class "other"}
    [avatar {:src avatar-src}]
    (when (seq caption)
      [:div.message-buble
       [:div.message-meta
        [:div.message-user (str "@" user)]
        [:div.message-time (ctf/unparse (ctf/formatters :date-time)
                                        (or time (ct/now)))]]
       [:div {:class "message-text" :dangerouslySetInnerHTML {:__html caption}}]])]
   (for [m items]
     ^{:key (:text m)}
     [:button.button
      {:style {:margin-right 15 :margin-top 15 :margin-bottom 15}
       :class "button"
       :on-click #(re-frame/dispatch [::events/send-message nil (:postback m) nil])}
      (:text m)])])

(defmethod message "video"
  [{:keys [items video-url caption avatar-src user time]} _]
  [:<>
   [:div.message {:class "other"}
    [avatar {:src avatar-src}]
    [:div.message-buble
     [:div.message-meta
      [:div.message-user (str "@" user)]
      [:div.message-time (ctf/unparse (ctf/formatters :date-time) (or time (ct/now)))]]
     (when (seq caption)
       [:div {:class "message-text" :dangerouslySetInnerHTML {:__html caption}}])
     [:video {:style {:margin-top 15} :width 400 :controls true}
      [:source {:src video-url}]]]]])

(defmethod message "image"
  [{:keys [items image-url caption avatar-src user time]} _]
  [:<>
   [:div.message {:class "other"}
    [avatar {:src avatar-src}]
    [:div.message-buble
     [:div.message-meta
      [:div.message-user (str "@" user)]
      [:div.message-time (ctf/unparse (ctf/formatters :date-time) (or time (ct/now)))]]
     (when (seq caption)
       [:div {:class "message-text"
              :dangerouslySetInnerHTML {:__html caption}}])
     [:img {:src image-url :style {:display :block :margin-top 15} :width 280}] ]]])

(def msg (r/atom nil))

(defn textarea-message []
  (let [written-text (r/atom "")
        text-area-key (r/atom 0)
        ref (r/atom nil)]
    (add-watch written-text :update-message
               (fn [_ _ _ s]
                 (when-not (seq s) (swap! text-area-key inc))))
    (fn [input]
      (when @ref
        (.focus @ref))
      ^{:key @text-area-key}
      [:textarea.input
       {:value @written-text
        :ref (fn [input] (reset! ref input))
        :autoFocus true
        :on-key-press
        (fn [e]
          (let [value (-> (.. e -target -value))]
            (when (and (seq value) (= (.-key e) "Enter"))
              (.preventDefault e)
              (re-frame/dispatch [::events/send-message nil @written-text "me"])
              (reset! written-text ""))))
        :on-change #(reset! written-text (.. % -target -value))}])))

(defn chat [{:keys [id]}]
  (let [msgs @(re-frame/subscribe [::subs/messages :ami])
        username "Me"
        messages-components (for [[i msg] (map-indexed (fn [i m] [i m]) msgs)]
                              ^{:key i} [message msg (= username (:user msg))])]
    [:div.screen
     [header {:title "Ami Chat Beta"
              :right [avatar {}]}]
     [autoscroll-list {:style {:margin-bottom 15}
                       :class :content :children messages-components :scroll? true}]
     [:div {:style {:float :left :clear :both}}]
     [:div.footer
      [textarea-message]
      [button {:class "btn"
               :on-click #(re-frame/dispatch [::events/send-message id @msg username])}
       [:i {:class ["fa fa-send"]}] "Send"]]]))


(defn main-panel []
  (let [{:keys [id params]} @(re-frame/subscribe [::subs/route])]
    [:div {:style {:display :flex :flex-direction :column-reverse}}
     [chat params]]))
