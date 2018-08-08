import java.lang.Thread;
import java.lang.Runtime;

import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import java.net.URL;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

class Fetcher {
	String realm;
	String guild_name;
	//String blizzard_API_key = "<API key>";
	String blizzard_API_key = "jwpmupwbb6ja8rmwdbre77e7w7gdcchs";

	String[] characters = new String[1000];
	String[] ilvl = new String[1000];

	int guild_size = 0;

	int cores = Runtime.getRuntime().availableProcessors();
	CyclicBarrier barrier = new CyclicBarrier(cores+1);
	AtomicInteger progress = new AtomicInteger();
	DecimalFormat decimal = new DecimalFormat("#.##");

	Fetcher(String realm, String guild_name) {
		System.out.print("Loading...\r");
		this.realm = realm;
		this.guild_name = guild_name;
		setup();
	}

	private void setup() {
		try {
			Scanner scanner = new Scanner(new URL(get_guild_URL()).openStream(), StandardCharsets.UTF_8.toString()).useDelimiter("\\A");
			String page = scanner.next();
			scanner.close();
			System.out.println("Guild:\t\t"+guild_name);
			System.out.println("Realm:\t\t"+realm);

			Pattern p = Pattern.compile("character.*?name\".\"(.*?)\"");
			Matcher m = p.matcher(page);
			
			while (m.find()) {
				characters[guild_size++] = m.group(1);
			}
			System.out.println("Members:\t"+guild_size+"\n");
		} catch (Exception e) {
			System.out.println(e);
			System.exit(-1);
		}
	}

	private String get_guild_URL() {
		String url = null;
		try {
			url = new URI("https", "eu.api.battle.net", "/wow/guild/"+realm+"/"+guild_name, "fields=members&locale=en_GB&apikey="+blizzard_API_key, null).toASCIIString();;
		} catch (Exception e) {
			System.out.println(e);
			System.exit(-1);
		}
		return url;
	}

	private String get_character_URL(String name) {
		String url = null;
		try {
			url = new URI("https", "eu.api.battle.net", "/wow/character/"+realm+"/"+name, "fields=items&locale=en_GB&apikey="+blizzard_API_key, null).toASCIIString();;
		} catch (Exception e) {
			System.out.println(e);
			System.exit(-1);
		}
		return url;
	}

	private String get_ilvl(String name) {
		try (Scanner scanner = new Scanner(new URL(get_character_URL(name)).openStream(), StandardCharsets.UTF_8.toString()).useDelimiter("\\A");) {
			String page = scanner.next();
			scanner.close();

			Pattern p = Pattern.compile("averageItemLevel\".(.*?),");
			Matcher m = p.matcher(page);
			
			if (m.find()) {
				String tmp_ilvl = m.group(1);
				if (tmp_ilvl.length()==1) tmp_ilvl ="00"+tmp_ilvl;
				if (tmp_ilvl.length()==2) tmp_ilvl ="0"+tmp_ilvl;
				return tmp_ilvl;
			}
		} catch (Exception e) {}
		return "000";
	}

	public void write_lua() {
		try {
			FileOutputStream fileStream = new FileOutputStream(new File("Core.lua"));
			OutputStreamWriter fileWriter = new OutputStreamWriter(fileStream, "UTF-8");
			fileWriter.write("local char_ilvl = {");
			for (int i = 0; i<guild_size; i++) {
				if (i == 0) fileWriter.write("[\""+characters[i]+"-"+realm+"\"] = \""+ilvl[i]+"\"");
				fileWriter.write(",[\""+characters[i]+"-"+realm+"\"] = \""+ilvl[i]+"\"");
			}
			fileWriter.write("}\nlocal f = CreateFrame(\"Frame\")\n");
			fileWriter.write("GuildRoster()\n");
			fileWriter.write("f:RegisterEvent(\"GUILD_ROSTER_UPDATE\")");
			fileWriter.write("f:SetScript(\"OnEvent\",function(self,event,...)\n");
			fileWriter.write("\tif 0~=GetNumGuildMembers() then\n");
			fileWriter.write("\t\tif GetGuildInfo(\"player\")==\""+guild_name+"\" then\n");
			fileWriter.write("\t\t\tfor key, val in pairs(char_ilvl) do\n");
			fileWriter.write("\t\t\t\tfor i=1,GetNumGuildMembers() do\n");
			fileWriter.write("\t\t\t\t\tlocal name = GetGuildRosterInfo(i)\n");
			fileWriter.write("\t\t\t\t\tif name==key then\n");
			fileWriter.write("\t\t\t\t\t\tGuildRosterSetPublicNote(i, \"ilvl \"..val)\n");
			fileWriter.write("\t\t\t\t\tend\n");
			fileWriter.write("\t\t\t\tend\n");
			fileWriter.write("\t\t\tend\n");
			fileWriter.write("\t\t\tf:UnregisterEvent(\"GUILD_ROSTER_UPDATE\");\n");
			fileWriter.write("\t\t\tprint(\"Guild ilvl Updated\")\n");
			fileWriter.write("\t\telse\n");
			fileWriter.write("\t\t\tf:UnregisterEvent(\"GUILD_ROSTER_UPDATE\");\n");
			fileWriter.write("\t\t\tprint(\"Guild ilvl did nothing, wrong guild\")\n");
			fileWriter.write("\t\tend\n");
			fileWriter.write("\tend\n");
			fileWriter.write("end)\n");
			fileWriter.close();
		} catch (Exception e) {
			System.out.println(e);
			System.exit(-1);
		}
	}

	public void run() {
		double time = System.nanoTime();
		for (int i=0; i<cores; i++) {
			new worker(i).start();
		}
		try {barrier.await();} catch (Exception e) {}
		time = System.nanoTime()-time;
		write_lua();
		System.out.println("\nFinished "+guild_size+" guild members in "+decimal.format(time/1000000000)+" sec");
	}

	private class worker extends Thread {
		int id;

		worker(int id) {
			this.id = id;
		}

		public void run() {
			int range = guild_size/cores;
			int start_index = id*range;
			int end_index = start_index+range;
			if (id == cores-1) end_index += guild_size%cores;
			for (int i = start_index; i<end_index; i++) {
				ilvl[i] = get_ilvl(characters[i]);
				System.out.print("Processing guild members: "+decimal.format((double)progress.incrementAndGet()/guild_size*100)+"%   \r");
			}
			try {barrier.await();} catch (Exception e) {}
		}
	}
}