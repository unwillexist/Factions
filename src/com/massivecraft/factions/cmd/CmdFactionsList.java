package com.massivecraft.factions.cmd;

import com.massivecraft.factions.comparator.ComparatorFactionList;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.Parameter;
import com.massivecraft.massivecore.pager.Pager;
import com.massivecraft.massivecore.pager.Stringifier;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.factions.Perm;

import java.util.List;

public class CmdFactionsList extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsList()
	{
		// Parameters
		this.setSetupEnabled(false);
		this.setAliases("listele","liste");
		this.addRequirements(RequirementHasPerm.get(Perm.LIST));
		this.addParameter(Parameter.getPage());
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public void perform() throws MassiveException
	{
		// Args
		int page = this.readArg();
		final CommandSender sender = this.sender;
		final MPlayer msender = this.msender;

		// NOTE: The faction list is quite slow and mostly thread safe.
		// We run it asynchronously to spare the primary server thread.

		// Pager Create
		final Pager<Faction> pager = new Pager<>(this, "Klan Listesi", page, new Stringifier<Faction>() {
			@Override
			public String toString(Faction faction, int index)
			{
				if (faction.isNone())
				{
					return Txt.parse("<i>Klansız<i> %d kişi çevrimiçi", FactionColl.get().getNone().getMPlayersWhereOnlineTo(sender).size());
				}
				else
				{
					return Txt.parse("%s<i> %d/%d çevrimiçi, Büyüklük: %d",
						faction.getName(msender),
						faction.getMPlayersWhereOnlineTo(sender).size(),
						faction.getMPlayers().size(),
						faction.getLandCount()
					);
				}
			}
		});

		Bukkit.getScheduler().runTaskAsynchronously(Factions.get(), new Runnable()
		{
			@Override
			public void run()
			{
				// Pager Items
				final List<Faction> factions = FactionColl.get().getAll(ComparatorFactionList.get(sender));
				pager.setItems(factions);

				// Pager Message
				pager.message();
			}
		});
	}

}
