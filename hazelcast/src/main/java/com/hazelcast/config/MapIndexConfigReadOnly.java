/*
 * Copyright (c) 2008-2016, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.config;

/**
 * Contains the configuration for a index of Map.
 *
 * @deprecated this class will be removed in 3.8; it is meant for internal usage only.
 */
public class MapIndexConfigReadOnly extends MapIndexConfig {

    public MapIndexConfigReadOnly(MapIndexConfig config) {
        super(config);
    }

    public MapIndexConfig setAttribute(String attribute) {
        throw new UnsupportedOperationException("This config is read-only");
    }

    public MapIndexConfig setOrdered(boolean ordered) {
        throw new UnsupportedOperationException("This config is read-only");
    }
}
