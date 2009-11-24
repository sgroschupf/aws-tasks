package com.dm.awstasks.ec2.ant.model;

public abstract class SshCommand {

    private String _targetInstances;

    public void setTargetInstances(String targetInstances) {
        _targetInstances = targetInstances;
    }

    public String getTargetInstances() {
        return _targetInstances;
    }

    public boolean isToAllInstances() {
        return _targetInstances == null || _targetInstances.trim().equals("all");
    }

    public int[] compileTargetInstances(int instanceCount) {
        int[] targetInstances;
        _targetInstances = _targetInstances.replaceAll("n", Integer.toString(instanceCount - 1));
        if (_targetInstances.contains(",")) {
            String[] split = _targetInstances.split(",");
            targetInstances = new int[split.length];
            for (int i = 0; i < split.length; i++) {
                targetInstances[i] = Integer.parseInt(split[i].trim());
            }
        } else if (_targetInstances.contains("-")) {
            String[] split = _targetInstances.split("-");
            int min = Integer.parseInt(split[0]);
            int max = Integer.parseInt(split[1]);
            targetInstances = new int[max - min + 1];
            for (int i = 0; i < targetInstances.length; i++) {
                targetInstances[i] = min + i;
            }
        } else {
            targetInstances = new int[1];
            targetInstances[0] = Integer.parseInt(_targetInstances);
        }

        // check validness
        for (int index : targetInstances) {
            if (index >= instanceCount) {
                throw new IllegalArgumentException("specified '" + index + "' as instance index, but max index is '" + (instanceCount - 1) + "'");
            }
        }
        return targetInstances;
    }
}
