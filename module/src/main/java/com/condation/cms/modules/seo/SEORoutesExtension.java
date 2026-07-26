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
import java.io.IOException;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import com.condation.cms.api.annotations.Route;
import com.condation.cms.api.extensions.http.routes.RoutesExtensionPoint;
import com.condation.cms.api.feature.features.DBFeature;
import com.condation.cms.api.feature.features.HookSystemFeature;
import com.condation.cms.api.feature.features.SitePropertiesFeature;
import com.condation.modules.api.annotation.Extension;

import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author t.marx
 */
@Slf4j
@Extension(RoutesExtensionPoint.class)
public class SEORoutesExtension extends RoutesExtensionPoint {

    @Route("/sitemap.xml")
    public boolean sitemap(Request request, Response response, Callback callback) throws Exception {
        final SiteProperties siteProperties = context.get(SitePropertiesFeature.class).siteProperties();

        if (siteProperties.getOrDefault("seo.sitemap.enabled", true)) {
            try (var sitemap = new SitemapGenerator(
                    Response.asBufferedOutputStream(request, response), siteProperties)) {
                response.getHeaders().add(HttpHeader.CONTENT_TYPE, "application/xml");
                sitemap.start();
                context.get(DBFeature.class).db().getContent().query((node, length) -> node).get().forEach(node -> {
                    try {
                        if (node.getMetaValue("seo.index", true)) {
                            sitemap.addNode(node);
                        }
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

        return false;

    }

    @Route("/robots.txt")
    public boolean robots_txt(Request request, Response response, Callback callback) throws Exception {

        final SiteProperties siteProperties = context.get(SitePropertiesFeature.class).siteProperties();
        
        if (siteProperties.getOrDefault("seo.robotstxt.enabled", true)) {
            try (var robotstxt = new RobotsTxtGenerator(
                    Response.asBufferedOutputStream(request, response),
                    siteProperties,
                    getRequestContext().get(HookSystemFeature.class).hookSystem())) {
                response.getHeaders().add(HttpHeader.CONTENT_TYPE, "text/plain");
                robotstxt.create();
            } catch (Exception e) {
                log.error(null, e);
            }
            callback.succeeded();

            return true;
        }
        return false;
    }

}
