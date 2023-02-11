package Enums;

public enum Effects {
  AFTERBURNER(1),
  ASTEROIDFIELD(2),
  GASCLOUD(4),
  SUPERFOOD(8),
  SHIELD(16);

  public final Integer value;

  private Effects(Integer value) {
    this.value = value;
  }
}