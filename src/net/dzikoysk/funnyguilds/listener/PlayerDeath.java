package net.dzikoysk.funnyguilds.listener;

import net.dzikoysk.funnyguilds.basic.Guild;
import net.dzikoysk.funnyguilds.basic.User;
import net.dzikoysk.funnyguilds.data.Messages;
import net.dzikoysk.funnyguilds.data.Settings;
import net.dzikoysk.funnyguilds.util.StringUtils;
import net.dzikoysk.funnyguilds.util.thread.ActionType;
import net.dzikoysk.funnyguilds.util.thread.IndependentThread;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeath implements Listener {
	
	@EventHandler
	public void onDeath(PlayerDeathEvent event){
		Player v = event.getEntity();
		Player a = event.getEntity().getKiller();
		
		User victim = User.get(v);
		victim.getRank().addDeath();
		
		if(a == null) return;
		User attacker = User.get(a);
		
		if(attacker.getLastVictim() != null && attacker.getLastVictim().equals(victim)){
			if(attacker.getLastVictimTime() + 7200000 > System.currentTimeMillis()){
				v.sendMessage(Messages.getInstance().getMessage("rankLastVictimV"));
				a.sendMessage(Messages.getInstance().getMessage("rankLastVictimA"));
				return;
			}
		} else if(victim.getLastAttacker() != null && victim.getLastAttacker().equals(attacker)){
			if(victim.getLastVictimTime() + 7200000 > System.currentTimeMillis()){
				v.sendMessage(Messages.getInstance().getMessage("rankLastAttackerV"));
				a.sendMessage(Messages.getInstance().getMessage("rankLastAttackerA"));
				return;
			}
		}
		
		Double d = victim.getRank().getPoints() * (Settings.getInstance().rankPercent / 100);
		int points = d.intValue();
		
		victim.getRank().removePoints(points);
		victim.setLastAttacker(attacker);
		
		attacker.getRank().addKill();
		attacker.getRank().addPoints(points);
		attacker.setLastVictim(victim);
		
		if(Settings.getInstance().mysql){
			if(victim.hasGuild()){
				Guild guild = victim.getGuild();
				IndependentThread.actions(ActionType.MYSQL_UPDATE_GUILD_POINTS, guild);
			}
			if(attacker.hasGuild()){
				Guild guild = attacker.getGuild();
				IndependentThread.actions(ActionType.MYSQL_UPDATE_GUILD_POINTS, guild);
			}
			IndependentThread.actions(ActionType.MYSQL_UPDATE_USER_POINTS, victim);
			IndependentThread.actions(ActionType.MYSQL_UPDATE_USER_POINTS, attacker);
		}
		
		IndependentThread.actions(ActionType.DUMMY_GLOBAL_UPDATE_USER, victim);
		IndependentThread.actions(ActionType.DUMMY_GLOBAL_UPDATE_USER, attacker);
		IndependentThread.actions(ActionType.RANK_UPDATE_USER, victim);
		IndependentThread.action(ActionType.RANK_UPDATE_USER, attacker);
		
		String death = Messages.getInstance().getMessage("rankDeathMessage");
		death = StringUtils.replace(death, "{ATTACKER}", attacker.getName());
		death = StringUtils.replace(death, "{VICTIM}", victim.getName());
		death = StringUtils.replace(death, "{-}", Integer.toString(points));
		death = StringUtils.replace(death, "{+}", Integer.toString(points));
		death = StringUtils.replace(death, "{POINTS}", Integer.toString(victim.getRank().getPoints()));
		if(victim.hasGuild()) death = StringUtils.replace(death, "{VTAG}", 
			StringUtils.replace(Settings.getInstance().chatGuild, "{TAG}", victim.getGuild().getTag()));
		if(attacker.hasGuild()) death = StringUtils.replace(death, "{ATAG}", 
			StringUtils.replace(Settings.getInstance().chatGuild, "{TAG}", attacker.getGuild().getTag()));
		death = StringUtils.replace(death, "{VTAG}", "");
		death = StringUtils.replace(death, "{ATAG}", "");
		event.setDeathMessage(death);
	}

}
