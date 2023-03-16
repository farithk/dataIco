(ns main01
  (:require [clojure.edn :as edn]))

;******************************************************************************
; Reading "src/invoice_data.edn" file as a string using slurp.
; Parsing the string to create a Clojure data structure.
; Stores the resulting data into invoice-data.
(def invoice-data (clojure.edn/read-string (slurp "src/invoice_data.edn")))


; Criteria to include or not an item:
; - At least have one item that has :iva 19%
; - At least one item has retention :ret\_fuente 1%
; - Every item must satisfy EXACTLY one of the above two conditions.
; This means that an item cannot have BOTH :iva 19% and retention :ret\_fuente 1%.
;*********************************************************************************
;
; CASES:
;
; (1) The following item has not to be included, because it contains both tax/rate equal to 19
; and retention/rate equal to 1:
; {:invoice-item/id          "ii1"
;                    :invoice-item/sku         "SKU 1"
;                    :taxable/taxes            [{:tax/id       "t1"
;                                                :tax/category :iva
;                                                :tax/rate     19}]
;                    :retentionable/retentions [{:retention/id       "r1"
;                                                :retention/category :ret_fuente
;                                                :retention/rate     1}]}
;
; (2) The following item has to be included because it only contains :retention/rate equal to 1
; but the tax/rate is different to 19, So this satisfy the fact that must satisfy EXACTLY one
; of the above two conditions; and this satisfy one of them (:ret\_fuente 1%.):
; {:invoice-item/id          "ii1"
;                    :invoice-item/sku         "SKU 1"
;                    :taxable/taxes            [{:tax/id       "t1"
;                                                :tax/category :iva
;                                                :tax/rate     16}]
;                    :retentionable/retentions [{:retention/id       "r1"
;                                                :retention/category :ret_fuente
;                                                :retention/rate     1}]}
;
; (3) This item has to be included because only has tax/rate included and
; is equal to 19:
; {:invoice-item/id  "ii3"
;                    :invoice-item/sku "SKU 3"
;                    :taxable/taxes    [{:tax/id       "t3"
;                                        :tax/category :iva
;                                        :tax/rate     19}]}
;
; (4) This item has to be included because only has retention/rate included and
; is equal to 1:
; {:invoice-item/id          "ii4"
;                    :invoice-item/sku         "SKU 3"
;                    :retentionable/retentions [{:retention/id       "r2"
;                                                :retention/category :ret_fuente
;                                                :retention/rate     1}]}
;
; (5) The following items have not to be included:
; {:invoice-item/id          "ii5"
;                    :invoice-item/sku         "SKU 4"
;                    :retentionable/retentions [{:retention/id       "r3"
;                                                :retention/category :ret_fuente
;                                                :retention/rate     2}]}]}
;
; {:invoice-item/id  "ii2"
;                    :invoice-item/sku "SKU 2"
;                    :taxable/taxes    [{:tax/id       "t2"
;                                        :tax/category :iva
;                                        :tax/rate     16}]}


;*********************************************************************************
; filter-items-with-either-tax-or-retention is a function that filters items
; keeping in mind that (item cannot have BOTH :iva 19% and retention :ret\_fuente 1%.)
(defn filter-items-with-either-tax-or-retention [tax-rate retention-rate] ;as input, we need tax and retention rate
  (let [items (:invoice/items invoice-data)]                ;extract items from invoice-data
    (filter
      (fn [item]
        (let [taxes (:taxable/taxes item)
              retentions (:retentionable/retentions item)]  ;we extract taxes and retention from :taxable/taxes and :retentionable/retentions
          (not (and (some #(= (:tax/rate %) tax-rate) taxes)  (some #(= (:retention/rate %) retention-rate) retentions)))
          ; We use the operator and to check if both tax and retention are equal to 19 and 1 respectively.
          ; if they are not equal we return a true value and item is included in the result.
          )
        )
      items)
    )
  )

;*********************************************************************************
; filter-by-taxes-and-retention-values is a function that filters items
; keeping in mind that :tax/rate needs to b e equal to 19 and :retention/rate
; needs to be equal to 19.
(defn filter-by-taxes-and-retention-values [items tax-rate retention-rate] ;as input, we need items, tax and retention rate
  (filter
    (fn [item]
      (let [taxes (:taxable/taxes item)
            retentions (:retentionable/retentions item)] ;extract taxes and retention from :taxable/taxes and :retentionable/retentions
        (or (some #(= (:tax/rate %) tax-rate) taxes) (some #(= (:retention/rate %) retention-rate) retentions))
        ; here we use the or operator to check if one of the rates are equal to theirs specific values
        )
      )
    items))

;******************************************************************************

(defn print-filtered-items [_]
  (let [resulting-items (filter-items-with-either-tax-or-retention 19 1) ;Calling filter-items-with-either-tax-or-retention with tax rate 19 and retention rate 1, and stores the result in resulting-items
        filtered-items-by-values (filter-by-taxes-and-retention-values resulting-items 19 1)] ; Calling filter-by-taxes-and-retention-values with the resulting-items, tax rate 19, and retention rate 1, and stores the result in filtered-items-by-values.
    (println (doall filtered-items-by-values))              ;Printing the filtered-items-by-values to the console.
    ))

; to run using REPL
; (1) (in-ns 'main01) --> to be sure that we are on main01 namespace
; (2) (main01/print-filtered-items? nil)