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
import com.condation.cms.api.db.ContentNode;
import com.condation.cms.api.utils.HTTPUtil;
import com.condation.cms.api.utils.PathUtil;

final class SeoUrlHelper {

	private SeoUrlHelper () {
	}
	
	public static String createCanonical (final SiteProperties siteProperties, final ContentNode node) {
		var canonicalUrl = PathUtil.toURL(node.uri());
		return siteProperties.baseUrl() + HTTPUtil.prependContext(canonicalUrl, siteProperties);
	}

	static String createUrl (final SiteProperties siteProperties, final ContentNode node) {
		return createUrl(siteProperties, node.uri());
	}

	static String createUrl (final SiteProperties siteProperties, final String uri) {
		var path = PathUtil.toURL(uri);
		return siteProperties.baseUrl() + HTTPUtil.prependContext(path, siteProperties);
	}
}
