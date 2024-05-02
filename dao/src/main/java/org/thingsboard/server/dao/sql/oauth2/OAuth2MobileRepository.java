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
package org.thingsboard.server.dao.sql.oauth2;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.OAuth2DomainEntity;
import org.thingsboard.server.dao.model.sql.OAuth2MobileEntity;

import java.util.List;
import java.util.UUID;

public interface OAuth2MobileRepository extends JpaRepository<OAuth2MobileEntity, UUID> {

    List<OAuth2MobileEntity> findByOauth2ParamsId(UUID oauth2ParamsId);

    @Query("SELECT m FROM OAuth2MobileEntity m WHERE m.oauth2ParamsId IN (SELECT p.id FROM OAuth2ParamsEntity p WHERE p.tenantId = :tenantId)")
    Page<OAuth2MobileEntity> findByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

}
