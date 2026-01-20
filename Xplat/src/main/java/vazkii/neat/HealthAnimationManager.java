package vazkii.neat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.Mth;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HealthAnimationManager {
	private static final Map<UUID, Float> animatedHealth = new ConcurrentHashMap<>();
	private static final Map<UUID, FadeState> fadeStates = new ConcurrentHashMap<>();
	private static final float LERP_SPEED = 0.15F; // Higher = faster animation (0.0 to 1.0)
	private static final float FADE_OUT_SPEED = 0.08F; // Higher = faster fade (0.0 to 1.0)
	private static final float FADE_OUT_DURATION_TICKS = 20.0F; // How many ticks to fade out (1 second at 20 TPS)
	
	/**
	 * Tracks the fade state for an entity (alpha value and whether it's fading out).
	 */
	private static class FadeState {
		float alpha = 1.0F;
		int fadeOutTicks = 0;
		boolean isFadingOut = false;
		
		void startFadeOut() {
			if (!isFadingOut) {
				isFadingOut = true;
				fadeOutTicks = 0;
			}
		}
		
		boolean update() {
			if (isFadingOut) {
				fadeOutTicks++;
				// Fade out alpha over time
				float progress = fadeOutTicks / FADE_OUT_DURATION_TICKS;
				alpha = Mth.clamp(1.0F - progress, 0.0F, 1.0F);
				
				// If fully faded, return true to indicate we should remove this entity
				if (alpha <= 0.0F) {
					return true;
				}
			}
			return false;
		}
	}
	
	/**
	 * Gets the animated health value for an entity, creating it if it doesn't exist.
	 * This should be called during rendering to get the smoothly interpolated health.
	 */
	public static float getAnimatedHealth(LivingEntity entity) {
		UUID id = entity.getUUID();
		float targetHealth = entity.getHealth();
		
		// If we don't have an entry for this entity, initialize it with current health
		if (!animatedHealth.containsKey(id)) {
			animatedHealth.put(id, targetHealth);
			return targetHealth;
		}
		
		return animatedHealth.get(id);
	}
	
	/**
	 * Gets the alpha (opacity) value for an entity's health bar.
	 * Returns 1.0F for normal entities, or a fading value for entities that are dead.
	 */
	public static float getAlpha(UUID entityId) {
		FadeState fadeState = fadeStates.get(entityId);
		if (fadeState == null) {
			return 1.0F;
		}
		return fadeState.alpha;
	}
	
	/**
	 * Checks if an entity is currently fading out (even if it's already dead/removed).
	 * This allows us to continue rendering the health bar during fade-out.
	 */
	public static boolean isFadingOut(UUID entityId) {
		FadeState fadeState = fadeStates.get(entityId);
		return fadeState != null && fadeState.isFadingOut;
	}
	
	/**
	 * Updates the animated health values towards their targets.
	 * This should be called every client tick.
	 */
	public static void tick() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			// Clear all entries when not in a world
			animatedHealth.clear();
			fadeStates.clear();
			return;
		}
		
		// Track which entities we've seen this tick
		Set<UUID> seenEntities = new HashSet<>();
		
		// Update animated health for all entities currently in the world
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (entity instanceof LivingEntity living) {
				UUID id = living.getUUID();
				seenEntities.add(id);
				
				float targetHealth = living.getHealth();
				boolean isDead = living.isDeadOrDying() || targetHealth <= 0.0F;
				
				// Initialize fade state if needed
				FadeState fadeState = fadeStates.computeIfAbsent(id, k -> new FadeState());
				
				// If entity is dead, start fade-out
				if (isDead) {
					fadeState.startFadeOut();
				} else {
					// Entity is alive, reset fade state
					fadeState.alpha = 1.0F;
					fadeState.isFadingOut = false;
					fadeState.fadeOutTicks = 0;
				}
				
				// Get or initialize animated health
				float currentAnimated = animatedHealth.getOrDefault(id, targetHealth);
				
				// Lerp towards target health
				float newAnimated = Mth.lerp(LERP_SPEED, currentAnimated, targetHealth);
				
				// If very close to target, snap to it to avoid floating point issues
				if (Math.abs(newAnimated - targetHealth) < 0.01F) {
					animatedHealth.put(id, targetHealth);
				} else {
					animatedHealth.put(id, newAnimated);
				}
			}
		}
		
		// Update fade states and remove completed fade-outs
		fadeStates.entrySet().removeIf(entry -> {
			UUID id = entry.getKey();
			FadeState fadeState = entry.getValue();
			
			// Update fade state
			boolean shouldRemove = fadeState.update();
			
			// If entity no longer exists and not fading out, remove it
			if (!seenEntities.contains(id) && !fadeState.isFadingOut) {
				return true;
			}
			
			// If fade-out is complete, remove the entry
			if (shouldRemove) {
				animatedHealth.remove(id);
				return true;
			}
			
			return false;
		});
		
		// Remove health entries for entities that no longer exist and aren't fading
		animatedHealth.entrySet().removeIf(entry -> {
			UUID id = entry.getKey();
			FadeState fadeState = fadeStates.get(id);
			// Keep entry if entity exists or is fading out
			return !seenEntities.contains(id) && (fadeState == null || !fadeState.isFadingOut);
		});
	}
	
	/**
	 * Cleans up health tracking for entities that are no longer in the world.
	 * This is called automatically during tick, but can be called manually if needed.
	 */
	public static void cleanup() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			animatedHealth.clear();
			fadeStates.clear();
			return;
		}
		
		// Build set of current entity UUIDs
		Set<UUID> currentEntities = new HashSet<>();
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (entity instanceof LivingEntity) {
				currentEntities.add(entity.getUUID());
			}
		}
		
		// Remove entries for entities that no longer exist and aren't fading
		animatedHealth.entrySet().removeIf(entry -> {
			UUID id = entry.getKey();
			FadeState fadeState = fadeStates.get(id);
			return !currentEntities.contains(id) && (fadeState == null || !fadeState.isFadingOut);
		});
		
		fadeStates.entrySet().removeIf(entry -> {
			UUID id = entry.getKey();
			FadeState fadeState = entry.getValue();
			return !currentEntities.contains(id) && !fadeState.isFadingOut;
		});
	}
}
