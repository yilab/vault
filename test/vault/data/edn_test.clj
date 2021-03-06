(ns vault.data.edn-test
  (:require
    [byte-streams :refer [bytes=]]
    [clojure.test :refer :all]
    [vault.blob.content :as content]
    [vault.data.edn :as edn]))


; FIXME: This is necessary for some reason to placate Cloverage...
(edn/register-tag! vault/ref
  vault.blob.content.HashID str
  content/parse-id)


(defn data-fixture
  "Builds a string representing a data blob from the given sequence of values."
  [& values]
  (->> values
       (interpose "\n\n")
       (apply str "#vault/data\n")
       content/read))


(deftest data-typing
  (are [t v] (is (= t (edn/value-type v)))
    String  "foo"
    clojure.lang.Keyword :bar
    :map    {:x 'y}
    :set    #{:foo :bar}
    :vector [:foo :bar]
    :test   {:vault/type :test}))



;; SERIALIZATION

(deftest blob-creation
  (let [blob (edn/data->blob [:foo])]
    (is (:id blob))
    (is (:content blob))
    (is (= :vector (:data/type blob)))
    (is (= [[:foo]] (:data/values blob)))
    (is (= [12 18] (:data/primary-bytes blob)))
    (is (= "[:foo]" (String. (edn/primary-bytes blob)))))
  (let [blob (edn/data->blob {:alpha 'omega} (comp vector count))]
    (is (= [{:alpha 'omega} 14] (:data/values blob)))
    (is (= [12 26] (:data/primary-bytes blob)))))


(deftest blob-printing
  (is (= "#vault/data\n{:alpha \"foo\" :omega \"bar\"}\n"
         (with-out-str
           (edn/print-data {:data/values [{:omega "bar" :alpha "foo"}]}))))
  (is (= "#vault/data\n[:foo \\b baz]\n\n{:name \"Aaron\"}\n\n:frobnitz\n"
         (with-out-str
           (edn/print-data {:data/values [[:foo \b 'baz] {:name "Aaron"} :frobnitz]})))))



;; DESERIALIZATION

(deftest read-non-edn-blob
  (let [blob (content/read "foobarbaz not a data blob")
        data (edn/parse-data blob)]
    (is (nil? data))))


(deftest read-data-blob
  (let [blob (data-fixture "{:foo bar, :vault/type :x/y}")
        data (edn/parse-data blob)]
    (is (= [{:foo 'bar, :vault/type :x/y}]
           (:data/values data)))
    (is (= :x/y (:data/type data)))))


(deftest read-primary-bytes
  (testing "data blob"
    (let [primary-value "[1 \\2 :three]"
          value-str (str primary-value " :x/y \"foo\"")
          blob (data-fixture value-str)
          data (edn/parse-data blob)
          values (:data/values data)
          primary-bytes (edn/primary-bytes data)]
      (is (= [[1 \2 :three] :x/y "foo"] values))
      (is (bytes= (.getBytes primary-value edn/data-charset) primary-bytes))))
  (testing "non-data blob"
    (let [blob (content/read "frobble babble")]
      (is (bytes= (:content blob) (edn/primary-bytes blob))))))


(deftest read-utf8-primary-bytes
  (let [value-str "\"€18.50\""
        blob (data-fixture value-str)
        data (edn/parse-data blob)]
    (is (bytes= (.getBytes value-str edn/data-charset)
                (edn/primary-bytes data)))))
