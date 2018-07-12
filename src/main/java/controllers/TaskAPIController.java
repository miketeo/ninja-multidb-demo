package controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import dao.TaskDAO;
import dao.exceptions.TaskNotFoundException;
import filters.SessionFilter;
import models.taskdb.Task;
import models.userdb.User;
import ninja.Context;
import ninja.FilterWith;
import ninja.Result;
import ninja.Results;
import ninja.params.Param;
import ninja.params.PathParam;

public class TaskAPIController {

	@Inject
	TaskDAO taskDAO;

	final Logger logger = LoggerFactory.getLogger(TaskAPIController.class);

	@FilterWith(SessionFilter.class)
	public Result createTask(Context context, @Param("description") String description) {
		User user = context.getAttribute(SessionFilter.CONTEXT_USER, User.class);
		Task task = taskDAO.createTaskForUser(user, description);

		return Results.ok().json().render("task", task);
	}

	@FilterWith(SessionFilter.class)
	public Result deleteTask(Context context, @PathParam("id") Integer taskID) {
		User user = context.getAttribute(SessionFilter.CONTEXT_USER, User.class);
		if (taskID!=null) {
			try {
				Task task = taskDAO.getTask(taskID);
				if (task.loginID.equals(user.id)) {
					taskDAO.deleteTask(task);
				}
			} catch (TaskNotFoundException e) {
				// Ignore if task is not found. Assume deletion is successful.
			}
		}
		return Results.noContent();
	}
}
