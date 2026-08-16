package top.ydog01.mmagic.spell;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public abstract class SpellNode {
    private UUID id;
    private SpellNodeType type;
    private float x;
    private float y;
    private CompoundTag params = new CompoundTag();
    private final List<List<Connection>> outputs = new ArrayList<>();

    void init(UUID id, SpellNodeType type) {
        this.id = id;
        this.type = type;
        this.params = new CompoundTag();
        this.outputs.clear();
        for (int i = 0; i < type.outputCount(); i++) {
            this.outputs.add(new ArrayList<>());
        }
    }

    public UUID id() {
        return id;
    }

    public SpellNodeType type() {
        return type;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public int inputCount() {
        return type.inputCount();
    }

    public int outputCount() {
        return type.outputCount();
    }

    public List<List<Connection>> allOutputs() {
        return outputs;
    }

    public List<Connection> outputs(int port) {
        if (port < 0) {
            return Collections.emptyList();
        }

        while (outputs.size() <= port) {
            outputs.add(new ArrayList<>());
        }
        return outputs.get(port);
    }

    public CompoundTag params() {
        return params;
    }

    public void setParams(CompoundTag tag) {
        this.params = tag == null ? new CompoundTag() : tag;
    }

    public int paramInt(String key) {
        if (params.contains(key)) {
            return params.getInt(key);
        }
        NodeParameter p = paramSpec(key);
        return p == null ? 0 : Math.round(p.defaultValue());
    }

    public float paramFloat(String key) {
        if (params.contains(key)) {
            return params.getFloat(key);
        }
        NodeParameter p = paramSpec(key);
        return p == null ? 0f : p.defaultValue();
    }

    public boolean paramBool(String key) {
        if (params.contains(key)) {
            return params.getBoolean(key);
        }
        NodeParameter p = paramSpec(key);
        return p != null && p.defaultValue() >= 1f;
    }

    public void setParam(String key, int value) {
        params.putInt(key, value);
    }

    public void setParam(String key, float value) {
        params.putFloat(key, value);
    }

    public void setParam(String key, boolean value) {
        params.putBoolean(key, value);
    }

    private NodeParameter paramSpec(String key) {
        for (NodeParameter p : type.parameters()) {
            if (p.key().equals(key)) {
                return p;
            }
        }
        return null;
    }

    public int manaCost() {
        float cost = type.manaCost();
        for (NodeParameter p : type.parameters()) {
            float cur = switch (p.kind()) {
                case INT -> paramInt(p.key());
                case FLOAT -> paramFloat(p.key());
                case BOOL -> paramBool(p.key()) ? 1f : 0f;
            };
            float def = switch (p.kind()) {
                case BOOL -> p.defaultValue() >= 1f ? 1f : 0f;
                default -> p.defaultValue();
            };
            cost += (cur - def) * p.costPerUnit();
        }
        int result = Math.round(cost);
        return type.manaCost() > 0 ? Math.max(1, result) : Math.max(0, result);
    }

    public boolean keepVelocity() {
        return true;
    }

    public int delayTicks() {
        return type().delayTicks();
    }

    public Vec3 outputVelocity(Vec3 vel) {
        return vel;
    }

    public Vec3 outputPosition(Vec3 at) {
        return at;
    }

    public float outputDamageMult(float in) {
        return in;
    }

    public float outputSpeedMult(float in) {
        return in;
    }

    public SpellModifiers outputModifiers(SpellModifiers in) {
        return in;
    }

    public abstract List<Integer> execute(SpellContext ctx, Vec3 at, Vec3 vel, float damageMult, float speedMult,
                                          SpellModifiers mods);

    public static final class Connection {
        public final UUID targetId;
        public final int targetInputPort;

        public Connection(UUID targetId, int targetInputPort) {
            this.targetId = targetId;
            this.targetInputPort = targetInputPort;
        }
    }
}
