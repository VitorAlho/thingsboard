/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.utils;

import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicKvEntry;
import org.thingsboard.server.common.util.KvProtoUtil;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldEntityCtxIdProto;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldStateProto;
import org.thingsboard.server.gen.transport.TransportProtos.SingleValueArgumentProto;
import org.thingsboard.server.gen.transport.TransportProtos.TsRollingArgumentEntryProto;
import org.thingsboard.server.gen.transport.TransportProtos.TsValueListProto;
import org.thingsboard.server.gen.transport.TransportProtos.TsValueProto;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.ScriptCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.SimpleCalculatedFieldState;
import org.thingsboard.server.service.cf.ctx.state.SingleValueArgumentEntry;
import org.thingsboard.server.service.cf.ctx.state.TsRollingArgumentEntry;

import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

public class CalculatedFieldUtils {

    public static CalculatedFieldEntityCtxIdProto toProto(CalculatedFieldEntityCtxId ctxId) {
        return CalculatedFieldEntityCtxIdProto.newBuilder()
                .setTenantIdMSB(ctxId.tenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(ctxId.tenantId().getId().getLeastSignificantBits())
                .setCalculatedFieldIdMSB(ctxId.cfId().getId().getMostSignificantBits())
                .setCalculatedFieldIdLSB(ctxId.cfId().getId().getLeastSignificantBits())
                .setEntityType(ctxId.entityId().getEntityType().name())
                .setEntityIdMSB(ctxId.entityId().getId().getMostSignificantBits())
                .setEntityIdLSB(ctxId.entityId().getId().getLeastSignificantBits())
                .build();
    }

    public static CalculatedFieldEntityCtxId fromProto(CalculatedFieldEntityCtxIdProto ctxIdProto) {
        TenantId tenantId = TenantId.fromUUID(new UUID(ctxIdProto.getTenantIdMSB(), ctxIdProto.getTenantIdLSB()));
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(ctxIdProto.getEntityType(), new UUID(ctxIdProto.getEntityIdMSB(), ctxIdProto.getEntityIdLSB()));
        CalculatedFieldId calculatedFieldId = new CalculatedFieldId(new UUID(ctxIdProto.getCalculatedFieldIdMSB(), ctxIdProto.getCalculatedFieldIdLSB()));
        return new CalculatedFieldEntityCtxId(tenantId, calculatedFieldId, entityId);
    }

    public static CalculatedFieldStateProto toProto(CalculatedFieldEntityCtxId stateId, CalculatedFieldState state) {
        CalculatedFieldStateProto.Builder builder = CalculatedFieldStateProto.newBuilder()
                .setId(toProto(stateId))
                .setType(state.getType().name());

        state.getArguments().forEach((argName, argEntry) -> {
            if (argEntry instanceof SingleValueArgumentEntry singleValueArgumentEntry) {
                builder.addSingleValueArguments(toSingleValueArgumentProto(argName, singleValueArgumentEntry));
            } else if (argEntry instanceof TsRollingArgumentEntry rollingArgumentEntry) {
                builder.addRollingValueArguments(toRollingArgumentProto(argName, rollingArgumentEntry));
            }
        });

        return builder.build();
    }

    public static SingleValueArgumentProto toSingleValueArgumentProto(String argName, SingleValueArgumentEntry entry) {
        SingleValueArgumentProto.Builder builder = SingleValueArgumentProto.newBuilder()
                .setArgName(argName);

        if (entry.getKvEntryValue() != null) {
            builder.setValue(KvProtoUtil.toTsValueProto(entry.getTs(), entry.getKvEntryValue()));
        }

        Optional.ofNullable(entry.getVersion()).ifPresent(builder::setVersion);

        return builder.build();
    }

    public static TsRollingArgumentEntryProto toRollingArgumentProto(String argName, TsRollingArgumentEntry entry) {
        TsValueListProto.Builder tsRecordsBuilder = TsValueListProto.newBuilder()
                .setKey(argName);

        entry.getTsRecords().forEach((ts, value) -> tsRecordsBuilder.addTsValue(KvProtoUtil.toTsValueProto(ts, value)));

        return TsRollingArgumentEntryProto.newBuilder()
                .setLimit(entry.getLimit())
                .setTimeWindow(entry.getTimeWindow())
                .setRecords(tsRecordsBuilder.build())
                .build();
    }

    public static CalculatedFieldState fromProto(CalculatedFieldStateProto proto) {
        if (StringUtils.isEmpty(proto.getType())) {
            return null;
        }

        CalculatedFieldType type = CalculatedFieldType.valueOf(proto.getType());

        CalculatedFieldState state = switch (type) {
            case SIMPLE -> new SimpleCalculatedFieldState();
            case SCRIPT -> new ScriptCalculatedFieldState();
        };

        proto.getSingleValueArgumentsList().forEach(argProto ->
                state.getArguments().put(argProto.getArgName(), fromSingleValueArgumentProto(argProto)));

        if (CalculatedFieldType.SCRIPT.equals(type)) {
            proto.getRollingValueArgumentsList().forEach(argProto ->
                    state.getArguments().put(argProto.getRecords().getKey(), fromRollingArgumentProto(argProto)));
        }

        return state;
    }

    public static SingleValueArgumentEntry fromSingleValueArgumentProto(SingleValueArgumentProto proto) {
        TsValueProto tsValueProto = proto.getValue();
        long ts = tsValueProto.getTs();
        BasicKvEntry kvEntry = (BasicKvEntry) KvProtoUtil.fromTsValueProto(proto.getArgName(), tsValueProto);
        return new SingleValueArgumentEntry(ts, kvEntry, proto.getVersion());
    }

    public static TsRollingArgumentEntry fromRollingArgumentProto(TsRollingArgumentEntryProto proto) {
        TsValueListProto records = proto.getRecords();
        String key = records.getKey();
        TreeMap<Long, BasicKvEntry> tsRecords = new TreeMap<>();
        records.getTsValueList().forEach(tsValueProto -> {
            BasicKvEntry kvEntry = (BasicKvEntry) KvProtoUtil.fromTsValueProto(key, tsValueProto);
            tsRecords.put(tsValueProto.getTs(), kvEntry);
        });
        return new TsRollingArgumentEntry(tsRecords, proto.getLimit(), proto.getTimeWindow());
    }

}
