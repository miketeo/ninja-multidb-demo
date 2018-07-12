package conf;

import controllers.ApplicationController;
import controllers.TaskAPIController;
import ninja.AssetsController;
import ninja.Router;
import ninja.application.ApplicationRoutes;

public class Routes implements ApplicationRoutes {

	@Override
	public void init(Router router) {
		router.GET().route("/").with(ApplicationController::indexPage);
		router.GET().route("/tasks").with(ApplicationController::tasksPage);
		router.GET().route("/simulateFetchingUserModel/").with(ApplicationController::simulateFetchingUserModelPage);
		router.GET().route("/logout/").with(ApplicationController::logout);
		router.POST().route("/login/").with(ApplicationController::login);

		router.POST().route("/api/tasks").with(TaskAPIController::createTask);
		router.DELETE().route("/api/tasks/{id}").with(TaskAPIController::deleteTask);

		router.GET().route("/assets/{fileName: .*}").with(AssetsController::serveStatic);
	}

}
