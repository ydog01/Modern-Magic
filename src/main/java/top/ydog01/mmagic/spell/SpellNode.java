package top.ydog01.mmagic.spell;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public abstract class SpellNode {
    //need to set
    private UUID uuid;
    private final String id;
    private final SpellNodeType type;
    private Vec3 position = Vec3.ZERO;
    private Vec3 velocity = Vec3.ZERO;
    private final List<InputPort> inputs = new ArrayList<>();
    private final List<OutputPort> outputs = new ArrayList<>();
    private final CompoundTag params = new CompoundTag();
    private final List<UUID> modifyingNodes = new ArrayList<>();
    
    public SpellNode(String id, SpellNodeType type) {
        this.id = id;
        this.type = type;
        for (int i = 0; i < type.inputCount(); i++) {
            inputs.add(new InputPort(i));
        }
        for (int i = 0; i < type.outputCount(); i++) {
            outputs.add(new OutputPort(i));
        }
    }
    
    public int paramInt(String key) { return params.getInt(key); }
    public float paramFloat(String key) { return params.getFloat(key); }
    public boolean paramBool(String key) { return params.getBoolean(key); }
    public void setParam(String key, int value) { params.putInt(key, value); }
    public void setParam(String key, float value) { params.putFloat(key, value); }
    public void setParam(String key, boolean value) { params.putBoolean(key, value); }
    public CompoundTag getParams() { return params; }
    public void setParams(CompoundTag tag) { 
        if (tag != null) {
            for (String key : tag.getAllKeys()) {
                params.put(key, tag.get(key));
            }
        }
    }
    
    public Vec3 getPosition() { return position; }
    public Vec3 getVelocity() { return velocity; }
    public void setPosition(Vec3 pos) { this.position = pos; }
    public void setVelocity(Vec3 vel) { this.velocity = vel; }
    
    public static class InputPort {
        public final int index;
        private UUID sourceNodeId;

        public InputPort(int index) {
            this.index = index;
        }

        public boolean isConnected() {
            return sourceNodeId != null;
        }

        public UUID getSourceNodeId() {
            return sourceNodeId;
        }

        public void connect(UUID nodeId) {
            this.sourceNodeId = nodeId;
        }

        public void disconnect() {
            this.sourceNodeId = null;
        }
    }

    public static class OutputPort {
        public final int index;
        private Connection connection;

        public OutputPort(int index) {
            this.index = index;
        }

        public boolean isConnected() {
            return connection != null;
        }

        public Connection getConnection() {
            return connection;
        }

        public void setConnection(UUID targetId, int targetPort) {
            this.connection = new Connection(targetId, targetPort);
        }

        public void removeConnection() {
            this.connection = null;
        }

        public void clearConnection() {
            this.connection = null;
        }
    }
    
    public static class Connection {
        public final UUID targetId;
        public final int targetPort;
        public Connection(UUID targetId, int targetPort) {
            this.targetId = targetId;
            this.targetPort = targetPort;
        }
    }
    
    public List<InputPort> getInputs() { return Collections.unmodifiableList(inputs); }
    public List<OutputPort> getOutputs() { return Collections.unmodifiableList(outputs); }
    
    public List<Connection> getConnections(int port) {
        if (port < 0 || port >= outputs.size()) return List.of();
        OutputPort out = outputs.get(port);
        if (out.isConnected()) {
            return List.of(out.getConnection());
        }
        return List.of();
    }
    
    public UUID getUuid() { return uuid; }
    
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
    
    public String getId() { return id; }
    public SpellNodeType getType() { return type; }
    
    public int getDelayTicks() { return type.delayTicks(); }
    public int getInputCount() { return type.inputCount(); }
    public int getOutputCount() { return type.outputCount(); }
    
    public SpellNode tryModify(SpellNode target, SpellContext ctx) {
        return target;
    }
    
    public boolean allowModification(SpellNode modifier) {
        return true;
    }
    
    public ExecutionResult onCollideEntity(Entity entity, SpellContext ctx) {
        return ExecutionResult.empty();
    }

    public ExecutionResult onCollideBlock(BlockPos pos, SpellContext ctx) {
        return ExecutionResult.empty();
    }
    
    public ExecutionResult onUpdate(SpellContext ctx) {
        return ExecutionResult.empty();
    }
    
    public abstract ExecutionResult execute(SpellContext ctx);
    
    public void addModifierNode(UUID nodeId) {
        modifyingNodes.add(nodeId);
    }
    
    public void removeModifierNode(UUID nodeId) {
        modifyingNodes.remove(nodeId);
    }
    
    public List<UUID> getModifierNodes() {
        return Collections.unmodifiableList(modifyingNodes);
    }

    public int getManaCost() {
        return type.manaCost();
    }
    
    public Vec3 outputVelocity(Vec3 vel, SpellContext ctx) {
        return vel;
    }
    
    public Vec3 outputPosition(Vec3 at, SpellContext ctx) {
        return at;
    }

    public abstract ExecutionResult tick(SpellContext ctx);
    
    public float outputDamageMult(float in, SpellContext ctx) {
        return in;
    }
    
    public float outputSpeedMult(float in, SpellContext ctx) {
        return in;
    }
}