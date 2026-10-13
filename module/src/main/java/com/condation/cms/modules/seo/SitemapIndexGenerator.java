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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

final class SitemapIndexGenerator implements AutoCloseable {

    private final OutputStream output;

    SitemapIndexGenerator(OutputStream output) {
        this.output = output;
    }

    void start() throws IOException {
        output.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
                .getBytes(StandardCharsets.UTF_8));
    }

    void addSitemap(String url) throws IOException {
        output.write("<sitemap><loc>%s</loc></sitemap>"
                .formatted(SitemapGenerator.escapeXml(url)).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void close() throws IOException {
        output.write("</sitemapindex>".getBytes(StandardCharsets.UTF_8));
        output.close();
    }
}
