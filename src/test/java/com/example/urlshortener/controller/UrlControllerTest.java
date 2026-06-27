package com.example.urlshortener.controller;

import com.example.urlshortener.domain.UrlMapping;
import com.example.urlshortener.exception.AliasAlreadyExistsException;
import com.example.urlshortener.service.UrlShortenerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Web-layer slice test: validates HTTP contract without a database. */
@WebMvcTest(controllers = {UrlController.class, RedirectController.class})
@TestPropertySource(properties = {
        "app.base-url=http://localhost:8080",
        "app.reserved-paths=api,actuator"
})
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UrlShortenerService service;

    @Test
    void createReturns201WithLocation() throws Exception {
        when(service.createShortUrl(eq("https://example.com"), isNull()))
                .thenReturn(new UrlMapping(1000000L, "4c92", "https://example.com"));

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost:8080/4c92"))
                .andExpect(jsonPath("$.shortCode").value("4c92"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/4c92"));
    }

    @Test
    void invalidUrlReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"not-a-url\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.url").exists());
    }

    @Test
    void duplicateCustomAliasReturns409() throws Exception {
        when(service.createShortUrl(any(), eq("taken")))
                .thenThrow(new AliasAlreadyExistsException("taken"));

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\",\"customAlias\":\"taken\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void redirectReturns302AndCountsAccess() throws Exception {
        when(service.resolve("4c92")).thenReturn("https://example.com");

        mockMvc.perform(get("/4c92"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com"));

        // The access must be recorded exactly once per visit.
        verify(service, times(1)).recordHit("4c92");
    }
}
