package datameer.awstasks.ant.emr.model;

import com.amazonaws.services.elasticmapreduce.model.BootstrapActionConfig;
import com.amazonaws.services.elasticmapreduce.model.ScriptBootstrapActionConfig;

public class BootstrapConfig {

    private String _name;
    private String _path;

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    public String getPath() {
        return _path;
    }

    public void setPath(String path) {
        _path = path;
    }

    public BootstrapActionConfig createBootstrapActionConfig() {
        return new BootstrapActionConfig().withName(_name).withScriptBootstrapAction(new ScriptBootstrapActionConfig().withPath(_path));
    }
}
