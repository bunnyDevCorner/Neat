package vazkii.neat;

import net.minecraft.world.entity.LivingEntity;

/**
 * Helper class for integrating with Cobblemon mod via reflection.
 * This allows accessing Pokemon health values and battle state without a compile-time dependency.
 * 
 * Key insight: During battle, Cobblemon uses a cloned "effectedPokemon" for wild/NPC Pokemon.
 * All battle damage goes to this clone, so we must read health from effectedPokemon to get accurate values.
 */
public class CobblemonIntegration {
	
	// Cached reflection data - Basic Pokemon access
	private static Class<?> pokemonEntityClass = null;
	private static java.lang.reflect.Method getPokemonMethod = null;
	private static java.lang.reflect.Method getCurrentHealthMethod = null;
	private static java.lang.reflect.Method getMaxHealthMethod = null;
	private static java.lang.reflect.Method pokemonGetUuidMethod = null;
	
	// Cached reflection data - Battle system access
	private static java.lang.reflect.Method getBattleIdMethod = null;
	private static Class<?> battleRegistryClass = null;
	private static java.lang.reflect.Method getBattleMethod = null;
	private static java.lang.reflect.Method getActorsMethod = null;
	private static java.lang.reflect.Method getPokemonListMethod = null;
	private static java.lang.reflect.Method getEffectedPokemonMethod = null;
	private static java.lang.reflect.Method getOriginalPokemonMethod = null;
	
	private static boolean initialized = false;
	private static boolean cobblemonAvailable = false;
	private static boolean battleSystemAvailable = false;
	
	/**
	 * Initialize Cobblemon integration by loading classes via reflection.
	 * Called lazily on first use.
	 */
	private static void initialize() {
		if (initialized) {
			return;
		}
		initialized = true;
		
		try {
			// Load PokemonEntity class
			pokemonEntityClass = Class.forName("com.cobblemon.mod.common.entity.pokemon.PokemonEntity");
			
			// Cache methods for better performance
			getPokemonMethod = pokemonEntityClass.getMethod("getPokemon");
			
			Class<?> pokemonClass = Class.forName("com.cobblemon.mod.common.pokemon.Pokemon");
			getCurrentHealthMethod = pokemonClass.getMethod("getCurrentHealth");
			getMaxHealthMethod = pokemonClass.getMethod("getMaxHealth");
			pokemonGetUuidMethod = pokemonClass.getMethod("getUuid");
			
			cobblemonAvailable = true;
			System.out.println("[Neat] Cobblemon integration initialized successfully!");
			
			// Try to initialize battle system access
			initializeBattleSystem();
			
		} catch (ClassNotFoundException | NoSuchMethodException e) {
			cobblemonAvailable = false;
			System.out.println("[Neat] Cobblemon not detected - using standard health values for all entities");
		}
	}
	
	/**
	 * Initialize battle system reflection for accessing battle health.
	 */
	private static void initializeBattleSystem() {
		try {
			// PokemonEntity.getBattleId() returns UUID?
			getBattleIdMethod = pokemonEntityClass.getMethod("getBattleId");
			
			// BattleRegistry.getBattle(UUID) returns PokemonBattle?
			battleRegistryClass = Class.forName("com.cobblemon.mod.common.battles.BattleRegistry");
			getBattleMethod = battleRegistryClass.getMethod("getBattle", java.util.UUID.class);
			
			// PokemonBattle.getActors() returns Iterable<BattleActor>
			Class<?> pokemonBattleClass = Class.forName("com.cobblemon.mod.common.api.battles.model.PokemonBattle");
			getActorsMethod = pokemonBattleClass.getMethod("getActors");
			
			// BattleActor.getPokemonList() returns List<BattlePokemon>
			Class<?> battleActorClass = Class.forName("com.cobblemon.mod.common.api.battles.model.actor.BattleActor");
			getPokemonListMethod = battleActorClass.getMethod("getPokemonList");
			
			// BattlePokemon methods
			Class<?> battlePokemonClass = Class.forName("com.cobblemon.mod.common.battles.pokemon.BattlePokemon");
			getEffectedPokemonMethod = battlePokemonClass.getMethod("getEffectedPokemon");
			getOriginalPokemonMethod = battlePokemonClass.getMethod("getOriginalPokemon");
			
			battleSystemAvailable = true;
			System.out.println("[Neat] Cobblemon battle system integration initialized!");
			
		} catch (ClassNotFoundException | NoSuchMethodException e) {
			battleSystemAvailable = false;
			System.out.println("[Neat] Cobblemon battle system not fully accessible - battle-only mode may not work correctly");
		}
	}
	
	/**
	 * Check if Cobblemon is available.
	 */
	public static boolean isCobblemonAvailable() {
		initialize();
		return cobblemonAvailable;
	}
	
	/**
	 * Check if the given entity is a Cobblemon Pokemon.
	 */
	public static boolean isPokemonEntity(LivingEntity entity) {
		initialize();
		if (!cobblemonAvailable || pokemonEntityClass == null) {
			return false;
		}
		return pokemonEntityClass.isInstance(entity);
	}
	
	/**
	 * Check if a Pokemon entity is currently in a battle.
	 * Returns false if not a Pokemon or not in battle.
	 */
	public static boolean isInBattle(LivingEntity entity) {
		if (!isPokemonEntity(entity) || !battleSystemAvailable) {
			return false;
		}
		
		try {
			Object battleId = getBattleIdMethod.invoke(entity);
			return battleId != null;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Get the current health of a Pokemon entity.
	 * If in battle, returns health from the battle system's effectedPokemon.
	 * Returns -1 if not a Pokemon or if an error occurs.
	 */
	public static int getPokemonCurrentHealth(LivingEntity entity) {
		if (!isPokemonEntity(entity)) {
			return -1;
		}
		
		try {
			Object pokemon = getPokemonMethod.invoke(entity);
			if (pokemon == null) {
				return -1;
			}
			
			// If in battle, get health from battle system
			if (battleSystemAvailable) {
				Object battlePokemon = getBattlePokemonForEntity(entity, pokemon);
				if (battlePokemon != null) {
					Object effectedPokemon = getEffectedPokemonMethod.invoke(battlePokemon);
					if (effectedPokemon != null) {
						return (int) getCurrentHealthMethod.invoke(effectedPokemon);
					}
				}
			}
			
			// Fallback to Pokemon's health (used when not in battle)
			return (int) getCurrentHealthMethod.invoke(pokemon);
			
		} catch (Exception e) {
			return -1;
		}
	}
	
	/**
	 * Get the max health of a Pokemon entity.
	 * If in battle, returns max health from the battle system's effectedPokemon.
	 * Returns -1 if not a Pokemon or if an error occurs.
	 */
	public static int getPokemonMaxHealth(LivingEntity entity) {
		if (!isPokemonEntity(entity)) {
			return -1;
		}
		
		try {
			Object pokemon = getPokemonMethod.invoke(entity);
			if (pokemon == null) {
				return -1;
			}
			
			// If in battle, get max health from battle system
			if (battleSystemAvailable) {
				Object battlePokemon = getBattlePokemonForEntity(entity, pokemon);
				if (battlePokemon != null) {
					Object effectedPokemon = getEffectedPokemonMethod.invoke(battlePokemon);
					if (effectedPokemon != null) {
						return (int) getMaxHealthMethod.invoke(effectedPokemon);
					}
				}
			}
			
			// Fallback to Pokemon's max health
			return (int) getMaxHealthMethod.invoke(pokemon);
			
		} catch (Exception e) {
			return -1;
		}
	}
	
	/**
	 * Find the BattlePokemon object for an entity that's in battle.
	 * 
	 * @return The BattlePokemon if in battle, null otherwise
	 */
	private static Object getBattlePokemonForEntity(LivingEntity entity, Object pokemon) {
		if (!battleSystemAvailable || getBattleIdMethod == null) {
			return null;
		}
		
		try {
			// Get battleId from entity
			Object battleId = getBattleIdMethod.invoke(entity);
			if (battleId == null) {
				return null; // Not in battle
			}
			
			// Get the battle from BattleRegistry (it's a Kotlin object, need INSTANCE)
			Object battleRegistryInstance = battleRegistryClass.getField("INSTANCE").get(null);
			Object battle = getBattleMethod.invoke(battleRegistryInstance, battleId);
			if (battle == null) {
				return null;
			}
			
			// Get the Pokemon's UUID to match against
			java.util.UUID pokemonUuid = (java.util.UUID) pokemonGetUuidMethod.invoke(pokemon);
			
			// Iterate through all actors in the battle
			@SuppressWarnings("unchecked")
			Iterable<Object> actors = (Iterable<Object>) getActorsMethod.invoke(battle);
			for (Object actor : actors) {
				// Get the pokemon list for this actor
				@SuppressWarnings("unchecked")
				java.util.List<Object> pokemonList = (java.util.List<Object>) getPokemonListMethod.invoke(actor);
				for (Object battlePokemon : pokemonList) {
					// Compare against ORIGINAL pokemon UUID (not effected, which may be a clone)
					Object originalPokemon = getOriginalPokemonMethod.invoke(battlePokemon);
					if (originalPokemon != null) {
						java.util.UUID originalUuid = (java.util.UUID) pokemonGetUuidMethod.invoke(originalPokemon);
						if (pokemonUuid.equals(originalUuid)) {
							return battlePokemon;
						}
					}
				}
			}
		} catch (Exception e) {
			// Battle system access failed
		}
		
		return null;
	}
}
