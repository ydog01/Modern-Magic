package top.ydog01.mmagic.spell;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SpellGraph {
    public static final String KEY = "spell";

    private final Map<UUID, SpellNode> nodes = new LinkedHashMap<>();
    private UUID startId;

    public SpellNode start() {
        return nodes.get(startId);
    }

    public SpellNode node(UUID id) {
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

    public SpellNode addNode(SpellNodeType type, UUID id, float x, float y) {
        SpellNode node = type.create(id);
        node.setPosition(x, y);
        nodes.put(id, node);
        return node;
    }

    public void removeNode(UUID id) {
        if (id.equals(startId)) {
            return;
        }
        nodes.remove(id);
        for (SpellNode n : nodes.values()) {
            for (List<SpellNode.Connection> list : n.allOutputs()) {
                list.removeIf(c -> c.targetId.equals(id));
            }
        }
    }

    public boolean connect(UUID sourceId, int outPort, UUID targetId, int inPort) {
        if (sourceId.equals(targetId)) {
            return false;
        }
        SpellNode source = nodes.get(sourceId);
        SpellNode target = nodes.get(targetId);
        if (source == null || target == null) {
            return false;
        }
        if (outPort < 0 || outPort >= source.outputCount()) {
            return false;
        }
        if (inPort < 0 || inPort >= target.inputCount()) {
            return false;
        }

        if (!source.outputs(outPort).isEmpty()) {
            return false;
        }
        disconnectInput(targetId, inPort);
        source.outputs(outPort).add(new SpellNode.Connection(targetId, inPort));
        return true;
    }

    public void disconnectInput(UUID targetId, int inPort) {
        for (SpellNode n : nodes.values()) {
            for (List<SpellNode.Connection> list : n.allOutputs()) {
                list.removeIf(c -> c.targetId.equals(targetId) && c.targetInputPort == inPort);
            }
        }
    }

    public void disconnectOutput(UUID sourceId, int outPort) {
        SpellNode n = nodes.get(sourceId);
        if (n != null) {
            n.outputs(outPort).clear();
        }
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (SpellNode n : nodes.values()) {
            CompoundTag nt = new CompoundTag();
            nt.putUUID("id", n.id());
            nt.putString("type", n.type().id().toString());
            nt.putFloat("x", n.x());
            nt.putFloat("y", n.y());
            ListTag conns = new ListTag();
            for (int p = 0; p < n.outputCount(); p++) {
                CompoundTag port = new CompoundTag();
                ListTag portList = new ListTag();
                for (SpellNode.Connection c : n.outputs(p)) {
                    CompoundTag ct = new CompoundTag();
                    ct.putUUID("target", c.targetId);
                    ct.putInt("port", c.targetInputPort);
                    portList.add(ct);
                }
                port.put("list", portList);
                conns.add(port);
            }
            nt.put("outputs", conns);
            nt.put("params", n.params());
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
            return graph;
        }
        ListTag list = tag.getList("nodes", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag nt = list.getCompound(i);
            UUID id = nt.getUUID("id");
            SpellNodeType type = SpellRegistry.get(ResourceLocation.parse(nt.getString("type")));
            if (type == null) {
                continue;
            }
            SpellNode node = type.create(id);
            node.setPosition(nt.getFloat("x"), nt.getFloat("y"));
            if (nt.contains("params", Tag.TAG_COMPOUND)) {
                node.setParams(nt.getCompound("params"));
            }
            ListTag conns = nt.getList("outputs", Tag.TAG_COMPOUND);
            for (int p = 0; p < conns.size() && p < node.outputCount(); p++) {
                ListTag portList = conns.getCompound(p).getList("list", Tag.TAG_COMPOUND);
                for (int j = 0; j < portList.size(); j++) {
                    CompoundTag ct = portList.getCompound(j);
                    node.outputs(p).add(new SpellNode.Connection(ct.getUUID("target"), ct.getInt("port")));
                }
            }
            graph.nodes.put(id, node);
        }
        if (tag.hasUUID("start")) {
            graph.startId = tag.getUUID("start");
        }
        if (graph.startId == null || !graph.nodes.containsKey(graph.startId)) {
            SpellNode start = SpellRegistry.get(SpellRegistry.START_ID).create(UUID.randomUUID());
            start.setPosition(20, 40);
            graph.startId = start.id();
            graph.nodes.put(start.id(), start);
        }
        return graph;
    }

    public static SpellGraph createDefault() {
        SpellGraph graph = new SpellGraph();
        SpellNode start = SpellRegistry.get(SpellRegistry.START_ID).create(UUID.randomUUID());
        start.setPosition(20, 40);
        graph.startId = start.id();
        graph.nodes.put(start.id(), start);
        return graph;
    }
}
