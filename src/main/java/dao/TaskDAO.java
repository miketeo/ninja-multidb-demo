package dao;

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import dao.exceptions.TaskNotFoundException;
import models.TaskEntityManagerProvider;
import models.UserEntityManagerProvider;
import models.taskdb.Task;
import models.userdb.User;

public class TaskDAO {

	Logger logger = LoggerFactory.getLogger(TaskDAO.class);

	@Inject
	TaskEntityManagerProvider taskEntityManagerProvider;

	// [multidb]: Not used in this class, but this shows that you can inject more than one providers in the same DAO class
	@Inject
	UserEntityManagerProvider userEntityManagerProvider;

	@Transactional
	public List<Task> listUncompletedTask() {
		EntityManager taskEM = taskEntityManagerProvider.get();

		TypedQuery<Task> q = taskEM.createQuery("SELECT x FROM Task x WHERE x.hasCompleted=false", Task.class);
		return q.getResultList();
	}


	@Transactional
	public void completeTask(Task task) {
		if (task.hasCompleted) {
			throw new IllegalArgumentException(String.format("completeTask: Task (id:%d) has already been completed", task.id));
		}

		EntityManager taskEM = taskEntityManagerProvider.get();
		task.hasCompleted = true;
		task.completionTime = new Date();
		taskEM.merge(task);
		logger.info("completeTask: Task (id:{} loginID:{}) is marked as completed", task.id, task.loginID);
	}

	@Transactional
	public Task getTask(int taskID) throws TaskNotFoundException {
		EntityManager entityManager = taskEntityManagerProvider.get();
		TypedQuery<Task> q = entityManager.createQuery("SELECT x FROM Task x WHERE x.id=:id", Task.class);
		q.setParameter("id", taskID);
		try {
			return q.getSingleResult();
		} catch (NoResultException e) {
			throw new TaskNotFoundException(taskID);
		}
	}

	@Transactional
	public List<Task> listTasksForUser(User user) {
		if (user==null) {
			throw new IllegalArgumentException("listTasksForUser: user parameter cannot be null");
		}

		EntityManager entityManager = taskEntityManagerProvider.get();
		TypedQuery<Task> q = entityManager.createQuery("SELECT x FROM Task x WHERE x.loginID=:loginID ORDER BY x.createTime DESC", Task.class);
		q.setParameter("loginID", user.id);
		return q.getResultList();
	}

	@Transactional
	public Task createTaskForUser(User user, String description) {
		if (user==null) {
			throw new IllegalArgumentException("createTaskForUser: user parameter cannot be null");
		}
		if (Strings.isNullOrEmpty(description)) {
			throw new IllegalArgumentException("createTaskForUser: description parameter cannot be null/empty");
		}

		EntityManager entityManager = taskEntityManagerProvider.get();
		Task task = new Task();
		task.loginID = user.id;
		task.description = description;
		task.createTime = new Date();
		entityManager.persist(task);

		logger.info("createTaskForUser: Task (id:{}) created for account '{}'", task.id, task.loginID);
		return task;
	}

	@Transactional
	public void deleteTask(Task task) {
		EntityManager entityManager = taskEntityManagerProvider.get();
		entityManager.remove(task);
		logger.info("deleteTask: Task (id:{} loginID:{}) deleted", task.id, task.loginID);
	}

	@Transactional
	public List<User> simulateFetchingUserModel() {
		EntityManager entityManager = taskEntityManagerProvider.get();

		// This query will fail as User model is in the getModelPackages() returned by TaskEntityManagerProvider.
		TypedQuery<User> q = entityManager.createQuery("SELECT x FROM User x", User.class);
		return q.getResultList();
	}
}
