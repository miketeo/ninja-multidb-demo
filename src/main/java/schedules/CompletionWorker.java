package schedules;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dao.TaskDAO;
import models.taskdb.Task;
import multidbsources.MultiDBSources;
import ninja.scheduler.Schedule;

@Singleton
public class CompletionWorker {

	final Logger logger = LoggerFactory.getLogger(CompletionWorker.class);

	@Inject
	MultiDBSources multiDBSources;

	@Inject
	TaskDAO taskDAO;

	@Schedule(delay = 60, initialDelay = 30, timeUnit = TimeUnit.SECONDS)
	public void completeTasks() {
		multiDBSources.beginWorkUnit();
		try {
			Date cutoffTime = new Date(System.currentTimeMillis() - 30*1000); // Tasks that are created 30 secs ago will be marked as completed.
			List<Task> uncompletedTasks = taskDAO.listUncompletedTask();
			for (Task task : uncompletedTasks) {
				if (cutoffTime.after(task.createTime)) {
					taskDAO.completeTask(task);
				}
			}
		} finally {
			multiDBSources.endWorkUnit();
		}
	}

}
