package io.cucumber.kotlin

import io.cucumber.java.en.Then
import io.cucumber.java8.En

class IssuesStepDefinition: En {
    init {
        Then("test") {

        }
        And("""^the product id is (\d+)$""") {

        }
    }

    @Then("""^the product id is (\d+)$""")
    fun `the product id is`(id: Int) {
    }
}