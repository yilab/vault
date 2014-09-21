(ns vault.index.defs
  (:require
    [clj-time.core :as time])
  (:import
    clojure.lang.Keyword
    org.joda.time.DateTime
    vault.blob.digest.HashID))


(def blob-index
  "Stores blob statistic data."
  {:record-schema
   {:id        HashID     ; blob hash-id (pk)
    :size      Long       ; blob byte length
    :type      Keyword    ; data type
    :label     String     ; type-specific annotation
    :stored-at DateTime}  ; time added to index

   :queries
   {:direct ^:unique [:id]  ; direct lookups (pk)
    :typed [:type :label]}  ; blobs by type/label

   :projection
   (fn [blob]
     [{:id (:id blob)
       :size (count (:content blob))
       :type (:data/type blob)
       ; TODO: label pgp keys
       :stored-at (time/now)}])})


(def ref-index
  "Stores forward and back references between blobs."
  {:record-schema
   {:blob HashID    ; source hash-id
    :type Keyword   ; source blob type
    :ref  HashID}   ; target hash-id

   :queries
   {:forward [:blob]        ; references from a source blob
    :reverse [:ref :type]}  ; references to a target blob (by type)

   :projection
   (fn [blob]
     (let [record {:blob (:id blob)
                   :type (:data/type blob)}]
       ; TODO: walk the blob data structure and record refs
       []))})


(def tx-log
  "Stores a log of entity transactions."
  {:record-schema
   {:tx    HashID       ; transaction blob hash-id
    :type  Keyword      ; transaction type (root/update)
    :time  DateTime     ; time of modification
    :owner HashID}      ; owner's public-key hash-id

   :queries
   {:history [:owner :time]}

   :projection
   (fn [blob]
     ; TODO: implement
     [])})