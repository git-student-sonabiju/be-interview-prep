package com.interviewprep.shortener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ShortLinkControllerTest {

    private static final String LONG_URL = "https://example.com/some/very/long/path?with=query";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShortLinkRepository shortLinkRepository;

    @Autowired
    private ShortLinkService shortLinkService;

    @BeforeEach
    void cleanUp() {
        shortLinkRepository.deleteAll();
    }

    @Test
    void shortenReturnsUrlSafeCodeOfAtMostEightCharacters() throws Exception {
        JsonNode body = shorten("{\"url\":\"" + LONG_URL + "\"}");

        assertThat(body.get("code").asText()).matches("[A-Za-z0-9]{1,8}");
        assertThat(body.get("shortUrl").asText()).endsWith("/s/" + body.get("code").asText());
        assertThat(body.get("originalUrl").asText()).isEqualTo(LONG_URL);
    }

    @Test
    void redirectGoesToOriginalUrlAndStatsCountVisits() throws Exception {
        String code = shorten("{\"url\":\"" + LONG_URL + "\"}").get("code").asText();

        mockMvc.perform(get("/s/{code}", code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", LONG_URL));
        mockMvc.perform(get("/s/{code}", code)).andExpect(status().isFound());

        mockMvc.perform(get("/api/links/{code}/stats", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalUrl").value(LONG_URL))
                .andExpect(jsonPath("$.visitCount").value(2))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void shorteningSameUrlTwiceCreatesTwoIndependentLinks() throws Exception {
        String first = shorten("{\"url\":\"" + LONG_URL + "\"}").get("code").asText();
        String second = shorten("{\"url\":\"" + LONG_URL + "\"}").get("code").asText();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void invalidUrlIsRejectedWithFieldError() throws Exception {
        mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"ftp:/not-a-url\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("url"));
    }

    @Test
    void unknownCodeReturnsNotFound() throws Exception {
        mockMvc.perform(get("/s/{code}", "nope123"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void expiredCodeReturnsGone() throws Exception {
        ShortLink link = new ShortLink();
        link.setCode("expired1");
        link.setOriginalUrl(LONG_URL);
        link.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        shortLinkRepository.save(link);

        mockMvc.perform(get("/s/{code}", "expired1"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.message").value("Short link expired1 has expired"));
    }

    @Test
    void concurrentVisitsAreAllCounted() throws Exception {
        String code = shorten("{\"url\":\"" + LONG_URL + "\"}").get("code").asText();
        int visitors = 50;
        ExecutorService pool = Executors.newFixedThreadPool(visitors);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> visits = new ArrayList<>();
        for (int i = 0; i < visitors; i++) {
            visits.add(pool.submit(() -> {
                start.await();
                return shortLinkService.resolveAndCountVisit(code);
            }));
        }

        start.countDown();
        for (Future<String> visit : visits) {
            visit.get();
        }
        pool.shutdown();

        assertThat(shortLinkService.stats(code).visitCount()).isEqualTo(visitors);
    }

    private JsonNode shorten(String body) throws Exception {
        String response = mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }
}
