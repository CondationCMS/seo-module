
package com.condation.cms.modules.seo.api.robots;

/*-
 * #%L
 * seo-api
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

import java.util.ArrayList;
import java.util.List;

public final class RobotsGroup {

    private final String userAgent;
    private final List<String> allows = new ArrayList<>();
    private final List<String> disallows = new ArrayList<>();

    public RobotsGroup(String userAgent) {
        this.userAgent = userAgent;
    }

    public String userAgent() {
        return userAgent;
    }

    public List<String> allows() {
        return allows;
    }

    public List<String> disallows() {
        return disallows;
    }

    public void allow (String url) {
        this.allows.add(url);
    }

    public void disallow (String url) {
        this.disallows.add(url);
    }

    public boolean isEmpty() {
        return userAgent == null || userAgent.isBlank();
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "";
        }

        StringBuilder output = new StringBuilder();

        output.append("User-agent: ")
                .append(userAgent.trim())
                .append('\n');

        for (String allow : allows) {
            appendDirective(output, "Allow", allow);
        }

        for (String disallow : disallows) {
            appendDirective(output, "Disallow", disallow);
        }

        return output.toString();
    }

    private void appendDirective(StringBuilder output, String directive, String value) {
        if (value == null) {
            return;
        }

        output.append(directive)
                .append(": ")
                .append(value.trim())
                .append('\n');
    }
}
