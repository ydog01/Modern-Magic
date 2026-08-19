package top.ydog01.mmagic.spell.casting;

import java.util.ArrayList;
import java.util.List;

public class ExecutionResult {
    
    private final List<Integer> outputPorts;
    private final boolean terminate;
    private final boolean shouldClone;
    private final int cloneCount;
    private final boolean keepAlive;
    
    private ExecutionResult(List<Integer> outputPorts, boolean terminate, boolean shouldClone, int cloneCount, boolean keepAlive) {
        this.outputPorts = outputPorts;
        this.terminate = terminate;
        this.shouldClone = shouldClone;
        this.cloneCount = cloneCount;
        this.keepAlive = keepAlive;
    }
    
    public static ExecutionResult continueTo(int... ports) {
        return new ExecutionResult(listOf(ports), false, false, 0, false);
    }
    
    public static ExecutionResult continueToAlive(int... ports) {
        return new ExecutionResult(listOf(ports), false, false, 0, true);
    }
    
    public static ExecutionResult terminate() {
        return new ExecutionResult(List.of(), true, false, 0, false);
    }
    
    public static ExecutionResult empty() {
        return new ExecutionResult(List.of(), false, false, 0, true);
    }
    
    public static ExecutionResult cloneTo(int count, int... ports) {
        return new ExecutionResult(listOf(ports), false, true, count, false);
    }
    
    public static ExecutionResult cloneToAlive(int count, int... ports) {
        return new ExecutionResult(listOf(ports), false, true, count, true);
    }
    
    private static List<Integer> listOf(int... ports) {
        List<Integer> list = new ArrayList<>();
        for (int p : ports) list.add(p);
        return list;
    }

    public List<Integer> getOutputPorts() { return outputPorts; }
    public boolean isTerminate() { return terminate; }
    public boolean shouldClone() { return shouldClone; }
    public int getCloneCount() { return cloneCount; }
    public boolean isKeepAlive() { return keepAlive; }
    public boolean shouldRemove() { return !keepAlive && !terminate; }
}