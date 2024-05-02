/*
 *  BNIndustry Inc., Software License, Version 1.0
 *
 *   Copyright (c) 2019. BNIndustry Inc.,
 *   All rights reserved.
 *
 *    DON'T COPY OR REDISTRIBUTE THIS SOURCE CODE WITHOUT PERMISSION.
 *    THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *    WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *    OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *    DISCLAIMED. IN NO EVENT SHALL <<BNIndustry Inc.>> OR ITS
 *    CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *    SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *    LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *    USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *    ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *    OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *    OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *    SUCH DAMAGE.
 *
 *    For more information on this product, please see www.bnids.com
 */
package com.bnids.gateway.repository;

import com.bnids.core.base.BaseJPARepository;
import com.bnids.gateway.entity.Settings;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * @author yannishin
 */
public interface SettingsRepository extends BaseJPARepository<Settings, Long> {

    // 입차제한로직정의 : 1000
    @Query(value = "select * from local_db.settings where parent_setting_id = 1000 and active = 1", nativeQuery=true)
    List<Settings> findCustomRestrictLogicList();

    @Query(value = "select * from local_db.settings where setting_id in (4001, 4002) and active = 1 order by setting_id", nativeQuery=true)
    List<Settings> findEntryExitBufferTime();

    @Query(value = "select * from local_db.settings where setting_id = 6000 and active = 1", nativeQuery=true)
    Optional<Settings> findExcludeInternalInOut();

    @Query(value = "select * from local_db.settings where setting_id = 7000 and active = 1", nativeQuery=true)
    Optional<Settings> findJSONSettingToForward();
}