package com.audio.test.steps;

import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

public class ResourceSteps {

    @Given("resource service is up")
    public void serviceUp() {
        RestAssured.baseURI = "http://localhost:8080";
        RestAssured.filters(new AllureRestAssured());
    }

    @When("I upload an mp3 file")
    public void upload() {
    }

    @Then("I receive 200 with id")
    public void verify() {
        given().when().get("/resources/1").then().statusCode(200).body("id", notNullValue());
    }

    @Then("processor receives message")
    public void checkMessage() {
    }
}
