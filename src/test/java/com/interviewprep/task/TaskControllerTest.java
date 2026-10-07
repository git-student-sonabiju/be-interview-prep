package com.interviewprep.task;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewprep.task.dto.TaskRequest;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    void cleanUp() {
        taskRepository.deleteAll();
    }

    @Test
    void createReturnsCreatedTaskWithDefaultStatus() throws Exception {
        TaskRequest request = new TaskRequest("Write tests", "cover the API", null, LocalDate.now().plusDays(1));

        ResultActions result = postTask(request);

        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Write tests"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void createWithInvalidFieldsReturnsFieldLevelErrors() throws Exception {
        TaskRequest request = new TaskRequest("x".repeat(101), null, null, LocalDate.now().minusDays(1));

        ResultActions result = postTask(request);

        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors", hasSize(2)))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("title")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("dueDate")));
    }

    @Test
    void createWithMissingTitleReturnsFieldError() throws Exception {
        TaskRequest request = new TaskRequest(" ", null, null, null);

        ResultActions result = postTask(request);

        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("title"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("title is required"));
    }

    @Test
    void createWithUnknownStatusReturnsFieldError() throws Exception {
        String body = "{\"title\":\"t\",\"status\":\"BLOCKED\"}";

        ResultActions result = mockMvc.perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body));

        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("status"));
    }

    @Test
    void unknownTaskReturnsNotFoundInErrorFormat() throws Exception {
        ResultActions result = mockMvc.perform(get("/api/tasks/9999"));

        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Task 9999 not found"))
                .andExpect(jsonPath("$.path").value("/api/tasks/9999"));
    }

    @Test
    void listFiltersByStatus() throws Exception {
        postTask(new TaskRequest("a", null, TaskStatus.TODO, null));
        postTask(new TaskRequest("b", null, TaskStatus.DONE, null));
        postTask(new TaskRequest("c", null, TaskStatus.DONE, null));

        ResultActions result = mockMvc.perform(get("/api/tasks").param("status", "DONE"));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].status", hasItem("DONE")));
    }

    @Test
    void updateChangesFieldsAndDeleteRemovesTask() throws Exception {
        String created = postTask(new TaskRequest("old", null, null, null)).andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).get("id").asLong();
        TaskRequest update = new TaskRequest("new", "desc", TaskStatus.IN_PROGRESS, LocalDate.now());

        mockMvc.perform(put("/api/tasks/{id}", id).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("new"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mockMvc.perform(delete("/api/tasks/{id}", id)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/{id}", id)).andExpect(status().isNotFound());
    }

    private ResultActions postTask(TaskRequest request) throws Exception {
        return mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }
}
