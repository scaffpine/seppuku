package me.rigamortis.seppuku.impl.management;

import me.rigamortis.seppuku.Seppuku;
import me.rigamortis.seppuku.api.config.Configurable;
import me.rigamortis.seppuku.api.event.client.EventLoadConfig;
import me.rigamortis.seppuku.api.event.client.EventSaveConfig;
import me.rigamortis.seppuku.impl.config.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author noil
 */
public final class ConfigManager {

    public static final String CONFIG_PATH = "Seppuku/Config/%s/";
    public String activeConfig;
    private File configDir;
    private File moduleConfigDir;
    private File hudComponentConfigDir;
    private boolean firstLaunch = false;
    private boolean customMainMenuHidden = false;
    private List<Configurable> configurableList = new ArrayList<>();

    public ConfigManager() {
        this.activeConfig = readActiveConfig();
        this.generateDirectories();
    }

    public void switchToConfig(final String config) {
        this.saveAll();

        this.activeConfig = config;
        this.writeActiveConfig(config);

        Seppuku.INSTANCE.unloadSimple();
        Seppuku.INSTANCE.init();
    }

    public String readActiveConfig() {
        try {
            final byte[] bytes = Files.readAllBytes(Paths.get("Seppuku/Config/active.txt"));
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "Default";
        }
    }

    public void writeActiveConfig(final String config) {
        try {
            final FileOutputStream fos = new FileOutputStream("Seppuku/Config/active.txt");
            fos.write(config.getBytes());
            fos.close();
        } catch (IOException e) {
            System.err.println("Could not create file active.txt in config directory.");
        }
    }

    private void generateDirectories() {
        this.configDir = new File(String.format(CONFIG_PATH, activeConfig));
        if (!this.configDir.exists()) {
            this.setFirstLaunch(true);
            this.configDir.mkdirs();
        }

        this.moduleConfigDir = new File(String.format(CONFIG_PATH, activeConfig) + "Modules" + "/");
        if (!this.moduleConfigDir.exists()) {
            this.moduleConfigDir.mkdirs();
        }

        this.hudComponentConfigDir = new File(String.format(CONFIG_PATH, activeConfig) + "HudComponents" + "/");
        if (!this.hudComponentConfigDir.exists()) {
            this.hudComponentConfigDir.mkdirs();
        }
    }

    public void init() {
        Seppuku.INSTANCE.getModuleManager().getModuleList().forEach(module -> {
            this.configurableList.add(new ModuleConfig(this.moduleConfigDir, module));
        });

        Seppuku.INSTANCE.getHudManager().getComponentList().stream().forEach(hudComponent -> {
            this.configurableList.add(new HudConfig(this.hudComponentConfigDir, hudComponent));
        });

        this.configurableList.add(new ClientConfig(configDir));
        this.configurableList.add(new FriendConfig(configDir));
        this.configurableList.add(new XrayConfig(configDir));
        this.configurableList.add(new SearchConfig(configDir));
        this.configurableList.add(new MacroConfig(configDir));
        this.configurableList.add(new WaypointsConfig(configDir));
        this.configurableList.add(new WorldConfig(configDir));
        this.configurableList.add(new IgnoreConfig(configDir));
        this.configurableList.add(new AutoIgnoreConfig(configDir));
        this.configurableList.add(new AltConfig(configDir));

        if (this.firstLaunch) {
            this.saveAll();
        } else {
            this.loadAll();
        }
    }

    public void save(Class configurableClassType) {
        for (Configurable cfg : configurableList) {
            if (cfg.getClass().isAssignableFrom(configurableClassType)) {
                cfg.onSave();
            }
        }

        Seppuku.INSTANCE.getEventManager().dispatchEvent(new EventSaveConfig());
    }

    public void saveAll() {
        for (Configurable cfg : configurableList) {
            cfg.onSave();
        }
        Seppuku.INSTANCE.getEventManager().dispatchEvent(new EventSaveConfig());
    }

    public void load(Class configurableClassType) {
        for (Configurable cfg : configurableList) {
            if (cfg.getClass().isAssignableFrom(configurableClassType)) {
                cfg.onLoad();
            }
        }
        Seppuku.INSTANCE.getEventManager().dispatchEvent(new EventLoadConfig());
    }

    public void loadAll() {
        for (Configurable cfg : configurableList) {
            cfg.onLoad();
        }
        Seppuku.INSTANCE.getEventManager().dispatchEvent(new EventLoadConfig());
    }

    public File getConfigDir() {
        return configDir;
    }

    public File getModuleConfigDir() {
        return moduleConfigDir;
    }

    public File getHudComponentConfigDir() {
        return hudComponentConfigDir;
    }

    public boolean isFirstLaunch() {
        return firstLaunch;
    }

    public void setFirstLaunch(boolean firstLaunch) {
        this.firstLaunch = firstLaunch;
    }

    public boolean isCustomMainMenuHidden() {
        return this.customMainMenuHidden;
    }

    public void setCustomMainMenuHidden(boolean hidden) {
        this.customMainMenuHidden = hidden;
    }

    public void addConfigurable(Configurable configurable) {
        this.configurableList.add(configurable);
    }

    public List<Configurable> getConfigurableList() {
        return configurableList;
    }

    public void setConfigurableList(List<Configurable> configurableList) {
        this.configurableList = configurableList;
    }
}
