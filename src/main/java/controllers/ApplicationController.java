
package controllers;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import dao.TaskDAO;
import dao.UserDAO;
import filters.SessionFilter;
import models.taskdb.Task;
import models.userdb.User;
import ninja.Context;
import ninja.FilterWith;
import ninja.Result;
import ninja.Results;
import ninja.ReverseRouter;
import ninja.params.Param;
import ninja.session.Session;

public class ApplicationController {

	@Inject
	ReverseRouter reverseRouter;

	@Inject
	UserDAO userDAO;

	@Inject
	TaskDAO taskDAO;

	final Logger logger = LoggerFactory.getLogger(ApplicationController.class);

	public Result indexPage(Context context) {
		return Results.html().doNotCacheContent();
	}

	public Result login(Context context, Session session, @Param("loginID") String loginID) {

		User user = userDAO.getOrCreate(loginID);
		user.lastLoginTime = new Date();
		userDAO.updateUser(user);
		logger.info("login: User '{}' has signed in", user.id);

		session.put(SessionFilter.LOGIN_ID, loginID);
		return reverseRouter.with(ApplicationController::tasksPage).redirect().doNotCacheContent();
	}

	public Result logout(Context context, Session session) {
		session.clear();
		return reverseRouter.with(ApplicationController::indexPage).redirect().doNotCacheContent();
	}

	@FilterWith(SessionFilter.class)
	public Result tasksPage(Context context) {
		User account = context.getAttribute(SessionFilter.CONTEXT_USER, User.class);
		List<Task> tasks = taskDAO.listTasksForUser(account);

		return Results.html().doNotCacheContent().render("tasks", tasks);
	}

	@FilterWith(SessionFilter.class)
	public Result simulateFetchingUserModelPage(Context context) {
		List<User> users = taskDAO.simulateFetchingUserModel();
		return Results.ok().html().doNotCacheContent().render("users", users);
	}
}
