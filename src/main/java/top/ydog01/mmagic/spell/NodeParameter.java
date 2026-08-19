package top.ydog01.mmagic.spell;

public record NodeParameter(
        String key,
        Kind kind,
        float min,
        float max,
        float step,
        float defaultValue,
        String labelKey,
        float costPerUnit
) {
    public enum Kind {
        INT, FLOAT, BOOL
    }
}