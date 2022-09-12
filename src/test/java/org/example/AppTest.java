package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import io.restassured.RestAssured;

import org.example.requests.ShoppingItemRequest;
import org.example.responses.DeleteItemResponse;
import org.example.responses.ShoppingItemResponse;
import org.example.responses.ShoppingListResponse;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import org.hamcrest.Matchers;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AppTest {
    final String API_KEY = "8795fdd66bae4e79b7f8496b2a2fc4d2";
    final String API_USER = "reogina";
    final String API_HASH = "a6a342ea2460f0b0079c2d0c6106cb84e238092a";

    final ObjectMapper objectMapper = new ObjectMapper();

    RequestSpecification requestSpec;
    ResponseSpecification responseSpec;

    @BeforeEach
    void init() {
        requestSpec = new RequestSpecBuilder()
                .log(LogDetail.ALL)
                .addQueryParam("apiKey", API_KEY)
                .addQueryParam("hash", API_HASH)
                .addPathParam("username", API_USER)
                .build()
        ;
        responseSpec = new ResponseSpecBuilder()
                .expectStatusCode(200)
                .expectStatusLine("HTTP/1.1 200 OK")
                .expectContentType(ContentType.JSON)
                .expectResponseTime(Matchers.lessThan(5000L))
                .build()
        ;
    }

    @DisplayName("Add item to shopping list")
    @ParameterizedTest
    @MethodSource("addItemSource")
    @Order(1)
    void addItemToShoppingList(ShoppingItemRequest item) throws JsonProcessingException {
        RestAssured.given()
                .spec(requestSpec)
                .body(objectMapper.writeValueAsString(item))
                .when()
                .post("https://api.spoonacular.com/mealplanner/{username}/shopping-list/items")
                .then()
                .spec(responseSpec)
        ;
    }

    static Stream<ShoppingItemRequest> addItemSource() {
        return Stream.of(
                new ShoppingItemRequest("1 package baking powder"),
                new ShoppingItemRequest("1 package baking powder", true),
                new ShoppingItemRequest("pasta", true)
        );
    }

    private static ShoppingListResponse shoppingListResponse;

    @DisplayName("Get shopping list")
    @Test
    @Order(2)
    void getShoppingList() {
        shoppingListResponse = RestAssured.given()
                .spec(requestSpec)
                .when()
                .get("https://api.spoonacular.com/mealplanner/{username}/shopping-list")
                .then()
                .spec(responseSpec)
                .extract()
                .body().as(ShoppingListResponse.class)
        ;
        assertThat(shoppingListResponse.getAisles().size(), equalTo(3));
        assertThat(shoppingListResponse.getAisles().stream()
                .filter(a -> a.getName().equals("Baking")).count(), equalTo(1L));
        assertThat(shoppingListResponse.getAisles().stream()
                .filter(a -> a.getName().equals("Pasta and Rice")).count(), equalTo(1L));
        assertThat(shoppingListResponse.getAisles().stream()
                .filter(a -> a.getName().equals("Non-Food Items")).count(), equalTo(1L));
    }

    @DisplayName("Delete item from shopping list")
    @ParameterizedTest
    @MethodSource("deleteItemSource")
    @Order(3)
    void deleteItemFromShoppingList(ShoppingItemResponse item) {
        var response = RestAssured.given()
                .spec(requestSpec)
                .pathParam("id", item.getId())
                .when()
                .delete("https://api.spoonacular.com/mealplanner/{username}/shopping-list/items/{id}")
                .then()
                .spec(responseSpec)
                .extract()
                .body().as(DeleteItemResponse.class)
                ;
        assertThat(response.getStatus(), equalTo("success"));
    }

    static Stream<ShoppingItemResponse> deleteItemSource() {
        return shoppingListResponse
                .getAisles().stream()
                .flatMap(a -> a.getItems().stream());
    }
}
