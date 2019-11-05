package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.Rel;
import com.massivecraft.factions.cmd.type.TypeMPlayer;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.event.EventFactionsMembershipChange;
import com.massivecraft.factions.event.EventFactionsMembershipChange.MembershipChangeReason;
import com.massivecraft.massivecore.MassiveException;
import org.bukkit.ChatColor;
import com.massivecraft.massivecore.util.IdUtil;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.factions.Perm;


public class CmdFactionsKick extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsKick()
	{

		this.setSetupEnabled(false);
		this.setAliases("at");

		// Parameters
		this.addParameter(TypeMPlayer.get(), "oyuncu");

		this.addRequirements(RequirementHasPerm.get(Perm.KICK));
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public void perform() throws MassiveException
	{
		// Arg
		MPlayer mplayer = this.readArg();

		// Validate
		if (msender == mplayer)
		{
			msg("<b>Kendini atamazsın.");
			message(mson(mson("Bunu yapmak istemiş olabilirsin: ").color(ChatColor.YELLOW), CmdFactions.get().cmdFactionsLeave.getTemplate(false)));
			return;
		}

		if (mplayer.getRole() == Rel.LEADER && !msender.isOverriding())
		{
			throw new MassiveException().addMsg("<b>Lider atılamaz.");
		}

		if (mplayer.getRole().isMoreThan(msender.getRole()) && ! msender.isOverriding())
		{
			throw new MassiveException().addMsg("<b>Senden daha yüksek rütbeye sahip kişileri atamazsın.");
		}

		if (mplayer.getRole() == msender.getRole() && ! msender.isOverriding())
		{
			throw new MassiveException().addMsg("<b>Seninle aynı rütbedeki kişileri atamazsın.");
		}

		if ( ! MConf.get().canLeaveWithNegativePower && mplayer.getPower() < 0 && ! msender.isOverriding())
		{
			msg("<b>Güçleri pozitif olana kadar bu kişileri atamazsın.");
			return;
		}

		// MPerm
		Faction mplayerFaction = mplayer.getFaction();
		if ( ! MPerm.getPermKick().has(msender, mplayerFaction, true)) return;

		// Event
		EventFactionsMembershipChange event = new EventFactionsMembershipChange(sender, mplayer, FactionColl.get().getNone(), MembershipChangeReason.KICK);
		event.run();
		if (event.isCancelled()) return;

		// Inform
		mplayerFaction.msg("%s<i> adlı oyuncu %s<i> adlı oyuncu klandan attı! :O", msender.describeTo(mplayerFaction, true), mplayer.describeTo(mplayerFaction, true));
		mplayer.msg("%s<i> adlı oyuncu seni %s<i> adlı klandan attı! :O", msender.describeTo(mplayer, true), mplayerFaction.describeTo(mplayer));
		if (mplayerFaction != msenderFaction)
		{
			msender.msg("<i>%s<i> adlı oyuncuyu %s<i> adlı klandan attın!", mplayer.describeTo(msender), mplayerFaction.describeTo(msender));
		}

		if (MConf.get().logFactionKick)
		{
			Factions.get().log(msender.getDisplayName(IdUtil.getConsole()) + " adlı oyuncu " + mplayer.getName() + " adlı oyuncuyu " + mplayerFaction.getName() + " klanından attı. ");
		}

		// Apply
		if (mplayer.getRole() == Rel.LEADER)
		{
			mplayerFaction.promoteNewLeader();
		}
		mplayerFaction.uninvite(mplayer);
		mplayer.resetFactionData();
	}

}
