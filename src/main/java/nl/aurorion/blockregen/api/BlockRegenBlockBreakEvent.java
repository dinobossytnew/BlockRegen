package nl.aurorion.blockregen.api;

import lombok.Getter;
import lombok.Setter;
import nl.aurorion.blockregen.system.preset.struct.BlockPreset;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Se modifico el evento extendido a este plugin en la api clase evento 1 
 * Se modifico el problema de rendimiento que causa fallos y otros problemas
 */
public class BlockRegenBlockBreakEvent extends BlockRegenBlockEvent implements Cancellable {

    /**
     * block break del retraso causado.
     */
    @Getter
    private final BlockBreakEvent blockBreakEvent;
    
    

    @Getter
    @Setter
    private boolean cancelled = false;

    public BlockRegenBlockBreakEvent(BlockBreakEvent blockBreakEvent, BlockPreset blockPreset) {
        super(blockBreakEvent.getBlock(), blockPreset);
        this.blockBreakEvent = blockBreakEvent;
    }
}
