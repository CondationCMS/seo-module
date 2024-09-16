package com.condation.cms.modules.seo;

/*-
 * #%L
 * example-module
 * %%
 * Copyright (C) 2023 Marx-Software
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

import com.condation.cms.api.extensions.HttpHandler;
import com.condation.cms.api.extensions.HttpHandlerExtensionPoint;
import com.condation.cms.api.extensions.Mapping;
import com.condation.cms.api.feature.features.DBFeature;
import com.condation.cms.api.feature.features.SitePropertiesFeature;
import com.condation.cms.api.module.CMSModuleContext;
import com.condation.modules.api.annotation.Extension;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.pathmap.PathSpec;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

/**
 *
 * @author t.marx
 */
@Slf4j
@Extension(HttpHandlerExtensionPoint.class)
public class SitemapJettyHttpHandlerExtension extends HttpHandlerExtensionPoint {

	@Override
	public Mapping getMapping() {
		Mapping mapping = new Mapping();
		mapping.add(PathSpec.from("/sitemap.xml"), new SitemapHandler(getContext()));
		return mapping;
	}
	
	@RequiredArgsConstructor
	public static class SitemapHandler implements HttpHandler {

		private final CMSModuleContext context;
		
		@Override
		public boolean handle(Request request, Response response, Callback callback) throws Exception {
			
			try (var sitemap = new SitemapGenerator(
					Response.asBufferedOutputStream(request, response),
					context.get(SitePropertiesFeature.class).siteProperties()
			)) {
				response.getHeaders().add(HttpHeader.CONTENT_TYPE, "application/xml");
				sitemap.start();
				context.get(DBFeature.class).db().getContent().query((node, length) -> node).get().forEach(node -> {
					try {
						sitemap.addNode(node);
					} catch (IOException ex) {
						log.error(null, ex);
					}
				});
			} catch (Exception e) {
				log.error(null, e);
			}
			callback.succeeded();
			
			return true;
		}
		
	}
}
