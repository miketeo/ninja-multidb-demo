package dao.exceptions;

public class TaskNotFoundException extends Exception {

	private static final long serialVersionUID = 1L;

	public int taskID;

	public TaskNotFoundException(int taskID) {
		this.taskID = taskID;
	}
}
