package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.Perm;
import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.cmd.type.TypeMPlayer;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MFlag;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.event.EventFactionsMembershipChange;
import com.massivecraft.factions.event.EventFactionsMembershipChange.MembershipChangeReason;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.mson.Mson;
import com.massivecraft.massivecore.util.Txt;
import org.bukkit.ChatColor;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;

public class CmdFactionsJoin extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //

	public CmdFactionsJoin()
	{
		// Parameters
		this.addParameter(TypeFaction.get(), "klan");
		this.addParameter(TypeMPlayer.get(), "oyuncu", "sen");

		this.setSetupEnabled(false);
		this.setAliases("katıl");

		this.addRequirements(RequirementHasPerm.get(Perm.JOIN));
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public void perform() throws MassiveException
	{
		// Args
		Faction faction = this.readArg();

		MPlayer mplayer = this.readArg(msender);
		Faction mplayerFaction = mplayer.getFaction();

		boolean samePlayer = mplayer == msender;

		// Validate
		if (!samePlayer  && ! Perm.JOIN_OTHERS.has(sender, false))
		{
			msg("<b>Başka insanları klanlara taşımaya yetkin yok.");
			return;
		}

		if (faction == mplayerFaction)
		{
			String command = CmdFactions.get().cmdFactionsKick.getCommandLine(mplayer.getName());

			// Mson creation
			Mson alreadyMember = Mson.mson(
				Mson.parse(mplayer.describeTo(msender, true)),
				mson((samePlayer ? "Sen" : "O") + faction.getName(msender) + " adlı klanın bir üyesi.").color(ChatColor.YELLOW)
			);

			message(alreadyMember.suggest(command).tooltip(Txt.parse("<i><c>%s için tıkla<i>.", command)));
			return;
		}

		if (MConf.get().factionMemberLimit > 0 && faction.getMPlayers().size() >= MConf.get().factionMemberLimit)
		{
			msg(" <b>!<white> %s adlı klan %d üyeye sahip bu maksimum üye sayısı olduğundan %s şuan klana giriş sağlayamaz.", faction.getName(msender), MConf.get().factionMemberLimit, mplayer.describeTo(msender, false));
			return;
		}

		if (mplayerFaction.isNormal())
		{
			String command = CmdFactions.get().cmdFactionsLeave.getCommandLine(mplayer.getName());

			// Mson creation
			Mson leaveFirst = Mson.mson(
				Mson.parse(mplayer.describeTo(msender, true)),
				mson((samePlayer ? "Senin" : "Onun") + " önce şuanki klanından " + (samePlayer ? "çıkman" : "çıkması") + " gerek.").color(ChatColor.RED)
			);
			message(leaveFirst.suggest(command).tooltip(Txt.parse("<i><c>%s<i> için tıkla.", command)));
			return;
		}

		if (!MConf.get().canLeaveWithNegativePower && mplayer.getPower() < 0)
		{
			msg("<b>%s adlı oyuncu negatif güç ile bir klana katılamaz.", mplayer.describeTo(msender, true));
			return;
		}

		if( ! (faction.getFlag(MFlag.getFlagOpen()) || faction.isInvited(mplayer) || msender.isOverriding()))
		{
			msg("<i>Bu klan davetiye gerektirir.");
			if (samePlayer)
			{
				faction.msg("%s<i> klanına katılmaya çalıştı.", mplayer.describeTo(faction, true));
			}
			return;
		}

		// Event
		EventFactionsMembershipChange membershipChangeEvent = new EventFactionsMembershipChange(sender, msender, faction, MembershipChangeReason.JOIN);
		membershipChangeEvent.run();
		if (membershipChangeEvent.isCancelled()) return;

		// Inform
		if (!samePlayer)
		{
			mplayer.msg("<i>%s <i>adlı oyuncu seni %s<i> klanına taşıdı.", msender.describeTo(mplayer, true), faction.getName(mplayer));
		}
		faction.msg("<i>%s <i>adlı oyuncu <lime>senin klanına<i> katıldı.", mplayer.describeTo(faction, true));
		msender.msg("<i>%s <i>adlı oyuncu başarıyla %s<i> klanına katıldı.", mplayer.describeTo(msender, true), faction.getName(msender));

		// Apply
		mplayer.resetFactionData();
		mplayer.setFaction(faction);

		faction.uninvite(mplayer);

		// Derplog
		if (MConf.get().logFactionJoin)
		{
			if (samePlayer)
			{
				Factions.get().log(Txt.parse("%s adlı oyuncu %s klanına katıldı.", mplayer.getName(), faction.getName()));
			}
			else
			{
				Factions.get().log(Txt.parse("%s adlı oyuncu %s adlı oyuncuyu %s klanına taşıdı.", msender.getName(), mplayer.getName(), faction.getName()));
			}
		}
	}

}
