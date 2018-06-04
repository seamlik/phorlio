package phorlio;

/**
 * Packet of PCP protocols.
 */
public abstract class Packet implements ToBytes {

  private final byte version;
  private final Opcode opcode;
  private final int lifetime;

  /**
   * Default constructor.
   */
  public Packet(final byte version, final Opcode opcode, final int lifetime) {
    this.version = version;
    this.opcode = opcode;
    this.lifetime = lifetime;
  }

  public byte getVersion() {
    return version;
  }

  public int getLifetime() {
    return lifetime;
  }

  public Opcode getOpcode() {
    return opcode;
  }
}