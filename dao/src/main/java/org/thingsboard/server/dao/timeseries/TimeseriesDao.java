/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.dao.timeseries;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.List;

/**
 * @author Andrew Shvayka
 */
public interface TimeseriesDao {

    ListenableFuture<List<ReadTsKvQueryResult>> findAllAsync(TenantId tenantId, EntityId entityId, List<ReadTsKvQuery> queries);

    ListenableFuture<Integer> save(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry, long ttl);

    ListenableFuture<Integer> savePartition(TenantId tenantId, EntityId entityId, long tsKvEntryTs, String key);

    ListenableFuture<Void> remove(TenantId tenantId, EntityId entityId, DeleteTsKvQuery query);

    void cleanup(long systemTtl);
}
