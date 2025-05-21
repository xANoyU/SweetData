package top.mrxiaom.sweetdata.func;
        
import top.mrxiaom.sweetdata.SweetData;

@SuppressWarnings({"unused"})
public abstract class AbstractPluginHolder extends top.mrxiaom.pluginbase.func.AbstractPluginHolder<SweetData> {
    public AbstractPluginHolder(SweetData plugin) {
        super(plugin);
    }

    public AbstractPluginHolder(SweetData plugin, boolean register) {
        super(plugin, register);
    }
}
