package com.budget.web.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(properties = {
        "app.database.url=jdbc:h2:mem:category_api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "app.security.oauth-enabled=false"
})
class CategoryApiControllerTest {

    // A valid groceries upsert body; tweak label/bucket/archived per test via String.format placeholders are avoided
    // by using small explicit variants.
    private static String groceriesBody(String label, String bucket, boolean archived) {
        return """
                {
                  "label": "%s",
                  "area": "Koszty codzienne",
                  "analyticsGroup": "Potrzeby podstawowe",
                  "groupId": "obligatoryVariable",
                  "budgetBucket": "%s",
                  "fixedness": "Zmienne konieczne",
                  "flowType": "livingExpense",
                  "discretionary": false,
                  "excluded": false,
                  "realIncome": false,
                  "dailyPaced": true,
                  "protectedFlag": false,
                  "sinkingFundEligible": false,
                  "archived": %s,
                  "sortOrder": 18
                }
                """.formatted(label, bucket, archived);
    }

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void listReturnsSeededGroupsAndCategories() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groups.length()").value(8))
                .andExpect(jsonPath("$.categories.length()").value(40))
                .andExpect(jsonPath("$.categories[?(@.categoryId=='groceries')].label").value(hasItem("Żywność i chemia")));
    }

    @Test
    void renamingACategoryReturnsTheRefreshedCatalogWithTheNewLabel() throws Exception {
        mockMvc.perform(put("/api/v1/categories/groceries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(groceriesBody("Zakupy spożywcze", "Obowiązkowe zmienne", false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[?(@.categoryId=='groceries')].label").value(hasItem("Zakupy spożywcze")));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(jsonPath("$.categories[?(@.categoryId=='groceries')].label").value(hasItem("Zakupy spożywcze")));
    }

    @Test
    void archivingACategoryHidesItButKeepsItInTheCatalog() throws Exception {
        mockMvc.perform(put("/api/v1/categories/electronics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "label": "Elektronika",
                                  "area": "Styl życia",
                                  "analyticsGroup": "Styl życia",
                                  "groupId": "discretionary",
                                  "budgetBucket": "Nieobowiązkowe",
                                  "fixedness": "Uznaniowe",
                                  "flowType": "livingExpense",
                                  "discretionary": true,
                                  "excluded": false,
                                  "realIncome": false,
                                  "dailyPaced": false,
                                  "protectedFlag": false,
                                  "sinkingFundEligible": true,
                                  "archived": true,
                                  "sortOrder": 35
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[?(@.categoryId=='electronics')].archived").value(hasItem(true)));
    }

    @Test
    void renamingAGroupReturnsTheRefreshedCatalog() throws Exception {
        mockMvc.perform(put("/api/v1/categories/groups/obligatoryVariable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"Zmienne konieczne (moje)\",\"sortOrder\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groups[?(@.groupId=='obligatoryVariable')].label").value(hasItem("Zmienne konieczne (moje)")));
    }

    @Test
    void reorderingCategoriesReturnsOk() throws Exception {
        mockMvc.perform(post("/api/v1/categories/reorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupIds\":[\"income\",\"obligatoryFixed\"],\"categoryIds\":[\"diningOut\",\"groceries\"]}"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsAnInvalidBudgetBucketWithBadRequest() throws Exception {
        mockMvc.perform(put("/api/v1/categories/groceries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(groceriesBody("Żywność i chemia", "Nieistniejący koszyk", false)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listReturnsSeededClassificationRules() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rules.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.rules[?(@.source=='builtin')]").isNotEmpty());
    }

    @Test
    void createsAUserRuleAndReturnsItInTheCatalog() throws Exception {
        mockMvc.perform(put("/api/v1/categories/rules/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pattern\":\"MOJ ULUBIONY SKLEP\",\"categoryId\":\"groceries\",\"priority\":100,\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rules[?(@.pattern=='MOJ ULUBIONY SKLEP')].categoryId").value(hasItem("groceries")));
    }

    @Test
    void rejectsAnInvalidRegexRuleWithBadRequest() throws Exception {
        mockMvc.perform(put("/api/v1/categories/rules/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pattern\":\"[unterminated\",\"categoryId\":\"groceries\",\"priority\":100,\"enabled\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsARuleTargetingAnUnknownCategoryWithBadRequest() throws Exception {
        mockMvc.perform(put("/api/v1/categories/rules/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pattern\":\"COKOLWIEK\",\"categoryId\":\"nieistniejaca\",\"priority\":100,\"enabled\":true}"))
                .andExpect(status().isBadRequest());
    }
}
