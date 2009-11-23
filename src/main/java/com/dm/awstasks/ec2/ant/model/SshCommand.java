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

    public int[] compileTargetInstances() {
        int[] targetInstances;
        if (_targetInstances.contains(",")) {
            String[] split = _targetInstances.split(",");
            targetInstances = new int[_targetInstances.length()];
            for (int i = 0; i < split.length; i++) {
                targetInstances[0] = Integer.parseInt(split[i]);
            }
        } else if (_targetInstances.contains("-")) {
            String[] split = _targetInstances.split("-");
            int min = Integer.parseInt(split[0]);
            int max = Integer.parseInt(split[1]);
            targetInstances = new int[max - min];
            for (int i = 0; i < targetInstances.length; i++) {
                targetInstances[i] = min + i;
            }
        } else {
            targetInstances = new int[1];
            targetInstances[0] = Integer.parseInt(_targetInstances);
        }
        return targetInstances;
    }

}
