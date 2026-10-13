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
import com.condation.cms.api.configuration.configs.CollectionConfiguration;
import com.condation.cms.api.configuration.configs.CollectionDefinition;
import com.condation.cms.api.db.collection.CollectionItemMetadata;
import com.condation.cms.api.feature.features.ConfigurationFeature;
import com.condation.cms.api.feature.features.RepositoryFeature;
import com.condation.cms.api.utils.MapUtil;
import com.condation.cms.content.CollectionRouteTemplate;
import java.util.Comparator;

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
            try (var sitemap = new SitemapIndexGenerator(Response.asBufferedOutputStream(request, response))) {
                response.getHeaders().add(HttpHeader.CONTENT_TYPE, "application/xml");
                sitemap.start();
                sitemap.addSitemap(SeoUrlHelper.createUrl(siteProperties, "sitemap-nodes.xml"));
                for (var definition : collectionDefinitions().collections().values().stream()
                        .filter(entry -> entry.detailPage().isPresent())
                        .sorted(Comparator.comparing(CollectionDefinition::name))
                        .toList()) {
                    sitemap.addSitemap(SeoUrlHelper.createUrl(siteProperties,
                            "sitemap-collection-" + definition.name() + ".xml"));
                }
            } catch (Exception e) {
                callback.failed(e);
                return true;
            }
            callback.succeeded();
            return true;
        }
        return false;
    }

    @Route("/sitemap-nodes.xml")
    public boolean sitemapNodes(Request request, Response response, Callback callback) throws Exception {
        final SiteProperties siteProperties = context.get(SitePropertiesFeature.class).siteProperties();
        if (!siteProperties.getOrDefault("seo.sitemap.enabled", true)) {
            return false;
        }

        try (var sitemap = new SitemapGenerator(
                Response.asBufferedOutputStream(request, response), siteProperties)) {
            response.getHeaders().add(HttpHeader.CONTENT_TYPE, "application/xml");
            sitemap.start();
            for (var node : context.get(DBFeature.class).db().getContent()
                    .query((entry, length) -> entry).get()) {
                if (node.getMetaValue("seo.index", true)) {
                    sitemap.addNode(node);
                }
            }
        } catch (Exception e) {
            callback.failed(e);
            return true;
        }
        callback.succeeded();
        return true;
    }

    @Route("^/sitemap-collection-([a-zA-Z0-9_-]+)\\.xml$")
    public boolean sitemapCollection(Request request, Response response, Callback callback) throws Exception {
        final SiteProperties siteProperties = context.get(SitePropertiesFeature.class).siteProperties();
        if (!siteProperties.getOrDefault("seo.sitemap.enabled", true)) {
            return false;
        }

        var path = request.getHttpURI().getPath();
        var name = path.substring(path.lastIndexOf("/sitemap-collection-") + "/sitemap-collection-".length(),
                path.length() - ".xml".length());
        var definition = collectionDefinitions().collection(name);
        if (definition.isEmpty() || definition.get().detailPage().isEmpty()) {
            response.setStatus(404);
            callback.succeeded();
            return true;
        }

        try (var sitemap = new SitemapGenerator(
                Response.asBufferedOutputStream(request, response), siteProperties)) {
            response.getHeaders().add(HttpHeader.CONTENT_TYPE, "application/xml");
            sitemap.start();
            var route = new CollectionRouteTemplate(definition.get().detailPage().orElseThrow());
            var repository = context.get(RepositoryFeature.class).collectionRepository();
            for (CollectionItemMetadata item : repository.metadataQuery(name).get()) {
                if (Boolean.FALSE.equals(MapUtil.getValue(item.meta(), "seo.index"))) {
                    continue;
                }
                try {
                    sitemap.addUrl(SeoUrlHelper.createUrl(siteProperties, route.render(item.id(), item.meta())));
                } catch (IllegalArgumentException e) {
                    log.warn("Skipping collection item {}/{} without a valid detail URL", name, item.id(), e);
                }
            }
        } catch (Exception e) {
            callback.failed(e);
            return true;
        }
        callback.succeeded();
        return true;
    }

    private CollectionConfiguration collectionDefinitions() {
        var configuration = context.get(ConfigurationFeature.class)
                .configuration().get(CollectionConfiguration.class);
        return configuration != null ? configuration : new CollectionConfiguration(java.util.Map.of());
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
