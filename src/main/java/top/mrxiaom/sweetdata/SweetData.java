package top.mrxiaom.sweetdata;
        
import top.mrxiaom.pluginbase.BukkitPlugin;
import top.mrxiaom.pluginbase.utils.scheduler.FoliaLibScheduler;

public class SweetData extends BukkitPlugin {
    public static SweetData getInstance() {
        return (SweetData) BukkitPlugin.getInstance();
    }

    public SweetData() {
        super(options()
                .bungee(false)
                .adventure(true)
                .database(true)
                .reconnectDatabaseWhenReloadConfig(false)
                .scanIgnore("top.mrxiaom.sweetdata.libs")
        );
        this.scheduler = new FoliaLibScheduler(this);
    }

    @Override
    protected void beforeEnable() {
        options.registerDatabase(
                // 在这里添加数据库 (如果需要的话)
        );
    }

    @Override
    protected void afterEnable() {
        getLogger().info("SweetData 加载完毕");
    }
}
