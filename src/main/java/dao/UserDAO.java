package dao;

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

import dao.exceptions.UserNotFoundException;
import models.TaskEntityManagerProvider;
import models.UserEntityManagerProvider;
import models.userdb.User;

public class UserDAO {

	Logger logger = LoggerFactory.getLogger(UserDAO.class);

	@Inject
	UserEntityManagerProvider userEntityManagerProvider;

	// [multidb]: Not used in this class, but this shows that you can inject more than one providers in the same DAO class
	@Inject
	TaskEntityManagerProvider taskEntityManagerProvider;

	@Transactional
	public List<User> listUsers() {
		EntityManager userEM = userEntityManagerProvider.get();
		TypedQuery<User> q = userEM.createQuery("SELECT x FROM User x ORDER BY x.id", User.class);
		return q.getResultList();
	}

	@Transactional
	public User get(String loginID) throws UserNotFoundException {
		EntityManager userEM = userEntityManagerProvider.get();

		TypedQuery<User> q = userEM.createQuery("SELECT x FROM User x WHERE x.id=:loginID", User.class);
		q.setParameter("loginID", loginID);
		try {
			return q.getSingleResult();
		} catch (NoResultException e) {
			throw new UserNotFoundException(loginID);
		}
	}

	@Transactional
	public User getOrCreate(String loginID) {
		EntityManager userEM = userEntityManagerProvider.get();

		TypedQuery<User> q = userEM.createQuery("SELECT x FROM User x WHERE x.id=:loginID", User.class);
		q.setParameter("loginID", loginID);
		try {
			return q.getSingleResult();
		} catch (NoResultException e) {
			User user = new User();
			user.id = loginID;
			user.createTime = new Date();
			userEM.persist(user);
			logger.info("getOrCreate: Created new user '{}'", loginID);
			return user;
		}
	}

	@Transactional
	public void updateUser(User user) {
		EntityManager userEM = userEntityManagerProvider.get();
		userEM.merge(user);
		logger.info("updateUser: User '{}' has been updated", user.id);
	}
}
