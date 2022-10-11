package com.llamalad7.mixinextras.utils;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes;

import java.util.*;
import java.util.stream.Stream;

public class InjectorUtils {
    public static boolean isVirtualRedirect(InjectionNodes.InjectionNode node) {
        return node.isReplaced() && node.hasDecoration("redirector") && node.getCurrentTarget().getOpcode() != Opcodes.INVOKESTATIC;
    }

    public static boolean exitsMethod(AbstractInsnNode node) {
        return node.getOpcode() >= Opcodes.IRETURN && node.getOpcode() <= Opcodes.RETURN || node.getOpcode() == Opcodes.ATHROW;
    }

    /**
     * Checks whether all paths following the node exit the method and whether all loaded variables
     * in all paths following the node are initialized. If that is case, the node can be removed or
     * conditionally surpassed and the method will still have valid bytecode, making the exit optional.
     */
    public static boolean isExitOptional(AbstractInsnNode node, int methodArgumentCount) {
        Set<Integer> unassignedVariables = new HashSet<>();
        Set<Integer> assignedVariables = new HashSet<>();
        // We add already initialized method parameters and 'this'
        for(int i = 0; i <= methodArgumentCount; i++) {
            assignedVariables.add(i);
        }
        boolean exits = checkAllFollowingPathsExitingAndFindLocals(node, new HashSet<>(64), true, false, assignedVariables, unassignedVariables);
        if(!exits || unassignedVariables.size() == 0) {
            return exits;
        }
        // We have to check whether all variables in unassignedVariables are assigned in all paths leading up to the node
        return areVariablesInitialized(node, unassignedVariables);
    }

    /**
     * Checks whether all paths following the node exit the method, adds all variables, that are assigned in the paths
     * following the node, to {@code assignedVariables} and adds all variables that are used but not assigned to {@code unassignedVariables}
     * @param node The method exiting node to check
     * @param visitedNodes All nodes already visited by this method that don't have to be visited again
     * @param defaultWhenAlreadyVisited The value this method should return upon reaching an already visited node
     * @param stopAtRet Whether this method should stop upon reaching {@link Opcodes#RET} and return false.
     * @param assignedVariables Set to add all variables to, that are assigned in the immediate path following the node.
     *                          Variables that are only assigned in any conditional branches are not added
     * @param unassignedVariables Set to add all variables to, that are used but not previously assigned in the paths following the node
     */
    private static boolean checkAllFollowingPathsExitingAndFindLocals(
            AbstractInsnNode node,
            HashSet<AbstractInsnNode> visitedNodes,
            boolean defaultWhenAlreadyVisited,
            boolean stopAtRet,
            Set<Integer> assignedVariables,
            Set<Integer> unassignedVariables) {
        if(!visitedNodes.add(node)) {
            return defaultWhenAlreadyVisited;
        }
        node = node.getNext();
        if(node == null) {
            return false;
        }
        while(!exitsMethod(node)) {
            if(!visitedNodes.add(node)) {
                return defaultWhenAlreadyVisited;
            }
            if(stopAtRet && node.getOpcode() == Opcodes.RET) {
                return false;
            }
            if(node instanceof JumpInsnNode) {
                JumpInsnNode jumpInsnNode = (JumpInsnNode) node;
                if(jumpInsnNode.getOpcode() == Opcodes.GOTO) {
                    node = jumpInsnNode.label;
                    if(!visitedNodes.add(node)) {
                        return defaultWhenAlreadyVisited;
                    }
                } else if(jumpInsnNode.getOpcode() == Opcodes.JSR) {
                    if(checkAllFollowingPathsExitingAndFindLocals(jumpInsnNode.label, visitedNodes, false, true, assignedVariables, unassignedVariables)) {
                        return true; // We know that the subroutine always exits
                    }
                } else if(!checkAllFollowingPathsExitingAndFindLocals(jumpInsnNode.label, visitedNodes, true, stopAtRet, assignedVariables, new HashSet<>(unassignedVariables))) {
                    return false; // We know that the conditional jump doesn't always return
                }
            } else if(node instanceof TableSwitchInsnNode) {
                TableSwitchInsnNode tableSwitchInsnNode = (TableSwitchInsnNode) node;
                for(LabelNode labelNode : tableSwitchInsnNode.labels) {
                    if (!checkAllFollowingPathsExitingAndFindLocals(labelNode, visitedNodes, true, stopAtRet, assignedVariables, new HashSet<>(unassignedVariables))) {
                        return false;
                    }
                }
                return checkAllFollowingPathsExitingAndFindLocals(tableSwitchInsnNode.dflt, visitedNodes, true, stopAtRet, assignedVariables, new HashSet<>(unassignedVariables));
            } else if(node instanceof LookupSwitchInsnNode) {
                LookupSwitchInsnNode lookupSwitchInsnNode = (LookupSwitchInsnNode) node;
                for(LabelNode labelNode : lookupSwitchInsnNode.labels) {
                    if (!checkAllFollowingPathsExitingAndFindLocals(labelNode, visitedNodes, true, stopAtRet, assignedVariables, new HashSet<>(unassignedVariables))) {
                        return false;
                    }
                }
                return checkAllFollowingPathsExitingAndFindLocals(lookupSwitchInsnNode.dflt, visitedNodes, true, stopAtRet, assignedVariables, new HashSet<>(unassignedVariables));
            } else if(node instanceof VarInsnNode) {
                int var = ((VarInsnNode) node).var;
                if(node.getOpcode() >= Opcodes.ISTORE && node.getOpcode() <= Opcodes.ASTORE) {
                    assignedVariables.add(var);
                } else if(node.getOpcode() >= Opcodes.ILOAD && node.getOpcode() <= Opcodes.ALOAD && !assignedVariables.contains(var)) {
                    unassignedVariables.add(var);
                }
            }
            node = node.getNext();
            if(node == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates a set consisting of all {@link JumpInsnNode}s, {@link LookupSwitchInsnNode}s and {@link TableSwitchInsnNode}s that jump to the specified target and
     * come after the given node or are the given node. The nodes can thus be reached through
     * {@link AbstractInsnNode#getNext()}
     * @param node The node to get all other nodes from using {@link AbstractInsnNode#getNext()}
     * @param target The label that the returned {@link JumpInsnNode}s should target
     */
    public static Set<AbstractInsnNode> getAllTargetingNodes(AbstractInsnNode node, LabelNode target) {

        // We don't need to create a new instance when no matching nodes are found
        Set<AbstractInsnNode> set = Collections.emptySet();

        while(node != null) {
            boolean isJumpingToTarget = false;
            if(node instanceof JumpInsnNode) {
                JumpInsnNode jumpInsnNode = (JumpInsnNode) node;
                isJumpingToTarget = jumpInsnNode.label == target;
            } else if(node instanceof TableSwitchInsnNode) {
                TableSwitchInsnNode tableSwitchInsnNode = (TableSwitchInsnNode) node;
                isJumpingToTarget = tableSwitchInsnNode.dflt == target || tableSwitchInsnNode.labels.contains(target);
            } else if(node instanceof LookupSwitchInsnNode) {
                LookupSwitchInsnNode lookupSwitchInsnNode = (LookupSwitchInsnNode) node;
                isJumpingToTarget = lookupSwitchInsnNode.dflt == target || lookupSwitchInsnNode.labels.contains(target);
            }
            if(isJumpingToTarget) {
                if(set.isEmpty()) {
                    set = new HashSet<>();
                }
                set.add(node);
            }
            node = node.getNext();
        }
        return set;
    }

    /**
     * Checks whether all given variables are initialized at the given node for every path that leads to the given node
     * @param node The node to check at
     * @param variables The variables to check for whether they are initialized. If variables are initialized
     *                  before the given node without jumps, they are removed from the set.
     * @return Whether all given variables are initialized
     */
    public static boolean areVariablesInitialized(AbstractInsnNode node, Set<Integer> variables) {

        // Find the first node to use in getAllTargetingNodes
        AbstractInsnNode first = node;
        while(first.getPrevious() != null) {
            first = first.getPrevious();
        }

        while(true) {
            if(node instanceof VarInsnNode) {
                if(node.getOpcode() >= Opcodes.ISTORE && node.getOpcode() <= Opcodes.ASTORE) {
                    VarInsnNode varInsnNode = (VarInsnNode) node;
                    variables.remove(varInsnNode.var);
                    if(variables.isEmpty()) {
                        return true;
                    }
                }
            } else if(node instanceof LabelNode) {
                LabelNode labelNode = (LabelNode) node;
                Set<AbstractInsnNode> targetingNodes = getAllTargetingNodes(first, labelNode);
                for(AbstractInsnNode targetingNode : targetingNodes) {
                    if(!areVariablesInitialized(targetingNode, new HashSet<>(variables))) {
                        return false;
                    }
                }
            } else if(node instanceof JumpInsnNode && node.getOpcode() == Opcodes.JSR) {
                variables = removeSubroutineAssignedVariables(((JumpInsnNode) node).label, new HashSet<>(variables));
                if(
                        variables == null
                        // We know that the subroutine always returns and
                        // that the current code will thus only be reached by
                        // jumps, which we already checked

                        || variables.isEmpty()
                        // We know that all variables left are initialized in the subroutine
                ) {
                    return true;
                }
            }

            node = node.getPrevious();
            if(node == null) {
                return false;
            }
            if(exitsMethod(node) || node instanceof JumpInsnNode && node.getOpcode() == Opcodes.GOTO) {
                // We know that the instruction won't continue to the
                // next instruction and that the current code will
                // thus only be reached by jumps, which we already checked
                return true;
            }
        }
    }

    /**
     * Returns a set of all variables from the given set that aren't always set in the
     * subroutine beginning at the given node. Variables that are set before any conditional jump,
     * will also be removed from the given set. If all paths lead to any RETURN instruction or end
     * at the end of the method (meaning there is no next node), {@code null} is returned
     * @param node The node in a subroutine to start at
     * @param variables A set of variables that will be checked for whether they are set in
     *                  the subroutine
     * @return The set of all variables from the given set that aren't always set in the subroutine
     */
    public static Set<Integer> removeSubroutineAssignedVariables(AbstractInsnNode node, Set<Integer> variables) {
        while(node != null) {
            if(exitsMethod(node)) {
                return null;
            }
            if(node.getOpcode() == Opcodes.RET) {
                return variables;
            }
            if(node instanceof VarInsnNode) {
                if(node.getOpcode() >= Opcodes.ISTORE && node.getOpcode() <= Opcodes.ASTORE) {
                    VarInsnNode varInsnNode = (VarInsnNode) node;
                    variables.remove(varInsnNode.var);
                }
            } else if(node instanceof JumpInsnNode) {
                JumpInsnNode jumpInsnNode = (JumpInsnNode) node;
                if(jumpInsnNode.getOpcode() == Opcodes.GOTO) {
                    node = jumpInsnNode.label;
                } else if(jumpInsnNode.getOpcode() == Opcodes.JSR) {
                    variables = removeSubroutineAssignedVariables(jumpInsnNode.label, variables);
                    if(variables == null) {
                        // We know that the nested subroutine always returns, so
                        // the current subroutine branch also does
                        return null;
                    }
                } else {
                    Set<Integer> first = removeSubroutineAssignedVariables(node.getNext(), new HashSet<>(variables));
                    Set<Integer> second = removeSubroutineAssignedVariables(jumpInsnNode.label, new HashSet<>(variables));
                    if(first == null) {
                        return second;
                    }
                    if(second == null) {
                        return first;
                    }
                    first.addAll(second);
                    return first;
                }
            } else if(node instanceof TableSwitchInsnNode) {
                TableSwitchInsnNode tableSwitchInsnNode = (TableSwitchInsnNode) node;
                Set<Integer> finalVariables = variables;
                return Stream.concat(tableSwitchInsnNode.labels.stream(), Stream.of(tableSwitchInsnNode.dflt))
                        .map(label -> removeSubroutineAssignedVariables(label, new HashSet<>(finalVariables)))
                        .reduce(null, (first, second) -> {
                            if(first == null) {
                                return second;
                            }
                            if(second == null) {
                                return first;
                            }
                            first.addAll(second);
                            return first;
                        });
            } else if(node instanceof LookupSwitchInsnNode) {
                LookupSwitchInsnNode tableSwitchInsnNode = (LookupSwitchInsnNode) node;
                Set<Integer> finalVariables = variables;
                return Stream.concat(tableSwitchInsnNode.labels.stream(), Stream.of(tableSwitchInsnNode.dflt))
                        .map(label -> removeSubroutineAssignedVariables(label, new HashSet<>(finalVariables)))
                        .reduce(null, (first, second) -> {
                            if(first == null) {
                                return second;
                            }
                            if(second == null) {
                                return first;
                            }
                            first.addAll(second);
                            return first;
                        });
            }

            node = node.getNext();
        }

        // Should never happen with valid bytecode, but we return null,
        // because no RET instruction was reached
        return null;
    }
}
