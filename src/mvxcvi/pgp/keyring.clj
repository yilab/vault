(ns mvxcvi.pgp.keyring
  "Keyring provider protocols."
  (:require
    [byte-streams]
    [clojure.java.io :as io]
    [clojure.string :as string]
    (mvxcvi.pgp
      [core :as pgp :refer [KeyProvider]]
      [util :refer [hex-str]]))
  (:import
    (org.bouncycastle.openpgp
      PGPPublicKeyRing
      PGPPublicKeyRingCollection
      PGPSecretKeyRing
      PGPSecretKeyRingCollection
      PGPUtil)))


;; KEYRING UTILITIES

(defn load-public-keyrings
  "Loads a public keyring file into a sequence of vectors of public keys."
  [source]
  (with-open [stream (PGPUtil/getDecoderStream
                       (byte-streams/to-input-stream source))]
    (map (fn [^PGPPublicKeyRing keyring]
           (vec (iterator-seq (.getPublicKeys keyring))))
         (-> stream
             PGPPublicKeyRingCollection.
             .getKeyRings
             iterator-seq))))


(defn load-secret-keyrings
  "Loads a secret keyring file into a sequence of vectors of secret keys."
  [source]
  (with-open [stream (PGPUtil/getDecoderStream
                       (byte-streams/to-input-stream source))]
    (map (fn [^PGPSecretKeyRing keyring]
           (vec (iterator-seq (.getSecretKeys keyring))))
         (-> stream
             PGPSecretKeyRingCollection.
             .getKeyRings
             iterator-seq))))


(defn- find-key
  "Locates a key in a sequence by id. Nested sequences are flattened, so this
  works directly on keyrings and keyring collections."
  [id key-seq]
  (let [id (pgp/key-id id)]
    (some #(when (= id (pgp/key-id %)) %)
          (flatten key-seq))))



;; KEYRING PROVIDER

(defrecord PGPKeyring
  [pubring secring]

  KeyProvider

  (load-public-key [this id]
    (find-key id (load-public-keyrings (:pubring this))))

  (load-private-key [this id passphrase]
    (-> id
        (find-key (load-secret-keyrings (:secring this)))
        (pgp/unlock-key passphrase))))


(defn pgp-keyring
  "Constructs a PGPKeyring for the given keyring files."
  ([keyring-dir]
   (pgp-keyring (io/file keyring-dir "pubring.gpg")
                (io/file keyring-dir "secring.gpg")))
  ([pubring secring]
   (->PGPKeyring (io/file pubring) (io/file secring))))



;; CACHING PROVIDER

(defrecord PrivateKeyCache
  [provider store]

  KeyProvider

  (load-public-key
    [this id]
    (pgp/load-public-key (:provider this) id))

  (load-private-key
    [this id]
    (let [id (pgp/key-id id)]
      (or (get @(:store this) id)
          (when-let [privkey (pgp/load-private-key (:provider this) id)]
            (swap! (:store this) assoc id privkey)
            privkey))))

  (load-private-key
    [this id passphrase]
    (let [id (pgp/key-id id)]
      (or (get @(:store this) id)
          (when-let [privkey (pgp/load-private-key (:provider this) id passphrase)]
            (swap! (:store this) assoc id privkey)
            privkey)))))


(defn key-cache
  "Wraps a key provider in a layer that keeps unlocked private keys in a map."
  [provider]
  (->PrivateKeyCache provider (atom {})))



;; INTERACTIVE PROVIDER

(defn interactive-unlocker
  "Wraps a key provider in a layer that will request a passphrase on the
  command-line when a private key needs to be unlocked."
  [provider]
  (reify KeyProvider

    (load-public-key
      [_ id]
      (pgp/load-public-key provider id))

    (load-private-key
      [_ id]
      (let [id (pgp/key-id id)]
        (println "Passphrase for private key " (hex-str id) ":")
        (pgp/load-private-key provider id (read-line))))

    (load-private-key
      [_ id passphrase]
      (pgp/load-private-key provider id passphrase))))
