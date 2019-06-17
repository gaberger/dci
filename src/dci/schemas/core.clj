(ns dci.schema.core
  (:require [schema.core :as s :include-macros true]))

(def cluster-spec
  "Cluster-spec schema"
  {:organization_id s/Uuid
   :project_name s/Str
   (s/optional-key :bootstrap_node)   s/Str
   :node_spec [{:replicas s/Int
                :plan s/Str
                :distribute s/Bool
                :factilities [s/Str]
                :tags [s/Str]
                :operating_system s/Str}
               (s/optional-key :operating_system) s/Str]}
  )

