package vazkii.neat;

import net.minecraft.world.entity.LivingEntity;

/**
 * Helper class for integrating with Cobblemon mod via reflection.
 * This allows accessing Pokemon health values and battle state without a compile-time dependency.
 * 
 * Key insight: During battle, Cobblemon uses a cloned "effectedPokemon" for wild/NPC Pokemon.
 * All battle damage goes to this clone, so we must read health from effectedPokemon to get accurate values.
 * 
 * For UI sync: The client-side battle system (ClientBattlePokemon) provides health values that are
 * perfectly synced with Cobblemon's native UI, including smooth animations and ally/enemy visibility rules.
 */
public class CobblemonIntegration {
	
	// Cached reflection data - Basic Pokemon access
	private static Class<?> pokemonEntityClass = null;
	private static java.lang.reflect.Method getPokemonMethod = null;
	private static java.lang.reflect.Method getCurrentHealthMethod = null;
	private static java.lang.reflect.Method getMaxHealthMethod = null;
	private static java.lang.reflect.Method pokemonGetUuidMethod = null;
	
	// Cached reflection data - Server-side Battle system access
	private static java.lang.reflect.Method getBattleIdMethod = null;
	private static Class<?> battleRegistryClass = null;
	private static java.lang.reflect.Method getBattleMethod = null;
	private static java.lang.reflect.Method getActorsMethod = null;
	private static java.lang.reflect.Method getPokemonListMethod = null;
	private static java.lang.reflect.Method getEffectedPokemonMethod = null;
	private static java.lang.reflect.Method getOriginalPokemonMethod = null;
	
	// Cached reflection data - Client-side Battle system access (for UI sync)
	private static Class<?> cobblemonClientClass = null;
	private static Object cobblemonClientInstance = null;
	private static java.lang.reflect.Method getClientBattleMethod = null;
	private static Class<?> clientBattleClass = null;
	private static java.lang.reflect.Method getSide1Method = null;
	private static java.lang.reflect.Method getSide2Method = null;
	private static Class<?> clientBattleSideClass = null;
	private static java.lang.reflect.Method getActiveClientBattlePokemonMethod = null;
	private static Class<?> activeClientBattlePokemonClass = null;
	private static java.lang.reflect.Method getClientBattlePokemonMethod = null;
	private static Class<?> clientBattlePokemonClass = null;
	private static java.lang.reflect.Method getClientHpValueMethod = null;
	private static java.lang.reflect.Method getClientMaxHpMethod = null;
	private static java.lang.reflect.Method isHpFlatMethod = null;
	private static java.lang.reflect.Method getClientPokemonUuidMethod = null;
	
	private static boolean initialized = false;
	private static boolean cobblemonAvailable = false;
	private static boolean battleSystemAvailable = false;
	private static boolean clientBattleSystemAvailable = false;
	
	/**
	 * Record containing client-side battle health information.
	 * This data is synced with Cobblemon's native UI.
	 * 
	 * @param hpValue Current HP - exact value if isHpFlat=true, ratio (0.0-1.0) if isHpFlat=false
	 * @param maxHp Maximum HP
	 * @param isHpFlat true for allies (exact HP shown), false for enemies (percentage shown)
	 * @param inBattle Whether the Pokemon is actively in a client-side battle
	 */
	public record ClientBattleHealthInfo(float hpValue, float maxHp, boolean isHpFlat, boolean inBattle) {}
	
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
			
			// Try to initialize client-side battle system for UI sync
			initializeClientBattleSystem();
			
		} catch (ClassNotFoundException | NoSuchMethodException e) {
			battleSystemAvailable = false;
			System.out.println("[Neat] Cobblemon battle system not fully accessible - battle-only mode may not work correctly");
		}
	}
	
	/**
	 * Initialize client-side battle system reflection for UI-synced health values.
	 * This allows reading health from ClientBattlePokemon which is perfectly synced with Cobblemon's UI.
	 */
	private static void initializeClientBattleSystem() {
		try {
			// CobblemonClient.INSTANCE.getBattle() -> ClientBattle?
			cobblemonClientClass = Class.forName("com.cobblemon.mod.common.client.CobblemonClient");
			cobblemonClientInstance = cobblemonClientClass.getField("INSTANCE").get(null);
			getClientBattleMethod = cobblemonClientClass.getMethod("getBattle");
			
			// ClientBattle.getSide1(), getSide2() -> ClientBattleSide
			clientBattleClass = Class.forName("com.cobblemon.mod.common.client.battle.ClientBattle");
			getSide1Method = clientBattleClass.getMethod("getSide1");
			getSide2Method = clientBattleClass.getMethod("getSide2");
			
			// ClientBattleSide.getActiveClientBattlePokemon() -> List<ActiveClientBattlePokemon>
			clientBattleSideClass = Class.forName("com.cobblemon.mod.common.client.battle.ClientBattleSide");
			getActiveClientBattlePokemonMethod = clientBattleSideClass.getMethod("getActiveClientBattlePokemon");
			
			// ActiveClientBattlePokemon.getBattlePokemon() -> ClientBattlePokemon?
			activeClientBattlePokemonClass = Class.forName("com.cobblemon.mod.common.client.battle.ActiveClientBattlePokemon");
			getClientBattlePokemonMethod = activeClientBattlePokemonClass.getMethod("getBattlePokemon");
			
			// ClientBattlePokemon fields: hpValue, maxHp, isHpFlat, uuid
			clientBattlePokemonClass = Class.forName("com.cobblemon.mod.common.client.battle.ClientBattlePokemon");
			getClientHpValueMethod = clientBattlePokemonClass.getMethod("getHpValue");
			getClientMaxHpMethod = clientBattlePokemonClass.getMethod("getMaxHp");
			isHpFlatMethod = clientBattlePokemonClass.getMethod("isHpFlat");
			getClientPokemonUuidMethod = clientBattlePokemonClass.getMethod("getUuid");
			
			clientBattleSystemAvailable = true;
			System.out.println("[Neat] Cobblemon client battle system integration initialized - UI sync enabled!");
			
		} catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException | IllegalAccessException e) {
			clientBattleSystemAvailable = false;
			System.out.println("[Neat] Cobblemon client battle system not accessible - falling back to server-side health values");
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
	
	/**
	 * Get client-side battle health information for a Pokemon entity.
	 * This reads from ClientBattlePokemon which is synced with Cobblemon's native UI.
	 * 
	 * @param entity The Pokemon entity to get health for
	 * @return ClientBattleHealthInfo if in client battle, null otherwise
	 */
	public static ClientBattleHealthInfo getClientBattleHealth(LivingEntity entity) {
		if (!isPokemonEntity(entity) || !clientBattleSystemAvailable) {
			return null;
		}
		
		try {
			// Get the Pokemon's UUID
			Object pokemon = getPokemonMethod.invoke(entity);
			if (pokemon == null) {
				return null;
			}
			java.util.UUID pokemonUuid = (java.util.UUID) pokemonGetUuidMethod.invoke(pokemon);
			
			// Find the ClientBattlePokemon by UUID
			Object clientBattlePokemon = findClientBattlePokemon(pokemonUuid);
			if (clientBattlePokemon == null) {
				return null;
			}
			
			// Extract health data from ClientBattlePokemon
			float hpValue = (float) getClientHpValueMethod.invoke(clientBattlePokemon);
			float maxHp = (float) getClientMaxHpMethod.invoke(clientBattlePokemon);
			boolean isHpFlat = (boolean) isHpFlatMethod.invoke(clientBattlePokemon);
			
			return new ClientBattleHealthInfo(hpValue, maxHp, isHpFlat, true);
			
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * Find a ClientBattlePokemon by UUID in the current client battle.
	 * Searches both side1 and side2's active pokemon lists.
	 * 
	 * @param pokemonUuid The UUID of the Pokemon to find
	 * @return The ClientBattlePokemon object if found, null otherwise
	 */
	private static Object findClientBattlePokemon(java.util.UUID pokemonUuid) {
		if (!clientBattleSystemAvailable || cobblemonClientInstance == null) {
			return null;
		}
		
		try {
			// Get current client battle
			Object clientBattle = getClientBattleMethod.invoke(cobblemonClientInstance);
			if (clientBattle == null) {
				return null; // Not in a battle
			}
			
			// Search side1
			Object side1 = getSide1Method.invoke(clientBattle);
			Object result = searchSideForPokemon(side1, pokemonUuid);
			if (result != null) {
				return result;
			}
			
			// Search side2
			Object side2 = getSide2Method.invoke(clientBattle);
			return searchSideForPokemon(side2, pokemonUuid);
			
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * Search a battle side's active pokemon list for a matching UUID.
	 * 
	 * @param side The ClientBattleSide to search
	 * @param pokemonUuid The UUID to match
	 * @return The ClientBattlePokemon if found, null otherwise
	 */
	private static Object searchSideForPokemon(Object side, java.util.UUID pokemonUuid) {
		if (side == null) {
			return null;
		}
		
		try {
			@SuppressWarnings("unchecked")
			java.util.List<Object> activeList = (java.util.List<Object>) getActiveClientBattlePokemonMethod.invoke(side);
			
			for (Object activePokemon : activeList) {
				Object clientBattlePokemon = getClientBattlePokemonMethod.invoke(activePokemon);
				if (clientBattlePokemon != null) {
					java.util.UUID uuid = (java.util.UUID) getClientPokemonUuidMethod.invoke(clientBattlePokemon);
					if (pokemonUuid.equals(uuid)) {
						return clientBattlePokemon;
					}
				}
			}
		} catch (Exception e) {
			// Search failed
		}
		
		return null;
	}
	
	/**
	 * Check if client-side battle system is available.
	 */
	public static boolean isClientBattleSystemAvailable() {
		initialize();
		return clientBattleSystemAvailable;
	}
}
