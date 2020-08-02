package nl.aurorion.blockregen.system.regeneration.struct;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import nl.aurorion.blockregen.BlockRegen;
import nl.aurorion.blockregen.api.BlockRegenBlockRegenerationEvent;
import nl.aurorion.blockregen.system.preset.struct.BlockPreset;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitTask;

@Data
public class RegenerationProcess implements Runnable {

    private SimpleLocation simpleLocation;

    @Getter
    private transient Block block;

    @Getter
    private Material originalMaterial;

    @Getter
    private String regionName;
    @Getter
    private String worldName;

    private String presetName;

    @Getter
    private transient BlockPreset preset;

    /**
     * Holds the system time when the block should regenerate.
     * -- is set after #start()
     */
    @Getter
    private transient long regenerationTime;

    @Getter
    private long timeLeft = -1;

    @Getter
    @Setter
    private transient Material regenerateInto;

    private transient BukkitTask task;

    public RegenerationProcess(Block block, BlockPreset preset) {
        this.block = block;
        this.preset = preset;
        this.presetName = preset.getName();
        this.originalMaterial = block.getType();
        this.simpleLocation = new SimpleLocation(block.getLocation());
    }

    public void start() {

        // If timeLeft is -1, generate a new one from preset regen delay.

        if (this.timeLeft == -1) {
            int regenDelay = Math.max(1, preset.getDelay().getInt());
            this.timeLeft = regenDelay * 1000;
        }

        // Pick a regenerateInto material

        regenerateInto = preset.getRegenMaterial().get();

        // Check if we even need to start the task.

        if (this.timeLeft <= 0) {
            regenerate();
            BlockRegen.getInstance().getConsoleOutput().debug("Regenerated the process already. timeLeft <= 0");
            return;
        }

        BlockRegen.getInstance().getConsoleOutput().debug("Time left: " + this.timeLeft / 1000 + "s");

        // Calculate the time when to regenerate

        this.regenerationTime = System.currentTimeMillis() + timeLeft;

        // Replace the block

        Material replaceMaterial = preset.getReplaceMaterial().get();

        Bukkit.getScheduler().runTask(BlockRegen.getInstance(), () -> block.setType(replaceMaterial));
        BlockRegen.getInstance().consoleOutput.debug("Replaced block with " + replaceMaterial.toString());

        // Start the regeneration task

        if (task != null) task.cancel();

        task = Bukkit.getScheduler().runTaskLaterAsynchronously(BlockRegen.getInstance(), this, timeLeft / 50);
        BlockRegen.getInstance().getConsoleOutput().debug("Started regeneration...");
    }

    @Override
    public void run() {
        regenerate();
    }

    public void regenerate() {

        if (task != null) {
            task.cancel();
            task = null;
        }

        BlockRegenBlockRegenerationEvent blockRegenBlockRegenEvent = new BlockRegenBlockRegenerationEvent(this);
        Bukkit.getScheduler().runTask(BlockRegen.getInstance(), () -> Bukkit.getServer().getPluginManager().callEvent(blockRegenBlockRegenEvent));

        BlockRegen.getInstance().getRegenerationManager().removeProcess(this);

        if (blockRegenBlockRegenEvent.isCancelled())
            return;

        Bukkit.getScheduler().runTask(BlockRegen.getInstance(), () -> block.setType(regenerateInto));
        BlockRegen.getInstance().getConsoleOutput().debug("Regenerated block " + originalMaterial);
    }

    /**
     * Revert process to original material.
     */
    public void revert() {

        if (task != null) {
            task.cancel();
            task = null;
        }

        BlockRegen.getInstance().getRegenerationManager().removeProcess(this);

        Bukkit.getScheduler().runTask(BlockRegen.getInstance(), () -> block.setType(originalMaterial));
        BlockRegen.getInstance().getConsoleOutput().debug("Reverted block " + originalMaterial);
    }

    public void updateTimeLeft(long timeLeft) {
        this.timeLeft = timeLeft;
        if (timeLeft > 0)
            start();
        else run();
    }

    public boolean convertSimpleLocation() {

        if (simpleLocation == null) {
            BlockRegen.getInstance().getConsoleOutput().err("Could not convert block from SimpleLocation in a Regeneration process for preset " + presetName);
            return false;
        }

        Location location = simpleLocation.toLocation();
        setBlock(location.getBlock());
        return true;
    }

    public boolean convertPreset() {
        BlockPreset preset = BlockRegen.getInstance().getPresetManager().getPreset(presetName).orElse(null);

        if (preset == null) {
            BlockRegen.getInstance().getConsoleOutput().err("BlockPreset " + presetName + " no longer exists, removing a left over regeneration process.");
            return false;
        }

        this.preset = preset;
        return true;
    }

    public boolean isRunning() {
        return task != null;
    }

    @Override
    public String toString() {
        return "id: " + (task != null ? task.getTaskId() : "NaN") + "=" + presetName + " : " + (block != null ? block.getLocation().toString() : simpleLocation.toString()) + " - oM:" + originalMaterial.toString() + ", tL: " + timeLeft + " rT: " + regenerationTime;
    }
}