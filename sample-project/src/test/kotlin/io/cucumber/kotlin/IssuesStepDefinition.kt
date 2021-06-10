package io.cucumber.kotlin

import io.cucumber.java.en.Then
import io.cucumber.java8.En

class IssuesStepDefinition: En {
    init {
        Then("test") {

        }
        And("""^the product id is (\d+)$""") {

        }
        And("I have {int} cuke(s) in my stomach") { i: Int ->

        }
        And("I have {int} cucumber(s) in my belly/stomach") { i: Int ->

        }
    }

    @Then("""^the product id is (\d+)$""")
    fun `the product id is`(id: Int) {
    }
}
