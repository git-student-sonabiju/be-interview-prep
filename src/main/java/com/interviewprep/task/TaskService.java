package com.interviewprep.task;

import com.interviewprep.common.PageResponse;
import com.interviewprep.common.error.ResourceNotFoundException;
import com.interviewprep.task.dto.TaskRequest;
import com.interviewprep.task.dto.TaskResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    @Transactional
    public TaskResponse create(TaskRequest request) {
        Task task = new Task();
        apply(task, request);
        if (task.getStatus() == null) {
            task.setStatus(TaskStatus.TODO);
        }
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> list(TaskStatus status, Pageable pageable) {
        Page<Task> page = status == null
                ? taskRepository.findAll(pageable)
                : taskRepository.findByStatus(status, pageable);
        return PageResponse.from(page, TaskResponse::from);
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long id) {
        return TaskResponse.from(findTask(id));
    }

    @Transactional
    public TaskResponse update(Long id, TaskRequest request) {
        Task task = findTask(id);
        apply(task, request);
        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(Long id) {
        taskRepository.delete(findTask(id));
    }

    private Task findTask(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }

    private void apply(Task task, TaskRequest request) {
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setDueDate(request.dueDate());
        if (request.status() != null) {
            task.setStatus(request.status());
        }
    }
}
