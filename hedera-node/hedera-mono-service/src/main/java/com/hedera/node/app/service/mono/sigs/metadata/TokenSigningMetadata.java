/*
 * Copyright (C) 2020-2022 Hedera Hashgraph, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hedera.node.app.service.mono.sigs.metadata;

import com.hedera.node.app.service.mono.legacy.core.jproto.JKey;
import com.hedera.node.app.service.mono.state.submerkle.EntityId;
import java.util.Optional;

/** Represents metadata about the signing attributes of a Hedera token. */
public record TokenSigningMetadata(
        Optional<JKey> adminKey,
        Optional<JKey> kycKey,
        Optional<JKey> wipeKey,
        Optional<JKey> freezeKey,
        Optional<JKey> supplyKey,
        Optional<JKey> feeScheduleKey,
        Optional<JKey> pauseKey,
        boolean hasRoyaltyWithFallback,
        EntityId treasury) {}
