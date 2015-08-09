/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehcache.loaderwriter.writebehind;

import org.ehcache.loaderwriter.writebehind.operations.SingleOperation;
import org.ehcache.spi.loaderwriter.CacheLoaderWriter;
import org.ehcache.spi.loaderwriter.WriteBehindConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Per Johansson
 *
 */
public class CoalescingLocalHeapWriteBehindQueue<K, V> extends AbstractLocalHeapWriteBehindQueue<K, V> {

    private Map<K, SingleOperation<K, V>> waiting = new LinkedHashMap<K, SingleOperation<K, V>>();

    CoalescingLocalHeapWriteBehindQueue(WriteBehindConfiguration config, CacheLoaderWriter<K, V> cacheLoaderWriter) {
        super(config, cacheLoaderWriter);
    }

    @Override
    protected List<SingleOperation<K, V>> quarantineItems() {
        List<SingleOperation<K, V>> quarantined = new ArrayList<SingleOperation<K, V>>(waiting.values());
        waiting = new LinkedHashMap<K, SingleOperation<K, V>>();
        return quarantined;
    }

    @Override
    protected void addItemToLocalHeap(SingleOperation<K, V> operation) {
        waiting.put(operation.getKey(), operation);
    }

    @Override
    protected void reinsertUnprocessedItems(List<SingleOperation<K, V>> operations) {
        Map<K, SingleOperation<K, V>> newQueue =  new LinkedHashMap<K, SingleOperation<K, V>>();
        for(SingleOperation<K, V> operation : operations) {
            newQueue.put(operation.getKey(), operation);
        }
        newQueue.putAll(waiting);
        waiting = newQueue;
    }

    @Override
    public long getQueueSize() {
        return waiting.size();
    }

}

