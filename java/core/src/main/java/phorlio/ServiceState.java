package phorlio;

/**
 * Generalized states of a network service.
 */
public enum ServiceState {

  /**
   * Has stopped and all system resources are released. Will not be re-usable once
   * reaching this state.
   */
  CLOSED,

  /**
   * Newly created before being initialized.
   */
  CREATED,

  /**
   * Initializing.
   */
  INITIALIZING,

  /**
   * Up and running.
   */
  RUNNING
}
