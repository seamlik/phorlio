package phorlio;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum Opcode {

  ANNOUNCE((byte) 0),
  AUTHENTICATION((byte) 3),
  MAP((byte) 1),
  PEER((byte) 2);

  public static Map<Byte, Opcode> VALUES = Stream
      .of(Opcode.values())
      .collect(Collectors.toUnmodifiableMap(
          Opcode::getValue,
          it -> it
      ));
  private byte value;

  Opcode(final byte value) {
    this.value = value;
  }

  public byte getValue() {
    return value;
  }
}