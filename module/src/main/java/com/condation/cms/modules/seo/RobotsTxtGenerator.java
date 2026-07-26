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

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.condation.cms.api.SiteProperties;
import com.condation.cms.api.hooks.HookSystem;
import com.condation.cms.modules.seo.api.robots.RobotsTxt;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RobotsTxtGenerator implements AutoCloseable {
    private final OutputStream output;
    private final SiteProperties siteProperties;
    
    private final HookSystem hookSystem;
    
    public void create() throws Exception {
        RobotsTxt robotstxt = new RobotsTxt();

        if (siteProperties.getOrDefault("seo.sitemap.enabled", true)) {
            robotstxt.addSitemap(SeoUrlHelper.createUrl(siteProperties, "sitemap.xml"));
        }
        
        robotstxt = hookSystem.doFilter("module/seo/robotstxt", robotstxt);
        
        output.write(robotstxt.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void close() throws Exception {
        output.close();
    }
}
