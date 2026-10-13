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

import com.condation.cms.api.configuration.configs.CollectionDetailConfiguration;
import com.condation.cms.content.CollectionRouteTemplate;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.eclipse.jetty.http.pathmap.PathSpec;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SitemapGenerationTest {

    @Test
    void indexContainsAbsoluteEscapedSitemapLocations() throws Exception {
        var output = new ByteArrayOutputStream();
        try (var index = new SitemapIndexGenerator(output)) {
            index.start();
            index.addSitemap("https://example.org/site/sitemap-nodes.xml?a=1&b=2");
            index.addSitemap("https://example.org/site/sitemap-collection-blog.xml");
        }

        var document = parse(output);
        assertEquals("sitemapindex", document.getDocumentElement().getLocalName());
        var locations = document.getElementsByTagNameNS("http://www.sitemaps.org/schemas/sitemap/0.9", "loc");
        assertEquals(2, locations.getLength());
        assertEquals("https://example.org/site/sitemap-nodes.xml?a=1&b=2", locations.item(0).getTextContent());
    }

    @Test
    void collectionSitemapUsesConfiguredDetailUrl() throws Exception {
        var detail = new CollectionDetailConfiguration(
                "/events/{date:yyyy}/{location.country}/{id}",
                "collections/event.html",
                Map.of("location.country", Map.of("de", "germany")));
        var route = new CollectionRouteTemplate(detail);
        var url = "https://example.org/site" + route.render("my-event", Map.of(
                "date", "2026-09-03", "location", Map.of("country", "de")));

        var output = new ByteArrayOutputStream();
        try (var sitemap = new SitemapGenerator(output, null)) {
            sitemap.start();
            sitemap.addUrl(url);
        }

        var document = parse(output);
        assertEquals("urlset", document.getDocumentElement().getLocalName());
        assertEquals(url, document.getElementsByTagName("loc").item(0).getTextContent());
        assertEquals(0, document.getElementsByTagName("lastmod").getLength());
    }

    @Test
    void collectionRouteMatchesOnlyCollectionSitemaps() throws Exception {
        var annotation = SEORoutesExtension.class
                .getMethod("sitemapCollection", org.eclipse.jetty.server.Request.class,
                        org.eclipse.jetty.server.Response.class, org.eclipse.jetty.util.Callback.class)
                .getAnnotation(com.condation.cms.api.annotations.Route.class);
        var path = PathSpec.from(annotation.value());
        assertTrue(path.matches("/sitemap-collection-blog.xml"));
        assertFalse(path.matches("/sitemap-nodes.xml"));
        assertFalse(path.matches("/sitemap-collection-blog.xml/extra"));
    }

    private static org.w3c.dom.Document parse(ByteArrayOutputStream output) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(new InputSource(
                new StringReader(output.toString(StandardCharsets.UTF_8))));
    }
}
