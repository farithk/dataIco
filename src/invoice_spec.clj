(ns invoice-spec
  (:require
    [clojure.spec.alpha :as s]
    [cheshire.core :as cheshire]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [java-time :as jt]))

(import 'java.time.format.DateTimeFormatter)
(import 'java.time.LocalDate)
(import 'java.time.ZoneId)

;******************************************************************************
;Reading the json file and store its data in my-json-invoice-data.
(defn read-json-file [filename]
  (with-open [reader (io/reader filename)]
    (cheshire/parse-stream reader true)))

(def my-json-invoice-data (read-json-file "src/invoice.json"))

;******************************************************************************
; Extracting fields values from my-json-invoice-data and assigning it
; to the new fields names to build the final invoice.
(def customer-name (get-in my-json-invoice-data [:invoice :customer :company_name]))
(def customer-email (get-in my-json-invoice-data [:invoice :customer :email]))
(def issue-date (get-in my-json-invoice-data [:invoice :issue_date]))
(def invoice-items (get-in my-json-invoice-data [:invoice :items ]))

;******************************************************************************
; because issue-date needs to be cast as (:invoice/issue-date inst?)
; we need to use the following clojure function to accomplish it.
; keeping in mind that the entry format is "dd/MM/yyyy"
(defn parse-date [date-str]
  (let [formatter (DateTimeFormatter/ofPattern "dd/MM/yyyy")
        local-date (LocalDate/parse date-str formatter)
        instant (-> local-date
                    (.atStartOfDay (ZoneId/systemDefault))
                    (.toInstant))]
    (jt/instant instant)))

(def issue-date-cast (parse-date issue-date))


; for the case of items, the entry item value from json file, is a map of items
; that also need to be formatted. For taxes, they need to be a % number, so we
; need to use convert-tax function to divide the incoming value by 100. Also wee
; need to store the :invoice-item/taxes as a vector to satisfy
; (s/def :invoice-item/taxes (s/coll-of ::tax :kind vector? :min-count 1)).
(defn convert-tax [tax]
  {:tax/category (keyword (clojure.string/lower-case (get tax :tax_category))) ;The value of the :tax_category key is converted to lower case and then to a keyword
   :tax/rate     (/ (double (get tax :tax_rate)) 100)       ;here we convert the entry tax_rate value into % as a double
   })

(defn convert-item [item]
  {:invoice-item/price    (get item :price)                 ;new :invoice-item/price with the price value
   :invoice-item/quantity (get item :quantity)              ;new :invoice-item/quantity with the quantity value
   :invoice-item/sku      (get item :sku)                   ;new :invoice-item/sku with the sku value
   :invoice-item/taxes    (into [] (map convert-tax) (get item :taxes)) ;here we map the convert-tax to every one of the taxes and convert the result into a vector with keys :tax/category and :tax/rate.
   })

; we need to convert the resulting from convert-item to a vector to satisfy
; (s/def :invoice/items (s/coll-of ::invoice-item :kind vector? :min-count 1))
; so we use (vec()) and assign it to converted-items
(def converted-items (vec (map convert-item invoice-items))); the map iterate over all the items present in my-json-invoice-data


;******************************************************************************
; Then we create the new invoice that holds the new values
(def final-invoice
  {:invoice/issue-date issue-date-cast
   :invoice/customer {:customer/name customer-name
                      :customer/email customer-email}
   :invoice/items converted-items})

;******************************************************************************
; Definition of the spec for the whole invoice that needs to be satisfied.
(s/def :customer/name string?)
(s/def :customer/email string?)
(s/def :invoice/customer (s/keys :req [:customer/name
                                       :customer/email]))

(s/def :tax/rate double?)
(s/def :tax/category #{:iva})
(s/def ::tax (s/keys :req [:tax/category
                           :tax/rate]))
(s/def :invoice-item/taxes (s/coll-of ::tax :kind vector? :min-count 1))

(s/def :invoice-item/price double?)
(s/def :invoice-item/quantity double?)
(s/def :invoice-item/sku string?)

(s/def ::invoice-item
  (s/keys :req [:invoice-item/price
                :invoice-item/quantity
                :invoice-item/sku
                :invoice-item/taxes]))

(s/def :invoice/issue-date inst?)
(s/def :invoice/items (s/coll-of ::invoice-item :kind vector? :min-count 1))

(s/def ::invoice
  (s/keys :req [:invoice/issue-date
                :invoice/customer
                :invoice/items]))

;******************************************************************************
; Checking if final-invoice is valid according to the spec provided before.
(def is-valid? (s/valid? ::invoice final-invoice))

;******************************************************************************
; Function to print the validation result.
(defn print-is-valid? [_]
  (println is-valid?))

; to run using REPL
; (1) (in-ns 'invoice-spec) --> to be sure that we are on invoice-spec namespace
; (2) (invoice-spec/print-is-valid? nil)