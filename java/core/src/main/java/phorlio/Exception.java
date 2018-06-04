package phorlio;

public class Exception extends java.lang.Exception {

  private final byte resultCode;

  public Exception(final byte resultCode) {
    this.resultCode = resultCode;
  }

  public Exception(final String message, final byte resultCode) {
    super(message);
    this.resultCode = resultCode;
  }

  public Exception(final String message, final Throwable cause, final byte resultCode) {
    super(message, cause);
    this.resultCode = resultCode;
  }

  public Exception(final Throwable cause, byte resultCode) {
    super(cause);
    this.resultCode = resultCode;
  }

  public byte getResultCode() {
    return resultCode;
  }

  @Override
  public String toString() {
    return String.format("[%1s] %2s", resultCode, super.toString());
  }
}