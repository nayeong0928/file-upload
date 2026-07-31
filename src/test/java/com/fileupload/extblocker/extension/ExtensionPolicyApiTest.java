package com.fileupload.extblocker.extension;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the real /api/extensions endpoints end-to-end (PRD 4.1 + 4.4).
 * @Transactional rolls each test back so they don't leak state into each other
 * or into the other test classes sharing the same in-memory DB.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExtensionPolicyApiTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void fixedExtensionsStartUncheckedAndTogglingPersists() throws Exception {
        mockMvc.perform(get("/api/extensions/fixed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[?(@.extension=='exe')].is_blocked").value(false));

        mockMvc.perform(patch("/api/extensions/fixed/exe")
                        .contentType("application/json")
                        .content("{\"is_blocked\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_blocked").value(true));

        // Simulates a page refresh: state must have actually persisted, not just been echoed back.
        mockMvc.perform(get("/api/extensions/fixed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.extension=='exe')].is_blocked").value(true));
    }

    @Test
    void addingDuplicateCustomExtensionIsRejectedCaseInsensitively() throws Exception {
        mockMvc.perform(post("/api/extensions/custom")
                        .contentType("application/json")
                        .content("{\"extension\":\"qaext1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.extension").value("qaext1"));

        mockMvc.perform(post("/api/extensions/custom")
                        .contentType("application/json")
                        .content("{\"extension\":\"QAEXT1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미 등록된 확장자입니다"));
    }

    @Test
    void addingCustomExtensionThatCollidesWithFixedIsRejected() throws Exception {
        mockMvc.perform(post("/api/extensions/custom")
                        .contentType("application/json")
                        .content("{\"extension\":\"EXE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("고정 확장자에 있는 확장자입니다"));
    }

    @Test
    void addingCustomExtensionNormalizesCaseAndTrimsWhitespace() throws Exception {
        mockMvc.perform(post("/api/extensions/custom")
                        .contentType("application/json")
                        .content("{\"extension\":\"  QaExt2  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.extension").value("qaext2"));
    }

    @Test
    void addingCustomExtensionWithNonAlphanumericCharactersIsRejected() throws Exception {
        mockMvc.perform(post("/api/extensions/custom")
                        .contentType("application/json")
                        .content("{\"extension\":\"a.b\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletingCustomExtensionRemovesItImmediatelyAndFromTheDatabase() throws Exception {
        String body = mockMvc.perform(post("/api/extensions/custom")
                        .contentType("application/json")
                        .content("{\"extension\":\"qaext3\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(delete("/api/extensions/custom/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/extensions/custom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + id + ")]").doesNotExist());

        // Deleted for real, not just hidden: the same value can be registered again.
        mockMvc.perform(post("/api/extensions/custom")
                        .contentType("application/json")
                        .content("{\"extension\":\"qaext3\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void exceedingMaxCustomExtensionCountIsRejected() throws Exception {
        for (int i = 0; i < 200; i++) {
            mockMvc.perform(post("/api/extensions/custom")
                            .contentType("application/json")
                            .content("{\"extension\":\"bulk" + i + "\"}"))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(post("/api/extensions/custom")
                        .contentType("application/json")
                        .content("{\"extension\":\"onemore\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("최대 200개까지 등록할 수 있습니다"));
    }
}
