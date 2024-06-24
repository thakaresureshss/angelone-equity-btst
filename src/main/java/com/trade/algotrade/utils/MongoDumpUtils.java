package com.trade.algotrade.utils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MongoDumpUtils {

	@Value("${application.mongodbTools}")
	private String mongoToolsPath;

	public void dumpDBColletions() {
		List<String> commands = Arrays.asList(mongoToolsPath + "/bin/mongodump", "--db", "algotrade",
				"--excludeCollection", "candle_data", "--excludeCollection", "instrument_master", "--excludeCollection",
				"instrument_watch_master", "--excludeCollection", "open_orders", "--excludeCollection", "closed_orders",
				"--excludeCollection", "open_trades", "--out", mongoToolsPath);

		ProcessBuilder builder = new ProcessBuilder(commands);
		builder.inheritIO();
		try {
			builder.start();
		} catch (IOException e) {
			System.out.println("error" + e);
		}
	}

	public void restoreDBDumps(String collection) {
		List<String> commands = Arrays.asList(mongoToolsPath + "/bin/mongorestore", "--nsInclude", "algotrade."
				.concat(collection).concat(" ").concat(mongoToolsPath).concat("/aglotrade/").concat(collection).concat(".bson"));
		ProcessBuilder builder = new ProcessBuilder(commands);
		builder.inheritIO();
		try {
			builder.start();
		} catch (IOException e) {
			System.out.println("error" + e);
		}
	}
}
