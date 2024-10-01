package io.cucumber.kotlin

import io.cucumber.java8.En

class IssuesStepDefinition : En {
    init {
        Then("test") {}
        And("^the product id is (\\d+)$") { number: Int -> }
        And("I have {int} cuke(s) in my stomach") { i: Int -> }
        And("I have {int} cucumber(s) in my belly/stomach") { i: Int -> }
        And("^Product(?: with id (\\d+))? is valid") { number: Int? -> }
    }
}
