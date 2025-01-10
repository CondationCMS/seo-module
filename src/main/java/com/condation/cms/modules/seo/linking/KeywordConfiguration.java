package com.condation.cms.modules.seo.linking;

/*-
 * #%L
 * seo-module
 * %%
 * Copyright (C) 2024 - 2025 CondationCMS
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author t.marx
 */
public class KeywordConfiguration {

	public final Path configFile;
	private FileTime lastModifiedTime;
	private final Consumer<Keyword> updateConsumer;
	private final ProcessingConfig config;

	public KeywordConfiguration(Path configFile, Consumer<Keyword> updateConsumer, ProcessingConfig config) throws IOException {
		this.configFile = configFile;
		this.updateConsumer = updateConsumer;
		this.config = config;
	}

	public boolean isUpdateNecassary () {
		try {
			return isModified();
		} catch (IOException ex) {
			return false;
		}
	}
	
	private boolean isModified() throws IOException {
		if (Files.exists(configFile) && lastModifiedTime == null) {
			return true;
		} else if (!Files.exists(configFile)) {
			return false;
		} else if (
				Files.exists(configFile)
				&& Files.getLastModifiedTime(configFile).compareTo(lastModifiedTime) > 0
				) {
			return true;
		}

		return false;
	}

	private void updateLastModifiedTime() throws IOException {
		if (Files.exists(configFile)) {
			this.lastModifiedTime = Files.getLastModifiedTime(configFile);
		}
	}

	public void update() {

		if (!Files.exists(configFile)) {
			return;
		}
		
		try (FileInputStream fis = new FileInputStream(this.configFile.toFile())) {
			Yaml yaml = new Yaml();
			Map<String, Object> yamlData = yaml.load(fis);
			
			config.setCaseSensitive((boolean) yamlData.getOrDefault("caseSensitive", true));
			config.setWholeWordsOnly((boolean) yamlData.getOrDefault("wholeWordsOnly", true));
			
			List<Map<String, Object>> replacements = (List<Map<String, Object>>) yamlData.getOrDefault("replacements", Collections.emptyList());
			
			if (replacements != null) {
				replacements.stream()
						.map(map -> new Keyword((String)map.get("link"), (List<String>)map.get("keywords")))
						.forEach(updateConsumer);
			}
			
			updateLastModifiedTime();
			
		} catch (IOException e) {
			throw new RuntimeException("Failed to load configuration from " + configFile.toString(), e);
		}
	}
	
	public static record Keyword (String url, List<String> keywords) {
		public String[] keywordArray () {
			return keywords.toArray(String[]::new);
		}
	}

}
