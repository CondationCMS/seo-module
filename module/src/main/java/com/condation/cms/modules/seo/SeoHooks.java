package com.condation.cms.modules.seo;

/*-
 * #%L
 * seo-module
 * %%
 * Copyright (C) 2024 - 2026 CondationCMS
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
import com.condation.cms.api.SiteProperties;
import com.condation.cms.api.annotations.Action;
import com.condation.cms.api.db.ContentNode;
import com.condation.cms.api.extensions.HookSystemRegisterExtensionPoint;
import com.condation.cms.api.feature.features.CurrentNodeFeature;
import com.condation.cms.api.feature.features.RequestFeature;
import com.condation.cms.api.feature.features.SitePropertiesFeature;
import com.condation.modules.api.annotation.Extension;
import java.util.List;

/**
 *
 * @author thorstenmarx
 */
@Extension(HookSystemRegisterExtensionPoint.class)
public class SeoHooks extends HookSystemRegisterExtensionPoint {

	@Action("system/layout/html/header")
	public String meta() {
		if (!getRequestContext().has(CurrentNodeFeature.class)) {
			return "";
		}
		ContentNode node = getRequestContext().get(CurrentNodeFeature.class).node();
		SiteProperties siteProperties = getRequestContext().get(SitePropertiesFeature.class).siteProperties();

		StringBuilder sb = new StringBuilder();

		appendRobots(sb, node, siteProperties);

		appendOpenGraph(sb, node, siteProperties);

		appendTwitterCard(sb, node, siteProperties);

		appendCanonical(sb, node, siteProperties);

		return sb.toString();
	}

	private void appendRobots(StringBuilder sb, ContentNode node, SiteProperties siteProperties) {
		boolean index = node.getMetaValue("seo.index", true);
		boolean follow = node.getMetaValue("seo.follow", true);

		if (!index) {
			appendRobotsTag(sb, false, follow);
			return;
		}

		var request = getRequestContext().get(RequestFeature.class);

		List<String> noindexParameters = siteProperties.getOrDefault(
				"seo.query.noindexParameters",
				List.of());

		boolean hasNoindexParameter = noindexParameters.stream()
				.anyMatch(request::hasQueryParameter);

		if (hasNoindexParameter) {
			appendRobotsTag(sb, false, true);
			return;
		}

		int maxIndexableParameters = siteProperties.getOrDefault(
				"seo.query.maxIndexableContentParameters",
				-1);

		if (maxIndexableParameters >= 0) {
			List<String> contentQueryParameters = siteProperties.getOrDefault(
					"seo.query.contentParameters",
					List.of("page"));

			long activeContentParamCount = contentQueryParameters.stream()
					.filter(name -> !"page".equals(name))
					.filter(name -> !noindexParameters.contains(name))
					.filter(request::hasQueryParameter)
					.count();

			if (activeContentParamCount > maxIndexableParameters) {
				appendRobotsTag(sb, false, true);
				return;
			}
		}
	}

	private void appendRobotsTag(StringBuilder sb, boolean index, boolean follow) {
		sb.append("<meta name=\"robots\" content=\"")
				.append(index ? "index" : "noindex")
				.append(',')
				.append(follow ? "follow" : "nofollow")
				.append("\" />\n");
	}

	private void appendCanonical(StringBuilder sb, ContentNode node, SiteProperties siteProperties) {
		if (!siteProperties.getOrDefault("seo.canonical.enabled", true)) {
			return;
		}
		String canonicalUrl = SeoUrlHelper.createCanonical(siteProperties, node);

		var request = getRequestContext().get(RequestFeature.class);
		List<String> canonicalQueryParameters = siteProperties.getOrDefault("seo.canonical.queryParameters",
				List.of("page"));

		StringBuilder query = new StringBuilder();
		for (String name : canonicalQueryParameters) {
			if ("page".equals(name)) {
				int page = request.getQueryParameterAsInt("page", 1);
				if (page > 1) {
					query.append(query.isEmpty() ? "?" : "&").append("page=").append(page);
				}
				continue;
			}
			if (request.hasQueryParameter(name)) {
				String value = request.getQueryParameter(name, "");
				if (!value.isBlank()) {
					query.append(query.isEmpty() ? "?" : "&").append(name).append("=").append(value);
				}
			}
		}
		canonicalUrl += query.toString();

		sb.append("<link rel=\"canonical\" href=\"").append(canonicalUrl).append("\" />");
	}

	private void appendOpenGraph(StringBuilder sb, ContentNode node, SiteProperties siteProperties) {
		if (!siteProperties.getOrDefault("seo.opengraph.enabled", true)) {
			return;
		}
		String sharedTitle = node.getMetaValue("seo.title", node.getMetaValue("title", ""));
		String sharedDescription = node.getMetaValue("seo.description", node.getMetaValue("excerpt", ""));
		String sharedImage = node.getMetaValue("seo.image", "");
		String sharedUrl = node.getMetaValue("seo.url", SeoUrlHelper.createUrl(siteProperties, node));

		String type = node.getMetaValue("seo.og.type", siteProperties.getOrDefault("seo.og.type", "website"));
		String url = node.getMetaValue("seo.og.url", sharedUrl);
		String title = node.getMetaValue("seo.og.title", sharedTitle);
		String description = node.getMetaValue("seo.og.description", sharedDescription);
		String image = node.getMetaValue("seo.og.image", sharedImage);

		sb.append("<meta property=\"og:type\" content=\"").append(escapeHtml(type)).append("\" />\n");
		sb.append("<meta property=\"og:url\" content=\"").append(escapeHtml(url)).append("\" />\n");
		if (!title.isBlank()) {
			sb.append("<meta property=\"og:title\" content=\"").append(escapeHtml(title)).append("\" />\n");
		}
		if (!description.isBlank()) {
			sb.append("<meta property=\"og:description\" content=\"").append(escapeHtml(description)).append("\" />\n");
		}
		if (!image.isBlank()) {
			sb.append("<meta property=\"og:image\" content=\"").append(escapeHtml(image)).append("\" />\n");
		}
	}

	private void appendTwitterCard(StringBuilder sb, ContentNode node, SiteProperties siteProperties) {
		if (!siteProperties.getOrDefault("seo.twitter.enabled", true)) {
			return;
		}
		String sharedTitle = node.getMetaValue("seo.title", node.getMetaValue("title", ""));
		String sharedDescription = node.getMetaValue("seo.description", node.getMetaValue("excerpt", ""));
		String sharedImage = node.getMetaValue("seo.image", "");
		String sharedUrl = node.getMetaValue("seo.url", SeoUrlHelper.createUrl(siteProperties, node));

		String card = node.getMetaValue("seo.twitter.card", siteProperties.getOrDefault("seo.twitter.card", "summary"));
		String url = node.getMetaValue("seo.twitter.url", sharedUrl);
		String title = node.getMetaValue("seo.twitter.title", sharedTitle);
		String description = node.getMetaValue("seo.twitter.description", sharedDescription);
		String image = node.getMetaValue("seo.twitter.image", sharedImage);
		String site = node.getMetaValue("seo.twitter.site", siteProperties.getOrDefault("seo.twitter.site", ""));
		String creator = node.getMetaValue("seo.twitter.creator",
				siteProperties.getOrDefault("seo.twitter.creator", ""));

		sb.append("<meta name=\"twitter:card\" content=\"").append(escapeHtml(card)).append("\" />\n");
		sb.append("<meta name=\"twitter:url\" content=\"").append(escapeHtml(url)).append("\" />\n");
		if (!title.isBlank()) {
			sb.append("<meta name=\"twitter:title\" content=\"").append(escapeHtml(title)).append("\" />\n");
		}
		if (!description.isBlank()) {
			sb.append("<meta name=\"twitter:description\" content=\"").append(escapeHtml(description))
					.append("\" />\n");
		}
		if (!image.isBlank()) {
			sb.append("<meta name=\"twitter:image\" content=\"").append(escapeHtml(image)).append("\" />\n");
		}
		if (!site.isBlank()) {
			sb.append("<meta name=\"twitter:site\" content=\"").append(escapeHtml(site)).append("\" />\n");
		}
		if (!creator.isBlank()) {
			sb.append("<meta name=\"twitter:creator\" content=\"").append(escapeHtml(creator)).append("\" />\n");
		}
	}

	private String escapeHtml(String value) {
		return value
				.replace("&", "&amp;")
				.replace("\"", "&quot;")
				.replace("<", "&lt;")
				.replace(">", "&gt;");
	}
}
