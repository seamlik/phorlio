package phorlio;

import java.nio.ByteBuffer;

/**
 * PCP response.
 */
public class Response extends Packet {

  private final int epoch;
  private final byte resultCode;

  public static Response fromBytes(final byte[] bytes) throws Exception {
    final var reader = ByteBuffer.wrap(bytes);

    final var version = reader.get();
    if (version != Constants.VERSION) {
      throw new Exception("Version not supported: " + (short) version, (byte) 1);
    }

    final var opcodeStanza = reader.get();
    if ((opcodeStanza & (byte) 0b10000000) == 0) {
      throw new Exception("Packet is not a response", (byte) 3);
    }
    final var opcode = Opcode.VALUES.get((byte) (opcodeStanza & (byte) 0b01111111));

    reader.get();

    final var resultCode = reader.get();
    final var lifetime = reader.getInt();
    final var epoch = reader.getInt();

    return new Response(version, opcode, lifetime, epoch, resultCode);
  }

  /**
   * Default constructor.
   */
  public Response(final byte version,
                  final Opcode opcode,
                  final int lifetime,
                  final int epoch,
                  final byte resultCode) {
    super(version, opcode, lifetime);
    this.epoch = epoch;
    this.resultCode = resultCode;
  }

  public int getEpoch() {
    return epoch;
  }

  public byte getResultCode() {
    return resultCode;
  }

  @Override
  public byte[] toBytes() {
    final var bytes = ByteBuffer.allocate(getLength());
    bytes.put(Constants.VERSION);
    bytes.put((byte) (getOpcode().getValue() | (byte) 0b10000000));
    bytes.put(resultCode);
    bytes.putInt(getLifetime());
    bytes.putInt(epoch);
    bytes.put(new byte[96]);
    // TODO: Support Opcode data and options
    return bytes.array();
  }

  @Override
  public int getLength() {
    return 32 * 6;
    // TODO: Support Opcode data and options
  }
}