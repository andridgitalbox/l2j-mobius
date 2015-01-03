/*
 * Copyright (C) 2004-2015 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2jserver.gameserver.instancemanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;

import com.l2jserver.L2DatabaseFactory;
import com.l2jserver.gameserver.datatables.NpcData;
import com.l2jserver.gameserver.model.L2Spawn;
import com.l2jserver.gameserver.model.actor.templates.L2NpcTemplate;
import com.l2jserver.gameserver.model.entity.Fort;

public final class FortSiegeGuardManager
{
	private static final Logger _log = Logger.getLogger(FortSiegeGuardManager.class.getName());
	
	private final Fort _fort;
	private final FastMap<Integer, FastList<L2Spawn>> _siegeGuards = new FastMap<>();
	
	public FortSiegeGuardManager(Fort fort)
	{
		_fort = fort;
	}
	
	/**
	 * Spawn guards.
	 */
	public void spawnSiegeGuard()
	{
		try
		{
			final FastList<L2Spawn> monsterList = getSiegeGuardSpawn().get(getFort().getResidenceId());
			if (monsterList != null)
			{
				for (L2Spawn spawnDat : monsterList)
				{
					spawnDat.doSpawn();
					if (spawnDat.getRespawnDelay() == 0)
					{
						spawnDat.stopRespawn();
					}
					else
					{
						spawnDat.startRespawn();
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Error spawning siege guards for fort " + getFort().getName() + ":" + e.getMessage(), e);
		}
	}
	
	/**
	 * Unspawn guards.
	 */
	public void unspawnSiegeGuard()
	{
		try
		{
			final FastList<L2Spawn> monsterList = getSiegeGuardSpawn().get(getFort().getResidenceId());
			
			if (monsterList != null)
			{
				for (L2Spawn spawnDat : monsterList)
				{
					spawnDat.stopRespawn();
					if (spawnDat.getLastSpawn() != null)
					{
						spawnDat.getLastSpawn().doDie(spawnDat.getLastSpawn());
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Error unspawning siege guards for fort " + getFort().getName() + ":" + e.getMessage(), e);
		}
	}
	
	/**
	 * Load guards.
	 */
	void loadSiegeGuard()
	{
		_siegeGuards.clear();
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT npcId, x, y, z, heading, respawnDelay FROM fort_siege_guards WHERE fortId = ?"))
		{
			final int fortId = getFort().getResidenceId();
			ps.setInt(1, fortId);
			try (ResultSet rs = ps.executeQuery())
			{
				FastList<L2Spawn> siegeGuardSpawns = new FastList<>();
				while (rs.next())
				{
					L2NpcTemplate template = NpcData.getInstance().getTemplate(rs.getInt("npcId"));
					if (template != null)
					{
						L2Spawn spawn = new L2Spawn(template);
						spawn.setAmount(1);
						spawn.setX(rs.getInt("x"));
						spawn.setY(rs.getInt("y"));
						spawn.setZ(rs.getInt("z"));
						spawn.setHeading(rs.getInt("heading"));
						spawn.setRespawnDelay(rs.getInt("respawnDelay"));
						spawn.setLocationId(0);
						
						siegeGuardSpawns.add(spawn);
					}
					else
					{
						_log.warning("Missing npc data in npc table for ID: " + rs.getInt("npcId"));
					}
				}
				_siegeGuards.put(fortId, siegeGuardSpawns);
			}
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Error loading siege guard for fort " + getFort().getName() + ": " + e.getMessage(), e);
		}
	}
	
	public final Fort getFort()
	{
		return _fort;
	}
	
	public final FastMap<Integer, FastList<L2Spawn>> getSiegeGuardSpawn()
	{
		return _siegeGuards;
	}
}