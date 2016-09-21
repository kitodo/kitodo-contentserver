/*
 * This file is a contribution to the the ContentServer project, mainly for research purposes.
 * 
 * Copyright 2009, Christian Mahnke<cmahnke@gmail.com>.
 * 
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.unigoettingen.sub.commons.util.datasource;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * The Class StructureDumper. A simple class for tests and debugging.
 */
public class StructureDumper {

    private static final Logger LOGGER = Logger.getLogger(StructureDumper.class);

    /** The struct. */
    List<Structure> structList = null;

    public StructureDumper(Structure struct) {
        structList = new ArrayList<Structure>();
        structList.add(struct);
    }

    @SuppressWarnings("unchecked")
    public StructureDumper(StructureSource structSource) {
        this.structList = (List<Structure>) structSource.getStructureList();
    }

    /**
     * Dump.
     */
    public void dump() {
        for (Structure struct : structList) {
            LOGGER.info("ROOT: " + struct.getContent());
            dump(struct, 1);
        }
    }

    /**
     * Dump.
     * 
     * @param struct the struct
     * @param level the level
     */
    protected void dump(Structure struct, Integer level) {
        StringBuffer ident = new StringBuffer();
        for (Structure child : struct.getChildren()) {
            ident.setLength(0);
            for (int i = 0; i < level; i++) {
                ident.append(' ');
            }
            LOGGER.info(ident.toString() + "+ " + child.getContent());
            if (struct.getChildren().size() != 0) {
                dump(struct, level + 1);
            }
        }
    }

}
