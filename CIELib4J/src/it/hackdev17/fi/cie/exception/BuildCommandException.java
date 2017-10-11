package it.hackdev17.fi.cie.exception;

/**
 * Raised if an error occurs while building an APDU command
 * @author Michele
 *
 */
public class BuildCommandException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 97847874327318127L;

	public BuildCommandException() {
	}

	public BuildCommandException(String message) {
		super(message);
	}

	public BuildCommandException(Throwable cause) {
		super(cause);
	}

	public BuildCommandException(String message, Throwable cause) {
		super(message, cause);
	}

	public BuildCommandException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
