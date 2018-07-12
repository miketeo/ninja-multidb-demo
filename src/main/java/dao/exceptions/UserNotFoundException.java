package dao.exceptions;

public class UserNotFoundException extends Exception {

	private static final long serialVersionUID = 1L;

	public String loginID;

	public UserNotFoundException(String loginID) {
		super();
		this.loginID = loginID;
	}
}
