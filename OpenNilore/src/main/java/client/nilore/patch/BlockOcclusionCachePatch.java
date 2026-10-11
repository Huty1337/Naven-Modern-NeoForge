package client.nilore.patch;

import asm.patchify.annotation.At;
import asm.patchify.annotation.Inject;
import asm.patchify.annotation.Patch;
import net.minecraft.world.level.block.state.BlockState;
import client.nilore.modules.impl.render.XRay;

/**
 * Compatibility patch for Embeddium/Sodium's {@code BlockOcclusionCache}.
 *
 * <p>Sodium/Embeddium replaces vanilla's chunk mesh building with its own pipeline. The
 * vanilla {@code Block.shouldRenderFace()} method is never called by Embeddium's
 * {@code BlockRenderer}. Instead, face visibility is decided by
 * {@code BlockOcclusionCache.shouldDrawSide()}, which has its own entirely independent
 * occlusion logic.</p>
 *
 * <p>This patch injects into {@code BlockOcclusionCache.shouldDrawSide()} so that when
 * XRay is enabled the result is forced to match {@link XRay#isXrayVisible}.</p>
 *
 * <p>The target class is referenced by {@link Patch#className()} rather than by
 * {@link Patch#value()} because Embeddium is an optional mod — the class is not
 * available at compile time. The method descriptor is left empty (name-only match)
 * because the parameter types in Embeddium's bytecode may differ between Yarn and
 * Mojmap mappings.</p>
 */
@Patch(className = "me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache")
public class BlockOcclusionCachePatch {

    @Inject(
            method = "shouldDrawSide",
            desc = "",
            at = @At(At.Type.HEAD)
    )
    public static void onShouldDrawSide(Object self, Object selfState, Object view,
                                         Object pos, Object direction, CallbackInfo ci) {
        XRay xray = XRay.INSTANCE;
        if (xray == null || !xray.isEnabled()) {
            return;
        }
        boolean visible = xray.isXrayVisible(((BlockState) selfState).getBlock());
        ci.result = visible ? Boolean.TRUE : Boolean.FALSE;
        ci.cancel();
    }
}
