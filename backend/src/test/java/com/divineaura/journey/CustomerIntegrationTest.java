package com.divineaura.journey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import com.divineaura.customer.CustomerDTO;
import com.divineaura.customer.CustomerRegistrationRequest;
import com.divineaura.customer.CustomerUpdateRequest;
import com.divineaura.customer.Gender;
import com.github.javafaker.Faker;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CustomerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;
    private final String CUSTOMER_URI = "/api/v1/customers";

    private String postCustomer(CustomerRegistrationRequest customerRegistrationRequest) {
        return webTestClient.post()
            .uri(CUSTOMER_URI)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(customerRegistrationRequest), CustomerUpdateRequest.class)
            .exchange()
            .expectStatus()
            .isOk()
            .returnResult(Void.class)
            .getResponseHeaders()
            .get(AUTHORIZATION)
            .get(0);
    }
    @Test
    void CanRegisterCustomer() {
        // Create a registration Request
        Faker faker = new Faker();
        int age = 20;
        String name = faker.name().fullName();
        Gender gender = Gender.MALE;
        String email = faker.internet().safeEmailAddress() + "-"
            + UUID.randomUUID()
            + "foobar123@gmail.com";
        CustomerRegistrationRequest customerRegistrationRequest =
            new CustomerRegistrationRequest(
                name,
                email,
                "password", age,
                gender
            );

        // Send a POST request to API
        String jwtToken = postCustomer(customerRegistrationRequest);

        // Get all Customers from API
        List<CustomerDTO> allCustomers = webTestClient.get()
            .uri(CUSTOMER_URI)
            .accept(MediaType.APPLICATION_JSON)
            .header(AUTHORIZATION, "Bearer " + jwtToken)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(new ParameterizedTypeReference<CustomerDTO>() {
            })
            .returnResult()
            .getResponseBody();

        // Get Customer By ID
        assert allCustomers != null;
        var id = allCustomers.stream()
            .filter(c -> c.email().equals(email))
            .map(CustomerDTO::id)
            .findFirst()
            .orElseThrow();

        // Make sure that Customer is present
        CustomerDTO expectedCustomer = new CustomerDTO(id, name, email, gender, age, List.of("ROLE_USER"), email);

        assertThat(allCustomers)
            .contains(expectedCustomer);

        webTestClient.get()
            .uri(CUSTOMER_URI + "/{id}", id)
            .accept(MediaType.APPLICATION_JSON)
            .header(AUTHORIZATION,"Bearer " + jwtToken)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(new ParameterizedTypeReference<CustomerDTO>() {
            })
            .isEqualTo(expectedCustomer);
    }

    @Test
    void canDeleteCustomer() {
        // Create a registration Request
        Faker faker = new Faker();
        int age = 20;
        Gender gender = Gender.MALE;
        String name = faker.name().fullName();
        String email = faker.internet().safeEmailAddress() + "-"
            + UUID.randomUUID()
            + "foobar123@gmail.com";
        CustomerRegistrationRequest customerRegistrationRequest =
            new CustomerRegistrationRequest(
                name,
                email,
                "password", age,
                gender
            );

        CustomerRegistrationRequest customerRegistrationRequest2 =
            new CustomerRegistrationRequest(
                name,
                email + ".in",
                "password", age,
                gender
            );


        // Send a POST request to API to create customer 1
        postCustomer(customerRegistrationRequest);

        // Send a POST request to API to create customer 2
        String jwtToken = postCustomer(customerRegistrationRequest2);

        // Get all Customers from API
        List<CustomerDTO> allCustomers = webTestClient.get()
            .uri(CUSTOMER_URI)
            .accept(MediaType.APPLICATION_JSON)
            .header(AUTHORIZATION,"Bearer " + jwtToken)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(new ParameterizedTypeReference<CustomerDTO>() {
            })
            .returnResult()
            .getResponseBody();



        // Customer 2 Delete Customer By ID
        assert allCustomers != null;
        var id = allCustomers.stream()
            .filter(c -> c.email().equals(email))
            .map(CustomerDTO::id)
            .findFirst()
            .orElseThrow();

        webTestClient.delete()
            .uri(CUSTOMER_URI + "/{id}", id)
            .accept(MediaType.APPLICATION_JSON)
            .header(AUTHORIZATION,"Bearer " + jwtToken)
            .exchange()
            .expectStatus()
            .isOk();

        // Get Customer By ID throw an error
        webTestClient.get()
            .uri(CUSTOMER_URI + "/{id}", id)
            .accept(MediaType.APPLICATION_JSON)
            .header(AUTHORIZATION,"Bearer " + jwtToken)
            .exchange()
            .expectStatus()
            .isNotFound();
    }


    @Test
    void canUpdaeCustomer() {
        // Create a registration Request
        Faker faker = new Faker();
        int age = 20;
        String name = faker.name().fullName();
        Gender gender = Gender.MALE;
        String email = faker.internet().safeEmailAddress() + "-"
            + UUID.randomUUID()
            + "foobar123@gmail.com";

        CustomerRegistrationRequest customerRegistrationRequest =
            new CustomerRegistrationRequest(
                name,
                email,
                "password", age,
                gender
            );

        // Send a POST request to API
        String jwtToken = postCustomer(customerRegistrationRequest);

        // Get all Customers from API
        List<CustomerDTO> allCustomers = webTestClient.get()
            .uri(CUSTOMER_URI)
            .accept(MediaType.APPLICATION_JSON)
            .header(AUTHORIZATION, "Bearer " + jwtToken)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(new ParameterizedTypeReference<CustomerDTO>() {
            })
            .returnResult()
            .getResponseBody();

        // Update Customer By ID
        assert allCustomers != null;
        var id = allCustomers.stream()
            .filter(c -> c.email().equals(email))
            .map(CustomerDTO::id)
            .findFirst()
            .orElseThrow();

        var updatedEmail = UUID.randomUUID() + "@fooUpdated.com";
        var updatedAge = 25;
        CustomerUpdateRequest customerUpdateRequest = new CustomerUpdateRequest(null, null, updatedAge, null);

        webTestClient.put()
            .uri(CUSTOMER_URI + "/{id}", id)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .header(AUTHORIZATION, "Bearer " + jwtToken)
            .body(Mono.just(customerUpdateRequest), CustomerUpdateRequest.class)
            .exchange()
            .expectStatus()
            .isOk();

        // Get Customer By ID
        var expectedCustomer = new CustomerDTO(id, name, email, gender, updatedAge, List.of("ROLE_USER"), email);

        CustomerDTO actual = webTestClient.get()
            .uri(CUSTOMER_URI + "/{id}", id)
            .accept(MediaType.APPLICATION_JSON)
            .header(AUTHORIZATION, "Bearer " + jwtToken)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(new ParameterizedTypeReference<CustomerDTO>() {
            })
            .returnResult()
            .getResponseBody();

        assertThat(actual).isEqualTo(expectedCustomer);

    }
}
