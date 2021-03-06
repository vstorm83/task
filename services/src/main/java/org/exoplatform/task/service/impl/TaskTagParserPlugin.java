/*
 * Copyright (C) 2015 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.task.service.impl;

import org.exoplatform.task.service.TaskBuilder;
import org.exoplatform.task.service.TaskParserPlugin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
public class TaskTagParserPlugin implements TaskParserPlugin {
  @Override
  public String parse(String input, TaskBuilder builder) {
    Pattern p = Pattern.compile("(\\s)(#)([0-9a-zA-Z_]+)");
    Matcher m = p.matcher(input);
    while(m.find()) {
      builder.addTag(m.group(3));
    }

    String in = input.replaceAll("\\s#[0-9a-zA-Z_]+\\s*", " ").trim();
    return in;
  }
}
