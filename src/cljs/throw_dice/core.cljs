(ns throw-dice.core
  (:require
   [reagent.core :as reagent]
   [reagent.dom :as rdom]
   [reagent.session :as session]
   [reitit.frontend :as reitit]
   [clerk.core :as clerk]
   [accountant.core :as accountant]
   [clojure.string :as str]))

;; -------------------------
;; Routes

(def router
  (reitit/router
   [["/" :index]
    ["/about" :about]]))

(defn path-for [route & [params]]
  (if params
    (:path (reitit/match-by-name router route params))
    (:path (reitit/match-by-name router route))))

;; -------------------------
;; Page components

(def dice (reagent/atom ""))
(def result (reagent/atom ""))

(defn throw-one-die [times sides]
  (* times (+ 1 (rand-int sides))))

(defn parse-dice [dice]
  (println dice)
  (let [;dice-thrown (first (str/split throw #"[+-]")) ;; supports only 1 die now, think about this later
        times (first (str/split dice #"d"))
        sides (last (str/split dice #"d"))]
    (swap! result #(throw-one-die times sides))))

(defn home-page []
  (fn []
    [:span.main
     [:h1 "Throw some dice"]
     [:div
      [:input {:type :text
               :value @dice
               :on-change #(reset! dice (.-value (.-target %)))}]]
     [:button {:type "submit"
               :on-click #(parse-dice @dice)} "Throw!"]
     [:p "The result: " @result]]))


;; -------------------------
;; Translate routes -> page components

(defn page-for [route]
  (case route
    :index #'home-page))


;; -------------------------
;; Page mounting component

(defn current-page []
  (fn []
    (let [page (:current-page (session/get :route))]
      [:div
       [:header]
       [page]
       [:footer]])))

;; -------------------------
;; Initialize app

(defn mount-root []
  (rdom/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (clerk/initialize!)
  (accountant/configure-navigation!
   {:nav-handler
    (fn [path]
      (let [match (reitit/match-by-path router path)
            current-page (:name (:data  match))
            route-params (:path-params match)]
        (reagent/after-render clerk/after-render!)
        (session/put! :route {:current-page (page-for current-page)
                              :route-params route-params})
        (clerk/navigate-page! path)))
    :path-exists?
    (fn [path]
      (boolean (reitit/match-by-path router path)))})
  (accountant/dispatch-current!)
  (mount-root))

(defn ^:dev/after-load reload! []
  (mount-root))
