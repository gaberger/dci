(ns dci.state)

(def app-state (atom {:debug false
                      :output :table
                      :runtime {}
                      :persist {}}))
