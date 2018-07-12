package filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.inject.Inject;

import controllers.ApplicationController;
import dao.UserDAO;
import dao.exceptions.UserNotFoundException;
import models.userdb.User;
import ninja.Context;
import ninja.Filter;
import ninja.FilterChain;
import ninja.Result;
import ninja.ReverseRouter;
import ninja.session.Session;

public class SessionFilter implements Filter {

	public static final String LOGIN_ID = "loginID";
	public static final String CONTEXT_USER = "user";

	final Logger logger = LoggerFactory.getLogger(SessionFilter.class);

	@Inject
	ReverseRouter reverseRouter;

	@Inject
	UserDAO userDAO;

	@Override
	public Result filter(FilterChain chain, Context context) {
		String loginID = null;

		Session session = context.getSession();
		if (session!=null) {
			loginID = session.get(LOGIN_ID);
		}

		if (!Strings.isNullOrEmpty(loginID)) {
			try {
				User user = userDAO.get(loginID);
				context.setAttribute(CONTEXT_USER, user);

				return chain.next(context);
			} catch (UserNotFoundException e) {
				session.clear();
				logger.warn("filter: Cannot locate loginID '{}' although it is specified in the session cookie", loginID);

				return reverseRouter.with(ApplicationController::indexPage).redirect();
			}
		} else {
			logger.warn("filter: No account for current session. Redirecting to login page...");
			return reverseRouter.with(ApplicationController::indexPage).redirect();
		}
	}
}
