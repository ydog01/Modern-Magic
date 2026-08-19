package top.ydog01.mmagic.spell;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public final class SpellGraph {
    
    public static final String KEY = "spell";
    
    private final Map<UUID, SpellNode> nodes = new LinkedHashMap<>();
    private final Map<UUID, Vec2> menuPositions = new HashMap<>();
    private UUID startId;
    
    public SpellNode start() {
        return nodes.get(startId);
    }
    
    public SpellNode getNode(UUID id) {
        return nodes.get(id);
    }
    
    public Collection<SpellNode> nodes() {
        return nodes.values();
    }
    
    public UUID startId() {
        return startId;
    }
    
    public boolean isStart(UUID id) {
        return id.equals(startId);
    }
    
    public SpellNode addNode(SpellNodeType type, UUID id, float menuX, float menuY) {
        SpellNode node = type.create(id);
        node.setPosition(Vec3.ZERO);
        nodes.put(id, node);
        menuPositions.put(id, new Vec2(menuX, menuY));
        return node;
    }

    public Vec2 getMenuPosition(UUID id) {
        return menuPositions.get(id);
    }

    public void setMenuPosition(UUID id, Vec2 pos) {
        menuPositions.put(id, pos);
    }
    
    public void removeNode(UUID id) {
        if (id.equals(startId)) return;
        nodes.remove(id);
        menuPositions.remove(id);
        for (SpellNode n : nodes.values()) {
            for (SpellNode.OutputPort port : n.getOutputs()) {
                if (port.isConnected()) {
                    SpellNode.Connection conn = port.getConnection();
                    if (conn.targetId.equals(id)) {
                        port.removeConnection();
                    }
                }
            }
        }
    }
    
    public boolean connect(UUID sourceId, int outPort, UUID targetId, int inPort) {
        if (sourceId.equals(targetId)) return false;
        SpellNode source = nodes.get(sourceId);
        SpellNode target = nodes.get(targetId);
        if (source == null || target == null) return false;
        if (outPort < 0 || outPort >= source.getOutputs().size()) return false;
        if (inPort < 0 || inPort >= target.getInputs().size()) return false;
        
        SpellNode.OutputPort out = source.getOutputs().get(outPort);
        if (out.isConnected()) {
            out.removeConnection();
        }
        
        out.setConnection(targetId, inPort);
        target.getInputs().get(inPort).connect(sourceId);
        return true;
    }
    
    public void disconnectInput(UUID targetId, int inPort) {
        SpellNode target = nodes.get(targetId);
        if (target == null) return;
        if (inPort < 0 || inPort >= target.getInputs().size()) return;
        
        UUID sourceId = target.getInputs().get(inPort).getSourceNodeId();
        if (sourceId != null) {
            SpellNode source = nodes.get(sourceId);
            if (source != null) {
                for (SpellNode.OutputPort port : source.getOutputs()) {
                    if (port.isConnected()) {
                        SpellNode.Connection conn = port.getConnection();
                        if (conn.targetId.equals(targetId)) {
                            port.removeConnection();
                            break;
                        }
                    }
                }
            }
        }
        target.getInputs().get(inPort).disconnect();
    }
    
    public void disconnectOutput(UUID sourceId, int outPort) {
        SpellNode source = nodes.get(sourceId);
        if (source == null) return;
        if (outPort < 0 || outPort >= source.getOutputs().size()) return;
        
        SpellNode.OutputPort port = source.getOutputs().get(outPort);
        if (port.isConnected()) {
            SpellNode.Connection conn = port.getConnection();
            SpellNode target = nodes.get(conn.targetId);
            if (target != null) {
                for (SpellNode.InputPort ip : target.getInputs()) {
                    if (ip.isConnected() && ip.getSourceNodeId().equals(sourceId)) {
                        ip.disconnect();
                        break;
                    }
                }
            }
            port.removeConnection();
        }
    }
    
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (SpellNode n : nodes.values()) {
            CompoundTag nt = new CompoundTag();
            nt.putUUID("uuid", n.getUuid());
            nt.putString("id", n.getId());
            nt.putString("type", n.getType().id().toString());
            nt.put("params", n.getParams());

            Vec2 menuPos = menuPositions.get(n.getUuid());
            if (menuPos != null) {
                nt.putFloat("menuX", menuPos.x);
                nt.putFloat("menuY", menuPos.y);
            }

            ListTag conns = new ListTag();
            for (SpellNode.OutputPort port : n.getOutputs()) {
                CompoundTag pt = new CompoundTag();
                ListTag portList = new ListTag();
                if (port.isConnected()) {
                    SpellNode.Connection c = port.getConnection();
                    CompoundTag ct = new CompoundTag();
                    ct.putUUID("target", c.targetId);
                    ct.putInt("port", c.targetPort);
                    portList.add(ct);
                }
                pt.put("connections", portList);
                conns.add(pt);
            }
            nt.put("outputs", conns);
            list.add(nt);
        }
        tag.put("nodes", list);
        if (startId != null) {
            tag.putUUID("start", startId);
        }
        return tag;
    }

    public static SpellGraph fromTag(CompoundTag tag) {
        SpellGraph graph = new SpellGraph();
        if (tag == null) {
            return createDefault();
        }
        ListTag list = tag.getList("nodes", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag nt = list.getCompound(i);
            UUID id = nt.getUUID("uuid");
            ResourceLocation typeId = ResourceLocation.parse(nt.getString("type"));
            SpellNodeType type = SpellRegistry.get(typeId);
            if (type == null)
                continue;
            SpellNode node = type.create(id);
            node.setParams(nt.getCompound("params"));

            if (nt.contains("menuX", Tag.TAG_FLOAT) && nt.contains("menuY", Tag.TAG_FLOAT)) {
                graph.menuPositions.put(id, new Vec2(nt.getFloat("menuX"), nt.getFloat("menuY")));
            }

            ListTag conns = nt.getList("outputs", Tag.TAG_COMPOUND);
            for (int p = 0; p < conns.size() && p < node.getOutputs().size(); p++) {
                ListTag portList = conns.getCompound(p).getList("connections", Tag.TAG_COMPOUND);
                if (!portList.isEmpty()) {
                    CompoundTag ct = portList.getCompound(0);
                    node.getOutputs().get(p).setConnection(ct.getUUID("target"), ct.getInt("port"));
                }
            }
            graph.nodes.put(id, node);
        }
        if (tag.hasUUID("start")) {
            graph.startId = tag.getUUID("start");
        }
        if (graph.startId == null || !graph.nodes.containsKey(graph.startId)) {
            return createDefault();
        }
        return graph;
    }

    public static SpellGraph createDefault() {
        SpellGraph graph = new SpellGraph();
        SpellNodeType startType = SpellRegistry.get(SpellRegistry.START_ID);
        if (startType == null) return graph;
        SpellNode start = startType.create(UUID.randomUUID());
        graph.startId = start.getUuid();
        graph.nodes.put(start.getUuid(), start);
        graph.menuPositions.put(start.getUuid(), new Vec2(0, 0));
        return graph;
    }
}