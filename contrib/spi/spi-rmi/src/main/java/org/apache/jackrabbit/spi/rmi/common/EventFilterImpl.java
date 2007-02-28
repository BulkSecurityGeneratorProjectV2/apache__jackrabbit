/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.spi.rmi.common;

import org.apache.jackrabbit.spi.EventFilter;
import org.apache.jackrabbit.spi.Event;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.MalformedPathException;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.PathNotFoundException;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Collections;
import java.io.Serializable;

/**
 * <code>EventFilterImpl</code> is the spi2dav implementation of an
 * {@link EventFilter}.
 * TODO: copied from spi2dav, move to spi-commons?
 */
public class EventFilterImpl implements EventFilter, Serializable {

    /**
     * The logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(EventFilterImpl.class);

    private final int eventTypes;

    private final boolean isDeep;

    private final Path absPath;

    private final Set uuids;

    private final Set nodeTypeNames;

    private final boolean noLocal;

    /**
     * Creates a new <code>EventFilterImpl</code>.
     *
     * @param eventTypes    the event types this filter is interested in.
     * @param absPath       filter events that are below this path.
     * @param isDeep        whether this filter is applied deep.
     * @param uuids         the jcr:uuid of the nodes this filter allows.
     * @param nodeTypeNames the QNames of the already resolved node types this
     *                      filter allows.
     * @param noLocal       whether this filter accepts local events or not.
     */
    public EventFilterImpl(int eventTypes,
                    Path absPath,
                    boolean isDeep,
                    String[] uuids,
                    Set nodeTypeNames,
                    boolean noLocal) {
        this.eventTypes = eventTypes;
        this.absPath = absPath;
        this.isDeep = isDeep;
        this.uuids = uuids != null ? new HashSet(Arrays.asList(uuids)) : null;
        this.nodeTypeNames = nodeTypeNames != null ? new HashSet(nodeTypeNames) : null;
        this.noLocal = noLocal;
    }

    /**
     * {@inheritDoc}
     */
    public boolean accept(Event event, boolean isLocal) {
        int type = event.getType();
        // check type
        if ((type & eventTypes) == 0) {
            return false;
        }

        // check local flag
        if (isLocal && noLocal) {
            return false;
        }

        // check UUIDs
        NodeId parentId = event.getParentId();
        if (uuids != null) {
            if (parentId.getPath() == null) {
                if (!uuids.contains(parentId.getUniqueID())) {
                    return false;
                }
            } else {
                return false;
            }
        }

        // check node types
        if (nodeTypeNames != null) {
            Set eventTypes = new HashSet();
            eventTypes.addAll(Arrays.asList(event.getMixinTypeNames()));
            eventTypes.add(event.getPrimaryNodeTypeName());
            // create intersection
            eventTypes.retainAll(nodeTypeNames);
            if (eventTypes.isEmpty()) {
                return false;
            }
        }

        // finally check path
        try {
            // the relevant path for the path filter depends on the event type
            // for node events, the relevant path is the one returned by
            // Event.getPath().
            // for property events, the relevant path is the path of the
            // node where the property belongs to.
            Path eventPath;
            if (type == Event.NODE_ADDED || type == Event.NODE_REMOVED) {
                eventPath = event.getQPath();
            } else {
                eventPath = event.getQPath().getAncestor(1);
            }

            boolean match = eventPath.equals(absPath);
            if (!match && isDeep) {
                match = eventPath.isDescendantOf(absPath);
            }
            return match;
        } catch (MalformedPathException e) {
            // should never get here
            log.warn("malformed path: " + e);
            log.debug("Exception: ", e);
        } catch (PathNotFoundException e) {
            // should never get here
            log.warn("invalid property path: " + e);
            log.debug("Exception: ", e);
        }
        // if we get here an exception occurred while checking for the path
        return false;
    }

    /**
     * @return the event types this event filter accepts.
     */
    public int getEventTypes() {
        return eventTypes;
    }

    /**
     * @return <code>true</code> if this event filter is deep.
     */
    public boolean isDeep() {
        return isDeep;
    }

    /**
     * @return the path to the item where events are filtered.
     */
    public Path getAbsPath() {
        return absPath;
    }

    /**
     * @return the uuids of the nodes of this filter or <code>null</code> if
     *         this filter does not care about uuids.
     */
    public String[] getUUIDs() {
        if (uuids == null) {
            return null;
        } else {
            return (String[]) uuids.toArray(new String[uuids.size()]);
        }
    }

    /**
     * @return an unmodifiable set of node type names or <code>null</code> if
     *         this filter does not care about node types.
     */
    public Set getNodeTypeNames() {
        if (nodeTypeNames == null) {
            return null;
        } else {
            return Collections.unmodifiableSet(nodeTypeNames);
        }
    }

    /**
     * @return if this filter accepts local events.
     */
    public boolean getNoLocal() {
        return noLocal;
    }
}
