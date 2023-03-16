(ns invoice-item
  (:require [clojure.test :refer :all]))

; discount-factor:  This is a Clojure function that takes an invoice item map
; as input, extracts the discount-rate field, and calculates the corresponding
; discount factor as a decimal number between 0 and 1.
;******************************************************************************
(defn- discount-factor [{:invoice-item/keys [discount-rate]
                         :or                {discount-rate 0}} ;destructuring discount-rate from item map, set discount to 0 is not presented
                        ]
  (- 1 (/ discount-rate 100.0)))

; subtotal function to test: uses the discount-factor function to calculate
; the discounted price of the item by multiplying the precise-quantity,
; precise-price, and discount-factor fields together. If the discount-rate
; field is not provided in the input map, it defaults to 0.
;******************************************************************************
(defn subtotal
  [{:invoice-item/keys [precise-quantity precise-price discount-rate]
    :as                item                                 ;item as clojure map
    :or                {discount-rate 0}                    ;set discount to 0 is not present
    }]
  (* precise-price precise-quantity (discount-factor item)) ;multiply (precise-price * precise-quantity) by the resulting discount factor
  )

; Test cases to test subtotal clojure function
; to run using REPL run:
; (1) (in-ns 'invoice-item) --> to be sure that we are on invoice-item namespace
; (2) (clojure.test/run-tests) --> to run the test on test-subtotal deftest
;******************************************************************************
(deftest test-subtotal
  (testing "Normal case with discount"
    (is (= 1800.0 (subtotal #:invoice-item{:precise-quantity 10, :precise-price 200, :discount-rate 10}))))
  (testing "Normal case without discount"
    (is (= 2000.0 (subtotal #:invoice-item{:precise-quantity 10, :precise-price 200}))))
  (testing "Edge case with 100% discount"
    (is (= 0.0 (subtotal #:invoice-item{:precise-quantity 10, :precise-price 200, :discount-rate 100}))))
  (testing "Edge case with 0 quantity"
    (is (= 0.0 (subtotal #:invoice-item{:precise-quantity 0, :precise-price 200, :discount-rate 10}))))
  (testing "Edge case with 0 precise-price"
    (is (= 0.0 (subtotal #:invoice-item{:precise-quantity 5, :precise-price 0, :discount-rate 10})))))

; To keep in mind --> the subtotal function does not keep in mind negative values neither for precise-quantity,
; nor for :precise-price